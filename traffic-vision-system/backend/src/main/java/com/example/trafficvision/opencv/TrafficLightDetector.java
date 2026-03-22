package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrafficLightDetector {

    private static final Logger logger = LoggerFactory.getLogger(TrafficLightDetector.class);

    // RED
    private static final Scalar LOWER_RED1 = new Scalar(0, 50, 50);
    private static final Scalar UPPER_RED1 = new Scalar(15, 255, 255);
    private static final Scalar LOWER_RED2 = new Scalar(160, 50, 50);
    private static final Scalar UPPER_RED2 = new Scalar(179, 255, 255);

    // GREEN
    private static final Scalar LOWER_GREEN = new Scalar(30, 40, 40);
    private static final Scalar UPPER_GREEN = new Scalar(95, 255, 255);

    // YELLOW
    private static final Scalar LOWER_YELLOW = new Scalar(10, 40, 40);
    private static final Scalar UPPER_YELLOW = new Scalar(45, 255, 255);

    private static final double MIN_TRAFFIC_LIGHT_AREA = 30;
    private static final int STATE_BUFFER_SIZE = 10;
    // Per-video state history
    private final Map<Long, LinkedList<String>> stateHistoryMap = new ConcurrentHashMap<>();

    /**
     * Determines the traffic light state with temporal smoothing and flashing red handling.
     */
    public String getTrafficLightState(Mat frame, Long videoId) {

        if (frame.empty()) return "UNKNOWN";

        Rect roiRect = new Rect(0, 0, frame.cols(), (int) (frame.rows() * 0.5));
        Mat roi = frame.submat(roiRect);

        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(roi, hsvFrame, Imgproc.COLOR_BGR2HSV);

        DetectionInfo red = getMaxContourInfo(hsvFrame, LOWER_RED1, UPPER_RED1, LOWER_RED2, UPPER_RED2);
        DetectionInfo green = getMaxContourInfo(hsvFrame, LOWER_GREEN, UPPER_GREEN, null, null);
        DetectionInfo yellow = getMaxContourInfo(hsvFrame, LOWER_YELLOW, UPPER_YELLOW, null, null);

        String currentState = "UNKNOWN";
        DetectionInfo best = red;

        if (red.area >= MIN_TRAFFIC_LIGHT_AREA) {
            currentState = "RED";
        }

        if (green.area > best.area && green.area >= MIN_TRAFFIC_LIGHT_AREA) {
            best = green;
            currentState = "GREEN";
        }

        if (yellow.area > best.area && yellow.area >= MIN_TRAFFIC_LIGHT_AREA) {
            best = yellow;
            currentState = "YELLOW";
        }

        updateStateHistory(videoId, currentState);

        String finalState = getDominantState(videoId);

        if (isFlashingRed(videoId)) {
            finalState = "RED";
        }
        Imgproc.rectangle(frame, best.box, new Scalar(0, 0, 255), 2);
        Imgproc.putText(frame, currentState, new Point(best.box.x, best.box.y - 5),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 2);

        hsvFrame.release();
        roi.release();

        return finalState;
    }

    private void updateStateHistory(Long videoId, String state) {

        LinkedList<String> history = stateHistoryMap.computeIfAbsent(videoId, k -> new LinkedList<>());

        history.add(state);

        if (history.size() > STATE_BUFFER_SIZE) {
            history.removeFirst();
        }
    }

    private String getDominantState(Long videoId) {

        LinkedList<String> history = stateHistoryMap.get(videoId);

        if (history == null || history.isEmpty()) return "UNKNOWN";

        Map<String, Integer> counts = new HashMap<>();

        for (String s : history) {
            counts.put(s, counts.getOrDefault(s, 0) + 1);
        }

        return Collections.max(counts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private boolean isFlashingRed(Long videoId) {

        LinkedList<String> history = stateHistoryMap.get(videoId);
        if (history == null) return false;

        long redCount = history.stream().filter(s -> s.equals("RED")).count();
        long unknownCount = history.stream().filter(s -> s.equals("UNKNOWN")).count();

        boolean noOtherColors = history.stream()
                .noneMatch(s -> s.equals("GREEN") || s.equals("YELLOW"));

        return redCount > 0 && unknownCount > 0 && noOtherColors;
    }

    private DetectionInfo getMaxContourInfo(Mat hsv, Scalar l1, Scalar u1, Scalar l2, Scalar u2) {

        Mat mask = new Mat();
        Core.inRange(hsv, l1, u1, mask);

        if (l2 != null && u2 != null) {
            Mat mask2 = new Mat();
            Core.inRange(hsv, l2, u2, mask2);
            Core.bitwise_or(mask, mask2, mask);
            mask2.release();
        }

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(mask, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        Rect bestBox = new Rect();
        Point bestCentroid = new Point();

        for (MatOfPoint contour : contours) {

            double area = Imgproc.contourArea(contour);

            if (area > maxArea) {

                maxArea = area;
                bestBox = Imgproc.boundingRect(contour);

                Moments m = Imgproc.moments(contour);
                if (m.m00 != 0) {
                    bestCentroid = new Point(m.m10 / m.m00, m.m01 / m.m00);
                }
            }

            contour.release();
        }

        mask.release();
        hierarchy.release();

        return new DetectionInfo(maxArea, bestBox, bestCentroid);
    }

    public void clearHistory(Long videoId) {
        stateHistoryMap.remove(videoId);
    }

    public List<Rect> detectTrafficLights(Mat frame) {
        return new ArrayList<>();
    }

    private static class DetectionInfo {

        double area;
        Rect box;
        Point centroid;

        DetectionInfo(double area, Rect box, Point centroid) {
            this.area = area;
            this.box = box;
            this.centroid = centroid;
        }
    }
}