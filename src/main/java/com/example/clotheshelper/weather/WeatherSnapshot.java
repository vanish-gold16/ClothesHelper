package com.example.clotheshelper.weather;

import java.time.LocalDateTime;

public record WeatherSnapshot(
        double temperatureCelsius,
        double apparentTemperatureCelsius,
        LocalDateTime observedAt
) {
}
