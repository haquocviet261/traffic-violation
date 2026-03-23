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

    // ================= COLOR RANGE (Optimized) =================

    private static final Scalar LOWER_RED1 = new Scalar(0, 70, 70);
    private static final Scalar UPPER_RED1 = new Scalar(10, 255, 255);
    private static final Scalar LOWER_RED2 = new Scalar(160, 70, 70);
    private static final Scalar UPPER_RED2 = new Scalar(179, 255, 255);

    private static final Scalar LOWER_GREEN = new Scalar(40, 50, 50);
    private static final Scalar UPPER_GREEN = new Scalar(90, 255, 255);

    private static final Scalar LOWER_YELLOW = new Scalar(15, 50, 50);
    private static final Scalar UPPER_YELLOW = new Scalar(35, 255, 255);

    private static final double MIN_TRAFFIC_LIGHT_AREA = 15; // Slightly lower to catch distant lights
    private static final int STATE_BUFFER_SIZE = 20; // Increased for better stability
    private static final int BRIGHTNESS_BUFFER_SIZE = 15;

    private final Map<Long, LinkedList<String>> stateHistoryMap = new ConcurrentHashMap<>();
    private final Map<Long, LinkedList<Double>> brightnessHistoryMap = new ConcurrentHashMap<>();


    // =========================================================
    // ================= DETECT TRẠNG THÁI ĐÈN =================
    // =========================================================

    public String getTrafficLightState(Mat frame, Long videoId) {

        if (frame.empty()) return "UNKNOWN";

        // Mở rộng ROI để bao quát hơn (50% trên của khung hình)
        Rect roiRect = new Rect(0, 0, frame.cols(), (int) (frame.rows() * 0.5));
        Mat roi = frame.submat(roiRect);

        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(roi, hsvFrame, Imgproc.COLOR_BGR2HSV);

        // Lấy thông tin contour lớn nhất cho từng dải màu
        DetectionInfo red = getMaxContourInfo(hsvFrame, LOWER_RED1, UPPER_RED1, LOWER_RED2, UPPER_RED2);
        DetectionInfo green = getMaxContourInfo(hsvFrame, LOWER_GREEN, UPPER_GREEN, null, null);
        DetectionInfo yellow = getMaxContourInfo(hsvFrame, LOWER_YELLOW, UPPER_YELLOW, null, null);

        DetectionInfo best = new DetectionInfo(0, new Rect(), new Point());
        String currentState = "UNKNOWN";

        // Ưu tiên chọn đèn có diện tích lớn nhất và vượt ngưỡng tối thiểu
        if (red.area >= MIN_TRAFFIC_LIGHT_AREA) {
            best = red;
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
        updateBrightnessHistory(videoId, getBrightness(roi, best.box));

        // Lấy trạng thái ổn định nhất từ lịch sử (lọc nhiễu)
        String finalState = getStableState(videoId);

        // Xử lý đèn đỏ nhấp nháy
        if (isFlashingRed(videoId)) {
            finalState = "RED";
        }

        // Vẽ phản hồi trực quan nếu tìm thấy đèn
        if (!"UNKNOWN".equals(finalState) && best.area > 0) {
            // Hiệu chỉnh tọa độ box về frame gốc (nếu ROI không bắt đầu từ 0,0)
            Rect drawBox = new Rect(best.box.x + roiRect.x, best.box.y + roiRect.y, best.box.width, best.box.height);
            Scalar drawColor = getScalarColor(finalState);
            Imgproc.rectangle(frame, drawBox, drawColor, 2);
            Imgproc.putText(frame, "STATE: " + finalState, new Point(drawBox.x, drawBox.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, drawColor, 2);
        }

        hsvFrame.release();
        roi.release();

        return finalState;
    }

    private Scalar getScalarColor(String state) {
        switch (state) {
            case "RED": return new Scalar(0, 0, 255);
            case "GREEN": return new Scalar(0, 255, 0);
            case "YELLOW": return new Scalar(0, 255, 255);
            default: return new Scalar(255, 255, 255);
        }
    }


    // =========================================================
    // ================= DETECT VỊ TRÍ ĐÈN (BẠN ĐANG DÙNG) =====
    // =========================================================

    public List<Rect> detectTrafficLights(Mat frame) {

        List<Rect> trafficLights = new ArrayList<>();

        if (frame.empty()) return trafficLights;

        Rect roiRect = new Rect(0, 0, frame.cols(), (int) (frame.rows() * 0.5));
        Mat roi = frame.submat(roiRect);

        Mat hsv = new Mat();
        Imgproc.cvtColor(roi, hsv, Imgproc.COLOR_BGR2HSV);

        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        Core.inRange(hsv, LOWER_RED1, UPPER_RED1, mask1);
        Core.inRange(hsv, LOWER_RED2, UPPER_RED2, mask2);

        Mat redMask = new Mat();
        Core.bitwise_or(mask1, mask2, redMask);

        Imgproc.GaussianBlur(redMask, redMask, new Size(5, 5), 0);
        Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_CLOSE,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(redMask, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {

            double area = Imgproc.contourArea(contour);
            if (area < 60 || area > 1500) continue;

            Rect box = Imgproc.boundingRect(contour);

            double ratio = (double) box.width / box.height;
            if (ratio < 0.6 || ratio > 1.4) continue;

            trafficLights.add(box);
            Imgproc.rectangle(frame, box, new Scalar(0, 0, 255), 2);

            contour.release();
        }

        hsv.release();
        mask1.release();
        mask2.release();
        redMask.release();
        hierarchy.release();
        roi.release();

        return trafficLights;
    }


    // =========================================================
    // ================= HELPER =================
    // =========================================================

    private void updateStateHistory(Long videoId, String state) {
        LinkedList<String> history = stateHistoryMap.computeIfAbsent(videoId, k -> new LinkedList<>());
        history.add(state);
        if (history.size() > STATE_BUFFER_SIZE) history.removeFirst();
    }

    private void updateBrightnessHistory(Long videoId, double value) {
        LinkedList<Double> history = brightnessHistoryMap.computeIfAbsent(videoId, k -> new LinkedList<>());
        history.add(value);
        if (history.size() > BRIGHTNESS_BUFFER_SIZE) history.removeFirst();
    }

    private String getStableState(Long videoId) {

        LinkedList<String> history = stateHistoryMap.get(videoId);
        if (history == null || history.isEmpty()) return "UNKNOWN";

        Map<String, Integer> counts = new HashMap<>();
        for (String s : history) {
            counts.put(s, counts.getOrDefault(s, 0) + 1);
        }

        // Ưu tiên các trạng thái thực (RED, GREEN, YELLOW) hơn UNKNOWN
        // Nếu một trạng thái thực có ít nhất 25% số lượng trong buffer, ta có thể cân nhắc nó
        String bestState = "UNKNOWN";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String state = entry.getKey();
            int count = entry.getValue();

            if ("UNKNOWN".equals(state)) continue;

            if (count > maxCount) {
                maxCount = count;
                bestState = state;
            }
        }

        // Nếu trạng thái thực tốt nhất chiếm ít nhất 30% buffer, chọn nó thay vì UNKNOWN
        if (maxCount >= STATE_BUFFER_SIZE * 0.3) {
            return bestState;
        }

        return counts.getOrDefault("UNKNOWN", 0) > (STATE_BUFFER_SIZE / 2) ? "UNKNOWN" : bestState;
    }

    private boolean isFlashingRed(Long videoId) {

        LinkedList<String> states = stateHistoryMap.get(videoId);
        LinkedList<Double> brightness = brightnessHistoryMap.get(videoId);

        if (states == null || brightness == null) return false;
        if (states.size() < 6 || brightness.size() < 6) return false;

        int flashCount = 0;

        for (int i = 1; i < brightness.size(); i++) {
            if (brightness.get(i) > 170 && brightness.get(i - 1) < 80) flashCount++;
        }

        return flashCount >= 2;
    }

    private double getBrightness(Mat roi, Rect box) {

        if (box.width == 0 || box.height == 0) return 0;

        Rect safe = new Rect(
                Math.max(box.x, 0),
                Math.max(box.y, 0),
                Math.min(box.width, roi.cols() - box.x),
                Math.min(box.height, roi.rows() - box.y)
        );

        Mat lightRoi = roi.submat(safe);
        Mat hsv = new Mat();
        Imgproc.cvtColor(lightRoi, hsv, Imgproc.COLOR_BGR2HSV);

        double brightness = Core.mean(hsv).val[2];

        hsv.release();
        lightRoi.release();

        return brightness;
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

        Imgproc.GaussianBlur(mask, mask, new Size(5, 5), 0);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(mask, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        Rect bestBox = new Rect();
        Point bestCentroid = new Point();

        for (MatOfPoint contour : contours) {

            double area = Imgproc.contourArea(contour);

            // ---- 1. bỏ nhiễu nhỏ (giảm nhẹ để bắt đèn xa)
            if (area < 15) continue;

            // ---- 2. bỏ vật thể quá lớn (biển quảng cáo, v.v.)
            if (area > 800) continue;

            Rect box = Imgproc.boundingRect(contour);

            // ---- 3. Tỷ lệ khung hình (nới lỏng để chấp nhận biến dạng phối cảnh)
            double ratio = (double) box.width / box.height;
            if (ratio < 0.3 || ratio > 2.8) continue;

            // ---- 4. Độ tròn (nới lỏng vì phối cảnh có thể làm đèn trông dẹt)
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            if (perimeter == 0) continue;

            double circularity = 4 * Math.PI * area / (perimeter * perimeter);
            if (circularity < 0.35) continue;

            // ---- Nếu vượt qua các bộ lọc, chọn vùng có diện tích lớn nhất
            if (area > maxArea) {
                maxArea = area;
                bestBox = box;

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