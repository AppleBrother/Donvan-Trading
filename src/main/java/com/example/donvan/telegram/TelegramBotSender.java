package com.example.donvan.telegram;

import com.example.donvan.forTest.vo.MonitorConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class TelegramBotSender {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_BOT_TOKEN = MonitorConstants.TELEGRAM_BOT_TOKEN;
    private static final String DEFAULT_CHAT_ID = MonitorConstants.TELEGRAM_CHAT_ID;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String telegramApiBaseUrl;

    public TelegramBotSender() {
        this(new ObjectMapper(), HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build(), "https://api.telegram.org");
    }

    TelegramBotSender(ObjectMapper objectMapper, HttpClient httpClient, String telegramApiBaseUrl) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.telegramApiBaseUrl = normalizeBaseUrl(telegramApiBaseUrl);
    }

    public void sendMessage(String botToken, String chatId, String text) {
        String normalizedBotToken = requireText(botToken, "botToken");
        String normalizedChatId = requireText(chatId, "chatId");
        String normalizedText = requireText(text, "text");

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("chat_id", normalizedChatId);
            payload.put("text", normalizedText);

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(buildSendMessageUri(normalizedBotToken))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            validateResponse(response);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("发送 Telegram 消息失败: " + safeMessage(e.getMessage()), e);
        }
    }

    public static void main(String[] args) {
        RuntimeConfig config = RuntimeConfig.from(args);
        System.out.println("准备发送 Telegram 消息，chatId=" + config.chatId());
        new TelegramBotSender().sendMessage(config.botToken(), config.chatId(), config.text());
        System.out.println("Telegram 消息发送成功，chatId=" + config.chatId());
    }

    private URI buildSendMessageUri(String botToken) {
        return URI.create(telegramApiBaseUrl + "/bot" + botToken + "/sendMessage");
    }

    private void validateResponse(HttpResponse<String> response) throws Exception {
        String responseBody = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Telegram 接口返回异常，HTTP " + response.statusCode() + ", body=" + safeBody(responseBody));
        }

        TelegramResponse telegramResponse = objectMapper.readValue(responseBody, TelegramResponse.class);
        if (telegramResponse == null || !Boolean.TRUE.equals(telegramResponse.ok)) {
            String description = telegramResponse == null ? "empty response" : safeMessage(telegramResponse.description);
            Integer errorCode = telegramResponse == null ? null : telegramResponse.errorCode;
            throw new IllegalStateException("Telegram 接口调用失败，errorCode=" + errorCode + ", description=" + description);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = requireText(baseUrl, "telegramApiBaseUrl");
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }

    private String safeBody(String body) {
        if (body == null) {
            return "null";
        }
        String trimmed = body.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "...";
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "unknown" : message.trim();
    }

    private record RuntimeConfig(String botToken, String chatId, String text) {

        private static RuntimeConfig from(String[] args) {
            String botToken = firstNonBlank(
                    systemProperty("telegram.bot-token"),
                    environment("TELEGRAM_BOT_TOKEN"),
                    arg(args, 0),
                    DEFAULT_BOT_TOKEN
            );
            String chatId = firstNonBlank(
                    systemProperty("telegram.chat-id"),
                    environment("TELEGRAM_CHAT_ID"),
                    arg(args, 1),
                    DEFAULT_CHAT_ID
            );
            String text = firstNonBlank(
                    systemProperty("telegram.text"),
                    environment("TELEGRAM_TEXT"),
                    remainingArgs(args, 2),
                    "Donvan Telegram 测试消息 - " + LocalDateTime.now().format(TIME_FORMATTER)
            );

            return new RuntimeConfig(botToken.trim(), chatId.trim(), text.trim());
        }

        private static String systemProperty(String key) {
            return System.getProperty(key);
        }

        private static String environment(String key) {
            return System.getenv(key);
        }

        private static String arg(String[] args, int index) {
            return args != null && args.length > index ? args[index] : null;
        }

        private static String remainingArgs(String[] args, int fromIndex) {
            if (args == null || args.length <= fromIndex) {
                return null;
            }
            StringBuilder textBuilder = new StringBuilder();
            for (int i = fromIndex; i < args.length; i++) {
                if (args[i] == null || args[i].isBlank()) {
                    continue;
                }
                if (!textBuilder.isEmpty()) {
                    textBuilder.append(' ');
                }
                textBuilder.append(args[i].trim());
            }
            return textBuilder.isEmpty() ? null : textBuilder.toString();
        }

        private static String firstNonBlank(String... values) {
            if (values == null) {
                return null;
            }
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TelegramResponse {
        public Boolean ok;
        public String description;
        @JsonProperty("error_code")
        public Integer errorCode;
    }
}

