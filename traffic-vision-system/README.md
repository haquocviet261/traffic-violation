# Traffic Vision System

This is a full-stack web application for traffic video analysis using classical computer vision techniques with OpenCV.

## Project Structure

The project is a monorepo containing a Spring Boot backend and a React frontend.

```
traffic-vision-system/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/trafficvision/
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── opencv/
│       │   │   ├── model/
│       │   │   ├── repository/
│       │   │   ├── dto/
│       │   │   └── TrafficVisionApplication.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── schema.sql
│       └── test/
└── frontend/
    ├── package.json
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── pages/
        ├── components/
        ├── services/
        └── styles/
            └── app.css
```

## Tech Stack

*   **Frontend:** React (functional components + hooks), JavaScript, HTML5 Video + Canvas API, Chart.js
*   **Backend:** Spring Boot (Java 17), Maven, OpenCV Java bindings, REST API
*   **Database:** MySQL (local instance)

## Application Goal

The web application allows users to upload a traffic intersection video and performs classical image processing to:

1.  Detect moving vehicles
2.  Detect traffic light state (red / yellow / green)
3.  Detect vehicles crossing a stop line when the light is red
4.  Compute traffic statistics

## Local Setup Instructions

Follow these steps to get the application running on your local machine.

### Prerequisites

*   **Java Development Kit (JDK) 17 or higher:** [Download JDK](https://www.oracle.com/java/technologies/downloads/)
*   **Maven:** [Install Maven](https://maven.apache.org/install.html)
*   **Node.js and npm:** [Install Node.js (includes npm)](https://nodejs.org/en/download/)
*   **MySQL Server:** [Install MySQL](https://dev.mysql.com/doc/refman/8.0/en/installing.html)
*   **OpenCV:** You will need to install OpenCV and configure its native libraries for Java.

### 1. Install MySQL and Create Database

1.  **Install MySQL Server** if you haven't already.
2.  **Start MySQL Server.**
3.  **Connect to MySQL** as a user with sufficient privileges (e.g., `root`).
4.  **Run the `schema.sql` script** to create the database and tables.
    You can typically do this from your MySQL client:
    ```bash
    mysql -u root -p < traffic-vision-system/backend/src/main/resources/schema.sql
    ```
    (Replace `root` with your MySQL username if different, and enter your password when prompted.)

    Alternatively, you can manually create the database and then run the `schema.sql` content:
    ```sql
    CREATE DATABASE IF NOT EXISTS traffic_vision_db;
    USE traffic_vision_db;
    -- Then copy-paste the content of backend/src/main/resources/schema.sql
    ```
5.  **Update `application.yml`**: Ensure the `spring.datasource.username` and `spring.datasource.password` in `traffic-vision-system/backend/src/main/resources/application.yml` match your MySQL credentials.

### 2. Configure OpenCV Native Library

The Spring Boot backend uses OpenCV Java bindings, which require loading a native library. The exact path to this library depends on your operating system and OpenCV installation.

**General Steps:**

1.  **Install OpenCV:** Download and install OpenCV for your operating system. For Windows, you can download the pre-built binaries. For Linux/macOS, you might use a package manager (e.g., `brew install opencv` for macOS, or build from source).
2.  **Locate the Native Library:**
    *   **Windows:** Look for `opencv_javaXXX.dll` (e.g., `opencv_java490.dll`) in your OpenCV installation directory, typically `[OpenCV_DIR]\build\java\x64`.
    *   **Linux/macOS:** Look for `libopencv_javaXXX.so` (Linux) or `libopencv_javaXXX.dylib` (macOS) in `[OpenCV_DIR]/build/lib`.
3.  **Load the Library:**
    There are a few ways to ensure the JVM can find the native library:
    *   **Option A (Recommended for Development): Add to `PATH` (Windows) or `LD_LIBRARY_PATH`/`DYLD_LIBRARY_PATH` (Linux/macOS):**
        Add the directory containing the native library to your system's `PATH` environment variable (Windows) or `LD_LIBRARY_PATH`/`DYLD_LIBRARY_PATH` (Linux/macOS).
        *   **Windows Example:** Add `C:\opencv\build\java\x64` to your system `PATH`.
        *   **Linux/macOS Example:**
            ```bash
            export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/share/opencv/java
            # Or for macOS
            export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:/usr/local/share/opencv/java
            ```
            (Adjust path as necessary based on your OpenCV installation).
    *   **Option B (JVM Argument):** When running the Java application, you can specify the library path using `-Djava.library.path`:
        ```bash
        java -Djava.library.path="[PATH_TO_OPENCV_NATIVE_LIB_DIR]" -jar target/traffic-vision-0.0.1-SNAPSHOT.jar
        ```
    *   **Option C (Direct `System.load` in `OpenCVConfig.java`):** You can uncomment and modify the `System.load()` line in `backend/src/main/java/com/example/trafficvision/config/OpenCVConfig.java` with the absolute path to your native library. This is less flexible but works.

### 3. Run the Spring Boot Backend

1.  Navigate to the `backend` directory:
    ```bash
    cd traffic-vision-system/backend
    ```
2.  Build the project using Maven:
    ```bash
    mvn clean install
    ```
3.  Run the application:
    ```bash
    mvn spring-boot:run
    ```
    The backend should start on `http://localhost:8080`.

### 4. Run the React Frontend

1.  Navigate to the `frontend` directory:
    ```bash
    cd traffic-vision-system/frontend
    ```
2.  Install the Node.js dependencies:
    ```bash
    npm install
    ```
3.  Start the React development server:
    ```bash
    npm start
    ```
    The frontend should open in your browser, typically at `http://localhost:3000`.

## Usage

1.  **Upload Video:** Go to the "Upload Video" page, select a traffic video file (e.g., MP4), and click "Upload Video."
2.  **View Analysis:** After uploading, you will be redirected to the analysis page for that video. The backend will process the video in the background. The page will automatically poll for status updates and display results once processing is complete.
3.  **Dashboard:** Visit the "Dashboard" page to see aggregated traffic statistics (currently with placeholder data).

---

**Note:** This is a starter project. The OpenCV processing logic in the backend services is currently skeletal and will need further implementation to perform the full video analysis as described in the requirements. The frontend provides basic UI elements and API integration.
