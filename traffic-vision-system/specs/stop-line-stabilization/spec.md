# Product Specification: Stop Line Stabilization

## 1. Problem Definition
The current stop line detection algorithm relies on a basic `HoughLinesP` transform applied to a broad region of interest (the lower half of the frame). In typical traffic scenes, this approach is highly unstable because:
*   **Multiple Candidates:** The algorithm often detects lane separators, directional arrows, or road edges as horizontal lines.
*   **Segment Fragmentation:** Stop lines are frequently worn or partially obscured by vehicles, leading to fragmented line segments.
*   **Lack of Context:** Basic Hough detection treats every frame as an independent event, causing the "jumping" effect where the detected stop line position oscillates between frames.
*   **Sensitivity to Lighting:** Standard edge detection (Canny) is sensitive to shadows and reflections on the asphalt, creating false line candidates.

## 2. Functional Requirements
The enhanced stop line detection system must:
*   **Temporal Consistency:** Ensure the stop line position does not jump abruptly between adjacent frames.
*   **Semantic Filtering:** Distinguish between actual stop lines and other road markings (arrows, lane lines).
*   **Proximity Prioritization:** Favor wide, horizontal lines that are located at the expected intersection boundary.
*   **Resilience:** Maintain a stable detection even if the stop line is temporarily obscured by a vehicle or is partially worn out.

## 3. Non-functional Requirements
*   **Performance:** Must run in real-time on standard CPU hardware without introducing significant latency to the video processing pipeline.
*   **Compatibility:** Must be implemented using OpenCV Java within the existing Spring Boot backend architecture.
*   **Integrability:** The output must remain compatible with the `ViolationDetectionService` crossing logic.

## 4. Success Criteria
*   The detected `stopLineY` value should stay within a +/- 5 pixel range over 30 consecutive frames in stable traffic.
*   Zero false detections triggered by lane arrows or longitudinal lane markings.
*   Successful lock-on within the first 60 frames of a video.
