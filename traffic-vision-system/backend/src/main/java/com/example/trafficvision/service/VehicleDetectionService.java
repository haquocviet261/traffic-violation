package com.example.trafficvision.service;

import com.example.trafficvision.opencv.ContourVehicleDetector;
import com.example.trafficvision.opencv.MotionDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Dịch vụ VehicleDetectionService kết hợp MotionDetector và ContourVehicleDetector
 * để phát hiện các phương tiện đang di chuyển trong một khung hình.
 */
@Service
@RequiredArgsConstructor
public class VehicleDetectionService {

    private final MotionDetector motionDetector;
    private final ContourVehicleDetector contourVehicleDetector;

    /**
     * Thực hiện phát hiện phương tiện trên khung hình đầu vào.
     *
     * @param frame Khung hình hiện tại (ảnh màu)
     * @return Danh sách các hình chữ nhật bao quanh phương tiện phát hiện được
     */
    public List<Rect> detectVehicles(Mat frame) {
        if (frame.empty()) {
            return new ArrayList<>();
        }

        // Chuyển khung hình sang ảnh xám để tối ưu tốc độ xử lý phát hiện chuyển động
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // Bước 1: Phát hiện chuyển động để lấy mặt nạ các vùng đang di chuyển
        Mat fgMask = motionDetector.detectMotion(grayFrame);

        // Bước 2: Tìm các đường bao trên mặt nạ chuyển động để xác định vị trí xe
        List<Rect> detectedRects = contourVehicleDetector.detectVehicles(fgMask);

        // Giải phóng bộ nhớ của các đối tượng Mat tạm thời
        fgMask.release();
        grayFrame.release();

        return detectedRects;
    }
}
