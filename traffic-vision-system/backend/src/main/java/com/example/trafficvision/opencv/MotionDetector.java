package com.example.trafficvision.opencv;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.springframework.stereotype.Component;

@Component
public class MotionDetector {

    private final BackgroundSubtractorMOG2 bgSubtractor;
    private final Mat fgMask;

    public MotionDetector() {
        bgSubtractor = Video.createBackgroundSubtractorMOG2(500, 16, false);
        fgMask = new Mat();
    }

    public Mat detectMotion(Mat frame) {
        // Apply background subtraction
        bgSubtractor.apply(frame, fgMask);

        // Apply morphological operations to clean up the mask
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.erode(fgMask, fgMask, kernel);
        Imgproc.dilate(fgMask, fgMask, kernel);
        kernel.release();

        return fgMask;
    }

    public void release() {
        if (fgMask != null) {
            fgMask.release();
        }
        // bgSubtractor doesn't have a release method, it's managed by JVM GC
    }
}
