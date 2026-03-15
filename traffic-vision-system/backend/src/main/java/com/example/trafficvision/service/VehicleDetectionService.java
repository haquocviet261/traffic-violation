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

@Service
@RequiredArgsConstructor
public class VehicleDetectionService {

    private final MotionDetector motionDetector;
    private final ContourVehicleDetector contourVehicleDetector;


    public List<Rect> detectVehicles(Mat frame) {
        if (frame.empty()) {
            return new ArrayList<>();
        }

        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        Mat fgMask = motionDetector.detectMotion(grayFrame);

        // Detect raw bounding boxes using contours
        List<Rect> detectedRects = contourVehicleDetector.detectVehicles(fgMask);

        // Release native memory
        fgMask.release();
        grayFrame.release();

        return detectedRects;
    }
}
