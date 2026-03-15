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

    // Simple storage for previous frame's centroids per video
    private final Map<Long, List<Point>> previousFrameCentroids = new ConcurrentHashMap<>();

    public int detectViolations(Mat frame, List<Rect> vehicles, Double stopLineY, String trafficLightState, Long videoId, int frameNumber) {
        if (stopLineY == null || vehicles.isEmpty()) {
            updateCentroids(videoId, vehicles);
            return 0;
        }

        // --- VISUALIZATION: STOP LINE (GREEN) ---
        Imgproc.line(frame, new Point(0, stopLineY), new Point(frame.cols(), stopLineY), new Scalar(0, 255, 0), 2);

        List<Point> prevCentroids = previousFrameCentroids.getOrDefault(videoId, new ArrayList<>());
        int violationsInFrame = 0;

        for (Rect vehicle : vehicles) {
            Point currentCentroid = new Point(vehicle.x + vehicle.width / 2.0, vehicle.y + vehicle.height / 2.0);
            
            // Find best match in previous frame
            Point bestMatch = findBestMatch(currentCentroid, prevCentroids);

            if (bestMatch != null) {
                // VIOLATION RULE: RED Light AND Crossing (prevY <= stopLineY AND currY > stopLineY)
                if ("RED".equals(trafficLightState) && bestMatch.y <= stopLineY && currentCentroid.y > stopLineY) {
                    logger.info("Violation Detected: VehicleY={}, StopLineY={}", (int)currentCentroid.y, stopLineY.intValue());
                    
                    // --- VISUALIZATION: RED BBOX AND LABEL ---
                    DebugDrawingUtils.drawDetection(frame, vehicle, "RED LIGHT VIOLATION", new Scalar(0, 0, 255));

                    recordViolation(frame, videoId, frameNumber, vehicle);
                    violationsInFrame++;
                    
                    // Save violation frame for debug
                    DebugDrawingUtils.saveDebugImage(frame, "violation_frame");
                }
            }
        }

        updateCentroids(videoId, vehicles);
        return violationsInFrame;
    }

    private Point findBestMatch(Point current, List<Point> previousList) {
        Point bestMatch = null;
        double minDistance = 50; // Max pixels a vehicle can move between frames
        for (Point prev : previousList) {
            double dist = Math.sqrt(Math.pow(current.x - prev.x, 2) + Math.pow(current.y - prev.y, 2));
            if (dist < minDistance) {
                minDistance = dist;
                bestMatch = prev;
            }
        }
        return bestMatch;
    }

    private void updateCentroids(Long videoId, List<Rect> vehicles) {
        List<Point> centroids = new ArrayList<>();
        for (Rect v : vehicles) {
            centroids.add(new Point(v.x + v.width / 2.0, v.y + v.height / 2.0));
        }
        previousFrameCentroids.put(videoId, centroids);
    }

    private void recordViolation(Mat frame, Long videoId, int frameNumber, Rect bbox) {
        try {
            Files.createDirectories(Paths.get(VIOLATIONS_DIR));
        } catch (IOException e) {
            logger.error("Could not create violations directory", e);
        }

        Mat cropped = new Mat(frame, bbox);
        String filename = "violation_" + UUID.randomUUID().toString() + ".jpg";
        String filePath = VIOLATIONS_DIR + "/" + filename;
        Imgcodecs.imwrite(filePath, cropped);
        cropped.release();

        TrafficEvent event = new TrafficEvent();
        event.setVideoId(videoId);
        event.setEventType("RED_LIGHT_VIOLATION");
        event.setFrameNumber(frameNumber);
        event.setTimestamp(LocalDateTime.now());
        event.setBboxX(bbox.x);
        event.setBboxY(bbox.y);
        event.setBboxWidth(bbox.width);
        event.setBboxHeight(bbox.height);
        event.setCentroidX(bbox.x + bbox.width / 2.0);
        event.setCentroidY(bbox.y + bbox.height / 2.0);
        event.setImagePath("/violations/" + filename);
        event.setDetails("Vehicle crossed stop line on red light.");
        trafficEventRepository.save(event);
    }
    
    public void clearHistory(Long videoId) {
        previousFrameCentroids.remove(videoId);
    }
}
