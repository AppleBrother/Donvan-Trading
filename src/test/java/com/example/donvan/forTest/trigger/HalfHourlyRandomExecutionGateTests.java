package com.example.donvan.forTest.trigger;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HalfHourlyRandomExecutionGateTests {

    @Test
    void startupExecutionDoesNotConsumeCurrentHalfHourWindow() {
        HalfHourlyRandomExecutionGate gate = new HalfHourlyRandomExecutionGate("TEST");
        LocalDateTime start = LocalDateTime.of(2026, 7, 14, 5, 29, 0);

        assertTrue(gate.shouldExecute(start), "startup must execute immediately");
        assertTrue(gate.shouldExecute(start.plusSeconds(1)),
                "startup must not consume the current half-hour window");
        assertFalse(gate.shouldExecute(start.plusSeconds(2)),
                "the current half-hour window must execute only once");
    }

    @Test
    void executesOnceInEachFollowingHalfHourWindow() {
        HalfHourlyRandomExecutionGate gate = new HalfHourlyRandomExecutionGate("TEST");

        assertTrue(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 5, 29, 0)));
        assertTrue(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 5, 29, 1)));
        assertTrue(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 5, 59, 0)));
        assertFalse(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 5, 59, 1)));
        assertTrue(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 6, 29, 0)));
        assertFalse(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 6, 29, 1)));
    }
}
