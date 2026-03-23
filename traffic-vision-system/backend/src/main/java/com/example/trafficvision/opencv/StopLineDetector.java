package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp StopLineDetector chịu trách nhiệm phát hiện vạch dừng trong khung hình video.
 * Sử dụng các kỹ thuật xử lý ảnh của OpenCV như ROI, ngưỡng Otsu, phép đóng hình học,
 * và làm mượt kết quả bằng trung bình trượt lũy thừa (exponential smoothing).
 */
@Component
public class StopLineDetector {
    private static final Logger logger = LoggerFactory.getLogger(StopLineDetector.class);

    private static final int BUFFER_SIZE = 20;
    // Kernel hình chữ nhật nằm ngang để kết nối các đoạn vạch đứt quãng
    private static final Mat HORIZONTAL_KERNEL =
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(60, 3));

    // Lưu trữ trạng thái theo từng video (videoId)
    private final Map<Long, LinkedList<Double>> historyMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> framesSinceLastDetectionMap = new ConcurrentHashMap<>();
    private final Map<Long, Double> lastStableYMap = new ConcurrentHashMap<>();

    /**
     * Phát hiện tọa độ Y của vạch dừng trong khung hình.
     * Cải tiến với ngưỡng Otsu và logic kết nối đường thẳng linh hoạt hơn.
     *
     * @param frame   Khung hình cần xử lý
     * @param videoId ID của video để duy trì trạng thái ổn định
     * @return Tọa độ Y đã được làm mượt của vạch dừng
     */
    public Double detectStopLineY(Mat frame, Long videoId) {
        int frameSkip = framesSinceLastDetectionMap.getOrDefault(videoId, 0);

        // Nếu vừa mới phát hiện gần đây, sử dụng lại giá trị cũ để tiết kiệm hiệu năng
        if (frameSkip < 10 && lastStableYMap.containsKey(videoId)) {
            framesSinceLastDetectionMap.put(videoId, frameSkip + 1);
            return lastStableYMap.get(videoId);
        }

        if (frame.empty()) return lastStableYMap.get(videoId);

        int width = frame.cols();
        int height = frame.rows();

        // ROI (Region of Interest): Tập trung vào nửa dưới khung hình, nơi vạch dừng thường xuất hiện (40% - 75% chiều cao)
        int startRow = (int) (height * 0.40);
        int endRow = (int) (height * 0.75);
        Rect roiRect = new Rect(0, startRow, width, endRow - startRow);
        Mat roi = frame.submat(roiRect);

        // Chuyển sang ảnh xám để xử lý
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);

        // Sử dụng ngưỡng Otsu để tự động thích ứng với điều kiện ánh sáng khác nhau
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // Phép đóng hình học (Morphological Closing) với kernel ngang lớn để nối các vạch đứt quãng
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, HORIZONTAL_KERNEL);

        // Phát hiện cạnh bằng thuật toán Canny sau khi làm mờ Gauss để giảm nhiễu
        Mat edges = new Mat();
        Imgproc.GaussianBlur(binary, binary, new Size(5, 5), 0);
        Imgproc.Canny(binary, edges, 40, 120);

        Mat lines = new Mat();
        // Sử dụng HoughLinesP để tìm các đoạn thẳng tiềm năng
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 30, width * 0.55, 60);

        // Tìm vạch dừng dựa trên mật độ điểm trắng theo hàng trong ảnh nhị phân
        Double currentY = detectStopLineByRows(binary, startRow);

        if (currentY != null) {
            framesSinceLastDetectionMap.put(videoId, 0);
            updateHistory(videoId, currentY); // Cập nhật và làm mượt tọa độ Y
        } else {
            int missed = framesSinceLastDetectionMap.getOrDefault(videoId, 0) + 1;
            framesSinceLastDetectionMap.put(videoId, missed);

            // Nếu không tìm thấy trong thời gian dài (150 khung hình), xóa lịch sử để tính toán lại từ đầu
            if (missed > 150) {
                lastStableYMap.remove(videoId);
                historyMap.remove(videoId);
            }
        }

        Double stableY = lastStableYMap.get(videoId);

        // Vẽ vạch dừng lên khung hình để debug
        if (stableY != null) {
            Scalar color = (currentY != null) ? new Scalar(0, 255, 0) : new Scalar(0, 255, 255);
            String label = (currentY != null) ? "STOP LINE" : "STOP LINE (ESTIMATED)";
            //  Imgproc.line(frame, new Point(0, stableY), new Point(width, stableY), color, 3);
            Imgproc.putText(frame, label, new Point(10, stableY.intValue() - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
        }

        roi.release();
        gray.release();
        binary.release();
        edges.release();
        lines.release();

        return stableY;
    }

    /**
     * Quét từng hàng trong ROI để tìm hàng có mật độ điểm trắng cao nhất (vạch dừng).
     */
    private Double detectStopLineByRows(Mat binary, int startRowOffset) {

        int width = binary.cols();
        int height = binary.rows();

        byte[] rowData = new byte[width];

        int bestRow = -1;
        int maxWhite = 0;

        // Quét 65% chiều cao của ROI
        for (int y = 0; y < height * 0.65; y++) {

            binary.get(y, 0, rowData);

            int whiteCount = 0;

            for (int x = 0; x < width; x++) {
                if ((rowData[x] & 0xFF) == 255) {
                    whiteCount++;
                }
            }

            // Nếu hàng có số điểm trắng > 25% chiều rộng và là lớn nhất từ trước đến nay
            if (whiteCount > width * 0.25 && whiteCount > maxWhite) {
                maxWhite = whiteCount;
                bestRow = y;
            }
        }

        if (bestRow == -1) return null;

        return (double) bestRow + startRowOffset;
    }

    /**
     * Cập nhật lịch sử và làm mượt tọa độ Y bằng công thức Exponential Moving Average.
     * Giúp vạch dừng không bị rung lắc giữa các khung hình.
     */
    private void updateHistory(Long videoId, Double y) {

        Double last = lastStableYMap.get(videoId);

        if (last == null) {
            lastStableYMap.put(videoId, y);
            return;
        }

        // Hệ số alpha càng nhỏ thì độ ổn định càng cao nhưng phản ứng chậm hơn với thay đổi
        double alpha = 0.05;
        double smoothed = alpha * y + (1 - alpha) * last;

        lastStableYMap.put(videoId, smoothed);
    }

    /**
     * Xóa sạch lịch sử theo dõi vạch dừng của một video.
     */
    public void clearHistory(Long videoId) {
        historyMap.remove(videoId);
        framesSinceLastDetectionMap.remove(videoId);
        lastStableYMap.remove(videoId);
    }
}
