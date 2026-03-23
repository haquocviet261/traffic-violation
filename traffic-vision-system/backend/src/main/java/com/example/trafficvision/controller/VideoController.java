package com.example.trafficvision.controller;

import com.example.trafficvision.dto.AnalysisResponse;
import com.example.trafficvision.dto.DashboardStatsResponse;
import com.example.trafficvision.dto.UploadResponse;
import com.example.trafficvision.model.Video;
import com.example.trafficvision.service.DashboardService;
import com.example.trafficvision.service.VideoProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * VideoController cung cấp các API RESTful để quản lý việc tải video,
 * theo dõi trạng thái xử lý, xem kết quả phân tích và phát video trực tuyến (streaming).
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoProcessingService videoProcessingService;
    @Autowired
    private DashboardService dashboardService;

    /**
     * Tải video lên hệ thống và bắt đầu quy trình xử lý không đồng bộ.
     *
     * @param file Tệp video được tải lên từ client
     * @return ID của video để client theo dõi
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(new UploadResponse("No file uploaded", null), HttpStatus.BAD_REQUEST);
        }
        try {
            // Lưu video vào thư mục uploads
            Video video = videoProcessingService.uploadVideo(file);
            // Kích hoạt xử lý video bằng OpenCV ở luồng nền (Async)
            videoProcessingService.processVideoAsync(video.getId());
            return new ResponseEntity<>(new UploadResponse("Video uploaded successfully", video.getId()), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Failed to upload video", e);
            return new ResponseEntity<>(new UploadResponse("Failed to upload video: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Kiểm tra trạng thái xử lý của video (UPLOADED, PROCESSING, COMPLETED, FAILED).
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<String> getVideoStatus(@PathVariable Long id) {
        String status = videoProcessingService.getVideoStatus(id);
        if (status != null) {
            return ResponseEntity.ok(status);
        }
        return new ResponseEntity<>("Video not found or status not available", HttpStatus.NOT_FOUND);
    }

    /**
     * Lấy kết quả phân tích chi tiết của video (số xe, số lỗi vi phạm, mật độ...).
     */
    @GetMapping("/{id}/analysis")
    public ResponseEntity<AnalysisResponse> getAnalysisResults(@PathVariable Long id) {
        AnalysisResponse analysisResponse = videoProcessingService.getAnalysisResults(id);
        if (analysisResponse != null) {
            return ResponseEntity.ok(analysisResponse);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * API hỗ trợ phát video trực tuyến (streaming) theo từng phần (Range requests).
     * Cho phép trình duyệt xem video mượt mà và tua nhanh mà không cần tải toàn bộ tệp.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceRegion> streamVideo(@PathVariable Long id, @RequestHeader HttpHeaders headers) throws IOException {
        Path videoPath = videoProcessingService.getVideoFilePath(id);
        if (videoPath == null || !videoPath.toFile().exists()) {
            logger.error("Video file not found for streaming: {}", videoPath);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource video = new FileSystemResource(videoPath);
        long length = video.contentLength();
        logger.info("Streaming video: {}, size: {} bytes", videoPath, length);

        ResourceRegion region = resourceRegion(video, headers);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }

    /**
     * Chia nhỏ tệp video thành các vùng dữ liệu (ResourceRegion) để phục vụ streaming.
     */
    private ResourceRegion resourceRegion(Resource video, HttpHeaders headers) throws IOException {
        long contentLength = video.contentLength();
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
        
        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            // Phục vụ theo từng khối 5MB
            long rangeLength = Math.min(5 * 1024 * 1024L, end - start + 1);
            logger.debug("Serving range: {}-{}, length: {}", start, start + rangeLength - 1, rangeLength);
            return new ResourceRegion(video, start, rangeLength);
        } else {
            long rangeLength = Math.min(5 * 1024 * 1024L, contentLength);
            logger.debug("No range requested, serving first {} bytes", rangeLength);
            return new ResourceRegion(video, 0, rangeLength);
        }
    }

    /**
     * Lấy URL để truy cập tệp video đã xử lý (có vẽ các khung nhận diện).
     */
    @GetMapping("/{id}/processed-video")
    public ResponseEntity<String> getProcessedVideo(@PathVariable Long id) {
        String processedVideoUrl = videoProcessingService.getProcessedVideoUrl(id);
        if (processedVideoUrl != null && !processedVideoUrl.isEmpty()) {
            return ResponseEntity.ok(processedVideoUrl);
        }
        return new ResponseEntity<>("Processed video not found", HttpStatus.NOT_FOUND);
    }

    /**
     * Lấy dữ liệu thống kê tổng quát cho dashboard.
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }
}
