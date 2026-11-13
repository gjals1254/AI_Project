package com.example.air.openapi;

import com.example.air.openapi.OpenApiClient.AirDaily;
import com.example.air.openapi.OpenApiClient.WeatherDaily;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class OpenMeteoClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode hourly(String pm10, String ozone) throws Exception {
        return JSON.readTree("{\"hourly\":{\"time\":[\"2026-11-12T00:00\",\"2026-11-12T01:00\"],"
            + "\"pm10\":" + pm10 + ",\"pm2_5\":[10,20],\"ozone\":" + ozone + ","
            + "\"nitrogen_dioxide\":[46.01,46.01],\"carbon_monoxide\":[280.1,280.1],\"sulphur_dioxide\":[64.07,64.07]}}");
    }

    @Test
    void averagesHourlyValues() throws Exception {
        AirDaily d = OpenMeteoClient.averageAir(hourly("[40,60]", "[100,100]"));
        assertThat(d.pm10()).isEqualTo(50.0);
        assertThat(d.pm25()).isEqualTo(15.0);
    }

    @Test
    void gasesConvertedFromMicrogramsToPpm() throws Exception {
        AirDaily d = OpenMeteoClient.averageAir(hourly("[40,60]", "[100,100]"));
        // ppm = µg/m³ × 24.45 / (분자량 × 1000), 25℃·1기압
        assertThat(d.o3()).isCloseTo(100 * 24.45 / 48000, within(1e-6));
        assertThat(d.no2()).isCloseTo(0.02445, within(1e-6));
        assertThat(d.co()).isCloseTo(0.2445, within(1e-6));
        assertThat(d.so2()).isCloseTo(0.02445, within(1e-6));
    }

    @Test
    void nullHoursExcluded() throws Exception {
        AirDaily d = OpenMeteoClient.averageAir(hourly("[null,60]", "[100,100]"));
        assertThat(d.pm10()).isEqualTo(60.0);
    }

    @Test
    void allNullIsMissing() throws Exception {
        AirDaily d = OpenMeteoClient.averageAir(hourly("[null,null]", "[null,null]"));
        assertThat(d.pm10()).isNull();
        assertThat(d.o3()).isNull();
    }

    @Test
    void parsesDailyWeather() throws Exception {
        JsonNode body = JSON.readTree("{\"daily\":{\"time\":[\"2026-11-12\"],\"temperature_2m_mean\":[9.1],"
            + "\"relative_humidity_2m_mean\":[85],\"wind_speed_10m_mean\":[1.56],\"precipitation_sum\":[null]}}");
        WeatherDaily w = OpenMeteoClient.parseWeather(body);
        assertThat(w.temperature()).isEqualTo(9.1);
        assertThat(w.humidity()).isEqualTo(85.0);
        assertThat(w.windSpeed()).isEqualTo(1.56);
        assertThat(w.precipitation()).isEqualTo(0.0);
    }

    @Test
    void missingTemperatureThrows() throws Exception {
        JsonNode body = JSON.readTree("{\"daily\":{\"time\":[\"2026-11-12\"],\"temperature_2m_mean\":[null],"
            + "\"relative_humidity_2m_mean\":[85],\"wind_speed_10m_mean\":[1.5],\"precipitation_sum\":[0]}}");
        assertThatThrownBy(() -> OpenMeteoClient.parseWeather(body)).hasMessageContaining("temperature_2m_mean");
    }
}
