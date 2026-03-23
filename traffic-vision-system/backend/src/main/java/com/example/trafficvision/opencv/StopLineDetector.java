package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StopLineDetector {

    private final Map<Long, Double> lastStableYMap = new ConcurrentHashMap<>();

    public Double detectStopLineY(Mat frame, Long videoId) {
        if (frame.empty()) return lastStableYMap.get(videoId);

        int width = frame.cols();
        int height = frame.rows();

        // ROI
        int startRow = (int) (height * 0.4);
        int endRow = (int) (height * 0.7);
        Mat roi = frame.submat(new Rect(0, startRow, width, endRow - startRow));

        // Grayscale + threshold + morphology + blur
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.GaussianBlur(binary, binary, new Size(5, 5), 0);

        // Canny + Hough
        Mat edges = new Mat();
        Imgproc.Canny(binary, edges, 40, 120);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 10, width * 0.2, 30);

        Double houghY = null;
        if (!lines.empty()) {
            double sumY = 0;
            int count = 0;
            for (int i = 0; i < lines.rows(); i++) {
                double[] l = lines.get(i, 0);
                double dx = l[2] - l[0], dy = l[3] - l[1];
                double angle = Math.atan2(dy, dx) * 180 / Math.PI;
                if (Math.abs(angle) < 30) {
                    sumY += (l[1] + l[3]) / 2;
                    count++;
                }
            }
            if (count > 0) houghY = sumY / count + startRow;
        }

        Double rowsY = detectStopLineByRows(binary, startRow);
        Double currentY = (houghY != null) ? houghY : rowsY;

        // --- smoothing ---
        if (currentY != null) {
            Double last = lastStableYMap.get(videoId);
            if (last == null) {
                lastStableYMap.put(videoId, currentY);
            } else {
                double alpha = 0.05;
                double smoothed = alpha * currentY + (1 - alpha) * last;
                double maxDelta = 5.0;
                if (Math.abs(smoothed - last) > maxDelta)
                    smoothed = last + Math.signum(smoothed - last) * maxDelta;
                lastStableYMap.put(videoId, smoothed);
            }
        }

        // --- vẽ ---
        Double stableY = lastStableYMap.get(videoId);
        if (stableY != null) {
            Imgproc.line(frame, new Point(0, stableY), new Point(width, stableY), new Scalar(0, 255, 0), 3);
            Imgproc.putText(frame, "STOP LINE", new Point(10, stableY.intValue() - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(0, 255, 0), 2);
        }

        // release mats
        roi.release();
        gray.release();
        binary.release();
        edges.release();
        lines.release();

        return stableY;
    }

    private Double detectStopLineByRows(Mat edges, int startRowOffset) {
        int width = edges.cols();
        int height = edges.rows();
        byte[] rowData = new byte[width];
        int bestRow = -1;
        int maxEdgePoints = 0;

        for (int y = 0; y < height; y++) {
            edges.get(y, 0, rowData);
            int edgeCount = 0;
            for (int x = 0; x < width; x++) if ((rowData[x] & 0xFF) > 0) edgeCount++;
            if (edgeCount > width * 0.3 && edgeCount > maxEdgePoints) {
                maxEdgePoints = edgeCount;
                bestRow = y;
            }
        }
        if (maxEdgePoints < width * 0.1) return null;
        return (bestRow == -1) ? null : (double) bestRow + startRowOffset;
    }

    public void clearHistory(Long videoId) {
        lastStableYMap.remove(videoId);
    }
}