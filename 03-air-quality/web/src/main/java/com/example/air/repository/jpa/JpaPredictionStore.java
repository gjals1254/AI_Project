package com.example.air.repository.jpa;

import com.example.air.domain.Prediction;
import com.example.air.dto.GradeCount;
import com.example.air.repository.PredictionStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaPredictionStore implements PredictionStore {

    private final PredictionJpaRepository repo;

    public JpaPredictionStore(PredictionJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Prediction save(Prediction prediction) {
        return repo.save(prediction);
    }

    @Override
    public Optional<Prediction> findByMeasurementId(Long measurementId) {
        return repo.findFirstByMeasurementIdOrderByIdDesc(measurementId);
    }

    @Override
    public List<Prediction> findRecent(Long stationId, int limit) {
        return repo.findByStationIdOrderByTargetDateDesc(stationId, PageRequest.of(0, limit));
    }

    @Override
    public List<Prediction> findPage(Long stationId, Integer grade, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("targetDate"), Sort.Order.desc("id"));
        return repo.search(stationId, grade, PageRequest.of(page, size, sort)).getContent();
    }

    @Override
    public long count(Long stationId, Integer grade) {
        return repo.search(stationId, grade, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<GradeCount> countByGrade() {
        return repo.countByGrade();
    }

    @Override
    public void deleteByMeasurementId(Long measurementId) {
        repo.deleteByMeasurementId(measurementId);
    }
}
