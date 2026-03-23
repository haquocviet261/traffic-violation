package com.example.trafficvision.service;

import com.example.trafficvision.dto.AnalysisResponse;
import com.example.trafficvision.dto.DetectionResult;
import com.example.trafficvision.dto.LineDto;
import com.example.trafficvision.dto.RectDto;
import com.example.trafficvision.model.AnalysisResult;
import com.example.trafficvision.model.TrafficEvent;
import com.example.trafficvision.model.Video;
import com.example.trafficvision.opencv.StopLineDetector;
import com.example.trafficvision.opencv.TrafficLightDetector;
import com.example.trafficvision.repository.AnalysisResultRepository;
import com.example.trafficvision.repository.TrafficEventRepository;
import com.example.trafficvision.repository.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dịch vụ VideoProcessingService xử lý toàn bộ quy trình từ tải lên video,
 * phân tích video không đồng bộ bằng OpenCV và lưu trữ kết quả phân tích.
 */
@Service
public class VideoProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);
    // ExecutorService để xử lý video ở luồng nền, tránh làm treo ứng dụng
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final String UPLOAD_DIR = "uploads";
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private AnalysisResultRepository analysisResultRepository;
    @Autowired
    private TrafficEventRepository trafficEventRepository;
    @Autowired
    private VehicleDetectionService vehicleDetectionService;
    @Autowired
    private TrafficLightDetector trafficLightDetector;
    @Autowired
    private StopLineDetector stopLineDetector;
    @Autowired
    private ViolationDetectionService violationDetectionService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Tải video lên và lưu thông tin vào cơ sở dữ liệu.
     */
    public Video uploadVideo(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Tạo tên tệp duy nhất bằng UUID để tránh trùng lặp
        String filename = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath);

        Video video = new Video();
        video.setFilename(filename);
        video.setStatus("UPLOADED");
        video.setUploadTime(LocalDateTime.now());
        return videoRepository.save(video);
    }

    /**
     * Kích hoạt quy trình xử lý video không đồng bộ.
     */
    public void processVideoAsync(Long videoId) {
        executorService.submit(() -> {
            try {
                processVideo(videoId);
            } catch (Exception e) {
                logger.error("Error processing video {}: {}", videoId, e.getMessage(), e);
                videoRepository.findById(videoId).ifPresent(video -> {
                    video.setStatus("FAILED");
                    videoRepository.save(video);
                });
            }
        });
    }

    /**
     * Quy trình xử lý video chính sử dụng OpenCV.
     */
    private void processVideo(Long videoId) {
        Optional<com.example.trafficvision.model.Video> videoOptional = videoRepository.findById(videoId);
        if (videoOptional.isEmpty()) {
            logger.error("Video with ID {} not found.", videoId);
            return;
        }

        com.example.trafficvision.model.Video video = videoOptional.get();
        video.setStatus("PROCESSING");
        videoRepository.save(video);

        String videoPath = Paths.get(UPLOAD_DIR, video.getFilename()).toAbsolutePath().toString();
        VideoCapture cap = new VideoCapture(videoPath);

        if (!cap.isOpened()) {
            video.setStatus("FAILED");
            videoRepository.save(video);
            return;
        }

        double fps = cap.get(Videoio.CAP_PROP_FPS) > 0 ? cap.get(Videoio.CAP_PROP_FPS) : 30.0;
        int width = (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        String outputPath = Paths.get(UPLOAD_DIR, "processed_" + video.getFilename()).toAbsolutePath().toString();

        VideoWriter writer = new VideoWriter(outputPath, VideoWriter.fourcc('a', 'v', 'c', '1'), fps, new Size(width, height));
        if (!writer.isOpened()) {
            writer = new VideoWriter(outputPath, VideoWriter.fourcc('m', 'p', '4', 'v'), fps, new Size(width, height));
        }

        // Pool dùng chung cho các tác vụ trong frame
        ExecutorService frameExecutor = Executors.newFixedThreadPool(3);

        Mat frame = new Mat();
        int totalVehicles = 0;
        int totalViolations = 0;
        int frameCount = 0;

        // Biến lưu trữ trạng thái để dùng giữa các frame
        Double stableStopLineY = null;
        String currentLightState = "UNKNOWN";
        List<Rect> uniqueLights = new ArrayList<>();
        DetectionResult lastResult = null;

        try {
            while (cap.read(frame)) {
                if (frame.empty()) break;

                final Mat currentFrame = frame.clone();

                // 2. CHẠY SONG SONG CÁC TÁC VỤ PHÁT HIỆN
                // Tác vụ 1: Phát hiện phương tiện (luôn chạy)
                Future<List<Rect>> vehicleFuture = frameExecutor.submit(() ->
                        vehicleDetectionService.detectVehicles(currentFrame));

                // Tác vụ 2: Trạng thái đèn (luôn chạy)
                Future<String> lightFuture = frameExecutor.submit(() ->
                        trafficLightDetector.getTrafficLightState(currentFrame, videoId));

                // Tác vụ 3: Vạch dừng (Chỉ tính lại mỗi 20 frame để giảm tải CPU và tránh nhiễu)
                Future<Double> stopLineFuture = null;
                if (frameCount < 20 || frameCount % 20 == 0 || stableStopLineY == null) {
                    stopLineFuture = frameExecutor.submit(() ->
                            stopLineDetector.detectStopLineY(currentFrame, videoId));
                }

                // 3. THU THẬP KẾT QUẢ
                List<Rect> vehicleRects = vehicleFuture.get();
                currentLightState = lightFuture.get();

                if (stopLineFuture != null) {
                    Double detectedY = stopLineFuture.get();
                    if (detectedY != null) stableStopLineY = detectedY;
                }

                // 4. LOGIC PHÁT HIỆN VI PHẠM & CẬP NHẬT THỐNG KÊ
                if (stableStopLineY != null) {
                    totalViolations += violationDetectionService.detectViolations(
                            frame, vehicleRects, stableStopLineY, currentLightState, videoId, frameCount
                    );
                }
                totalVehicles += vehicleRects.size();

                // 5. CẬP NHẬT DỮ LIỆU ĐỊNH KỲ (Để lưu vào DB sau này)
                if (frameCount % 20 == 0) {
                    List<Rect> lightRects = trafficLightDetector.detectTrafficLights(frame);
                    mergeUniqueLights(lightRects, uniqueLights);

                    List<org.opencv.core.Point[]> stopLines = new ArrayList<>();
                    if (stableStopLineY != null) {
                        stopLines.add(new org.opencv.core.Point[]{
                                new org.opencv.core.Point(0, stableStopLineY),
                                new org.opencv.core.Point(width, stableStopLineY)
                        });
                    }
                    lastResult = new DetectionResult(lightRects, stopLines, vehicleRects);
                }

                // 6. GHI VIDEO & GIẢI PHÓNG BỘ NHỚ
                if (writer.isOpened()) writer.write(frame);
                currentFrame.release();
                frameCount++;
            }
        } catch (Exception e) {
            logger.error("Error processing video ID {}: {}", videoId, e.getMessage());
        } finally {
            // Dọn dẹp tài nguyên
            frameExecutor.shutdownNow();
            cap.release();
            if (writer.isOpened()) writer.release();
            frame.release();
            saveAnalysisResult(videoId, totalVehicles, totalViolations, frameCount, uniqueLights.size(), lastResult);

            video.setStatus("COMPLETED");
            videoRepository.save(video);
            violationDetectionService.clearHistory(videoId);
            stopLineDetector.clearHistory(videoId);
        }
    }

    /**
     * Hàm hỗ trợ tách biệt phần lưu DB cho sạch code
     */
    private void saveAnalysisResult(Long videoId, int totalVehicles, int totalViolations, int frames, int lightsCount, DetectionResult lastRes) {
        AnalysisResult result = new AnalysisResult();
        result.setVideoId(videoId);
        result.setVehicleCount(totalVehicles);
        result.setViolationCount(totalViolations);
        result.setTrafficDensity(frames > 0 ? (double) totalVehicles / frames : 0);
        result.setCreatedAt(LocalDateTime.now());
        result.setTrafficLightDetections(lightsCount);

        if (lastRes != null) {
            try {
                // Chuyển đổi sang JSON để lưu
                result.setSerializedTrafficLightRects(objectMapper.writeValueAsString(lastRes.getTrafficLightRects()));
                result.setSerializedStopLines(objectMapper.writeValueAsString(lastRes.getStopLines()));
            } catch (Exception e) {
                logger.error("Serialization error: {}", e.getMessage());
            }
        }
        analysisResultRepository.save(result);
    }

    /**
     * Lấy kết quả phân tích chi tiết của video.
     */
    public AnalysisResponse getAnalysisResults(Long id) {
        return analysisResultRepository.findByVideoId(id)
                .map(ar -> {
                    List<TrafficEvent> trafficEvents = trafficEventRepository.findByVideoId(id);
                    List<RectDto> trafficLightRects = new ArrayList<>();
                    List<LineDto> stopLines = new ArrayList<>();
                    try {
                        if (ar.getSerializedTrafficLightRects() != null) {
                            trafficLightRects = objectMapper.readValue(ar.getSerializedTrafficLightRects(), new com.fasterxml.jackson.core.type.TypeReference<List<RectDto>>() {
                            });
                        }
                        if (ar.getSerializedStopLines() != null) {
                            stopLines = objectMapper.readValue(ar.getSerializedStopLines(), new com.fasterxml.jackson.core.type.TypeReference<List<LineDto>>() {
                            });
                        }
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        logger.error("Error deserializing RectDto/LineDto: {}", e.getMessage());
                    }
                    return new AnalysisResponse(id, ar.getVehicleCount(), ar.getViolationCount(), ar.getTrafficDensity(), ar.getCreatedAt(), ar.getTrafficLightDetections(), ar.getStopLinesDetected(), trafficLightRects, stopLines, trafficEvents);
                })
                .orElse(null);
    }

    /**
     * Lấy trạng thái hiện tại của quy trình xử lý video.
     */
    public String getVideoStatus(Long id) {
        return videoRepository.findById(id).map(com.example.trafficvision.model.Video::getStatus).orElse(null);
    }

    /**
     * Lấy URL để truy cập video đã xử lý.
     */
    public String getProcessedVideoUrl(Long id) {
        Optional<com.example.trafficvision.model.Video> videoOptional = videoRepository.findById(id);
        if (videoOptional.isPresent() && "COMPLETED".equals(videoOptional.get().getStatus())) {
            return "/processed-videos/processed_" + videoOptional.get().getFilename();
        }
        return null;
    }

    /**
     * Lấy đường dẫn tệp video (ưu tiên video đã xử lý nếu có).
     */
    public Path getVideoFilePath(Long id) {
        return videoRepository.findById(id)
                .map(v -> {
                    Path processedPath = Paths.get(UPLOAD_DIR, "processed_" + v.getFilename());
                    if (Files.exists(processedPath)) {
                        return processedPath.toAbsolutePath();
                    }
                    return Paths.get(UPLOAD_DIR, v.getFilename()).toAbsolutePath();
                })
                .orElse(null);
    }

    /**
     * Hợp nhất các đèn giao thông phát hiện được, loại bỏ các vùng trùng lặp gần nhau.
     */
    private void mergeUniqueLights(List<Rect> newLights, List<Rect> uniqueLights) {
        for (Rect newLight : newLights) {
            boolean found = false;
            for (Rect existing : uniqueLights) {
                double distance = Math.sqrt(Math.pow(newLight.x - existing.x, 2) + Math.pow(newLight.y - existing.y, 2));
                if (distance < 50) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                uniqueLights.add(newLight);
            }
        }
    }
}
