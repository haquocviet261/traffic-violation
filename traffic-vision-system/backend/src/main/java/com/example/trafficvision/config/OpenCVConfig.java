package com.example.trafficvision.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import org.opencv.core.Core;

@Configuration
public class OpenCVConfig {

    @PostConstruct
    public void loadOpenCV() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV native library loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV native library: " + e.getMessage());
            System.err.println("Please ensure the OpenCV native library is in your system's PATH or specified directly.");
            // Handle the error appropriately, perhaps by exiting or throwing an exception
        }
    }
}
