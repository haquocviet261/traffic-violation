package com.example.trafficvision.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_result")
@Getter
@Setter
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "traffic_light_detections")
    private Integer trafficLightDetections;

    @Column(name = "stop_lines_detected")
    private Integer stopLinesDetected;

    @Column(name = "violation_count")
    private Integer violationCount;

    @Column(name = "vehicle_count")
    private Integer vehicleCount;

    @Column(name = "traffic_density")
    private Double trafficDensity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Lob
    @Column(name = "serialized_traffic_light_rects", columnDefinition = "TEXT")
    private String serializedTrafficLightRects;

    @Lob
    @Column(name = "serialized_stop_lines", columnDefinition = "TEXT")
    private String serializedStopLines;
}
