package com.example.trafficvision.service;

import com.example.trafficvision.dto.DashboardStatsResponse;
import com.example.trafficvision.repository.AnalysisResultRepository;
import com.example.trafficvision.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    public DashboardStatsResponse getDashboardStatistics() {
        Long totalVideosProcessed = videoRepository.countByStatus("COMPLETED");
        Double averageTrafficDensity = analysisResultRepository.findAverageTrafficDensity();
        Long totalViolationsDetected = analysisResultRepository.findTotalViolationCount();

        return new DashboardStatsResponse(
                totalVideosProcessed,
                totalViolationsDetected != null ? totalViolationsDetected : 0L,
                averageTrafficDensity != null ? averageTrafficDensity : 0.0,
                LocalDateTime.now()
        );
    }
}
