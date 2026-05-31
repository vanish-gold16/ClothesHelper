package com.example.clotheshelper.weather;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the rain/wind thresholds used to pick weather-appropriate outfits. */
class WeatherSnapshotTest {

    private WeatherSnapshot snapshot(double precipitation, double wind) {
        return new WeatherSnapshot(15.0, 14.0, precipitation, wind, LocalDateTime.of(2026, 5, 31, 12, 0));
    }

    @Test
    void isRainingOnlyAboveThreshold() {
        assertFalse(snapshot(0.1, 0).isRaining());
        assertTrue(snapshot(0.2, 0).isRaining(), "0.2 mm is the rain threshold");
        assertTrue(snapshot(5.0, 0).isRaining());
    }

    @Test
    void isWindyOnlyAboveThreshold() {
        assertFalse(snapshot(0, 24.9).isWindy());
        assertTrue(snapshot(0, 25.0).isWindy(), "25 km/h is the wind threshold");
        assertTrue(snapshot(0, 40.0).isWindy());
    }
}
