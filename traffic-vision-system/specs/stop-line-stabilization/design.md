# Technical Design: Stop Line Stabilization Algorithm

## 1. Algorithmic Pipeline

### A. Pre-processing & White Filtering
Instead of using standard grayscale conversion, we will employ HSV color thresholding to isolate road markings.
*   **HSV Thresholding:** Convert frame to HSV. Define ranges for "bright white" (e.g., Saturation < 50, Value > 200).
*   **Result:** A binary mask containing only bright road markings, significantly reducing noise from dark asphalt, shadows, and colored vehicles.

### B. Morphological Operations
To handle worn or fragmented lines:
*   **Closing (Dilation then Erosion):** Apply a horizontal rectangular kernel (e.g., 15x1) to bridge small gaps in the horizontal markings.
*   **Benefit:** Merges fragmented stop line segments into a single cohesive candidate for Hough detection.

### C. Refined ROI (Region of Interest)
*   Limit detection to a horizontal band between 60% and 85% of the frame height.
*   This band excludes the sky/horizon (top) and the car dashboard/immediate foreground (bottom).

### D. Scoring & Filtering of Hough Candidates
After `HoughLinesP` detection, each candidate line will be scored based on:
1.  **Horizontal Alignment:** Higher score for lines with angle < 5 degrees from horizontal.
2.  **Width:** Higher score for longer lines (normalized by frame width).
3.  **Proximity to Center:** Higher score for lines centered in the lane region.
*   **Final Candidate Selection:** Pick the line with the highest aggregate score.

### E. Temporal Filtering & Stabilization
To prevent jumping, we implement a **One-Euro Filter** or a simplified **Moving Average Filter**:
*   **Moving Average:** Maintain a window of the last 15 frames of detected `stopLineY`.
*   **Outlier Rejection:** If a new detection deviates more than 30 pixels from the moving average, it is rejected as a candidate for that frame.
*   **Smoothing:** The current `stopLineY` returned by the system is the average of the windowed detections.

### F. Stop Line Locking Logic
*   **Detection Counter:** Increment a counter for every consecutive frame a line is detected with high confidence.
*   **Locking:** Once the counter exceeds 20 frames, the line is "Locked". 
*   **Grace Period:** If detection fails in a single frame, the system uses the last known "Locked" value for up to 10 frames before reverting to a "Searching" state.

## 2. Integration Architecture

### StopLineDetector.java
*   Add state variables: `movingAverageWindow`, `lockedStopLineY`, `detectionConfidenceCounter`.
*   Update `detectStopLineY(Mat frame)` to implement the new HSV -> Morphology -> Scoring pipeline.

### ViolationDetectionService.java
*   Call `stopLineDetector.getStabilizedStopLineY()` to ensure crossing logic uses the smooth, locked coordinate instead of the raw detection.

## 3. Configuration Parameters
| Parameter | Proposed Value | Description |
| :--- | :--- | :--- |
| `Hough_Threshold` | 80 | Minimum intersections to detect a line |
| `Min_Line_Width` | 150 px | Minimum length for a stop line candidate |
| `Stability_Window` | 15 frames | Number of frames for moving average |
| `Max_Jump_Threshold` | 30 px | Pixel distance allowed before detection is rejected |
