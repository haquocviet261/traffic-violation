# Tasks: Stop Line Stabilization Implementation

## Phase 1: Pre-processing & Candidate Detection
- [ ] **HSV White Filter:** Implement `filterWhiteMarkings(Mat frame)` in `StopLineDetector` using HSV inRange thresholding.
- [ ] **Morphological Closing:** Implement horizontal dilation/closing to bridge gaps in white markings.
- [ ] **Hough scoring logic:** Modify `detectStopLineY` to calculate a score for each line based on length and angle (favoring horizontal).

## Phase 2: Temporal Stabilization
- [ ] **Moving Average Buffer:** Implement a `LinkedList` or `Queue` to store the last 15 valid detections of `stopLineY`.
- [ ] **Outlier Rejection:** Implement logic to compare new detections against the buffer average and reject abrupt jumps.
- [ ] **Smooth Output:** Update the method to return the mean of the stabilized buffer.

## Phase 3: Confidence & Locking
- [ ] **Confidence Counter:** Add logic to track how many consecutive frames have produced a high-confidence line detection.
- [ ] **Lock Mechanism:** Implement the "Locked" state where the detector persists the last known good position during temporary obscuration or detection failure.
- [ ] **State Reset:** Implement logic to reset the lock if high-confidence detection fails for more than 10 consecutive frames.

## Phase 4: Integration & Visualization
- [ ] **Unified Interface:** Update `ViolationDetectionService` to use the stabilized output for crossing logic.
- [ ] **Enhanced Debug Drawing:** Update `DebugDrawingUtils` to draw the "Locked" stop line in a different style (e.g., solid green) vs candidate lines (dashed blue).

## Phase 5: Verification
- [ ] **Stability Test:** Process a sample video and log `stopLineY` variance to verify it remains within +/- 5 pixels during stable scenes.
- [ ] **Noise Rejection Test:** Verify that lane arrows and longitudinal markers no longer trigger horizontal line candidates.
