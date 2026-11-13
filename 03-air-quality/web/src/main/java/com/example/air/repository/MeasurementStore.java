package com.example.air.repository;

import com.example.air.domain.Measurement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 측정 데이터 저장소 (JpaMeasurementStore / MybatisMeasurementStore) */
public interface MeasurementStore {

    Measurement save(Measurement measurement);

    Optional<Measurement> findById(Long id);

    Optional<Measurement> findByStationAndDate(Long stationId, LocalDate date);

    /** 최근 limit 건 (측정일 내림차순) */
    List<Measurement> findRecent(Long stationId, int limit);

    /** stationId 가 null 이면 전체 */
    List<Measurement> findPage(Long stationId, int page, int size);

    long count(Long stationId);

    void deleteById(Long id);
}
