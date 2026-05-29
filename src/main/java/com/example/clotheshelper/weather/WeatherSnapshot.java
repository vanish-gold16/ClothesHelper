package com.example.clotheshelper.weather;

import java.time.LocalDateTime;

public record WeatherSnapshot(
        double temperatureCelsius,
        double apparentTemperatureCelsius,
        double precipitationMillimeters,
        double windSpeedKilometersPerHour,
        LocalDateTime observedAt
) {
    public boolean isRaining() {
        return precipitationMillimeters >= 0.2;
    }

    public boolean isWindy() {
        return windSpeedKilometersPerHour >= 25.0;
    }
}
