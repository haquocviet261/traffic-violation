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

@Service
public class VideoProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);

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

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final String UPLOAD_DIR = "uploads";

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
        String filename = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath);

        Video video = new Video();
        video.setFilename(filename);
        video.setStatus("UPLOADED");
        video.setUploadTime(LocalDateTime.now());
        return videoRepository.save(video);
    }

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

    private void processVideo(Long videoId) {
        Optional<com.example.trafficvision.model.Video> videoOptional = videoRepository.findById(videoId);
        if (videoOptional.isEmpty()) {
            logger.error("Video with ID {} not found for processing.", videoId);
            return;
        }

        com.example.trafficvision.model.Video video = videoOptional.get();
        video.setStatus("PROCESSING");
        videoRepository.save(video);

        logger.info("Starting OpenCV processing for video: {}", video.getFilename());

        String videoFilePath = Paths.get(UPLOAD_DIR, video.getFilename()).toAbsolutePath().toString();
        VideoCapture cap = new VideoCapture(videoFilePath);

        if (!cap.isOpened()) {
            logger.error("Error: Could not open video file: {}", videoFilePath);
            video.setStatus("FAILED");
            videoRepository.save(video);
            return;
        }

        // --- VideoWriter Initialization ---
        double fps = cap.get(Videoio.CAP_PROP_FPS);
        if (fps <= 0) fps = 30.0;
        int width = (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        String processedFilename = "processed_" + video.getFilename();
        String outputPath = Paths.get(UPLOAD_DIR, processedFilename).toAbsolutePath().toString();
        
        // H264/AVC1 is best for browsers. FALLBACK to MP4V if needed.
        int fourcc = VideoWriter.fourcc('a', 'v', 'c', '1'); 
        VideoWriter writer = new VideoWriter(outputPath, fourcc, fps, new Size(width, height));

        if (!writer.isOpened()) {
            logger.warn("Could not open VideoWriter with H264 (avc1), falling back to MP4V");
            fourcc = VideoWriter.fourcc('m', 'p', '4', 'v');
            writer = new VideoWriter(outputPath, fourcc, fps, new Size(width, height));
        }

        Mat frame = new Mat();
        int totalVehicleDetections = 0;
        int totalStopLinesDetected = 0;
        int totalViolationCount = 0;
        int frameCount = 0;
        DetectionResult lastDetectionResult = null;
        String currentTrafficLightState = "UNKNOWN";
        List<Rect> uniqueTrafficLights = new ArrayList<>();

        try {
            while (cap.read(frame)) {
                if (frame.empty()) {
                    break;
                }

                // 1. Detect stop line
                Double stopLineY = stopLineDetector.detectStopLineY(frame);
                if (stopLineY != null) totalStopLinesDetected++;

                // 2. Detect traffic light state
                if (frameCount % 30 == 0) {
                    currentTrafficLightState = trafficLightDetector.getTrafficLightState(frame);
                    List<Rect> trafficLightRects = trafficLightDetector.detectTrafficLights(frame);
                    mergeUniqueLights(trafficLightRects, uniqueTrafficLights);
                    
                    List<org.opencv.core.Point[]> stopLinePoints = new ArrayList<>();
                    if (stopLineY != null) {
                        stopLinePoints.add(new org.opencv.core.Point[]{new org.opencv.core.Point(0, stopLineY), new org.opencv.core.Point(frame.cols(), stopLineY)});
                    }
                    lastDetectionResult = new DetectionResult(trafficLightRects, stopLinePoints, vehicleDetectionService.detectVehicles(frame));
                }

                // 3. Detect vehicles
                List<Rect> vehicleRects = vehicleDetectionService.detectVehicles(frame);
                totalVehicleDetections += vehicleRects.size();

                // 4. Red light violation detection (Draws overlays on 'frame')
                totalViolationCount += violationDetectionService.detectViolations(frame, vehicleRects, stopLineY, currentTrafficLightState, videoId, frameCount);

                // --- Write processed frame to output video ---
                if (writer.isOpened()) {
                    writer.write(frame);
                }

                frameCount++;
            }
        } finally {
            cap.release();
            if (writer != null && writer.isOpened()) {
                writer.release();
            }
            frame.release();
        }

        double trafficDensity = frameCount > 0 ? (double) totalVehicleDetections / frameCount : 0.0;

        AnalysisResult result = new AnalysisResult();
        result.setVideoId(videoId);
        result.setVehicleCount(totalVehicleDetections);
        result.setViolationCount(totalViolationCount);
        result.setTrafficDensity(trafficDensity);
        result.setCreatedAt(LocalDateTime.now());
        result.setTrafficLightDetections(uniqueTrafficLights.size());
        result.setStopLinesDetected(totalStopLinesDetected);

        if (lastDetectionResult != null) {
            try {
                List<RectDto> trafficLightRectDtos = lastDetectionResult.getTrafficLightRects().stream()
                        .map(rect -> new RectDto(rect.x, rect.y, rect.width, rect.height))
                        .collect(java.util.stream.Collectors.toList());
                result.setSerializedTrafficLightRects(objectMapper.writeValueAsString(trafficLightRectDtos));

                List<LineDto> stopLineDtos = lastDetectionResult.getStopLines().stream()
                        .map(line -> new LineDto(line[0].x, line[0].y, line[1].x, line[1].y))
                        .collect(java.util.stream.Collectors.toList());
                result.setSerializedStopLines(objectMapper.writeValueAsString(stopLineDtos));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                logger.error("Error serializing RectDto/LineDto: {}", e.getMessage());
            }
        }
        analysisResultRepository.save(result);

        video.setStatus("COMPLETED");
        videoRepository.save(video);
        violationDetectionService.clearHistory(videoId);

        logger.info("Finished OpenCV processing for video: {}", video.getFilename());
        logger.info("Total violations detected: {}", totalViolationCount);
    }

    public String getVideoStatus(Long id) {
        return videoRepository.findById(id).map(com.example.trafficvision.model.Video::getStatus).orElse(null);
    }

    public AnalysisResponse getAnalysisResults(Long id) {
        return analysisResultRepository.findByVideoId(id)
                .map(ar -> {
                    List<TrafficEvent> trafficEvents = trafficEventRepository.findByVideoId(id);
                    List<RectDto> trafficLightRects = new ArrayList<>();
                    List<LineDto> stopLines = new ArrayList<>();
                    try {
                        if (ar.getSerializedTrafficLightRects() != null) {
                            trafficLightRects = objectMapper.readValue(ar.getSerializedTrafficLightRects(), new com.fasterxml.jackson.core.type.TypeReference<List<RectDto>>() {});
                        }
                        if (ar.getSerializedStopLines() != null) {
                            stopLines = objectMapper.readValue(ar.getSerializedStopLines(), new com.fasterxml.jackson.core.type.TypeReference<List<LineDto>>() {});
                        }
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        logger.error("Error deserializing RectDto/LineDto: {}", e.getMessage());
                    }
                    return new AnalysisResponse(id, ar.getVehicleCount(), ar.getViolationCount(), ar.getTrafficDensity(), ar.getCreatedAt(), ar.getTrafficLightDetections(), ar.getStopLinesDetected(), trafficLightRects, stopLines, trafficEvents);
                })
                .orElse(null);
    }

    public String getProcessedVideoUrl(Long id) {
        Optional<com.example.trafficvision.model.Video> videoOptional = videoRepository.findById(id);
        if (videoOptional.isPresent() && "COMPLETED".equals(videoOptional.get().getStatus())) {
            return "/processed-videos/processed_" + videoOptional.get().getFilename();
        }
        return null;
    }

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
