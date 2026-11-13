package com.example.stress.service;

import com.example.stress.ai.PredictionClient;
import com.example.stress.ai.PredictionClient.Factor;
import com.example.stress.ai.PredictionClient.PredictResponse;
import com.example.stress.domain.Assessment;
import com.example.stress.dto.AssessmentForm;
import com.example.stress.dto.LevelCount;
import com.example.stress.dto.PageResult;
import com.example.stress.exception.AppExceptions.NotFoundException;
import com.example.stress.repository.AssessmentStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기능: 설문 등록 → AI 예측 → 저장, 결과 조회, 이력, 통계
 */
@Service
public class AssessmentService {

    public static final List<String> LABELS = List.of("정상", "주의", "고위험");

    private final AssessmentStore store;
    private final PredictionClient client;
    private final ObjectMapper objectMapper;

    public AssessmentService(AssessmentStore store, PredictionClient client, ObjectMapper objectMapper) {
        this.store = store;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    /** 설문 저장 + 예측. AI 서버 장애 시 AiServerException → 저장하지 않음 */
    @Transactional
    public Assessment create(AssessmentForm form, String username) {
        Assessment a = form.toEntity();
        a.setCreatedBy(username);
        a.setCreatedAt(LocalDateTime.now());
        applyPrediction(a, client.predict(a));
        return store.save(a);
    }

    /** 모델 재학습 후 기존 평가를 새 모델로 다시 예측 */
    @Transactional
    public Assessment repredict(Long id) {
        Assessment a = get(id);
        applyPrediction(a, client.predict(a));
        return store.save(a);
    }

    @Transactional(readOnly = true)
    public Assessment get(Long id) {
        return store.findById(id).orElseThrow(() -> new NotFoundException("평가를 찾을 수 없습니다. id=" + id));
    }

    @Transactional(readOnly = true)
    public PageResult<Assessment> page(String keyword, Integer level, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int p = Math.max(page, 0);
        return new PageResult<>(store.findPage(kw, level, p, size), p, size, store.count(kw, level));
    }

    @Transactional(readOnly = true)
    public List<Assessment> history(String respondentCode) {
        return store.findByRespondentCode(respondentCode);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        store.deleteById(id);
    }

    public List<Factor> factors(Assessment a) {
        if (!StringUtils.hasText(a.getTopFactors())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(a.getTopFactors(), new TypeReference<List<Factor>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** 수준별 건수 (0,1,2 모두 포함) */
    @Transactional(readOnly = true)
    public Map<String, Long> levelDistribution() {
        Map<String, Long> m = new LinkedHashMap<>();
        LABELS.forEach(l -> m.put(l, 0L));
        for (LevelCount lc : store.countByLevel()) {
            if (lc.getLevel() != null && lc.getLevel() >= 0 && lc.getLevel() < LABELS.size()) {
                m.put(LABELS.get(lc.getLevel()), lc.getCount());
            }
        }
        return m;
    }

    public Double averageRiskScore() {
        return store.averageRiskScore();
    }

    public long countSince(LocalDateTime from) {
        return store.countSince(from);
    }

    /** 수준별 안내 문구 (진단이 아닌 선별 결과) */
    public static String guidance(Integer level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case 0 -> "현재 스트레스 수준은 안정적입니다. 수면·운동 등 지금의 생활 습관을 유지하도록 안내하세요.";
            case 1 -> "스트레스 관리가 필요한 단계입니다. 양육 분담, 휴식 시간 확보, 지역 육아지원센터 프로그램을 안내하세요.";
            default -> "정신건강 전문가 상담을 권고합니다. 정신건강위기상담전화(1577-0199), "
                + "지역 정신건강복지센터 연계를 검토하세요.";
        };
    }

    private void applyPrediction(Assessment a, PredictResponse r) {
        a.setStressLevel(r.level());
        a.setStressLabel(r.label());
        a.setRiskScore(r.riskScore());
        Map<String, Double> p = r.probabilities();
        a.setProbNormal(p.getOrDefault("정상", 0.0));
        a.setProbCaution(p.getOrDefault("주의", 0.0));
        a.setProbHigh(p.getOrDefault("고위험", 0.0));
        a.setModelVersion(r.modelVersion());
        try {
            a.setTopFactors(objectMapper.writeValueAsString(r.topFactors()));
        } catch (JsonProcessingException e) {
            a.setTopFactors("[]");
        }
    }
}
