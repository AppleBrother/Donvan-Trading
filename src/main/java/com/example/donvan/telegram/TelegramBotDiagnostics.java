package com.example.donvan.telegram;

import com.example.donvan.forTest.vo.MonitorConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

public class TelegramBotDiagnostics {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public static void main(String[] args) {
        String botToken = firstNonBlank(
                System.getProperty("telegram.bot-token"),
                System.getenv("TELEGRAM_BOT_TOKEN"),
                args != null && args.length > 0 ? args[0] : null,
                MonitorConstants.TELEGRAM_BOT_TOKEN
        );
        new TelegramBotDiagnostics().run(botToken.trim());
    }

    public void run(String botToken) {
        try {
            JsonNode me = getJson(botToken, "getMe");
            System.out.println("Bot 校验成功: id=" + me.path("result").path("id").asText()
                    + ", username=@" + me.path("result").path("username").asText());

            JsonNode updates = getJson(botToken, "getUpdates");
            JsonNode results = updates.path("result");
            if (!results.isArray() || results.isEmpty()) {
                System.out.println("最近没有 getUpdates 记录。请先在 Telegram 里给机器人发送 /start 或任意消息，然后再运行一次。");
                return;
            }

            Set<String> chatSummaries = new LinkedHashSet<>();
            for (JsonNode item : results) {
                JsonNode chat = item.path("message").path("chat");
                if (chat.isMissingNode() || chat.isEmpty()) {
                    chat = item.path("channel_post").path("chat");
                }
                if (chat.isMissingNode() || chat.isEmpty()) {
                    continue;
                }
                String chatId = chat.path("id").asText("");
                String type = chat.path("type").asText("unknown");
                String title = firstNonBlank(chat.path("title").asText(null), chat.path("username").asText(null), chat.path("first_name").asText(null), "unknown");
                if (!chatId.isBlank()) {
                    chatSummaries.add("chatId=" + chatId + ", type=" + type + ", name=" + title);
                }
            }

            if (chatSummaries.isEmpty()) {
                System.out.println("已获取到 updates，但没有解析出可用 chatId。可以把控制台输出贴给我，我继续帮你看。");
                return;
            }

            System.out.println("最近可见的 chat 信息：");
            for (String summary : chatSummaries) {
                System.out.println("- " + summary);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Telegram 诊断失败: " + e.getMessage(), e);
        }
    }

    private JsonNode getJson(String botToken, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + botToken + "/" + method))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ", body=" + safeBody(response.body()));
        }

        JsonNode json = objectMapper.readTree(response.body());
        if (!json.path("ok").asBoolean(false)) {
            throw new IllegalStateException("errorCode=" + json.path("error_code").asText()
                    + ", description=" + json.path("description").asText());
        }
        return json;
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

    private String safeBody(String body) {
        if (body == null) {
            return "null";
        }
        String trimmed = body.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "...";
    }
}

