package com.example.air.ai;

import com.example.air.config.AppProperties;
import com.example.air.domain.Measurement;
import com.example.air.exception.AppExceptions.AiServerException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FastAPI 대기질 예측 서버 호출 */
@Component
public class AirPredictionClient {

    public record PredictResponse(double pm10, double pm25, int pm10Grade, int pm25Grade, int grade,
                                  String gradeLabel, List<Factor> topFactors, String modelVersion) {
    }

    public record Factor(String feature, String label, Object value, double reference, double impact) {
    }

    /** 재학습은 Open-Meteo 2년치 다운로드 + 학습이라 예측보다 훨씬 오래 걸림(약 40초) */
    private static final int RETRAIN_READ_TIMEOUT_MS = 180_000;

    private final RestClient restClient;
    private final RestClient retrainClient;

    public AirPredictionClient(AppProperties props, RestClient.Builder builder) {
        this.restClient = build(builder.clone(), props, props.getAi().getReadTimeoutMs());
        this.retrainClient = build(builder.clone(), props, RETRAIN_READ_TIMEOUT_MS);
    }

    private static RestClient build(RestClient.Builder builder, AppProperties props, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getAi().getConnectTimeoutMs());
        factory.setReadTimeout(readTimeoutMs);
        return builder.baseUrl(props.getAi().getBaseUrl()).requestFactory(factory).build();
    }

    public PredictResponse predict(Measurement m) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("month", m.getMeasureDate().getMonthValue());
        body.put("pm10", m.getPm10());
        body.put("pm25", m.getPm25());
        body.put("o3", m.getO3());
        body.put("no2", m.getNo2());
        body.put("co", m.getCo());
        body.put("so2", m.getSo2());
        body.put("temperature", m.getTemperature());
        body.put("humidity", m.getHumidity());
        body.put("windSpeed", m.getWindSpeed());
        body.put("precipitation", m.getPrecipitation());
        try {
            return restClient.post().uri("/api/v1/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(PredictResponse.class);
        } catch (RestClientException e) {
            throw new AiServerException("AI 예측 서버 호출 실패 (" + e.getMessage() + ")", e);
        }
    }

    public Map<String, Object> health() {
        return getMap("/health");
    }

    public Map<String, Object> modelInfo() {
        return getMap("/api/v1/model-info");
    }

    public Map<String, Object> retrain() {
        try {
            return retrainClient.post().uri("/api/v1/train")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (RestClientException e) {
            throw new AiServerException("재학습 요청 실패 (" + e.getMessage() + ")", e);
        }
    }

    private Map<String, Object> getMap(String uri) {
        try {
            Map<String, Object> m = restClient.get().uri(uri).retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            return m == null ? Map.of() : m;
        } catch (RestClientException e) {
            return Map.of("status", "DOWN", "error", String.valueOf(e.getMessage()));
        }
    }
}
