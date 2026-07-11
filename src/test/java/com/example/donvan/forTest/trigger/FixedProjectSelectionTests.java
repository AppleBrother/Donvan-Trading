package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.MonitorConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedProjectSelectionTests {

    @Test
    void futuresMonitorUsesOnlyTheFixedHana2Project() throws Exception {
        CoinrFuturesPnlVolumeMonitor monitor = new CoinrFuturesPnlVolumeMonitor();

        assertFixedProject(monitor);
    }

    @Test
    void spotMonitorUsesOnlyTheFixedHana2Project() throws Exception {
        CoinrSpotPnlVolumeMonitor monitor = new CoinrSpotPnlVolumeMonitor();

        assertFixedProject(monitor);
    }

    private void assertFixedProject(Object monitor) throws Exception {
        var projectIdField = assertDoesNotThrow(
                () -> MonitorConstants.class.getField("MONITORED_PROJECT_ID")
        );
        var projectNameField = assertDoesNotThrow(
                () -> MonitorConstants.class.getField("MONITORED_PROJECT_NAME")
        );
        assertEquals(56L, projectIdField.getLong(null));
        assertEquals("HANA2", projectNameField.get(null));

        Method resolveProjectIds = monitor.getClass().getDeclaredMethod("resolveProjectIds");
        resolveProjectIds.setAccessible(true);
        assertEquals(List.of(56L), resolveProjectIds.invoke(monitor));

        Method projectLabel = monitor.getClass().getDeclaredMethod("projectLabel", Long.class);
        projectLabel.setAccessible(true);
        assertEquals("hana2", projectLabel.invoke(monitor, 56L));
    }
}
