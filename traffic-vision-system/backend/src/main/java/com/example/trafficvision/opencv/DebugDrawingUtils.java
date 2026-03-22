package com.example.trafficvision.opencv;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

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

    public static void drawDetection(Mat frame, Rect box, String label, Scalar color) {
        Imgproc.rectangle(frame, box.tl(), box.br(), color, 2);
        Imgproc.putText(frame, label, new Point(box.x, box.y - 5), Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
    }

    public static void saveDebugImage(Mat frame, String prefix) {
        String filename = DEBUG_DIR + "/" + prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
        Imgcodecs.imwrite(filename, frame);
        logger.debug("Saved debug image: {}", filename);
    }
}
