package com.example.trafficvision.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LineDto {
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public LineDto() {
    }

    public LineDto(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }


}
