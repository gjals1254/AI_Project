package com.example.air.repository.mybatis;

import com.example.air.domain.Prediction;
import com.example.air.dto.GradeCount;
import com.example.air.repository.PredictionStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
public class MybatisPredictionStore implements PredictionStore {

    private final PredictionMapper mapper;

    public MybatisPredictionStore(PredictionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Prediction save(Prediction prediction) {
        mapper.insert(prediction);   // 예측은 수정하지 않고 새로 쌓는다
        return prediction;
    }

    @Override
    public Optional<Prediction> findByMeasurementId(Long measurementId) {
        return Optional.ofNullable(mapper.findByMeasurementId(measurementId));
    }

    @Override
    public List<Prediction> findRecent(Long stationId, int limit) {
        return mapper.findRecent(stationId, limit);
    }

    @Override
    public List<Prediction> findPage(Long stationId, Integer grade, int page, int size) {
        return mapper.findPage(stationId, grade, page * size, size);
    }

    @Override
    public long count(Long stationId, Integer grade) {
        return mapper.count(stationId, grade);
    }

    @Override
    public List<GradeCount> countByGrade() {
        return mapper.countByGrade();
    }

    @Override
    public void deleteByMeasurementId(Long measurementId) {
        mapper.deleteByMeasurementId(measurementId);
    }
}
