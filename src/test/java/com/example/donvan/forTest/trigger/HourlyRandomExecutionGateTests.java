package com.example.donvan.forTest.trigger;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HourlyRandomExecutionGateTests {

    @Test
    void startupExecutionDoesNotConsumeCurrentHoursRandomExecution() {
        HourlyRandomExecutionGate gate = new HourlyRandomExecutionGate("TEST");
        LocalDateTime startupAtLastMinute = LocalDateTime.of(2026, 7, 12, 5, 59, 0);

        assertTrue(gate.shouldExecute(startupAtLastMinute),
                "the first invocation after startup must execute immediately");
        assertTrue(gate.shouldExecute(startupAtLastMinute.plusSeconds(1)),
                "startup execution must not consume the current hour's random execution");
        assertFalse(gate.shouldExecute(startupAtLastMinute.plusSeconds(2)),
                "the current hour's random execution must happen only once");
    }
}
