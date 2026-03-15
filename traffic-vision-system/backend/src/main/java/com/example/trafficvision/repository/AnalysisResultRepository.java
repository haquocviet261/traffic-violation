package com.example.trafficvision.repository;

import com.example.trafficvision.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    Optional<AnalysisResult> findByVideoId(Long videoId);

    @Query("SELECT AVG(ar.trafficDensity) FROM AnalysisResult ar")
    Double findAverageTrafficDensity();

    @Query("SELECT SUM(ar.violationCount) FROM AnalysisResult ar")
    Long findTotalViolationCount();
}
