package com.example.donvan.forTest.trigger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

final class HourlyRandomExecutionGate {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String name;
    private LocalDateTime currentHour;
    private LocalDateTime executedHour;
    private boolean startupExecutionPending = true;
    private int triggerMinute = -1;
    private int previousTriggerMinute = -1;

    HourlyRandomExecutionGate(String name) {
        this.name = name;
    }

    synchronized boolean shouldExecute(LocalDateTime now) {
        LocalDateTime hour = now.truncatedTo(ChronoUnit.HOURS);
        if (!hour.equals(currentHour)) {
            scheduleHour(hour, now.getMinute());
        }
        if (startupExecutionPending) {
            startupExecutionPending = false;
            System.out.println("[SCHEDULE] " + name + " startup execution"
                    + " | time=" + TIME_FORMATTER.format(now));
            return true;
        }
        if (hour.equals(executedHour) || now.getMinute() < triggerMinute) {
            return false;
        }
        executedHour = hour;
        System.out.println("[SCHEDULE] " + name + " hourly random execution"
                + " | time=" + TIME_FORMATTER.format(now)
                + " | triggerMinute=" + triggerMinute);
        return true;
    }

    private void scheduleHour(LocalDateTime hour, int earliestMinute) {
        currentHour = hour;
        triggerMinute = randomMinute(Math.max(0, Math.min(earliestMinute, 59)));
        previousTriggerMinute = triggerMinute;
        System.out.println("[SCHEDULE] " + name + " hourly random schedule"
                + " | hour=" + TIME_FORMATTER.format(hour)
                + " | triggerMinute=" + triggerMinute
                + " | firstEligibleTime=" + TIME_FORMATTER.format(hour.plusMinutes(triggerMinute)));
    }

    private int randomMinute(int earliestMinute) {
        int candidate = ThreadLocalRandom.current().nextInt(earliestMinute, 60);
        if (60 - earliestMinute <= 1 || candidate != previousTriggerMinute) {
            return candidate;
        }
        return earliestMinute + ((candidate - earliestMinute + 1) % (60 - earliestMinute));
    }
}
