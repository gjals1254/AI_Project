package com.example.air.repository.jpa;

import com.example.air.domain.Measurement;
import com.example.air.repository.MeasurementStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaMeasurementStore implements MeasurementStore {

    private final MeasurementJpaRepository repo;

    public JpaMeasurementStore(MeasurementJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Measurement save(Measurement measurement) {
        return repo.save(measurement);
    }

    @Override
    public Optional<Measurement> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Measurement> findByStationAndDate(Long stationId, LocalDate date) {
        return repo.findByStationIdAndMeasureDate(stationId, date);
    }

    @Override
    public List<Measurement> findRecent(Long stationId, int limit) {
        return repo.findByStationIdOrderByMeasureDateDesc(stationId, PageRequest.of(0, limit));
    }

    @Override
    public List<Measurement> findPage(Long stationId, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("measureDate"), Sort.Order.desc("id"));
        return repo.search(stationId, PageRequest.of(page, size, sort)).getContent();
    }

    @Override
    public long count(Long stationId) {
        return repo.search(stationId, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}
