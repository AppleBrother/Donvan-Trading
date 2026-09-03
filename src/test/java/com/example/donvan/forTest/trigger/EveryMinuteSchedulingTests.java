package com.example.donvan.forTest.trigger;

import com.example.donvan.forTest.vo.MonitorConstants;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EveryMinuteSchedulingTests {

    @Test
    void futuresRunsAfterStartupDelayAndThenEveryMinute() throws Exception {
        assertEveryMinuteSchedule(CoinrFuturesPnlVolumeMonitor.class, "pollOpenTradeVolume");
    }

    @Test
    void spotRunsAfterStartupDelayAndThenEveryMinute() throws Exception {
        assertEveryMinuteSchedule(CoinrSpotPnlVolumeMonitor.class, "pollSpotVolume");
    }

    private void assertEveryMinuteSchedule(Class<?> monitorClass, String methodName) throws Exception {
        Method method = monitorClass.getDeclaredMethod(methodName);
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals(MonitorConstants.INITIAL_DELAY_MILLIS, scheduled.initialDelay());
        assertEquals(60_000L, scheduled.fixedDelay());
    }
}
