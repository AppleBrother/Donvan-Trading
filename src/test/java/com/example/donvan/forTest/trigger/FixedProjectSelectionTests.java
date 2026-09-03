package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.MonitorConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedProjectSelectionTests {

    @Test
    void futuresMonitorUsesBtrAndAkeProjects() throws Exception {
        CoinrFuturesPnlVolumeMonitor monitor = new CoinrFuturesPnlVolumeMonitor(
                new CoinrAuthenticationCircuitBreaker());

        assertFixedProject(monitor);
    }

    @Test
    void spotMonitorUsesBtrAndAkeProjects() throws Exception {
        CoinrSpotPnlVolumeMonitor monitor = new CoinrSpotPnlVolumeMonitor(
                new CoinrAuthenticationCircuitBreaker());

        assertFixedProject(monitor);
    }

    private void assertFixedProject(Object monitor) throws Exception {
        var projectsField = assertDoesNotThrow(
                () -> MonitorConstants.class.getField("MONITORED_PROJECTS")
        );
        assertEquals(List.of(
                new MonitorConstants.MonitoredProject(58L, "BTR"),
                new MonitorConstants.MonitoredProject(59L, "AKE")
        ), projectsField.get(null));

        Method resolveProjectIds = monitor.getClass().getDeclaredMethod("resolveProjectIds");
        resolveProjectIds.setAccessible(true);
        assertEquals(List.of(58L, 59L), resolveProjectIds.invoke(monitor));

        Method projectLabel = monitor.getClass().getDeclaredMethod("projectLabel", Long.class);
        projectLabel.setAccessible(true);
        assertEquals("btr", projectLabel.invoke(monitor, 58L));
        assertEquals("ake", projectLabel.invoke(monitor, 59L));
    }
}
