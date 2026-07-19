package com.example.donvan.forTest.trigger;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public final class CoinrAuthenticationCircuitBreaker {

    private static final String DEFAULT_REASON = "authentication failure";

    private final AtomicReference<String> firstFailureReason = new AtomicReference<>();

    public CoinrAuthenticationCircuitBreaker() {
    }

    boolean isOpen() {
        return firstFailureReason.get() != null;
    }

    boolean open(String reason) {
        return firstFailureReason.compareAndSet(null, normalizeReason(reason));
    }

    String reason() {
        return firstFailureReason.get();
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? DEFAULT_REASON : reason.trim();
    }
}
