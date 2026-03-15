package com.example.trafficvision.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadResponse {
    private String message;
    private Long videoId;

    public UploadResponse(String message, Long videoId) {
        this.message = message;
        this.videoId = videoId;
    }


}
