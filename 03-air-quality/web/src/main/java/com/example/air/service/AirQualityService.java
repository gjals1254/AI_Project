package com.example.air.service;

import com.example.air.ai.AirPredictionClient;
import com.example.air.ai.AirPredictionClient.Factor;
import com.example.air.ai.AirPredictionClient.PredictResponse;
import com.example.air.domain.AirGrade;
import com.example.air.domain.Measurement;
import com.example.air.domain.Prediction;
import com.example.air.domain.Station;
import com.example.air.dto.GradeCount;
import com.example.air.dto.MeasurementForm;
import com.example.air.dto.PageResult;
import com.example.air.dto.Views.ImportResult;
import com.example.air.dto.Views.StationChart;
import com.example.air.dto.Views.StationStatus;
import com.example.air.exception.AppExceptions.InvalidFileException;
import com.example.air.exception.AppExceptions.NotFoundException;
import com.example.air.repository.MeasurementStore;
import com.example.air.repository.PredictionStore;
import com.example.air.repository.StationStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 측정 데이터 등록 → AI 예측 → 결과 저장/조회/시각화
 */
@Service
public class AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityService.class);
    public static final List<String> CSV_COLUMNS = List.of(
        "date", "pm10", "pm25", "o3", "no2", "co", "so2", "temperature", "humidity", "windSpeed", "precipitation");

    private final StationStore stationStore;
    private final MeasurementStore measurementStore;
    private final PredictionStore predictionStore;
    private final AirPredictionClient client;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AirQualityService(StationStore stationStore, MeasurementStore measurementStore,
                             PredictionStore predictionStore, AirPredictionClient client,
                             ObjectMapper objectMapper, Validator validator) {
        this.validator = validator;
        this.stationStore = stationStore;
        this.measurementStore = measurementStore;
        this.predictionStore = predictionStore;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- 등록 + 예측

    /** 같은 측정소·같은 날짜가 있으면 값을 갱신하고 예측을 다시 만든다 */
    @Transactional
    public Prediction registerMeasurement(MeasurementForm form) {
        return upsertAndPredict(form);
    }

    @Transactional
    public Prediction repredict(Long measurementId) {
        Measurement m = getMeasurement(measurementId);
        predictionStore.deleteByMeasurementId(m.getId());
        return predict(m);
    }

    @Transactional
    public void deleteMeasurement(Long id) {
        getMeasurement(id);
        predictionStore.deleteByMeasurementId(id);
        measurementStore.deleteById(id);
    }

    /**
     * CSV 일괄 등록. 헤더: date,pm10,pm25,o3,no2,co,so2,temperature,humidity,windSpeed,precipitation
     * 행마다 독립적으로 처리해 한 행이 실패해도 나머지는 저장된다.
     */
    public ImportResult importCsv(Long stationId, MultipartFile file) {
        getStation(stationId);
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("CSV 파일을 선택하세요.");
        }
        int ok = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new InvalidFileException("빈 파일입니다.");
            }
            Map<String, Integer> idx = headerIndex(header.replace("﻿", ""));
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    MeasurementForm form = parseRow(stationId, line.split(",", -1), idx);
                    for (ConstraintViolation<MeasurementForm> v : validator.validate(form)) {
                        throw new IllegalArgumentException(v.getPropertyPath() + " " + v.getMessage());
                    }
                    upsertAndPredict(form);
                    ok++;
                } catch (RuntimeException e) {
                    failed++;
                    if (errors.size() < 20) {
                        errors.add(row + "행: " + e.getMessage());
                    }
                    log.warn("CSV {}행 실패: {}", row, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new InvalidFileException("CSV 를 읽을 수 없습니다: " + e.getMessage());
        }
        return new ImportResult(ok, failed, errors);
    }

    private Map<String, Integer> headerIndex(String header) {
        String[] cols = header.split(",");
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            idx.put(cols[i].trim(), i);
        }
        for (String c : CSV_COLUMNS) {
            if (!idx.containsKey(c)) {
                throw new InvalidFileException("CSV 헤더에 '" + c + "' 컬럼이 없습니다. 필요 컬럼: " + CSV_COLUMNS);
            }
        }
        return idx;
    }

    private MeasurementForm parseRow(Long stationId, String[] v, Map<String, Integer> idx) {
        MeasurementForm f = new MeasurementForm();
        f.setStationId(stationId);
        f.setMeasureDate(LocalDate.parse(v[idx.get("date")].trim()));
        f.setPm10(num(v, idx, "pm10"));
        f.setPm25(num(v, idx, "pm25"));
        f.setO3(num(v, idx, "o3"));
        f.setNo2(num(v, idx, "no2"));
        f.setCo(num(v, idx, "co"));
        f.setSo2(num(v, idx, "so2"));
        f.setTemperature(num(v, idx, "temperature"));
        f.setHumidity(num(v, idx, "humidity"));
        f.setWindSpeed(num(v, idx, "windSpeed"));
        f.setPrecipitation(num(v, idx, "precipitation"));
        return f;
    }

    private Double num(String[] v, Map<String, Integer> idx, String col) {
        String s = v[idx.get(col)].trim();
        return s.isEmpty() ? 0.0 : Double.valueOf(s);
    }

    private Prediction upsertAndPredict(MeasurementForm form) {
        Measurement m = measurementStore.findByStationAndDate(form.getStationId(), form.getMeasureDate())
            .map(existing -> {
                form.copyValuesTo(existing);
                predictionStore.deleteByMeasurementId(existing.getId());
                return existing;
            })
            .orElseGet(() -> {
                Measurement created = form.toEntity();
                created.setCreatedAt(LocalDateTime.now());
                return created;
            });
        measurementStore.save(m);
        return predict(m);
    }

    private Prediction predict(Measurement m) {
        PredictResponse r = client.predict(m);
        Prediction p = new Prediction();
        p.setStationId(m.getStationId());
        p.setMeasurementId(m.getId());
        p.setTargetDate(m.getMeasureDate().plusDays(1));
        p.setPredPm10(r.pm10());
        p.setPredPm25(r.pm25());
        p.setPm10Grade(r.pm10Grade());
        p.setPm25Grade(r.pm25Grade());
        p.setGrade(r.grade());
        p.setGradeLabel(r.gradeLabel());
        p.setModelVersion(r.modelVersion());
        p.setCreatedAt(LocalDateTime.now());
        try {
            p.setTopFactors(objectMapper.writeValueAsString(r.topFactors()));
        } catch (JsonProcessingException e) {
            p.setTopFactors("[]");
        }
        return predictionStore.save(p);
    }

    // ---------------------------------------------------------------- 조회

    @Transactional(readOnly = true)
    public List<StationStatus> dashboard() {
        List<StationStatus> list = new ArrayList<>();
        for (Station s : stationStore.findAll(true)) {
            Measurement latest = first(measurementStore.findRecent(s.getId(), 1));
            Prediction forecast = first(predictionStore.findRecent(s.getId(), 1));
            list.add(new StationStatus(s, latest, forecast));
        }
        return list;
    }

    /** 최근 days 일 실측값과 예측값을 같은 날짜 축으로 정렬 */
    @Transactional(readOnly = true)
    public StationChart chart(Long stationId, int days) {
        Map<LocalDate, Measurement> actual = new HashMap<>();
        measurementStore.findRecent(stationId, days).forEach(m -> actual.put(m.getMeasureDate(), m));
        Map<LocalDate, Prediction> predicted = new HashMap<>();
        predictionStore.findRecent(stationId, days).forEach(p -> predicted.putIfAbsent(p.getTargetDate(), p));

        TreeSet<LocalDate> dates = new TreeSet<>(actual.keySet());
        dates.addAll(predicted.keySet());
        List<String> labels = new ArrayList<>();
        List<Double> a10 = new ArrayList<>();
        List<Double> a25 = new ArrayList<>();
        List<Double> p10 = new ArrayList<>();
        List<Double> p25 = new ArrayList<>();
        for (LocalDate d : dates) {
            labels.add(d.toString());
            Measurement m = actual.get(d);
            Prediction p = predicted.get(d);
            a10.add(m == null ? null : m.getPm10());
            a25.add(m == null ? null : m.getPm25());
            p10.add(p == null ? null : p.getPredPm10());
            p25.add(p == null ? null : p.getPredPm25());
        }
        return new StationChart(labels, a10, a25, p10, p25);
    }

    @Transactional(readOnly = true)
    public List<Measurement> recentMeasurements(Long stationId, int limit) {
        return measurementStore.findRecent(stationId, limit);
    }

    @Transactional(readOnly = true)
    public Prediction latestPrediction(Long stationId) {
        return first(predictionStore.findRecent(stationId, 1));
    }

    @Transactional(readOnly = true)
    public PageResult<Measurement> measurementPage(Long stationId, int page, int size) {
        int p = Math.max(page, 0);
        return new PageResult<>(measurementStore.findPage(stationId, p, size), p, size, measurementStore.count(stationId));
    }

    @Transactional(readOnly = true)
    public PageResult<Prediction> predictionPage(Long stationId, Integer grade, int page, int size) {
        int p = Math.max(page, 0);
        return new PageResult<>(predictionStore.findPage(stationId, grade, p, size), p, size,
            predictionStore.count(stationId, grade));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> gradeDistribution() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (int g = 1; g <= 4; g++) {
            m.put(AirGrade.label(g), 0L);
        }
        for (GradeCount gc : predictionStore.countByGrade()) {
            if (gc.getGrade() != null) {
                m.put(AirGrade.label(gc.getGrade()), gc.getCount());
            }
        }
        return m;
    }

    public List<Factor> factors(Prediction p) {
        if (p == null || !StringUtils.hasText(p.getTopFactors())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(p.getTopFactors(), new TypeReference<List<Factor>>() { });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public Station getStation(Long id) {
        return stationStore.findById(id).orElseThrow(() -> new NotFoundException("측정소를 찾을 수 없습니다. id=" + id));
    }

    public Measurement getMeasurement(Long id) {
        return measurementStore.findById(id)
            .orElseThrow(() -> new NotFoundException("측정 데이터를 찾을 수 없습니다. id=" + id));
    }

    /** 등급별 행동 요령 (환경부 안내 요약) */
    public static String advice(int grade) {
        return switch (grade) {
            case 1 -> "대기 상태가 좋습니다. 실외 활동에 지장이 없습니다.";
            case 2 -> "민감군은 장시간·무리한 실외 활동을 줄이는 것이 좋습니다.";
            case 3 -> "장시간 실외 활동을 자제하고, 외출 시 보건용 마스크(KF80 이상)를 착용하세요.";
            case 4 -> "실외 활동을 피하고, 창문을 닫고 실내 공기청정기를 사용하세요.";
            default -> "";
        };
    }

    private static <T> T first(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
}
