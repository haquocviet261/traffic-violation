package com.example.trafficvision.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardStatsResponse {
    private Long totalVideosProcessed;
    private Long totalViolationsDetected;
    private Double averageTrafficDensity;
    private LocalDateTime lastUpdated;

    public DashboardStatsResponse(Long totalVideosProcessed, Long totalViolationsDetected, Double averageTrafficDensity, LocalDateTime lastUpdated) {
        this.totalVideosProcessed = totalVideosProcessed;
        this.totalViolationsDetected = totalViolationsDetected;
        this.averageTrafficDensity = averageTrafficDensity;
        this.lastUpdated = lastUpdated;
    }


}
