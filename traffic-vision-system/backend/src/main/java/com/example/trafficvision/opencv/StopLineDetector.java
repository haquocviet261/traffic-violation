package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StopLineDetector {
    private static final Logger logger = LoggerFactory.getLogger(StopLineDetector.class);

    // Hough parameters
    private static final double RHO = 1;
    private static final double THETA = Math.PI / 180;
    private static final int THRESHOLD = 50;
    private static final double MIN_LINE_LENGTH = 100;
    private static final double MAX_LINE_GAP = 20;

    public Double detectStopLineY(Mat frame) {
        if (frame.empty()) return null;

        // ROI: Middle-lower road region (50% to 80% of frame height)
        // Avoiding the very bottom to exclude artifacts or car dashboards
        int startRow = (int) (frame.rows() * 0.5);
        int endRow = (int) (frame.rows() * 0.8);
        Rect roiRect = new Rect(0, startRow, frame.cols(), endRow - startRow);
        Mat roi = frame.submat(roiRect);

        // 1. Grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);

        // 2. Gaussian Blur
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // 3. Canny Edge Detection
        Mat edges = new Mat();
        Imgproc.Canny(blurred, edges, 50, 150);

        // 4. Hough Line Transform
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, RHO, THETA, THRESHOLD, MIN_LINE_LENGTH, MAX_LINE_GAP);

        Double closestY = null;
        double maxY = -1;

        if (lines.cols() > 0) {
            for (int i = 0; i < lines.rows(); i++) {
                double[] l = lines.get(i, 0);
                double y1 = l[1] + startRow;
                double y2 = l[3] + startRow;
                
                // Filter for horizontal-ish lines (angle < 10 degrees)
                double angle = Math.abs(Math.atan2(y2 - y1, l[2] - l[0]));
                if (angle < 0.174 || angle > Math.PI - 0.174) {
                    // Preference: max Y coordinate (closest to camera)
                    double currentY = (y1 + y2) / 2.0;
                    if (currentY > maxY) {
                        maxY = currentY;
                        closestY = currentY;
                    }
                }
            }
        }

        gray.release();
        blurred.release();
        edges.release();
        lines.release();
        roi.release();

        if (closestY != null) {
            logger.info("StopLineY={}", closestY.intValue());
        }
        return closestY;
    }
}
