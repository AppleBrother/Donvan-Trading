package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.MonitorConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedProjectSelectionTests {

    @Test
    void futuresMonitorUsesOnlyTheFixedAkeProject() throws Exception {
        CoinrFuturesPnlVolumeMonitor monitor = new CoinrFuturesPnlVolumeMonitor(
                new CoinrAuthenticationCircuitBreaker());

        assertFixedProject(monitor);
    }

    @Test
    void spotMonitorUsesOnlyTheFixedAkeProject() throws Exception {
        CoinrSpotPnlVolumeMonitor monitor = new CoinrSpotPnlVolumeMonitor(
                new CoinrAuthenticationCircuitBreaker());

        assertFixedProject(monitor);
    }

    private void assertFixedProject(Object monitor) throws Exception {
        var projectIdField = assertDoesNotThrow(
                () -> MonitorConstants.class.getField("MONITORED_PROJECT_ID")
        );
        var projectNameField = assertDoesNotThrow(
                () -> MonitorConstants.class.getField("MONITORED_PROJECT_NAME")
        );
        assertEquals(59L, projectIdField.getLong(null));
        assertEquals("AKE", projectNameField.get(null));

        Method resolveProjectIds = monitor.getClass().getDeclaredMethod("resolveProjectIds");
        resolveProjectIds.setAccessible(true);
        assertEquals(List.of(59L), resolveProjectIds.invoke(monitor));

        Method projectLabel = monitor.getClass().getDeclaredMethod("projectLabel", Long.class);
        projectLabel.setAccessible(true);
        assertEquals("ake", projectLabel.invoke(monitor, 59L));
    }
}
