package com.example.air.openapi;

import com.example.air.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** 공공데이터포털 호출: 에어코리아 측정소별 측정값, 기상청 ASOS 일자료 */
@Component
public class OpenApiClient {

    /** 대기오염 일평균. 항목이 null 이면 그날 그 항목은 전부 결측 */
    public record AirDaily(Double pm10, Double pm25, Double o3, Double no2, Double co, Double so2) {
    }

    public record WeatherDaily(double temperature, double humidity, double windSpeed, double precipitation) {
    }

    private static final String[] AIR_FIELDS = {"pm10Value", "pm25Value", "o3Value", "no2Value", "coValue", "so2Value"};
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final AppProperties.OpenApi props;
    private final RestClient restClient;
    private final ObjectMapper json;

    public OpenApiClient(AppProperties appProps, RestClient.Builder builder, ObjectMapper json) {
        this.props = appProps.getOpenapi();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutMs());
        factory.setReadTimeout(props.getTimeoutMs());
        this.restClient = builder.requestFactory(factory).build();
        this.json = json;
    }

    public boolean hasKey() {
        return props.getServiceKey() != null && !props.getServiceKey().isBlank();
    }

    /** 최근 한 달 시간값을 받아 date 하루치만 평균 */
    public AirDaily fetchAir(String stationName, LocalDate date) {
        String body = restClient.get()
            .uri(props.getAirkoreaBaseUrl() + "/getMsrstnAcctoRltmMesureDnsty?stationName={s}&dataTerm=MONTH"
                + "&returnType=json&numOfRows=1000&pageNo=1&ver=1.0&serviceKey={k}", stationName, props.getServiceKey())
            .retrieve().body(String.class);
        return averageAir(read(body), date);
    }

    public WeatherDaily fetchWeather(int stnId, LocalDate date) {
        String d = date.format(YMD);
        String body = restClient.get()
            .uri(props.getKmaBaseUrl() + "/getWthrDataList?dataCd=ASOS&dateCd=DAY&startDt={d}&endDt={d}&stnIds={n}"
                + "&dataType=JSON&numOfRows=10&pageNo=1&serviceKey={k}", d, d, stnId, props.getServiceKey())
            .retrieve().body(String.class);
        return parseWeather(read(body));
    }

    static AirDaily averageAir(JsonNode body, LocalDate date) {
        String prefix = date.toString(); // "2025-01-01" — "2025-01-01 24:00" 도 같은 날로 포함
        double[] sum = new double[AIR_FIELDS.length];
        int[] cnt = new int[AIR_FIELDS.length];
        for (JsonNode row : items(body)) {
            if (!row.path("dataTime").asText().startsWith(prefix)) {
                continue;
            }
            for (int i = 0; i < AIR_FIELDS.length; i++) {
                Double v = number(row.path(AIR_FIELDS[i]).asText(""));
                if (v != null) {
                    sum[i] += v;
                    cnt[i]++;
                }
            }
        }
        Double[] avg = new Double[AIR_FIELDS.length];
        for (int i = 0; i < AIR_FIELDS.length; i++) {
            avg[i] = cnt[i] == 0 ? null : sum[i] / cnt[i];
        }
        return new AirDaily(avg[0], avg[1], avg[2], avg[3], avg[4], avg[5]);
    }

    static WeatherDaily parseWeather(JsonNode body) {
        List<JsonNode> rows = items(body);
        if (rows.isEmpty()) {
            throw new IllegalStateException("기상청 자료 없음");
        }
        JsonNode r = rows.get(0);
        Double rain = number(r.path("sumRn").asText(""));
        return new WeatherDaily(required(r, "avgTa"), required(r, "avgRhm"), required(r, "avgWs"),
            rain == null ? 0.0 : rain);
    }

    /** items 가 배열(에어코리아)이거나 items.item 배열(기상청)인 두 형태 모두 처리 */
    private static List<JsonNode> items(JsonNode body) {
        JsonNode items = body.path("response").path("body").path("items");
        JsonNode arr = items.isArray() ? items : items.path("item");
        List<JsonNode> out = new ArrayList<>();
        arr.forEach(out::add);
        return out;
    }

    private static double required(JsonNode row, String field) {
        Double v = number(row.path(field).asText(""));
        if (v == null) {
            throw new IllegalStateException("기상청 " + field + " 결측");
        }
        return v;
    }

    private static Double number(String s) {
        try {
            return s.isBlank() || "-".equals(s.trim()) ? null : Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 인증키 오류(게이트웨이)·결과코드 오류를 "결측" 대신 API 메시지 그대로 알린다 */
    static void checkError(JsonNode body) {
        JsonNode gw = body.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
        if (!gw.isMissingNode()) {
            throw new IllegalStateException("공공데이터 API 오류: " + gw.path("returnAuthMsg").asText()
                + " (" + gw.path("errMsg").asText() + ")");
        }
        JsonNode header = body.path("response").path("header");
        String code = header.path("resultCode").asText("00");
        if (!"00".equals(code)) {
            throw new IllegalStateException("공공데이터 API 오류 " + code + ": " + header.path("resultMsg").asText());
        }
    }

    /** 키 오류 등은 JSON 대신 XML 오류문을 주기도 하므로 그 내용을 그대로 예외 메시지로 */
    private JsonNode read(String body) {
        JsonNode node;
        try {
            node = json.readTree(body);
        } catch (Exception e) {
            String head = body == null ? "" : body.substring(0, Math.min(200, body.length()));
            throw new IllegalStateException("API 응답을 해석할 수 없습니다: " + head);
        }
        checkError(node);
        return node;
    }
}
