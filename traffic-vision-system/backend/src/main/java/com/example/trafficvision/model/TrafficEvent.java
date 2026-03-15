package com.example.trafficvision.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_event")
@Getter
@Setter
public class TrafficEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "event_type", nullable = false)
    private String eventType; // e.g., RED_LIGHT_VIOLATION, VEHICLE_DETECTED

    @Column(name = "frame_number")
    private Integer frameNumber;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "bbox_x")
    private Integer bboxX;

    @Column(name = "bbox_y")
    private Integer bboxY;

    @Column(name = "bbox_width")
    private Integer bboxWidth;

    @Column(name = "bbox_height")
    private Integer bboxHeight;

    @Column(name = "centroid_x")
    private Double centroidX;

    @Column(name = "centroid_y")
    private Double centroidY;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "details")
    private String details;
}
