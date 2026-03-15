package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TrafficLightDetector {
    private static final Logger logger = LoggerFactory.getLogger(TrafficLightDetector.class);

    private static final Scalar LOWER_RED1 = new Scalar(0, 100, 100);
    private static final Scalar UPPER_RED1 = new Scalar(10, 255, 255);
    private static final Scalar LOWER_RED2 = new Scalar(160, 100, 100);
    private static final Scalar UPPER_RED2 = new Scalar(179, 255, 255);
    private static final Scalar LOWER_GREEN = new Scalar(40, 100, 100);
    private static final Scalar UPPER_GREEN = new Scalar(80, 255, 255);
    private static final Scalar LOWER_YELLOW = new Scalar(20, 100, 100);
    private static final Scalar UPPER_YELLOW = new Scalar(30, 255, 255);

    private static final double MIN_TRAFFIC_LIGHT_AREA = 50;

    public String getTrafficLightState(Mat frame) {
        if (frame.empty()) return "UNKNOWN";

        // ROI: Top half and sides (where traffic lights usually are)
        // For simplicity, let's use the top 60% of the frame
        Rect roiRect = new Rect(0, 0, frame.cols(), (int) (frame.rows() * 0.6));
        Mat roi = frame.submat(roiRect);
        
        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(roi, hsvFrame, Imgproc.COLOR_BGR2HSV);

        DetectionInfo red = getMaxContourInfo(hsvFrame, LOWER_RED1, UPPER_RED1, LOWER_RED2, UPPER_RED2);
        DetectionInfo green = getMaxContourInfo(hsvFrame, LOWER_GREEN, UPPER_GREEN, null, null);
        DetectionInfo yellow = getMaxContourInfo(hsvFrame, LOWER_YELLOW, UPPER_YELLOW, null, null);

        hsvFrame.release();
        roi.release();

        DetectionInfo best = red;
        String state = "RED";
        if (green.area > best.area) { best = green; state = "GREEN"; }
        if (yellow.area > best.area) { best = yellow; state = "YELLOW"; }

        if (best.area < MIN_TRAFFIC_LIGHT_AREA) {
            logger.debug("No traffic light detected (max area: {})", best.area);
            return "UNKNOWN";
        }

        // Adjust coordinates from ROI to full frame
        Rect fullBox = new Rect(best.box.x + roiRect.x, best.box.y + roiRect.y, best.box.width, best.box.height);
        
        if (state.equals("RED")) {
            DebugDrawingUtils.saveDebugImage(frame, "traffic_light_red");
        }

        logger.info("LightState={}", state);
        return state;
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
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        Rect bestBox = new Rect();
        Point bestCentroid = new Point();

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                bestBox = Imgproc.boundingRect(contour);
                Moments m = Imgproc.moments(contour);
                bestCentroid = new Point(m.m10 / m.m00, m.m01 / m.m00);
            }
            contour.release();
        }
        mask.release();
        hierarchy.release();
        return new DetectionInfo(maxArea, bestBox, bestCentroid);
    }

    private static class DetectionInfo {
        double area;
        Rect box;
        Point centroid;
        DetectionInfo(double area, Rect box, Point centroid) {
            this.area = area; this.box = box; this.centroid = centroid;
        }
    }

    public List<Rect> detectTrafficLights(Mat frame) {
        // Keeping this for backward compatibility if needed, but getTrafficLightState is preferred for debug
        return new ArrayList<>();
    }
}
