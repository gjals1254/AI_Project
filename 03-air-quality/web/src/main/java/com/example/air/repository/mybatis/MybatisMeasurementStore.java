package com.example.air.repository.mybatis;

import com.example.air.domain.Measurement;
import com.example.air.repository.MeasurementStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
public class MybatisMeasurementStore implements MeasurementStore {

    private final MeasurementMapper mapper;

    public MybatisMeasurementStore(MeasurementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Measurement save(Measurement measurement) {
        if (measurement.getId() == null) {
            mapper.insert(measurement);
        } else {
            mapper.update(measurement);
        }
        return measurement;
    }

    @Override
    public Optional<Measurement> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    @Override
    public Optional<Measurement> findByStationAndDate(Long stationId, LocalDate date) {
        return Optional.ofNullable(mapper.findByStationAndDate(stationId, date));
    }

    @Override
    public List<Measurement> findRecent(Long stationId, int limit) {
        return mapper.findRecent(stationId, limit);
    }

    @Override
    public List<Measurement> findPage(Long stationId, int page, int size) {
        return mapper.findPage(stationId, page * size, size);
    }

    @Override
    public long count(Long stationId) {
        return mapper.count(stationId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
