package com.example.trafficvision.service;

import com.example.trafficvision.dto.DashboardStatsResponse;
import com.example.trafficvision.repository.AnalysisResultRepository;
import com.example.trafficvision.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Dịch vụ DashboardService cung cấp các số liệu thống kê tổng hợp để hiển thị trên trang Dashboard.
 * Lấy dữ liệu từ cơ sở dữ liệu về các video đã xử lý, số vi phạm và mật độ giao thông.
 */
@Service
public class DashboardService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    /**
     * Lấy các thông số thống kê tổng quát của hệ thống.
     *
     * @return Đối tượng chứa số video hoàn thành, tổng vi phạm, mật độ trung bình và thời gian cập nhật.
     */
    public DashboardStatsResponse getDashboardStatistics() {
        // Đếm số lượng video đã xử lý thành công
        Long totalVideosProcessed = videoRepository.countByStatus("COMPLETED");
        // Tính mật độ giao thông trung bình từ tất cả các kết quả phân tích
        Double averageTrafficDensity = analysisResultRepository.findAverageTrafficDensity();
        // Tổng số lỗi vi phạm đã phát hiện được
        Long totalViolationsDetected = analysisResultRepository.findTotalViolationCount();

        return new DashboardStatsResponse(
                totalVideosProcessed,
                totalViolationsDetected != null ? totalViolationsDetected : 0L,
                averageTrafficDensity != null ? averageTrafficDensity : 0.0,
                LocalDateTime.now()
        );
    }
}
