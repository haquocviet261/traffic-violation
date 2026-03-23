package com.example.trafficvision.opencv;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Lớp tiện ích DebugDrawingUtils dùng để hỗ trợ vẽ các khung phát hiện lên ảnh
 * và lưu trữ các khung hình debug để hỗ trợ kiểm tra kết quả xử lý.
 */
public class DebugDrawingUtils {
    private static final Logger logger = LoggerFactory.getLogger(DebugDrawingUtils.class);
    private static final String DEBUG_DIR = "debug";

    static {
        try {
            Files.createDirectories(Paths.get(DEBUG_DIR));
        } catch (IOException e) {
            logger.error("Could not create debug directory", e);
        }
    }

    /**
     * Vẽ một hình chữ nhật và nhãn tương ứng lên khung hình.
     *
     * @param frame Khung hình cần vẽ
     * @param box   Hình chữ nhật cần vẽ (vùng phát hiện)
     * @param label Nhãn văn bản (ví dụ: "VEHICLE")
     * @param color Màu sắc của hình vẽ
     */
    public static void drawDetection(Mat frame, Rect box, String label, Scalar color) {
        // Vẽ hình chữ nhật quanh vùng phát hiện
        Imgproc.rectangle(frame, box.tl(), box.br(), color, 2);
        // Vẽ nhãn văn bản ngay phía trên hình chữ nhật
        Imgproc.putText(frame, label, new Point(box.x, box.y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
    }


}
