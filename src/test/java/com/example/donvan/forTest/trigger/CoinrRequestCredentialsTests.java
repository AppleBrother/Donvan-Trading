package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.MonitorConstants;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinrRequestCredentialsTests {

    @Test
    void appliesDecodedTokenAndDeviceIdWithoutDoubleEncodingCookie() {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://example.test/data"));

        CoinrRequestCredentials.applyTo(builder, "57%3Aexample%3Asignature", "device-123");

        HttpRequest request = builder.build();
        assertEquals("57:example:signature", request.headers().firstValue("X-Token").orElseThrow());
        assertEquals("Bearer 57:example:signature",
                request.headers().firstValue("Authorization").orElseThrow());
        assertEquals("device-123", request.headers().firstValue("Device-ID").orElseThrow());
        assertEquals("tickup-token=57%3Aexample%3Asignature; Device-ID=device-123",
                request.headers().firstValue("Cookie").orElseThrow());
    }

    @Test
    void requiresBothTokenAndDeviceId() {
        assertTrue(CoinrRequestCredentials.isConfigured("57%3Aexample", "device-123"));
        assertFalse(CoinrRequestCredentials.isConfigured("", "device-123"));
        assertFalse(CoinrRequestCredentials.isConfigured("57%3Aexample", ""));
        assertFalse(CoinrRequestCredentials.isConfigured(MonitorConstants.TOKEN_PLACEHOLDER, "device-123"));
    }
}
