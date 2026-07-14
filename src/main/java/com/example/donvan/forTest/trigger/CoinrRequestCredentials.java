package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.MonitorConstants;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

final class CoinrRequestCredentials {

    private CoinrRequestCredentials() {
    }

    static boolean isConfigured(String token, String deviceId) {
        String normalizedToken = normalizeToken(token);
        return !normalizedToken.isBlank()
                && !MonitorConstants.TOKEN_PLACEHOLDER.equals(normalizedToken)
                && deviceId != null
                && !deviceId.isBlank();
    }

    static void applyTo(HttpRequest.Builder builder, String token, String deviceId) {
        String normalizedToken = normalizeToken(token);
        String normalizedDeviceId = deviceId.trim();
        builder.header("X-Token", normalizedToken);
        builder.header("Authorization", "Bearer " + normalizedToken);
        builder.header("Device-ID", normalizedDeviceId);
        builder.header("Cookie", "tickup-token=" + encode(normalizedToken)
                + "; Device-ID=" + encode(normalizedDeviceId));
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            normalized = normalized.substring(7).trim();
        }
        return normalized.contains("%")
                ? URLDecoder.decode(normalized, StandardCharsets.UTF_8)
                : normalized;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
