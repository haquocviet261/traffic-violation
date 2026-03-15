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

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoProcessingService videoProcessingService;
    @Autowired
    private DashboardService dashboardService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(new UploadResponse("No file uploaded", null), HttpStatus.BAD_REQUEST);
        }
        try {
            Video video = videoProcessingService.uploadVideo(file);
            videoProcessingService.processVideoAsync(video.getId());
            return new ResponseEntity<>(new UploadResponse("Video uploaded successfully", video.getId()), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Failed to upload video", e);
            return new ResponseEntity<>(new UploadResponse("Failed to upload video: " + e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getVideoStatus(@PathVariable Long id) {
        String status = videoProcessingService.getVideoStatus(id);
        if (status != null) {
            return ResponseEntity.ok(status);
        }
        return new ResponseEntity<>("Video not found or status not available", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<AnalysisResponse> getAnalysisResults(@PathVariable Long id) {
        AnalysisResponse analysisResponse = videoProcessingService.getAnalysisResults(id);
        if (analysisResponse != null) {
            return ResponseEntity.ok(analysisResponse);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

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

    private ResourceRegion resourceRegion(Resource video, HttpHeaders headers) throws IOException {
        long contentLength = video.contentLength();
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
        
        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(5 * 1024 * 1024L, end - start + 1); // Increased to 5MB chunks
            logger.debug("Serving range: {}-{}, length: {}", start, start + rangeLength - 1, rangeLength);
            return new ResourceRegion(video, start, rangeLength);
        } else {
            long rangeLength = Math.min(5 * 1024 * 1024L, contentLength); // Serve first 5MB
            logger.debug("No range requested, serving first {} bytes", rangeLength);
            return new ResourceRegion(video, 0, rangeLength);
        }
    }

    @GetMapping("/{id}/processed-video")
    public ResponseEntity<String> getProcessedVideo(@PathVariable Long id) {
        String processedVideoUrl = videoProcessingService.getProcessedVideoUrl(id);
        if (processedVideoUrl != null && !processedVideoUrl.isEmpty()) {
            return ResponseEntity.ok(processedVideoUrl);
        }
        return new ResponseEntity<>("Processed video not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }
}
