package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContourVehicleDetector {

    // Minimum contour area to consider as a vehicle (adjust as needed)
    private static final double MIN_VEHICLE_AREA = 500;

    public List<Rect> detectVehicles(Mat motionMask) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // Find contours in the motion mask
        Imgproc.findContours(motionMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> vehicleBoundingBoxes = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            if (area > MIN_VEHICLE_AREA) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                vehicleBoundingBoxes.add(boundingRect);
            }
            contour.release(); // Release native memory
        }
        hierarchy.release(); // Release native memory
        return vehicleBoundingBoxes;
    }
}
