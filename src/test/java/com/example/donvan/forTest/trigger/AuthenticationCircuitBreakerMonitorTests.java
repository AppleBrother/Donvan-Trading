package com.example.donvan.forTest.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationCircuitBreakerMonitorTests {

    @Test
    void openBreakerStopsFuturesBeforeSchedulingOrHttpWork() throws Exception {
        CoinrAuthenticationCircuitBreaker breaker = openBreaker();
        CoinrFuturesPnlVolumeMonitor monitor = new CoinrFuturesPnlVolumeMonitor(breaker);

        assertDoesNotThrow(monitor::pollOpenTradeVolume);
        assertNoHalfHourlyGate(monitor);
    }

    @Test
    void openBreakerStopsSpotBeforeSchedulingOrHttpWork() throws Exception {
        CoinrAuthenticationCircuitBreaker breaker = openBreaker();
        CoinrSpotPnlVolumeMonitor monitor = new CoinrSpotPnlVolumeMonitor(breaker);

        assertDoesNotThrow(monitor::pollSpotVolume);
        assertNoHalfHourlyGate(monitor);
    }

    private CoinrAuthenticationCircuitBreaker openBreaker() {
        CoinrAuthenticationCircuitBreaker breaker = new CoinrAuthenticationCircuitBreaker();
        assertTrue(breaker.open("token expired"));
        return breaker;
    }

    private void assertNoHalfHourlyGate(Object monitor) {
        assertThrows(NoSuchFieldException.class,
                () -> monitor.getClass().getDeclaredField("halfHourlyExecutionGate"),
                "per-minute polling must not retain the half-hour execution gate");
    }
}
