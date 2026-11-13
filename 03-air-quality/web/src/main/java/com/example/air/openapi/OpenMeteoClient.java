package com.example.air.openapi;

import com.example.air.config.AppProperties;
import com.example.air.openapi.OpenApiClient.AirDaily;
import com.example.air.openapi.OpenApiClient.WeatherDaily;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

/**
 * Open-Meteo (키 불필요) — 위경도 기준 대기질(CAMS 모델 추정값)·기상 일자료.
 * shortcut: 실측이 아닌 모델값이라 국내 측정소 값과 차이가 있음, 공공데이터 키가 활성화되면 app.sync.source=publicdata 로 전환.
 */
@Component
public class OpenMeteoClient {

    private static final String AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";
    // forecast 호스트는 이 환경에서 TLS 실패, archive 호스트는 전날 자료까지 제공
    private static final String WEATHER_URL = "https://archive-api.open-meteo.com/v1/archive";
    private static final double MOLAR_VOLUME = 24.45; // 25℃·1기압, ppm = µg/m³ × 24.45 / (분자량 × 1000)

    private final RestClient restClient;

    public OpenMeteoClient(AppProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getOpenapi().getTimeoutMs());
        factory.setReadTimeout(props.getOpenapi().getTimeoutMs());
        this.restClient = builder.requestFactory(factory).build();
    }

    public AirDaily fetchAir(double lat, double lon, LocalDate date) {
        return averageAir(restClient.get()
            .uri(AIR_URL + "?latitude={lat}&longitude={lon}&hourly=pm10,pm2_5,ozone,nitrogen_dioxide,carbon_monoxide,"
                + "sulphur_dioxide&start_date={d}&end_date={d}&timezone=Asia/Seoul", lat, lon, date, date)
            .retrieve().body(JsonNode.class));
    }

    public WeatherDaily fetchWeather(double lat, double lon, LocalDate date) {
        return parseWeather(restClient.get()
            .uri(WEATHER_URL + "?latitude={lat}&longitude={lon}&daily=temperature_2m_mean,relative_humidity_2m_mean,"
                + "wind_speed_10m_mean,precipitation_sum&wind_speed_unit=ms&start_date={d}&end_date={d}"
                + "&timezone=Asia/Seoul", lat, lon, date, date)
            .retrieve().body(JsonNode.class));
    }

    static AirDaily averageAir(JsonNode body) {
        JsonNode h = body.path("hourly");
        return new AirDaily(avg(h, "pm10"), avg(h, "pm2_5"),
            ppm(avg(h, "ozone"), 48.00), ppm(avg(h, "nitrogen_dioxide"), 46.01),
            ppm(avg(h, "carbon_monoxide"), 28.01), ppm(avg(h, "sulphur_dioxide"), 64.07));
    }

    static WeatherDaily parseWeather(JsonNode body) {
        JsonNode d = body.path("daily");
        JsonNode rain = d.path("precipitation_sum").path(0);
        return new WeatherDaily(first(d, "temperature_2m_mean"), first(d, "relative_humidity_2m_mean"),
            first(d, "wind_speed_10m_mean"), rain.isNumber() ? rain.asDouble() : 0.0);
    }

    private static Double avg(JsonNode hourly, String field) {
        double sum = 0;
        int n = 0;
        for (JsonNode v : hourly.path(field)) {
            if (v.isNumber()) {
                sum += v.asDouble();
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    private static Double ppm(Double microgram, double molecularWeight) {
        return microgram == null ? null : microgram * MOLAR_VOLUME / (molecularWeight * 1000);
    }

    private static double first(JsonNode daily, String field) {
        JsonNode v = daily.path(field).path(0);
        if (!v.isNumber()) {
            throw new IllegalStateException("Open-Meteo " + field + " 결측");
        }
        return v.asDouble();
    }
}
