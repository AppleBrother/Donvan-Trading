package com.example.donvan.forTest.trigger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorMessageSupportTests {

    @Test
    void shouldNotifyDiffLessThanOne_whenChangedAndBelowOne() {
        assertTrue(MonitorMessageSupport.shouldNotifyDiffLessThanOne(
                new BigDecimal("10.00"),
                new BigDecimal("10.50")
        ));
    }

    @Test
    void shouldNotifyDiffLessThanOne_whenChangedAndNegative() {
        assertTrue(MonitorMessageSupport.shouldNotifyDiffLessThanOne(
                new BigDecimal("10.00"),
                new BigDecimal("8.20")
        ));
    }

    @Test
    void shouldNotNotifyDiffLessThanOne_whenUnchanged() {
        assertFalse(MonitorMessageSupport.shouldNotifyDiffLessThanOne(
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        ));
    }

    @Test
    void shouldNotNotifyDiffLessThanOne_whenDiffIsOneOrGreater() {
        assertFalse(MonitorMessageSupport.shouldNotifyDiffLessThanOne(
                new BigDecimal("10.00"),
                new BigDecimal("11.00")
        ));
        assertFalse(MonitorMessageSupport.shouldNotifyDiffLessThanOne(
                new BigDecimal("10.00"),
                new BigDecimal("11.20")
        ));
    }

    @Test
    void formatInteger_roundsToWholeNumber() {
        assertEquals("124", MonitorMessageSupport.formatInteger(new BigDecimal("123.6")));
        assertEquals("-2", MonitorMessageSupport.formatInteger(new BigDecimal("-1.6")));
        assertEquals("--", MonitorMessageSupport.formatInteger(null));
    }

    @Test
    void formatPrice_keepsEightDecimalPlaces() {
        assertEquals("0.12345679", MonitorMessageSupport.formatPrice(new BigDecimal("0.123456789")));
        assertEquals("1.20000000", MonitorMessageSupport.formatPrice(new BigDecimal("1.2")));
        assertEquals("--", MonitorMessageSupport.formatPrice(null));
    }

    @Test
    void normalizeNotificationText_convertsMultilineTextToSingleCommaSeparatedLine() {
        assertEquals(
                "FUT vol change,proj: abctest,B diff: 1,B curr pri: 0.12345678",
                MonitorMessageSupport.normalizeNotificationText("\nFUT vol change\n proj: abctest \n\n B diff: 1\nB curr pri: 0.12345678\n")
        );
        assertEquals("", MonitorMessageSupport.normalizeNotificationText("   \n \n  "));
    }
}

