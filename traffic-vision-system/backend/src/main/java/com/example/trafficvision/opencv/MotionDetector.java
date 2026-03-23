package com.example.trafficvision.opencv;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.springframework.stereotype.Component;

/**
 * Lớp MotionDetector chịu trách nhiệm phát hiện các vật thể chuyển động trong video.
 * Sử dụng thuật toán BackgroundSubtractorMOG2 của OpenCV cùng với các phép biến đổi hình học
 * để loại bỏ nhiễu và làm rõ vùng chuyển động.
 */
@Component
public class MotionDetector {

    // Công cụ trừ nền MOG2 (Mixture of Gaussians) để phân tách nền và vật thể chuyển động
    private final BackgroundSubtractorMOG2 bgSubtractor;
    // Mặt nạ vật thể phía trước (foreground mask) - chứa các vùng có chuyển động
    private final Mat fgMask;

    public MotionDetector() {
        // Tham số 500: Lịch sử khung hình để xây dựng nền; 16: Ngưỡng Mahalanobis; false: Không phát hiện bóng (shadows)
        bgSubtractor = Video.createBackgroundSubtractorMOG2(500, 16, false);
        fgMask = new Mat();
    }

    /**
     * Thực hiện phát hiện chuyển động trên khung hình đầu vào.
     *
     * @param frame Khung hình hiện tại
     * @return Mặt nạ chứa các vùng đang chuyển động
     */
    public Mat detectMotion(Mat frame) {
        // Áp dụng thuật toán trừ nền để lấy mặt nạ chuyển động
        bgSubtractor.apply(frame, fgMask);

        // Sử dụng các phép toán hình học để giảm nhiễu
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        
        // Erode (Xói mòn): Loại bỏ các điểm trắng lẻ tẻ (nhiễu nhỏ)
        Imgproc.erode(fgMask, fgMask, kernel);
        
        // Dilate (Giãn nở): Lấp đầy các lỗ hổng bên trong vật thể chuyển động
        Imgproc.dilate(fgMask, fgMask, kernel);
        
        kernel.release();

        return fgMask;
    }

}
