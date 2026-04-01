package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.FuturesAccountPnlSumStringVo;
import com.example.donvan.forTest.vo.FuturesAccountTotalPnlVo;
import com.example.donvan.forTest.vo.MonitorConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CoinrFuturesPnlVolumeMonitor {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int HTTP_RETRY_TIMES = 3;
    private static final long HTTP_RETRY_BACKOFF_MILLIS = 800L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;
    private final Map<Long, VolumeSnapshot> lastSnapshots = new ConcurrentHashMap<>();
    private final Set<Long> startupNotifiedProjectIds = ConcurrentHashMap.newKeySet();
    private final Map<Long, String> authFailureReasonByProject = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Long>> nonAuthFailureNotifyAt = new ConcurrentHashMap<>();
    private volatile String lastProjectConfigError;


    @jakarta.annotation.PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(MonitorConstants.CONNECT_TIMEOUT_SECONDS))
                .version(HttpClient.Version.HTTP_1_1)
                .sslParameters(buildSslParameters())
                .build();
    }

    @Scheduled(
            initialDelay = MonitorConstants.INITIAL_DELAY_MILLIS,
            fixedDelay = MonitorConstants.FIXED_DELAY_MILLIS
    )
    public synchronized void pollOpenTradeVolume() {
        if (!MonitorConstants.Futures.ENABLED) {
            return;
        }
        List<Long> projectIds = resolveProjectIds();
        if (projectIds.isEmpty()) {
            return;
        }
        if (isPublicMode() && isTokenMissing()) {
            for (Long projectId : projectIds) {
                handleAuthFailure(projectId, "CoinrPnlMonitorConstants.Futures.ACCESS_TOKEN 尚未配置，请先在常量类中填写有效 token");
            }
            return;
        }

        RequestWindow requestWindow = buildRequestWindow();
        for (Long projectId : projectIds) {
            pollProject(projectId, requestWindow);
        }
    }

    private List<Long> resolveProjectIds() {
        List<Long> projectIds = MonitorConstants.Futures.PROJECT_IDS;
        if (projectIds.isEmpty()) {
            String reason = "CoinrPnlMonitorConstants.Futures.PROJECT_IDS must not be empty";
            if (!Objects.equals(lastProjectConfigError, reason)) {
                lastProjectConfigError = reason;
                sendLarkText("合约监控配置错误\n"
                        + "时间: " + nowText() + "\n"
                        + "原因: " + reason);
            }
            return List.of();
        }
        lastProjectConfigError = null;
        return projectIds;
    }

    private void pollProject(Long projectId, RequestWindow requestWindow) {
        try {
            FetchResult buyResult = fetchOpenTradeVolume(projectId, requestWindow, Side.BUY);
            if (!buyResult.success()) {
                handleFetchFailure(projectId, Side.BUY, buyResult);
                return;
            }

            FetchResult sellResult = fetchOpenTradeVolume(projectId, requestWindow, Side.SELL);
            if (!sellResult.success()) {
                handleFetchFailure(projectId, Side.SELL, sellResult);
                return;
            }

            resetAuthFailureState(projectId);

            VolumeSnapshot currentSnapshot = new VolumeSnapshot(
                    new SideSnapshot(buyResult.contractCostAmount(), buyResult.averageOpenPrice()),
                    new SideSnapshot(sellResult.contractCostAmount(), sellResult.averageOpenPrice()),
                    requestWindow
            );
            VolumeSnapshot previousSnapshot = lastSnapshots.get(projectId);
            if (previousSnapshot == null) {
                lastSnapshots.put(projectId, currentSnapshot);
                clearNonAuthFailureState(projectId);
                notifyMonitorStarted(projectId, currentSnapshot);
                return;
            }

            boolean buyChanged = changedNullableDecimal(previousSnapshot.buy().contractCostAmount(), currentSnapshot.buy().contractCostAmount());
            boolean sellChanged = changedNullableDecimal(previousSnapshot.sell().contractCostAmount(), currentSnapshot.sell().contractCostAmount());
            if (buyChanged || sellChanged) {
                notifyVolumeChanged(projectId, previousSnapshot, currentSnapshot, buyChanged, sellChanged);
            }

            clearNonAuthFailureState(projectId);
            lastSnapshots.put(projectId, currentSnapshot);
        } catch (Exception e) {
            notifyNonAuthFailure(projectId, null, e.getClass().getSimpleName() + ": " + safeMessage(e.getMessage()));
        }
    }

    private FetchResult fetchOpenTradeVolume(Long projectId, RequestWindow requestWindow, Side side) {
        return fetchOpenTradeVolumeByPublicApi(projectId, requestWindow, side);
    }

    private FetchResult fetchOpenTradeVolumeByPublicApi(Long projectId, RequestWindow requestWindow, Side side) {
        try {
            String requestUrl = buildPublicRequestUrl(projectId, requestWindow, side);
            HttpRequest request = buildPublicGetRequest(requestUrl)
                    .timeout(Duration.ofSeconds(MonitorConstants.REQUEST_TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = sendRequestWithRetry(request, "Futures " + side.displayName() + " public API");
            String responseBody = response.body();
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return FetchResult.authFailure("HTTP " + response.statusCode() + ", body=" + safeBody(responseBody));
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return FetchResult.failure("HTTP " + response.statusCode() + ", body=" + safeBody(responseBody));
            }

            PublicApiResponse apiResponse = objectMapper.readValue(responseBody, PublicApiResponse.class);
            if (apiResponse == null) {
                return FetchResult.failure(side + " public response is null");
            }
            if (!Objects.equals(apiResponse.code, 0)) {
                String reason = "code=" + apiResponse.code + ", message=" + apiResponse.message;
                if (Objects.equals(apiResponse.code, 2001) || isAuthFailureMessage(apiResponse.message)) {
                    return FetchResult.authFailure(reason);
                }
                return FetchResult.failure(reason);
            }
            if (apiResponse.data == null) {
                return FetchResult.failure(side + " public response data is null");
            }

            FuturesAccountPnlSumStringVo pnlNode = side == Side.BUY
                    ? apiResponse.data.getLongAccountTotalPnl()
                    : apiResponse.data.getShortAccountTotalPnl();
            BigDecimal contractCostAmount = pnlNode == null ? null : parseOptionalDecimal(pnlNode.getContractCostAmount());
            if (pnlNode == null || contractCostAmount == null) {
                return FetchResult.failure(side + " contractCostAmount is empty");
            }
            return FetchResult.success(contractCostAmount, parseOptionalDecimal(pnlNode.getAverageOpenPrice()));
        } catch (Exception e) {
            return FetchResult.failure(e.getClass().getSimpleName() + ": " + safeMessage(e.getMessage()));
        }
    }

    private HttpRequest.Builder buildPublicGetRequest(String url) {
        URI uri = URI.create(url);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String signature = hmacSha256Hex(buildNormalizedGetString(uri, timestamp, nonce), MonitorConstants.CLIENT_SECRET);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("X-Client-Id", MonitorConstants.CLIENT_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature);

        String tokenHeader = buildLoginTokenHeader();
        if (!tokenHeader.isBlank()) {
            builder.header("X-Token", tokenHeader);
            builder.header("Cookie", buildCookieHeader(tokenHeader));
            builder.header("Authorization", "Bearer " + tokenHeader);
        }
        return builder;
    }

    private String buildPublicRequestUrl(Long projectId, RequestWindow requestWindow, Side side) {
        return MonitorConstants.Futures.API_BASE_URL
                + "/" + side.pathSegment()
                + "?projectId=" + projectId
                + "&startTime=" + requestWindow.startTimeMillis()
                + "&endTime=" + requestWindow.endTimeMillis()
                + "&sortOrder=desc";
    }

    private void handleFetchFailure(Long projectId, Side side, FetchResult result) {
        if (result.authFailure()) {
            handleAuthFailure(projectId, side.displayName() + " 接口鉴权失败，reason=" + result.reason());
            return;
        }
        notifyNonAuthFailure(projectId, side, result.reason());
    }

    private void handleAuthFailure(Long projectId, String reason) {
        if (Objects.equals(authFailureReasonByProject.get(projectId), reason)) {
            return;
        }
        authFailureReasonByProject.put(projectId, reason);
        sendLarkText("token 可能已过期或鉴权失败\n"
                + "时间: " + nowText() + "\n"
                + "projectId: " + projectId + "\n"
                + "模式: " + currentMode() + "\n"
                + "原因: " + reason + "\n"
                + "请尽快替换新的 token。");
    }

    private void resetAuthFailureState(Long projectId) {
        authFailureReasonByProject.remove(projectId);
    }

    private void notifyVolumeChanged(Long projectId, VolumeSnapshot previous, VolumeSnapshot current,
                                     boolean buyChanged, boolean sellChanged) {
        StringBuilder content = new StringBuilder();
        content.append("成本金额发生变更\n")
                .append("时间: ").append(nowText()).append("\n")
                .append("projectId: ").append(projectId).append("\n")
                .append("模式: ").append(currentMode()).append("\n")
                .append("窗口: ").append(formatWindow(current.requestWindow())).append("\n\n");

        if (buyChanged) {
            appendSideChange(content, Side.BUY, previous.buy(), current.buy());
        }
        if (sellChanged) {
            appendSideChange(content, Side.SELL, previous.sell(), current.sell());
        }
        sendLarkText(content.toString().trim());
    }

    private void notifyMonitorStarted(Long projectId, VolumeSnapshot snapshot) {
        if (!startupNotifiedProjectIds.add(projectId)) {
            return;
        }
        String content = "监控已启动\n"
                + "时间: " + nowText() + "\n"
                + "projectId: " + projectId + "\n"
                + "BUY 当前 成本金额: " + formatDecimal(snapshot.buy().contractCostAmount()) + "\n"
                + "BUY 当前 averageOpenPrice: " + formatDecimal(snapshot.buy().averageOpenPrice()) + "\n"
                + "SELL 当前 成本金额: " + formatDecimal(snapshot.sell().contractCostAmount()) + "\n"
                + "SELL 当前 averageOpenPrice: " + formatDecimal(snapshot.sell().averageOpenPrice());
        sendLarkText(content);
    }

    private void notifyNonAuthFailure(Long projectId, Side side, String reason) {
        long now = System.currentTimeMillis();
        String normalizedReason = normalizeFailureReason(reason);
        String sideKey = side == null ? "COMMON" : side.displayName();
        Map<String, Long> projectFailures = nonAuthFailureNotifyAt.computeIfAbsent(projectId, ignored -> new ConcurrentHashMap<>());
        String failureKey = sideKey + "|" + normalizedReason;
        Long lastNotifyAt = projectFailures.get(failureKey);
        if (lastNotifyAt != null && now - lastNotifyAt < failureNotifyCooldownMillis()) {
            return;
        }

        projectFailures.put(failureKey, now);
        String content = "普通接口失败告警\n"
                + "时间: " + nowText() + "\n"
                + "projectId: " + projectId + "\n"
                + "接口: " + sideKey + "\n"
                + "原因: " + normalizedReason + "\n"
                + "冷却时间: " + MonitorConstants.FAILURE_NOTIFY_COOLDOWN_MINUTES + " 分钟内相同错误不重复通知";
        sendLarkText(content);
    }

    private void clearNonAuthFailureState(Long projectId) {
        nonAuthFailureNotifyAt.remove(projectId);
    }

    private void sendLarkText(String text) {
        String webhookUrl = configuredWebhookUrl();
        if (webhookUrl.isBlank()) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("msg_type", "text");
            payload.put("content", Map.of("text", text));

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(MonitorConstants.REQUEST_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = sendRequestWithRetry(request, "Futures Lark webhook");
            if (response.statusCode() >= 200 && response.statusCode() < 300
                    && response.body() != null && !response.body().isBlank()) {
                LarkResponse ignored = objectMapper.readValue(response.body(), LarkResponse.class);
            }
        } catch (Exception ignored) {
        }
    }

    private String configuredWebhookUrl() {
        String larkWebhookUrl = MonitorConstants.Futures.LARK_WEBHOOK_URL;
        return larkWebhookUrl;
    }

    private SSLParameters buildSslParameters() {
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setProtocols(new String[]{"TLSv1.2"});
        return sslParameters;
    }

    private HttpResponse<String> sendRequestWithRetry(HttpRequest request, String requestName) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= HTTP_RETRY_TIMES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                logHttpResponse(requestName, request, response);
                return response;
            } catch (Exception e) {
                lastException = e;
                logHttpException(requestName, request, attempt, e);
                if (!isRetryableHttpException(e) || attempt >= HTTP_RETRY_TIMES) {
                    throw e;
                }
                sleepBeforeRetry(attempt);
            }
        }
        throw lastException == null ? new IllegalStateException("Unknown HTTP request failure") : lastException;
    }

    private boolean isRetryableHttpException(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return false;
        }
        Throwable current = e;
        while (current != null) {
            if (current instanceof SSLHandshakeException || current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepBeforeRetry(int attempt) throws InterruptedException {
        Thread.sleep(HTTP_RETRY_BACKOFF_MILLIS * attempt);
    }

    private void logHttpResponse(String requestName, HttpRequest request, HttpResponse<String> response) {
        System.out.println("[HTTP] " + requestName
                + " | uri=" + request.uri()
                + " | status=" + response.statusCode()
                + " | version=" + response.version()
                + " | body=" + safeBody(response.body()));
    }

    private void logHttpException(String requestName, HttpRequest request, int attempt, Exception e) {
        System.out.println("[HTTP] " + requestName
                + " | uri=" + request.uri()
                + " | attempt=" + attempt + "/" + HTTP_RETRY_TIMES
                + " | exception=" + e.getClass().getSimpleName()
                + ": " + safeMessage(e.getMessage()));
    }

    private boolean isPublicMode() {
        return "public".equalsIgnoreCase(currentMode());
    }

    private String currentMode() {
        String mode = MonitorConstants.Futures.MODE;
        return mode.trim();
    }

    private boolean isTokenMissing() {
        String token = normalizedAccessToken();
        return token.isBlank() || MonitorConstants.TOKEN_PLACEHOLDER.equals(token);
    }

    private String buildLoginTokenHeader() {
        String token = normalizedAccessToken();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7).trim();
        }
        return token;
    }

    private String buildCookieHeader(String tokenHeader) {
        return "tickup-token=" + URLEncoder.encode(tokenHeader, StandardCharsets.UTF_8);
    }

    private String normalizedAccessToken() {
        String token = MonitorConstants.Futures.ACCESS_TOKEN;
        if (token == null) {
            return "";
        }
        String trimmed = token.trim();
        String prefix = "";
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            prefix = "Bearer ";
            trimmed = trimmed.substring(7).trim();
        }
        if (trimmed.contains("%")) {
            trimmed = URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        }
        return prefix + trimmed;
    }

    private String nowText() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    private RequestWindow buildRequestWindow() {
        long now = Instant.now().toEpochMilli();
        long startTimeMillis = now - Duration.ofMinutes(MonitorConstants.LOOK_BACK_MINUTES).toMillis();
        long endTimeMillis = now + Duration.ofMinutes(MonitorConstants.LOOK_AHEAD_MINUTES).toMillis();
        if (startTimeMillis >= endTimeMillis) {
            throw new IllegalStateException("Invalid request window, startTime must be earlier than endTime");
        }
        return new RequestWindow(startTimeMillis, endTimeMillis);
    }

    private long failureNotifyCooldownMillis() {
        return Duration.ofMinutes(MonitorConstants.FAILURE_NOTIFY_COOLDOWN_MINUTES).toMillis();
    }

    private void appendSideChange(StringBuilder content, Side side, SideSnapshot previous, SideSnapshot current) {
        BigDecimal diff = subtractNullable(current.contractCostAmount(), previous.contractCostAmount());
        content.append(side.displayName()).append(" 差值: ").append(formatDecimal(diff)).append("\n")
                .append(side.displayName()).append(" 当前 成本金额: ").append(formatDecimal(current.contractCostAmount())).append("\n")
                .append(side.displayName()).append(" 上次 成本金额: ").append(formatDecimal(previous.contractCostAmount())).append("\n")
                .append(side.displayName()).append(" 当前 averageOpenPrice: ").append(formatDecimal(current.averageOpenPrice())).append("\n")
                .append(side.displayName()).append(" 上次 averageOpenPrice: ").append(formatDecimal(previous.averageOpenPrice())).append("\n\n");
    }

    private BigDecimal subtractNullable(BigDecimal left, BigDecimal right) {
        BigDecimal safeLeft = left == null ? BigDecimal.ZERO : left;
        BigDecimal safeRight = right == null ? BigDecimal.ZERO : right;
        return safeLeft.subtract(safeRight);
    }

    private boolean changedNullableDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return false;
        }
        if (left == null || right == null) {
            return true;
        }
        return left.compareTo(right) != 0;
    }

    private BigDecimal parseOptionalDecimal(String value) {
        if (value == null || value.isBlank() || "--".equals(value.trim())) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0, RoundingMode.UNNECESSARY);
        }
        return normalized.toPlainString();
    }

    private String formatWindow(RequestWindow requestWindow) {
        return formatEpochMillis(requestWindow.startTimeMillis()) + " ~ " + formatEpochMillis(requestWindow.endTimeMillis());
    }

    private String formatFixedDelay() {
        long fixedDelayMillis = MonitorConstants.FIXED_DELAY_MILLIS;
        if (fixedDelayMillis % 60_000L == 0) {
            return (fixedDelayMillis / 60_000L) + "min";
        }
        if (fixedDelayMillis % 1_000L == 0) {
            return (fixedDelayMillis / 1_000L) + "s";
        }
        return fixedDelayMillis + "ms";
    }

    private String normalizeFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        return reason.replaceAll("\\s+", " ").trim();
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "unknown" : message.trim();
    }

    private String formatEpochMillis(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                .format(TIME_FORMATTER)
                + " ("
                + epochMillis
                + ")";
    }

    private String buildNormalizedGetString(URI uri, String timestampMs, String nonce) {
        return String.join("\n",
                "GET",
                uri.getRawPath() == null ? "" : uri.getRawPath(),
                canonicalQuery(uri.getRawQuery()),
                timestampMs,
                nonce,
                ""
        );
    }

    private String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        Map<String, List<String>> queryMap = new LinkedHashMap<>();
        for (String part : rawQuery.split("&")) {
            String[] keyValue = part.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
            queryMap.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        List<String> pairs = new ArrayList<>();
        for (String key : new TreeSet<>(queryMap.keySet())) {
            List<String> values = queryMap.get(key);
            values.sort(String::compareTo);
            for (String value : values) {
                pairs.add(key + "=" + value);
            }
        }
        return String.join("&", pairs);
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build signature", e);
        }
    }

    private boolean isAuthFailureMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("token")
                || lower.contains("expired")
                || lower.contains("unauthorized")
                || lower.contains("authentication")
                || lower.contains("auth")
                || message.contains("过期")
                || message.contains("鉴权")
                || message.contains("认证");
    }

    private String safeBody(String body) {
        if (body == null) {
            return "null";
        }
        String trimmed = body.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 500) + "...";
    }

    private enum Side {
        BUY("BUY", "buy"),
        SELL("SELL", "sell");

        private final String displayName;
        private final String pathSegment;

        Side(String displayName, String pathSegment) {
            this.displayName = displayName;
            this.pathSegment = pathSegment;
        }

        public String displayName() {
            return displayName;
        }

        public String pathSegment() {
            return pathSegment;
        }
    }

    private record RequestWindow(long startTimeMillis, long endTimeMillis) {
    }

    private record SideSnapshot(BigDecimal contractCostAmount, BigDecimal averageOpenPrice) {
    }

    private record VolumeSnapshot(SideSnapshot buy, SideSnapshot sell, RequestWindow requestWindow) {
    }

    private record FetchResult(boolean success, boolean authFailure, BigDecimal contractCostAmount,
                               BigDecimal averageOpenPrice, String reason) {

        private static FetchResult success(BigDecimal contractCostAmount, BigDecimal averageOpenPrice) {
            return new FetchResult(true, false, contractCostAmount, averageOpenPrice, null);
        }

        private static FetchResult failure(String reason) {
            return new FetchResult(false, false, null, null, reason);
        }

        private static FetchResult authFailure(String reason) {
            return new FetchResult(false, true, null, null, reason);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PublicApiResponse {
        public Integer code;
        public String message;
        public FuturesAccountTotalPnlVo data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LarkResponse {
        public Integer code;
        public String msg;
    }
}
