package com.example.donvan.forTest.trigger;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MonitorMessageSupport {

    private static final BigDecimal NOTIFY_DIFF_THRESHOLD = BigDecimal.ONE;
    private static final int PRICE_SCALE = 8;

    private MonitorMessageSupport() {
    }

    static boolean hasValueChanged(BigDecimal previous, BigDecimal current) {
        if (previous == null && current == null) {
            return false;
        }
        if (previous == null || current == null) {
            return true;
        }
        return previous.compareTo(current) != 0;
    }

    static boolean shouldNotifyWhenDiffAtLeastOne(BigDecimal previous, BigDecimal current) {
        return hasValueChanged(previous, current)
                && subtractNullable(current, previous).abs().compareTo(NOTIFY_DIFF_THRESHOLD) >= 0;
    }

    static BigDecimal subtractNullable(BigDecimal left, BigDecimal right) {
        BigDecimal safeLeft = left == null ? BigDecimal.ZERO : left;
        BigDecimal safeRight = right == null ? BigDecimal.ZERO : right;
        return safeLeft.subtract(safeRight);
    }

    static String formatInteger(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    static String formatPrice(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.setScale(PRICE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    static String normalizeNotificationText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("\\s*\\R\\s*", ",")
                .replaceAll(",+", ",")
                .replaceAll("^,+|,+$", "")
                .trim();
    }
}

