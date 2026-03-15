package com.example.trafficvision.dto;

import lombok.Getter;
import lombok.Setter;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.List;
@Getter
@Setter
public class DetectionResult {
    private List<Rect> trafficLightRects;
    private List<Point[]> stopLines;
    private List<Rect> vehicles;


    public DetectionResult(List<Rect> trafficLightRects, List<Point[]> stopLines, List<Rect> vehicles) {
        this.trafficLightRects = trafficLightRects;
        this.stopLines = stopLines;
        this.vehicles = vehicles;
    }

}
