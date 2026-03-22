# ABSTRACT
This report presents a comprehensive technical study and implementation of an automated Red Light Violation Detection System (RLVDS) developed as a university project for the Image Processing subject. The system utilizes traditional computer vision techniques, specifically the OpenCV library within a Java Spring Boot framework, to detect and record traffic violations without the use of deep learning models. The core methodology involves background subtraction for vehicle detection, color-based thresholding in the HSV space for traffic light state recognition, and morphological analysis for stop-line identification. By tracking vehicle centroids across consecutive frames and correlating their positions with the detected stop line and traffic light status, the system identifies vehicles that cross the intersection during a red signal. Experimental results demonstrate that the proposed solution is capable of processing traffic videos to identify violations, calculate traffic density, and provide visual feedback with a high degree of structural stability. The implementation emphasizes real-time-capable processing through parallelized frame analysis, ensuring efficient resource utilization. This report details the fundamental theories, architectural design, implementation specifics, and evaluation of the system, highlighting its potential as a low-cost traffic monitoring solution.

# CHAPTER 1: INTRODUCTION
## 1.1 Problem Statement
Urban traffic management faces significant challenges due to the increasing volume of vehicles and the resulting rise in traffic violations. Red light running is one of the most dangerous traffic infractions, often leading to severe accidents and fatalities. Conventional monitoring systems rely heavily on manual observation or expensive hardware-integrated solutions. This project addresses the need for an automated, software-based system that can reliably detect red light violations from standard video feeds using traditional image processing techniques, thereby providing a scalable and accessible alternative for traffic enforcement.

## 1.2 Motivation
The primary motivation for this project is to leverage computer vision technology to improve road safety. In educational contexts, understanding the application of traditional image processing algorithms—such as thresholding, contour analysis, and background subtraction—is crucial for developing robust engineering solutions. By creating a system that operates without deep learning, we focus on the mathematical foundations of image manipulation and the efficiency of algorithmic implementation, which are essential skills in the field of image processing.

## 1.3 Challenges
Implementing a robust RLVDS using traditional methods presents several technical challenges:
- **Environmental Variability**: Changes in lighting conditions, shadows, and weather can significantly affect color-based detection and background modeling.
- **Dynamic Occlusion**: Vehicles overlapping each other or being partially blocked by infrastructure can lead to fragmented contours and tracking errors.
- **Stop Line Stabilization**: Perspective distortion and camera vibration make it difficult to maintain a consistent reference for the stop line.
- **State Recognition**: Differentiating between various traffic light states (Red, Yellow, Green, and Flashing Red) requires precise color segmentation and temporal smoothing to avoid noise-induced errors.

## 1.4 Related Work
Traditional traffic violation detection systems have historically utilized various sensors, including inductive loops and radar. In the domain of computer vision, researchers have explored techniques such as Optical Flow for motion analysis and Canny edge detection for structural identification. Most classical approaches rely on Background Subtraction (BGS) algorithms like MOG2 (Mixture of Gaussians) for robust foreground extraction. Related studies emphasize the importance of Region of Interest (ROI) selection to optimize processing speed and accuracy in traffic scenarios.

## 1.5 Proposed Solution
The proposed system is a Java-based application that integrates Spring Boot for backend management and OpenCV for image processing. The solution consists of four primary modules:
1. **Vehicle Detection Module**: Employs BackgroundSubtractorMOG2 followed by morphological operations to identify moving objects.
2. **Traffic Light Module**: Uses HSV color masking and contour area analysis to determine the current signal state.
3. **Stop Line Module**: Utilizes Otsu's thresholding and horizontal morphological closing to detect and stabilize the stop line position.
4. **Violation Analysis Module**: Implements centroid tracking and logic-based intersection detection to identify vehicles crossing the stop line during a red signal.

## 1.6 Report Structure
The remainder of this report is organized as follows: Chapter 2 discusses the fundamental image processing theories employed; Chapter 3 details the system architecture, implementation, and experimental results; and Chapter 4 concludes the study with suggestions for future improvements.

# CHAPTER 2: FUNDAMENTAL THEORY
The system relies on several core image processing techniques:
- **Color Space Transformation**: Converting frames from BGR to HSV (Hue, Saturation, Value) is essential for traffic light detection, as HSV is more robust to changes in lighting intensity compared to BGR.
- **Background Subtraction (MOG2)**: The Mixture of Gaussians algorithm is used to model the static background and extract moving foreground objects (vehicles) by calculating the difference between the current frame and the background model.
- **Morphological Operations**: Erosion and Dilation (Opening/Closing) are used to remove noise from binary masks and to connect fragmented structural elements like dashed stop lines.
- **Otsu’s Binarization**: An adaptive thresholding technique that automatically calculates the optimal threshold value by maximizing inter-class variance, used here for stop line extraction.
- **Contour Analysis**: Used to identify and group connected pixels into distinct objects, allowing for the calculation of area, bounding boxes, and centroids.
- **Temporal Smoothing**: A technique used to stabilize detections across multiple frames using exponential moving averages or dominant state buffers, reducing flickering and transient errors.

# CHAPTER 3: SYSTEM IMPLEMENTATION AND EXPERIMENTS
## 3.1 System Architecture
The system follows a modern client-server architecture. The backend is built with **Spring Boot 3.2.3** and **Java 17**, utilizing the **OpenCV 4.9.0** library for all vision tasks. Data persistence is managed via JPA with a MySQL database. The video processing pipeline is parallelized using Java's `ExecutorService`, allowing the system to perform vehicle detection, traffic light recognition, and stop line stabilization concurrently on multiple threads.

## 3.2 Dataset / Input Video
The system was tested using high-definition traffic videos (MP4 format) stored in the `uploads` directory. These videos capture typical intersection scenarios with visible traffic lights and stop lines. The inputs vary in lighting and traffic density to test the robustness of the detection algorithms.

## 3.3 Implementation Details
- **Vehicle Detection**: The `MotionDetector` class creates a foreground mask using `BackgroundSubtractorMOG2`. The `ContourVehicleDetector` then extracts contours with an area greater than 450 pixels to filter out small noise.
- **Traffic Light Recognition**: The `TrafficLightDetector` focuses on the upper 50% of the frame. It uses specific HSV ranges for Red, Green, and Yellow. A temporal buffer of size 10 is used to find the dominant state, ensuring stability during signal transitions.
- **Stop Line Detection**: The `StopLineDetector` operates on an ROI between 40% and 75% of the frame height. It uses a 60x3 horizontal kernel to connect line segments and applies an exponential smoothing factor ($\alpha = 0.05$) to the Y-coordinate.
- **Violation Logic**: The `ViolationDetectionService` tracks the centroid of each vehicle. A violation is recorded if a vehicle's centroid moves from a position $Y \le Y_{stopline}$ to $Y > Y_{stopline}$ while the traffic light state is "RED".

## 3.4 Experimental Results
The system successfully identified red light violations in the provided test videos. For each violation, it automatically:
- Captures a cropped image of the offending vehicle.
- Records the frame number and timestamp.
- Stores the event details in the database.
Processed videos are generated with visual overlays showing the detected stop line (green), traffic light state, and bounding boxes for detected vehicles.

## 3.5 Evaluation
The system demonstrates high accuracy in controlled environments with clear visibility. The use of Otsu's thresholding successfully adapts to varying asphalt colors for stop line detection. However, some false positives occur when large vehicles partially occlude the traffic light or when shadows are incorrectly identified as moving objects. The parallel processing architecture significantly improves throughput, maintaining a consistent frame rate during analysis.

# CHAPTER 4: CONCLUSION AND FUTURE WORK
This project successfully implemented a Red Light Violation Detection System using traditional image processing techniques. By combining background subtraction, color segmentation, and structural analysis, the system provides a functional prototype for automated traffic monitoring. The use of Java and Spring Boot ensures a robust and scalable backend capable of handling multiple video streams.
Future work could include:
- Implementing a multi-camera calibration system to handle perspective more accurately.
- Enhancing vehicle tracking with Kalman Filters to better handle occlusions.
- Integrating more advanced background modeling techniques to reduce the impact of dynamic shadows and weather conditions.

# REFERENCES
1. OpenCV Documentation. (2024). *Image Processing in OpenCV*.
2. Bradski, G., & Kaehler, A. (2008). *Learning OpenCV: Computer Vision with the OpenCV Library*. O'Reilly Media.
3. Spring Framework Documentation. (2024). *Spring Boot Reference Guide*.
4. Otsu, N. (1979). "A Threshold Selection Method from Gray-Level Histograms". *IEEE Transactions on Systems, Man, and Cybernetics*.
