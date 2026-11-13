package com.example.air.openapi;

import com.example.air.openapi.OpenApiClient.AirDaily;
import com.example.air.openapi.OpenApiClient.WeatherDaily;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenApiClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate JAN1 = LocalDate.of(2025, 1, 1);

    /** 에어코리아 응답: 각 행 = {dataTime, pm10Value} (나머지 항목은 같은 값 "1") */
    private static JsonNode air(String... timeAndPm10) throws Exception {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < timeAndPm10.length; i += 2) {
            if (items.length() > 0) items.append(',');
            items.append("{\"dataTime\":\"").append(timeAndPm10[i]).append("\",\"pm10Value\":\"")
                .append(timeAndPm10[i + 1]).append("\",\"pm25Value\":\"1\",\"o3Value\":\"1\",\"no2Value\":\"1\","
                    + "\"coValue\":\"1\",\"so2Value\":\"1\"}");
        }
        return JSON.readTree("{\"response\":{\"body\":{\"items\":[" + items + "]}}}");
    }

    @Test
    void averagesOnlyTargetDate() throws Exception {
        AirDaily d = OpenApiClient.averageAir(
            air("2025-01-01 13:00", "40", "2025-01-01 14:00", "60", "2025-01-02 01:00", "999"), JAN1);
        assertThat(d.pm10()).isEqualTo(50.0);
        assertThat(d.pm25()).isEqualTo(1.0);
    }

    @Test
    void midnight24BelongsToSameDay() throws Exception {
        AirDaily d = OpenApiClient.averageAir(air("2025-01-01 23:00", "10", "2025-01-01 24:00", "30"), JAN1);
        assertThat(d.pm10()).isEqualTo(20.0);
    }

    @Test
    void dashAndBlankExcluded() throws Exception {
        AirDaily d = OpenApiClient.averageAir(
            air("2025-01-01 01:00", "-", "2025-01-01 02:00", "", "2025-01-01 03:00", "20"), JAN1);
        assertThat(d.pm10()).isEqualTo(20.0);
    }

    @Test
    void noRowsForDateIsMissing() throws Exception {
        AirDaily d = OpenApiClient.averageAir(air("2025-01-02 01:00", "20"), JAN1);
        assertThat(d.pm10()).isNull();
        assertThat(d.pm25()).isNull();
        assertThat(d.o3()).isNull();
        assertThat(d.no2()).isNull();
        assertThat(d.co()).isNull();
        assertThat(d.so2()).isNull();
    }

    @Test
    void weatherBlankRainIsZero() throws Exception {
        JsonNode body = JSON.readTree("{\"response\":{\"body\":{\"items\":{\"item\":[{\"avgTa\":\"3.4\","
            + "\"avgRhm\":\"60.5\",\"avgWs\":\"2.1\",\"sumRn\":\"\"}]}}}}");
        WeatherDaily w = OpenApiClient.parseWeather(body);
        assertThat(w.temperature()).isEqualTo(3.4);
        assertThat(w.humidity()).isEqualTo(60.5);
        assertThat(w.windSpeed()).isEqualTo(2.1);
        assertThat(w.precipitation()).isEqualTo(0.0);
    }

    @Test
    void networkErrorDoesNotExposeServiceKey() {
        com.example.air.config.AppProperties props = new com.example.air.config.AppProperties();
        props.getOpenapi().setServiceKey("SECRET+KEY/123==");
        props.getOpenapi().setAirkoreaBaseUrl("http://127.0.0.1:1");
        props.getOpenapi().setKmaBaseUrl("http://127.0.0.1:1");
        OpenApiClient client = new OpenApiClient(props, org.springframework.web.client.RestClient.builder(), JSON);

        assertThatThrownBy(() -> client.fetchAir("중구", JAN1))
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("SECRET"));
        assertThatThrownBy(() -> client.fetchWeather(108, JAN1))
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("SECRET"));
    }

    @Test
    void gatewayAuthErrorIsReported() throws Exception {
        JsonNode body = JSON.readTree("{\"OpenAPI_ServiceResponse\":{\"cmmMsgHeader\":{\"errMsg\":"
            + "\"SERVICE_KEY_IS_NOT_REGISTERED_ERROR\",\"returnAuthMsg\":\"등록되지 않은 서비스키\",\"returnReasonCode\":\"30\"}}}");
        assertThatThrownBy(() -> OpenApiClient.checkError(body)).hasMessageContaining("등록되지 않은 서비스키");
    }

    @Test
    void resultCodeErrorIsReported() throws Exception {
        JsonNode body = JSON.readTree("{\"response\":{\"header\":{\"resultCode\":\"99\",\"resultMsg\":\"전날 자료까지 제공됩니다.\"}}}");
        assertThatThrownBy(() -> OpenApiClient.checkError(body)).hasMessageContaining("전날 자료까지");
    }

    @Test
    void normalResponsePassesCheck() throws Exception {
        OpenApiClient.checkError(JSON.readTree("{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_CODE\"}}}"));
    }

    @Test
    void weatherWithoutItemsThrows() throws Exception {
        JsonNode body = JSON.readTree("{\"response\":{\"body\":{\"items\":{\"item\":[]}}}}");
        assertThatThrownBy(() -> OpenApiClient.parseWeather(body)).hasMessageContaining("기상청 자료 없음");
    }
}
