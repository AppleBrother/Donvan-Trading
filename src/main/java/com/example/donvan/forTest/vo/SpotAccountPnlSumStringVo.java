package com.example.donvan.forTest.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotAccountPnlSumStringVo {
    private String spotVolume;
    private String averageOpenPrice;

    public String getSpotVolume() {
        return spotVolume;
    }

    public void setSpotVolume(String spotVolume) {
        this.spotVolume = spotVolume;
    }

    public String getAverageOpenPrice() {
        return averageOpenPrice;
    }

    public void setAverageOpenPrice(String averageOpenPrice) {
        this.averageOpenPrice = averageOpenPrice;
    }
}

