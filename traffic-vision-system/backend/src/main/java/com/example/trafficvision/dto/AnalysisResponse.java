package com.example.trafficvision.dto;

import com.example.trafficvision.dto.RectDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalysisResponse {
    private Long videoId;
    private Integer vehicleCount;
    private Integer violationCount;
    private Double trafficDensity;
    private LocalDateTime analysisTime;
    private Integer trafficLightDetections;
    private Integer stopLinesDetected;
    private List<RectDto> trafficLightRects;
    private List<LineDto> stopLines;
    private List<com.example.trafficvision.model.TrafficEvent> trafficEvents;

    public AnalysisResponse(Long videoId, Integer vehicleCount, Integer violationCount, Double trafficDensity, LocalDateTime analysisTime, Integer trafficLightDetections, Integer stopLinesDetected, List<RectDto> trafficLightRects, List<LineDto> stopLines, List<com.example.trafficvision.model.TrafficEvent> trafficEvents) {
        this.videoId = videoId;
        this.vehicleCount = vehicleCount;
        this.violationCount = violationCount;
        this.trafficDensity = trafficDensity;
        this.analysisTime = analysisTime;
        this.trafficLightDetections = trafficLightDetections;
        this.stopLinesDetected = stopLinesDetected;
        this.trafficLightRects = trafficLightRects;
        this.stopLines = stopLines;
        this.trafficEvents = trafficEvents;
    }


}
