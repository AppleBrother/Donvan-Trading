package com.example.donvan.forTest.trigger;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationCircuitBreakerMonitorTests {

    @Test
    void openBreakerStopsFuturesBeforeSchedulingOrHttpWork() throws Exception {
        CoinrAuthenticationCircuitBreaker breaker = openBreaker();
        CoinrFuturesPnlVolumeMonitor monitor = new CoinrFuturesPnlVolumeMonitor(breaker);

        monitor.pollOpenTradeVolume();

        assertStartupExecutionStillPending(monitor);
    }

    @Test
    void openBreakerStopsSpotBeforeSchedulingOrHttpWork() throws Exception {
        CoinrAuthenticationCircuitBreaker breaker = openBreaker();
        CoinrSpotPnlVolumeMonitor monitor = new CoinrSpotPnlVolumeMonitor(breaker);

        monitor.pollSpotVolume();

        assertStartupExecutionStillPending(monitor);
    }

    private CoinrAuthenticationCircuitBreaker openBreaker() {
        CoinrAuthenticationCircuitBreaker breaker = new CoinrAuthenticationCircuitBreaker();
        assertTrue(breaker.open("token expired"));
        return breaker;
    }

    private void assertStartupExecutionStillPending(Object monitor) throws Exception {
        Field gateField = monitor.getClass().getDeclaredField("halfHourlyExecutionGate");
        gateField.setAccessible(true);
        Object gate = gateField.get(monitor);

        Field startupPendingField = gate.getClass().getDeclaredField("startupExecutionPending");
        startupPendingField.setAccessible(true);
        assertTrue(startupPendingField.getBoolean(gate),
                "an open authentication breaker must return before advancing the schedule gate");
    }
}
