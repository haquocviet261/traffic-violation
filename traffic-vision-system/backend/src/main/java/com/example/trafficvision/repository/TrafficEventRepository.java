package com.example.trafficvision.repository;

import com.example.trafficvision.model.TrafficEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrafficEventRepository extends JpaRepository<TrafficEvent, Long> {
    List<TrafficEvent> findByVideoId(Long videoId);
}
