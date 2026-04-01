package com.example.donvan.forTest.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesAccountTotalPnlVo {
    private FuturesAccountPnlSumStringVo longAccountTotalPnl;
    private FuturesAccountPnlSumStringVo shortAccountTotalPnl;

    public FuturesAccountPnlSumStringVo getLongAccountTotalPnl() {
        return longAccountTotalPnl;
    }

    public void setLongAccountTotalPnl(FuturesAccountPnlSumStringVo longAccountTotalPnl) {
        this.longAccountTotalPnl = longAccountTotalPnl;
    }

    public FuturesAccountPnlSumStringVo getShortAccountTotalPnl() {
        return shortAccountTotalPnl;
    }

    public void setShortAccountTotalPnl(FuturesAccountPnlSumStringVo shortAccountTotalPnl) {
        this.shortAccountTotalPnl = shortAccountTotalPnl;
    }
}

