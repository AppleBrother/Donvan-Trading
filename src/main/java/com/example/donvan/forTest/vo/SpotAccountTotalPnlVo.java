package com.example.donvan.forTest.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotAccountTotalPnlVo {
    private SpotAccountPnlSumStringVo accountTotalPnl;

    public SpotAccountPnlSumStringVo getAccountTotalPnl() {
        return accountTotalPnl;
    }

    public void setAccountTotalPnl(SpotAccountPnlSumStringVo accountTotalPnl) {
        this.accountTotalPnl = accountTotalPnl;
    }
}

