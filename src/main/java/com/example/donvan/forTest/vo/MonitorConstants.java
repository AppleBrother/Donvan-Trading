package com.example.donvan.forTest.vo;

import java.util.List;

public class MonitorConstants {

    private MonitorConstants() {
    }

    public static final String TOKEN_PLACEHOLDER = "REPLACE_WITH_YOUR_TOKEN";
    public static final String DEFAULT_MODE = "public";
    public static final String CLIENT_ID = "ios";
    public static final String CLIENT_SECRET = "bcdefghijklmnopqrstuvwxyz12345";
    public static final String DEFAULT_TELEGRAM_BOT_TOKEN = "8765298980:AAGi7Rl6fSn3m4FkUnxLDDG2N2p1G_6rdwM";
    public static final String DEFAULT_TELEGRAM_CHAT_ID = "-5251109574";
    public static final String DEFAULT_TELEGRAM_CHAT_ID_2 = "-5124288203";
    public static final List<String> DEFAULT_TELEGRAM_CHAT_IDS = List.of(
            DEFAULT_TELEGRAM_CHAT_ID_2
    );
    public static final String TELEGRAM_BOT_TOKEN = firstNonBlank(
            System.getProperty("monitor.telegram.bot-token"),
            System.getenv("MONITOR_TELEGRAM_BOT_TOKEN"),
            System.getProperty("telegram.bot-token"),
            System.getenv("TELEGRAM_BOT_TOKEN"),
            DEFAULT_TELEGRAM_BOT_TOKEN
    );
    public static final String TELEGRAM_CHAT_ID = firstNonBlank(
            System.getProperty("monitor.telegram.chat-id"),
            System.getenv("MONITOR_TELEGRAM_CHAT_ID"),
            System.getProperty("telegram.chat-id"),
            System.getenv("TELEGRAM_CHAT_ID"),
            DEFAULT_TELEGRAM_CHAT_ID
    );
    public static final List<String> TELEGRAM_CHAT_IDS = resolveTelegramChatIds(
            System.getProperty("monitor.telegram.chat-ids"),
            System.getenv("MONITOR_TELEGRAM_CHAT_IDS"),
            System.getProperty("telegram.chat-ids"),
            System.getenv("TELEGRAM_CHAT_IDS"),
            System.getProperty("monitor.telegram.chat-id"),
            System.getenv("MONITOR_TELEGRAM_CHAT_ID"),
            System.getProperty("telegram.chat-id"),
            System.getenv("TELEGRAM_CHAT_ID")
    );
    public static final long LOOK_BACK_MINUTES = 30L;
    public static final long LOOK_AHEAD_MINUTES = 10L;
    public static final long FAILURE_NOTIFY_COOLDOWN_MINUTES = 10L;
    public static final long CONNECT_TIMEOUT_SECONDS = 5L;
    public static final long REQUEST_TIMEOUT_SECONDS = 10L;
    public static final long INITIAL_DELAY_MILLIS = 5_000L;
    public static final long FIXED_DELAY_MILLIS = 60_000L;
    public static final String DEFAULT_LARK_WEBHOOK_URL = "https://open.feishu.cn/open-apis/bot/v2/hook/3dafb188-b512-4313-a313-4a7ec4da3d03";
    public static final String DEFAULT_LARK_WEBHOOK_URL_2 = "https://open.feishu.cn/open-apis/bot/v2/hook/4ebcd433-2bd5-4d9d-a6ef-0339c9137e72";
    public static final List<String> DEFAULT_LARK_WEBHOOK_URLS = List.of(
            DEFAULT_LARK_WEBHOOK_URL,
            DEFAULT_LARK_WEBHOOK_URL_2
    );
    public static final String ENABLED_PROJECTS_API_URL = "https://trade.coinr.vip/api/v1/user/projects/enabled?type=futures";

    public static final class Futures {
        public static final boolean ENABLED = true;
        public static final String MODE = DEFAULT_MODE;
        public static final String API_BASE_URL = "https://trade.coinr.vip/api/v1/assets/futures-pnl-details";
        public static final String LARK_WEBHOOK_URL = DEFAULT_LARK_WEBHOOK_URL;
        public static final List<String> LARK_WEBHOOK_URLS = DEFAULT_LARK_WEBHOOK_URLS;
        public static final String TELEGRAM_BOT_TOKEN = firstNonBlank(
                System.getProperty("monitor.futures.telegram.bot-token"),
                System.getenv("MONITOR_FUTURES_TELEGRAM_BOT_TOKEN"),
                MonitorConstants.TELEGRAM_BOT_TOKEN
        );
        public static final String TELEGRAM_CHAT_ID = firstNonBlank(
                System.getProperty("monitor.futures.telegram.chat-id"),
                System.getenv("MONITOR_FUTURES_TELEGRAM_CHAT_ID"),
                MonitorConstants.TELEGRAM_CHAT_ID
        );
        public static final List<String> TELEGRAM_CHAT_IDS = resolveTelegramChatIds(
                System.getProperty("monitor.futures.telegram.chat-ids"),
                System.getenv("MONITOR_FUTURES_TELEGRAM_CHAT_IDS"),
                System.getProperty("monitor.futures.telegram.chat-id"),
                System.getenv("MONITOR_FUTURES_TELEGRAM_CHAT_ID"),
                String.join(",", MonitorConstants.TELEGRAM_CHAT_IDS)
        );
        public static final String ACCESS_TOKEN = "57:1775113169403:BENFmt1USXlCEXVIeQspjr4NknFuqSsIskIUl1_R9yo:7389b81e5f2d07a071d6b57ef99e239d5f134aa89952c0016013db36f15b45b9";

        private Futures() {
        }
    }

    public static final class Spot {
        public static final boolean ENABLED = true;
        public static final String MODE = DEFAULT_MODE;
        public static final String API_BASE_URL = "https://trade.coinr.vip/api/v1/assets/spot-pnl-details";
        public static final String LARK_WEBHOOK_URL = DEFAULT_LARK_WEBHOOK_URL;
        public static final List<String> LARK_WEBHOOK_URLS = DEFAULT_LARK_WEBHOOK_URLS;
        public static final String TELEGRAM_BOT_TOKEN = firstNonBlank(
                System.getProperty("monitor.spot.telegram.bot-token"),
                System.getenv("MONITOR_SPOT_TELEGRAM_BOT_TOKEN"),
                MonitorConstants.TELEGRAM_BOT_TOKEN
        );
        public static final String TELEGRAM_CHAT_ID = firstNonBlank(
                System.getProperty("monitor.spot.telegram.chat-id"),
                System.getenv("MONITOR_SPOT_TELEGRAM_CHAT_ID"),
                MonitorConstants.TELEGRAM_CHAT_ID
        );
        public static final List<String> TELEGRAM_CHAT_IDS = resolveTelegramChatIds(
                System.getProperty("monitor.spot.telegram.chat-ids"),
                System.getenv("MONITOR_SPOT_TELEGRAM_CHAT_IDS"),
                System.getProperty("monitor.spot.telegram.chat-id"),
                System.getenv("MONITOR_SPOT_TELEGRAM_CHAT_ID"),
                String.join(",", MonitorConstants.TELEGRAM_CHAT_IDS)
        );
        public static final String ACCESS_TOKEN = "57:1775113169403:BENFmt1USXlCEXVIeQspjr4NknFuqSsIskIUl1_R9yo:7389b81e5f2d07a071d6b57ef99e239d5f134aa89952c0016013db36f15b45b9";

        private Spot() {
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static List<String> resolveTelegramChatIds(String... values) {
        if (values != null) {
            for (String value : values) {
                List<String> chatIds = splitAndTrim(value);
                if (!chatIds.isEmpty()) {
                    return chatIds;
                }
            }
        }
        return DEFAULT_TELEGRAM_CHAT_IDS;
    }

    private static List<String> splitAndTrim(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }
}
