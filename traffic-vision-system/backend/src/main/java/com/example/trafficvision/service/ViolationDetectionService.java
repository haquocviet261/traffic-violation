package com.example.trafficvision.service;

import com.example.trafficvision.model.TrafficEvent;
import com.example.trafficvision.repository.TrafficEventRepository;
import com.example.trafficvision.opencv.DebugDrawingUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ViolationDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(ViolationDetectionService.class);

    @Autowired
    private TrafficEventRepository trafficEventRepository;

    private final String VIOLATIONS_DIR = "violations";

    // Tracks vehicle bottom-center points across frames for motion analysis
    private final Map<Long, List<Point>> previousFrameBottomPoints = new ConcurrentHashMap<>();

    /**
     * PART 4: RED LIGHT VIOLATION DETECTION
     * Detects violations based on bottom of vehicle crossing the stop line during a RED light.
     */
    public int detectViolations(Mat frame, List<Rect> vehicles, Double stopLineY, String trafficLightState, Long videoId, int frameNumber) {
        if (stopLineY == null || vehicles.isEmpty()) {
            updateBottomPoints(videoId, vehicles);
            return 0;
        }

        // Visualize stop line
        drawStopLine(frame, stopLineY);

        List<Point> prevBottomPoints = previousFrameBottomPoints.getOrDefault(videoId, new ArrayList<>());
        int violationsInFrame = 0;
        
        // Tolerance zone to avoid false violations (vehicle stops slightly over the line)
        double violationZone = stopLineY + 10;

        for (Rect vehicle : vehicles) {
            double currentBottomY = (double) vehicle.y + vehicle.height;
            Point currentBottomPoint = new Point(vehicle.x + vehicle.width / 2.0, currentBottomY);
            Point bestMatch = findBestMatch(currentBottomPoint, prevBottomPoints);

            if ("RED".equals(trafficLightState) && bestMatch != null) {
                // Logic: Bottom was above/at stop line, and now is beyond the violation zone
                if (bestMatch.y <= stopLineY && currentBottomY > violationZone) {
                    logger.info("Violation Detected: Vehicle bottom crossed stop line. Frame: {}, Y: {} -> {}", 
                            frameNumber, (int)bestMatch.y, (int)currentBottomY);
                    
                    processViolation(frame, vehicle, videoId, frameNumber);
                    violationsInFrame++;
                }
            }
        }

        updateBottomPoints(videoId, vehicles);
        return violationsInFrame;
    }

    private void drawStopLine(Mat frame, Double stopLineY) {
        Imgproc.line(frame, new Point(0, stopLineY), new Point(frame.cols(), stopLineY), new Scalar(0, 255, 0), 2);
    }

    private void processViolation(Mat frame, Rect vehicle, Long videoId, int frameNumber) {
        // Visual feedback on frame
        DebugDrawingUtils.drawDetection(frame, vehicle, "RED LIGHT VIOLATION", new Scalar(0, 0, 255));
        
        // Record to DB and save image
        recordViolation(frame, videoId, frameNumber, vehicle);
    }

    private Point findBestMatch(Point current, List<Point> previousList) {
        Point bestMatch = null;
        double minDistance = 100; // Search radius for matching vehicle from previous frame
        for (Point prev : previousList) {
            double dist = Math.sqrt(Math.pow(current.x - prev.x, 2) + Math.pow(current.y - prev.y, 2));
            if (dist < minDistance) {
                minDistance = dist;
                bestMatch = prev;
            }
        }
        return bestMatch;
    }

    private void updateBottomPoints(Long videoId, List<Rect> vehicles) {
        List<Point> bottomPoints = new ArrayList<>();
        for (Rect v : vehicles) {
            bottomPoints.add(new Point(v.x + v.width / 2.0, (double) v.y + v.height));
        }
        previousFrameBottomPoints.put(videoId, bottomPoints);
    }

    private void recordViolation(Mat frame, Long videoId, int frameNumber, Rect bbox) {
        try {
            Files.createDirectories(Paths.get(VIOLATIONS_DIR));
        } catch (IOException e) {
            logger.error("Could not create violations directory", e);
        }

        // PART 5: Avoid memory leaks with Mat objects
        Mat cropped = null;
        try {
            // Ensure bbox is within frame boundaries to avoid crash
            Rect safeBbox = getSafeBbox(frame, bbox);
            cropped = new Mat(frame, safeBbox);
            
            String filename = "violation_" + UUID.randomUUID() + ".jpg";
            String filePath = VIOLATIONS_DIR + "/" + filename;
            Imgcodecs.imwrite(filePath, cropped);

            TrafficEvent event = new TrafficEvent();
            event.setVideoId(videoId);
            event.setEventType("RED_LIGHT_VIOLATION");
            event.setFrameNumber(frameNumber);
            event.setTimestamp(LocalDateTime.now());
            event.setBboxX(safeBbox.x);
            event.setBboxY(safeBbox.y);
            event.setBboxWidth(safeBbox.width);
            event.setBboxHeight(safeBbox.height);
            event.setCentroidX(safeBbox.x + safeBbox.width / 2.0);
            event.setCentroidY(safeBbox.y + safeBbox.height / 2.0);
            event.setImagePath("/violations/" + filename);
            event.setDetails("Vehicle crossed stop line on red light.");
            trafficEventRepository.save(event);
        } finally {
            if (cropped != null) cropped.release();
        }
    }

    private Rect getSafeBbox(Mat frame, Rect bbox) {
        int x = Math.max(0, bbox.x);
        int y = Math.max(0, bbox.y);
        int width = Math.min(bbox.width, frame.cols() - x);
        int height = Math.min(bbox.height, frame.rows() - y);
        return new Rect(x, y, width, height);
    }
    
    public void clearHistory(Long videoId) {
        previousFrameBottomPoints.remove(videoId);
    }
}
