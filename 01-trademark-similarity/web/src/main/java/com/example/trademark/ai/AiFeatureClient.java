package com.example.trademark.ai;

import com.example.trademark.config.AppProperties;
import com.example.trademark.exception.AppExceptions.AiServerException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Python FastAPI AI 서버 호출 클라이언트 */
@Component
public class AiFeatureClient {

    private final RestClient restClient;

    public AiFeatureClient(AppProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getAi().getConnectTimeoutMs());
        factory.setReadTimeout(props.getAi().getReadTimeoutMs());
        this.restClient = builder
            .baseUrl(props.getAi().getBaseUrl())
            .requestFactory(factory)
            .build();
    }

    /** 이미지 바이트 → 특징 벡터 */
    public FeatureResponse extract(byte[] imageBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename == null ? "image.png" : filename;
            }
        });
        try {
            FeatureResponse response = restClient.post()
                .uri("/api/v1/features")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(FeatureResponse.class);
            if (response == null || response.vector() == null || response.vector().length == 0) {
                throw new AiServerException("AI 서버가 빈 벡터를 반환했습니다.", null);
            }
            return response;
        } catch (RestClientException e) {
            throw new AiServerException("AI 서버에 연결할 수 없습니다. (" + e.getMessage() + ")", e);
        }
    }

    /** /health 결과. 연결 실패 시 status=DOWN */
    public Map<String, Object> health() {
        try {
            Map<String, Object> body = restClient.get()
                .uri("/health")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            return body == null ? Map.of("status", "DOWN") : body;
        } catch (RestClientException e) {
            return Map.of("status", "DOWN", "error", String.valueOf(e.getMessage()));
        }
    }

    /** 현재 AI 서버의 모델 이름 (연결 실패 시 null) */
    public String currentModel() {
        Object model = health().get("model");
        return model == null ? null : model.toString();
    }
}
