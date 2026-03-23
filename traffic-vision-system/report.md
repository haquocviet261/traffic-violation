# ABSTRACT
This report presents a comprehensive technical study and implementation of an automated Red Light Violation Detection System (RLVDS) developed as a university project for the Image Processing subject. The system utilizes traditional computer vision techniques, specifically the OpenCV 4.9.0 library within a Java Spring Boot 3.2.3 framework, to detect and record traffic violations without the use of deep learning models. The core methodology involves background subtraction for vehicle detection, color-based thresholding in the HSV space for traffic light state recognition, and morphological analysis for stop-line identification. A key innovation in this implementation is the use of vehicle bottom-Y coordinates and a 10-pixel tolerance zone for crossing detection, which significantly improves accuracy by accounting for camera perspective and preventing false positives from vehicles stopping near the line. The implementation emphasizes performance through parallelized frame analysis using Java's concurrent thread pools. Experimental results demonstrate that the proposed solution effectively identifies violations, calculates traffic density, and provides stabilized visual feedback. This report details the fundamental theories, architectural design, implementation specifics, and evaluation of the system, highlighting its potential as a robust traffic monitoring solution.

# CHAPTER 1: INTRODUCTION
## 1.1 Problem Statement
Urban traffic management faces significant challenges due to the increasing volume of vehicles and the resulting rise in traffic violations. Red light running is one of the most dangerous traffic infractions, often leading to severe accidents and fatalities. Conventional monitoring systems rely heavily on manual observation or expensive hardware-integrated solutions. This project addresses the need for an automated, software-based system that can reliably detect red light violations from standard video feeds using traditional image processing techniques, thereby providing a scalable and accessible alternative for traffic enforcement.

## 1.2 Motivation
The primary motivation for this project is to leverage computer vision technology to improve road safety. In educational contexts, understanding the application of traditional image processing algorithms—such as thresholding, contour analysis, and background subtraction—is crucial for developing robust engineering solutions. By creating a system that operates without deep learning, we focus on the mathematical foundations of image manipulation and the efficiency of algorithmic implementation, which are essential skills in the field of image processing.

## 1.3 Challenges
Implementing a robust RLVDS using traditional methods presents several technical challenges:
- **Environmental Variability**: Changes in lighting conditions, shadows, and weather can significantly affect color-based detection and background modeling.
- **Dynamic Occlusion**: Vehicles overlapping each other or being partially blocked by infrastructure can lead to fragmented contours and tracking errors.
- **Perspective Distortion**: Cameras positioned behind the stop line and looking down require logic that focuses on the vehicle's contact point with the road (tires) rather than its center.
- **Stop Line Stabilization**: Perspective and camera vibration make it difficult to maintain a consistent reference for the stop line.
- **State Recognition**: Differentiating between various traffic light states (Red, Yellow, Green, and Flashing Red) requires precise color segmentation and temporal smoothing.

## 1.4 Related Work
Traditional traffic violation detection systems have historically utilized various sensors, including inductive loops and radar. In the domain of computer vision, researchers have explored techniques such as Optical Flow for motion analysis and Canny edge detection for structural identification. Most classical approaches rely on Background Subtraction (BGS) algorithms like MOG2 (Mixture of Gaussians) for robust foreground extraction. Related studies emphasize the importance of Region of Interest (ROI) selection to optimize processing speed and accuracy in traffic scenarios.

## 1.5 Proposed Solution
The proposed system is a Java-based application that integrates Spring Boot for backend management and OpenCV for image processing. The solution consists of four primary modules:
1. **Vehicle Detection Module**: Employs BackgroundSubtractorMOG2 followed by morphological operations to identify moving objects.
2. **Traffic Light Module**: Uses HSV color masking and temporal dominant state analysis to determine the current signal state.
3. **Stop Line Module**: Utilizes Otsu's thresholding, horizontal morphological closing, and exponential smoothing to detect and stabilize the stop line position.
4. **Violation Analysis Module**: Implements bottom-Y coordinate tracking and tolerance-zone logic to identify vehicles crossing the intersection during a red signal.

## 1.6 Report Structure
The remainder of this report is organized as follows: Chapter 2 discusses the fundamental image processing theories employed; Chapter 3 details the system architecture, implementation, and experimental results; and Chapter 4 concludes the study with suggestions for future improvements.

# CHAPTER 2: FUNDAMENTAL THEORY
The system relies on several core image processing techniques:
- **Color Space Transformation**: Converting frames from BGR to HSV (Hue, Saturation, Value) is essential for traffic light detection, as HSV is more robust to changes in lighting intensity compared to BGR.
- **Background Subtraction (MOG2)**: The Mixture of Gaussians algorithm is used to model the static background and extract moving foreground objects (vehicles) by calculating the difference between the current frame and the background model.
- **Morphological Operations**: Erosion and Dilation (Opening/Closing) are used to remove noise from binary masks. A specific horizontal kernel (60x3) is used to connect fragmented structural elements like dashed stop lines.
- **Otsu’s Binarization**: An adaptive thresholding technique that automatically calculates the optimal threshold value by maximizing inter-class variance, used here for robust stop line extraction under varying asphalt conditions.
- **Contour Analysis**: Used to identify and group connected pixels into distinct objects, allowing for the calculation of area, bounding boxes, and contact points.
- **Temporal Smoothing**: Techniques such as Exponential Smoothing ($\alpha = 0.05$) for the stop line and Dominant State Buffers for traffic lights are used to stabilize detections across multiple frames.

# CHAPTER 3: SYSTEM IMPLEMENTATION AND EXPERIMENTS
## 3.1 System Architecture
The system follows a modern client-server architecture. The backend is built with **Spring Boot 3.2.3** and **Java 17**, utilizing the **OpenCV 4.9.0** library. To achieve near real-time performance, the system utilizes a parallelized processing pipeline using **Java's ExecutorService**. Each frame is processed by three concurrent tasks: vehicle detection, traffic light recognition, and stop line stabilization.

## 3.2 Dataset / Input Video
The system was tested using high-definition traffic videos (MP4 format) stored in the `uploads` directory. These videos capture typical intersection scenarios with visible traffic lights and stop lines. The inputs vary in lighting and traffic density to test the robustness of the detection algorithms.

## 3.3 Implementation Details
- **Vehicle Detection**: The `MotionDetector` class creates a foreground mask using `BackgroundSubtractorMOG2`. The `ContourVehicleDetector` then extracts contours with an area greater than 450 pixels to filter out small noise.
- **Traffic Light Recognition**: The `TrafficLightDetector` focuses on the upper 50% of the frame. It uses specific HSV ranges for Red, Green, and Yellow. A temporal buffer of size 10 is used to find the dominant state, and specific logic is implemented to handle "Flashing Red" scenarios.
- **Stop Line Detection**: The `StopLineDetector` operates on an ROI between 40% and 75% of the frame height. It uses Otsu thresholding and a horizontal morphological closing to connect line segments. The Y-coordinate is stabilized using an exponential smoothing factor.
- **Violation Logic (Advanced)**: The `ViolationDetectionService` tracks the **bottom-Y coordinate** (vehicle.y + vehicle.height) of each vehicle, representing its contact point with the road. A violation is recorded if a vehicle's bottom-Y moves from above the stop line ($Y \le Y_{stopline}$) to beyond a **10-pixel tolerance zone** ($Y > Y_{stopline} + 10$) while the light is "RED". This accounts for perspective and reduces false positives from vehicles stopping slightly over the line.

## 3.4 Experimental Results
The system successfully identified red light violations in the provided test videos. For each violation, it automatically:
- Captures a cropped image of the offending vehicle's contact point.
- Records the frame number and timestamp.
- Stores the event details in a MySQL database.
Processed videos are generated with visual overlays showing the stabilized stop line (green), current traffic light state, and RED labels for violations.

## 3.5 Evaluation
The system demonstrates high accuracy, especially after transitioning to bottom-Y tracking, which effectively handled camera angles that previously caused false positives with centroid-based logic. The 10-pixel tolerance zone further stabilized the results. The parallel processing architecture improved throughput significantly, allowing the system to handle HD video streams efficiently.

# CHAPTER 4: CONCLUSION AND FUTURE WORK
This project successfully implemented a Red Light Violation Detection System using traditional image processing techniques. By combining background subtraction, adaptive thresholding, and contact-point tracking, the system provides a functional and reliable prototype for automated traffic monitoring. The implementation of parallel task execution and temporal smoothing ensures both performance and stability.
Future work could include:
- Implementing multi-point contact tracking for more precise crossing detection.
- Enhancing the vehicle detector to better handle overlapping vehicles during high-density traffic.
- Adding automated camera calibration to map pixel coordinates to real-world metric distances.

# REFERENCES
1. OpenCV Documentation. (2024). *Image Processing in OpenCV*.
2. Bradski, G., & Kaehler, A. (2008). *Learning OpenCV: Computer Vision with the OpenCV Library*. O'Reilly Media.
3. Spring Framework Documentation. (2024). *Spring Boot Reference Guide*.
4. Otsu, N. (1979). "A Threshold Selection Method from Gray-Level Histograms". *IEEE Transactions on Systems, Man, and Cybernetics*.
5. Java Concurrency in Practice. (2006). *Goetz et al.* (For Parallel Processing details).
