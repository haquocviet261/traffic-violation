package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StopLineDetector {
    private static final Logger logger = LoggerFactory.getLogger(StopLineDetector.class);

    private static final int BUFFER_SIZE = 20;
    private static final Mat HORIZONTAL_KERNEL =
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(60, 3));

    // Per-video state
    private final Map<Long, LinkedList<Double>> historyMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> framesSinceLastDetectionMap = new ConcurrentHashMap<>();
    private final Map<Long, Double> lastStableYMap = new ConcurrentHashMap<>();

    /**
     * Detects the stop line Y coordinate in the frame.
     * Improved with Otsu thresholding and more lenient line connecting logic.
     */
    public Double detectStopLineY(Mat frame, Long videoId) {
        int frameSkip = framesSinceLastDetectionMap.getOrDefault(videoId, 0);

        if (frameSkip < 10 && lastStableYMap.containsKey(videoId)) {
            framesSinceLastDetectionMap.put(videoId, frameSkip + 1);
            return lastStableYMap.get(videoId);
        }

        if (frame.empty()) return lastStableYMap.get(videoId);

        int width = frame.cols();
        int height = frame.rows();

        // ROI: Focus on lower half where stop lines usually appear
        int startRow = (int) (height * 0.40);
        int endRow = (int) (height * 0.75);
        Rect roiRect = new Rect(0, startRow, width, endRow - startRow);
        Mat roi = frame.submat(roiRect);

        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);

        // Use Otsu's thresholding for automatic light adaptation
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // Morphological closing with a large horizontal kernel to connect dashed lines
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, HORIZONTAL_KERNEL);

        Mat edges = new Mat();
        Imgproc.GaussianBlur(binary, binary, new Size(5, 5), 0);
        Imgproc.Canny(binary, edges, 40, 120);

        Mat lines = new Mat();
        // Be more lenient: lower threshold (30), smaller min length (20% of width), larger gap (100)
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 30, width * 0.55, 60);

        Double currentY = detectStopLineByRows(binary, startRow);

        if (currentY != null) {
            framesSinceLastDetectionMap.put(videoId, 0);
            updateHistory(videoId, currentY);
        } else {
            int missed = framesSinceLastDetectionMap.getOrDefault(videoId, 0) + 1;
            framesSinceLastDetectionMap.put(videoId, missed);

            if (missed > 150) { // Reset after a long period of no detection
                lastStableYMap.remove(videoId);
                historyMap.remove(videoId);
            }
        }

        Double stableY = lastStableYMap.get(videoId);

        // Visual debug on frame
        if (stableY != null) {
            Scalar color = (currentY != null) ? new Scalar(0, 255, 0) : new Scalar(0, 255, 255);
            String label = (currentY != null) ? "STOP LINE" : "STOP LINE (ESTIMATED)";
            Imgproc.line(frame, new Point(0, stableY), new Point(width, stableY), color, 3);
            Imgproc.putText(frame, label, new Point(10, stableY.intValue() - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
        }

        // Cleanup
        roi.release();
        gray.release();
        binary.release();
        edges.release();
        lines.release();

        return stableY;
    }

    private Double detectStopLineByRows(Mat binary, int startRowOffset) {

        int width = binary.cols();
        int height = binary.rows();

        byte[] rowData = new byte[width];

        int bestRow = -1;
        int maxWhite = 0;

        for (int y = 0; y < height * 0.65; y++) {

            binary.get(y, 0, rowData);

            int whiteCount = 0;

            for (int x = 0; x < width; x++) {
                if ((rowData[x] & 0xFF) == 255) {
                    whiteCount++;
                }
            }

            if (whiteCount > width * 0.25 && whiteCount > maxWhite) {
                maxWhite = whiteCount;
                bestRow = y;
            }
        }

        if (bestRow == -1) return null;

        return (double) bestRow + startRowOffset;
    }

    private void updateHistory(Long videoId, Double y) {

        Double last = lastStableYMap.get(videoId);

        if (last == null) {
            lastStableYMap.put(videoId, y);
            return;
        }

        double alpha = 0.05;
        double smoothed = alpha * y + (1 - alpha) * last;

        lastStableYMap.put(videoId, smoothed);
    }

    public void clearHistory(Long videoId) {
        historyMap.remove(videoId);
        framesSinceLastDetectionMap.remove(videoId);
        lastStableYMap.remove(videoId);
    }
}
