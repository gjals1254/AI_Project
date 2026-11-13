package com.example.stress.ai;

import com.example.stress.config.AppProperties;
import com.example.stress.domain.Assessment;
import com.example.stress.exception.AppExceptions.AiServerException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FastAPI 예측 서버 호출 */
@Component
public class PredictionClient {

    /** /api/v1/predict 응답 */
    public record PredictResponse(int level, String label, double riskScore,
                                  Map<String, Double> probabilities,
                                  List<Factor> topFactors, String modelVersion) {
    }

    public record Factor(String feature, String label, Object value, double reference, double impact) {
    }

    private final RestClient restClient;

    public PredictionClient(AppProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getAi().getConnectTimeoutMs());
        factory.setReadTimeout(props.getAi().getReadTimeoutMs());
        this.restClient = builder.baseUrl(props.getAi().getBaseUrl()).requestFactory(factory).build();
    }

    public PredictResponse predict(Assessment a) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parentAge", a.getParentAge());
        body.put("childCount", a.getChildCount());
        body.put("youngestChildAge", a.getYoungestChildAge());
        body.put("sleepHours", a.getSleepHours());
        body.put("weeklyWorkHours", a.getWeeklyWorkHours());
        body.put("parentingEfficacy", a.getParentingEfficacy());
        body.put("socialSupport", a.getSocialSupport());
        body.put("familyEnvironment", a.getFamilyEnvironment());
        body.put("resilience", a.getResilience());
        body.put("qualityOfLife", a.getQualityOfLife());
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

    public Map<String, Object> modelInfo() {
        return getMap("/api/v1/model-info");
    }

    public Map<String, Object> health() {
        return getMap("/health");
    }

    public Map<String, Object> retrain() {
        try {
            return restClient.post().uri("/api/v1/train")
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
