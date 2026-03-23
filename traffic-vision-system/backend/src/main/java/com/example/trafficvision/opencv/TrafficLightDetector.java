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
     * Xác định trạng thái đèn giao thông với cơ chế làm mượt theo thời gian và xử lý đèn đỏ nhấp nháy.
     */
    public String getTrafficLightState(Mat frame, Long videoId) {

        if (frame.empty()) return "UNKNOWN";

        // ROI: Chỉ tập trung vào nửa trên của khung hình nơi đèn giao thông thường xuất hiện
        Rect roiRect = new Rect(0, 0, frame.cols(), (int) (frame.rows() * 0.5));
        Mat roi = frame.submat(roiRect);

        // Chuyển sang không gian màu HSV để phân đoạn màu sắc tốt hơn dưới các điều kiện ánh sáng khác nhau
        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(roi, hsvFrame, Imgproc.COLOR_BGR2HSV);

        // Lấy thông tin contour lớn nhất cho từng màu (Đỏ, Xanh, Vàng)
        DetectionInfo red = getMaxContourInfo(hsvFrame, LOWER_RED1, UPPER_RED1, LOWER_RED2, UPPER_RED2);
        DetectionInfo green = getMaxContourInfo(hsvFrame, LOWER_GREEN, UPPER_GREEN, null, null);
        DetectionInfo yellow = getMaxContourInfo(hsvFrame, LOWER_YELLOW, UPPER_YELLOW, null, null);

        String currentState = "UNKNOWN";
        DetectionInfo best = red;

        // Logic so sánh diện tích contour để xác định màu đèn hiện tại (phải vượt ngưỡng diện tích tối thiểu)
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

        // Cập nhật lịch sử trạng thái vào buffer để lọc nhiễu (Temporal Smoothing)
        updateStateHistory(videoId, currentState);

        // Lấy trạng thái xuất hiện nhiều nhất trong buffer (Dominant State)
        String finalState = getDominantState(videoId);

        // Xử lý đặc biệt cho đèn đỏ nhấp nháy (khi trạng thái xen kẽ giữa RED và UNKNOWN)
        if (isFlashingRed(videoId)) {
            finalState = "RED";
        }

        // Vẽ box debug lên frame
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

        List<Rect> trafficLights = new ArrayList<>();

        if (frame.empty()) return trafficLights;

        // 1. Chỉ lấy vùng phía trên (đèn giao thông luôn nằm trên cao)
        Rect roiRect = new Rect(0, 0, frame.cols(), (int) (frame.rows() * 0.5));
        Mat roi = frame.submat(roiRect);

        // 2. Chuyển sang HSV
        Mat hsv = new Mat();
        Imgproc.cvtColor(roi, hsv, Imgproc.COLOR_BGR2HSV);

        // 3. Mask màu đỏ (2 vùng đỏ trong HSV)
        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        Core.inRange(hsv, LOWER_RED1, UPPER_RED1, mask1);
        Core.inRange(hsv, LOWER_RED2, UPPER_RED2, mask2);

        Mat redMask = new Mat();
        Core.bitwise_or(mask1, mask2, redMask);

        // 4. Giảm nhiễu
        Imgproc.GaussianBlur(redMask, redMask, new Size(5, 5), 0);
        Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_CLOSE,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

        // 5. Tìm contour
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(redMask, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // 6. Lọc contour để chỉ giữ đèn giao thông thật
        for (MatOfPoint contour : contours) {

            double area = Imgproc.contourArea(contour);

            // Lọc diện tích (loại nhiễu nhỏ + biển quảng cáo lớn)
            if (area < 60 || area > 1500) continue;

            Rect box = Imgproc.boundingRect(contour);

            // Hình gần tròn
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            double circularity = 4 * Math.PI * area / (perimeter * perimeter);
            if (circularity < 0.5) continue;

            // Bounding box gần vuông
            double ratio = (double) box.width / box.height;
            if (ratio < 0.7 || ratio > 1.3) continue;

            // Không quá to (biển quảng cáo sẽ bị loại)
            if (box.width > 80 || box.height > 80) continue;

            // Phải nằm phía trên frame
            if (box.y > roi.rows() * 0.8) continue;

            trafficLights.add(box);

            // Debug box
            Imgproc.rectangle(frame, box, new Scalar(0, 0, 255), 2);
        }

        // 7. Giải phóng bộ nhớ
        hsv.release();
        mask1.release();
        mask2.release();
        redMask.release();
        hierarchy.release();
        roi.release();

        return trafficLights;
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