package com.example.trafficvision.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RectDto {
    private int x;
    private int y;
    private int width;
    private int height;

    public RectDto() {
    }

    public RectDto(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


}
