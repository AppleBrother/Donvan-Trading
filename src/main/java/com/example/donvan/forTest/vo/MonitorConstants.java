package com.example.donvan.forTest.vo;

import java.util.List;

public class MonitorConstants {

    private MonitorConstants() {
    }

    public static final String TOKEN_PLACEHOLDER = "REPLACE_WITH_YOUR_TOKEN";
    public static final String DEFAULT_MODE = "public";
    public static final String CLIENT_ID = "ios";
    public static final String CLIENT_SECRET = "bcdefghijklmnopqrstuvwxyz12345";
    public static final long LOOK_BACK_MINUTES = 30L;
    public static final long LOOK_AHEAD_MINUTES = 10L;
    public static final long FAILURE_NOTIFY_COOLDOWN_MINUTES = 10L;
    public static final long CONNECT_TIMEOUT_SECONDS = 5L;
    public static final long REQUEST_TIMEOUT_SECONDS = 10L;
    public static final long INITIAL_DELAY_MILLIS = 5_000L;
    public static final long FIXED_DELAY_MILLIS = 60_000L;

    public static final class Futures {
        public static final boolean ENABLED = true;
        public static final String MODE = DEFAULT_MODE;
        public static final String API_BASE_URL = "https://trade.coinr.vip/api/v1/assets/futures-pnl-details";
        /** 支持多个项目，例如：List.of(57L, 58L, 59L) */
        public static final List<Long> PROJECT_IDS = List.of(56L);
        public static final String LARK_WEBHOOK_URL = "https://open.larksuite.com/open-apis/bot/v2/hook/60716897-c5c7-4c2e-966e-11e8f5a7a170";
        public static final String ACCESS_TOKEN = "57:1774860943456:8jq5BPBZFCLhiHgk_Zk27BgjHsw4nwYai6tuZOTJ76s:b53ef3172cf6639e34f048038d8f6f3a8e08826c1171887b40ff5392d02c9ac4";

        private Futures() {
        }
    }

    public static final class Spot {
        public static final boolean ENABLED = true;
        public static final String MODE = DEFAULT_MODE;
        public static final String API_BASE_URL = "https://trade.coinr.vip/api/v1/assets/spot-pnl-details";
        /** 支持多个项目，例如：List.of(57L, 58L, 59L) */
        public static final List<Long> PROJECT_IDS = List.of(56L);
        public static final String LARK_WEBHOOK_URL = "https://open.larksuite.com/open-apis/bot/v2/hook/60716897-c5c7-4c2e-966e-11e8f5a7a170";
        public static final String ACCESS_TOKEN = "57:1774860943456:8jq5BPBZFCLhiHgk_Zk27BgjHsw4nwYai6tuZOTJ76s:b53ef3172cf6639e34f048038d8f6f3a8e08826c1171887b40ff5392d02c9ac4";

        private Spot() {
        }
    }
}
