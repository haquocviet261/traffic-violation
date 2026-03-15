-- SQL schema for MySQL database

CREATE DATABASE IF NOT EXISTS traffic_vision_db;

USE traffic_vision_db;

-- Table for uploaded videos
CREATE TABLE IF NOT EXISTS video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- e.g., UPLOADED, PROCESSING, COMPLETED, FAILED
    upload_time DATETIME NOT NULL
);

-- Table for analysis results
CREATE TABLE IF NOT EXISTS analysis_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    vehicle_count INT,
    violation_count INT,
    traffic_density DOUBLE,
    traffic_light_detections INT,
    stop_lines_detected INT,
    created_at DATETIME NOT NULL,
    serialized_traffic_light_rects TEXT,
    serialized_stop_lines TEXT,
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
);

-- Table for specific traffic events (e.g., red light violations)
CREATE TABLE IF NOT EXISTS traffic_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL, -- e.g., RED_LIGHT_VIOLATION, VEHICLE_DETECTED
    frame_number INT,
    timestamp DATETIME NOT NULL,
    bbox_x INT,
    bbox_y INT,
    bbox_width INT,
    bbox_height INT,
    centroid_x DOUBLE,
    centroid_y DOUBLE,
    image_path VARCHAR(255),
    details TEXT,
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
);
