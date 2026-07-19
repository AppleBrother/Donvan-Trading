package com.example.donvan.forTest.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinrAuthenticationCircuitBreakerTests {

    @Test
    void opensOnceAndRetainsTheFirstFailureReason() {
        CoinrAuthenticationCircuitBreaker breaker = new CoinrAuthenticationCircuitBreaker();

        assertFalse(breaker.isOpen());
        assertTrue(breaker.open("HTTP 401"));
        assertTrue(breaker.isOpen());
        assertEquals("HTTP 401", breaker.reason());
        assertFalse(breaker.open("HTTP 403"));
        assertEquals("HTTP 401", breaker.reason());
    }

    @Test
    void normalizesBlankFailureReason() {
        CoinrAuthenticationCircuitBreaker breaker = new CoinrAuthenticationCircuitBreaker();

        assertTrue(breaker.open("  "));
        assertEquals("authentication failure", breaker.reason());
    }
}
