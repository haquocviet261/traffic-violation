package com.example.trafficvision.opencv;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class FrameExtractor {

    private VideoCapture videoCapture;
    private Path videoPath;

    public FrameExtractor(Path videoPath) {
        this.videoPath = videoPath;
        this.videoCapture = new VideoCapture();
    }

    public boolean open() {
        return videoCapture.open(videoPath.toString());
    }

    public Optional<Mat> getNextFrame() {
        Mat frame = new Mat();
        if (videoCapture.read(frame)) {
            return Optional.of(frame);
        }
        return Optional.empty();
    }

    public void release() {
        if (videoCapture.isOpened()) {
            videoCapture.release();
        }
    }

    public double getFrameRate() {
        return videoCapture.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);
    }

    public long getTotalFrames() {
        return (long) videoCapture.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT);
    }
}
