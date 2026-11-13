package com.example.air.repository;

import com.example.air.domain.Prediction;
import com.example.air.dto.GradeCount;

import java.util.List;
import java.util.Optional;

/** 예측 결과 저장소 (JpaPredictionStore / MybatisPredictionStore) */
public interface PredictionStore {

    Prediction save(Prediction prediction);

    Optional<Prediction> findByMeasurementId(Long measurementId);

    /** 최근 limit 건 (예측 대상일 내림차순) */
    List<Prediction> findRecent(Long stationId, int limit);

    /** stationId, grade 가 null 이면 조건 없음 */
    List<Prediction> findPage(Long stationId, Integer grade, int page, int size);

    long count(Long stationId, Integer grade);

    List<GradeCount> countByGrade();

    void deleteByMeasurementId(Long measurementId);
}
