package com.example.donvan.forTest.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesAccountPnlSumStringVo {
    private String contractCostAmount;
    private String averageOpenPrice;

    public String getContractCostAmount() {
        return contractCostAmount;
    }

    public void setContractCostAmount(String contractCostAmount) {
        this.contractCostAmount = contractCostAmount;
    }

    public String getAverageOpenPrice() {
        return averageOpenPrice;
    }

    public void setAverageOpenPrice(String averageOpenPrice) {
        this.averageOpenPrice = averageOpenPrice;
    }
}
