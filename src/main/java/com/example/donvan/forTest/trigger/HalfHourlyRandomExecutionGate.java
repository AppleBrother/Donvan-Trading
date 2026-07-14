package com.example.donvan.forTest.trigger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

final class HalfHourlyRandomExecutionGate {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int WINDOW_MINUTES = 30;

    private final String name;
    private LocalDateTime currentWindowStart;
    private LocalDateTime executedWindowStart;
    private LocalDateTime triggerAt;
    private boolean startupExecutionPending = true;
    private int previousTriggerOffset = -1;

    HalfHourlyRandomExecutionGate(String name) {
        this.name = name;
    }

    synchronized boolean shouldExecute(LocalDateTime now) {
        LocalDateTime windowStart = windowStart(now);
        if (!windowStart.equals(currentWindowStart)) {
            scheduleWindow(windowStart, now);
        }
        if (startupExecutionPending) {
            startupExecutionPending = false;
            System.out.println("[SCHEDULE] " + name + " startup execution"
                    + " | time=" + TIME_FORMATTER.format(now));
            return true;
        }
        if (windowStart.equals(executedWindowStart) || now.isBefore(triggerAt)) {
            return false;
        }
        executedWindowStart = windowStart;
        System.out.println("[SCHEDULE] " + name + " half-hour random execution"
                + " | time=" + TIME_FORMATTER.format(now)
                + " | triggerAt=" + TIME_FORMATTER.format(triggerAt));
        return true;
    }

    private LocalDateTime windowStart(LocalDateTime now) {
        LocalDateTime hour = now.truncatedTo(ChronoUnit.HOURS);
        return now.getMinute() < WINDOW_MINUTES ? hour : hour.plusMinutes(WINDOW_MINUTES);
    }

    private void scheduleWindow(LocalDateTime windowStart, LocalDateTime now) {
        currentWindowStart = windowStart;
        int earliestOffset = Math.max(0, Math.min((int) ChronoUnit.MINUTES.between(windowStart, now), WINDOW_MINUTES - 1));
        int triggerOffset = randomOffset(earliestOffset);
        previousTriggerOffset = triggerOffset;
        triggerAt = windowStart.plusMinutes(triggerOffset);
        System.out.println("[SCHEDULE] " + name + " half-hour random schedule"
                + " | windowStart=" + TIME_FORMATTER.format(windowStart)
                + " | triggerAt=" + TIME_FORMATTER.format(triggerAt));
    }

    private int randomOffset(int earliestOffset) {
        int candidate = ThreadLocalRandom.current().nextInt(earliestOffset, WINDOW_MINUTES);
        if (WINDOW_MINUTES - earliestOffset <= 1 || candidate != previousTriggerOffset) {
            return candidate;
        }
        return earliestOffset + ((candidate - earliestOffset + 1) % (WINDOW_MINUTES - earliestOffset));
    }
}
