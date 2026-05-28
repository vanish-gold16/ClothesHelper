package com.example.clotheshelper.weather;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PragueWeatherClient {
    private static final URI WEATHER_URI = URI.create(
            "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=50.0755"
                    + "&longitude=14.4378"
                    + "&current=temperature_2m,apparent_temperature"
                    + "&timezone=Europe%2FPrague"
    );
    private static final Pattern CURRENT_BLOCK_PATTERN = Pattern.compile(
            "\"current\"\\s*:\\s*\\{(?<current>[^}]*)}",
            Pattern.DOTALL
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public CompletableFuture<WeatherSnapshot> loadCurrentWeather() {
        HttpRequest request = HttpRequest.newBuilder(WEATHER_URI)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException(
                                "Open-Meteo returned status " + response.statusCode()
                        ));
                    }
                    return parseWeather(response.body());
                });
    }

    private WeatherSnapshot parseWeather(String responseBody) {
        String current = readCurrentBlock(responseBody);

        return new WeatherSnapshot(
                readDouble(current, "temperature_2m"),
                readDouble(current, "apparent_temperature"),
                LocalDateTime.parse(readString(current, "time"))
        );
    }

    private String readCurrentBlock(String responseBody) {
        Matcher matcher = CURRENT_BLOCK_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Open-Meteo response does not include current weather");
        }
        return matcher.group("current");
    }

    private double readDouble(String json, String fieldName) {
        Matcher matcher = numberPattern(fieldName).matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Open-Meteo response misses " + fieldName);
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String readString(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Open-Meteo response misses " + fieldName);
        }
        return matcher.group(1);
    }

    private Pattern numberPattern(String fieldName) {
        return Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    }
}
