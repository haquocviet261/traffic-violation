package com.example.trafficvision.opencv;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp ContourVehicleDetector dùng để phát hiện phương tiện dựa trên các đường bao (contours)
 * từ mặt nạ chuyển động (motion mask).
 */
@Component
public class ContourVehicleDetector {

    // Diện tích tối thiểu của một đường bao để được coi là phương tiện (giúp loại bỏ nhiễu nhỏ)
    private static final double MIN_VEHICLE_AREA = 650;

    /**
     * Phát hiện các phương tiện từ mặt nạ chuyển động.
     *
     * @param motionMask Mặt nạ nhị phân thể hiện các vùng có chuyển động
     * @return Danh sách các hình chữ nhật bao quanh phương tiện phát hiện được
     */
    public List<Rect> detectVehicles(Mat motionMask) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // Tìm các đường bao bên ngoài (RETR_EXTERNAL) và đơn giản hóa các điểm (CHAIN_APPROX_SIMPLE)
        Imgproc.findContours(motionMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> vehicleBoundingBoxes = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            // Tính diện tích của đường bao
            double area = Imgproc.contourArea(contour);

            // Chỉ giữ lại các đường bao có diện tích đủ lớn để là phương tiện
            if (area > MIN_VEHICLE_AREA) {
                // Tạo hình chữ nhật bao quanh đường bao
                Rect boundingRect = Imgproc.boundingRect(contour);
                vehicleBoundingBoxes.add(boundingRect);
            }
            // Giải phóng bộ nhớ của contour sau khi xử lý
            contour.release();
        }
        // Giải phóng bộ nhớ của hierarchy
        hierarchy.release();
        return vehicleBoundingBoxes;
    }
}
