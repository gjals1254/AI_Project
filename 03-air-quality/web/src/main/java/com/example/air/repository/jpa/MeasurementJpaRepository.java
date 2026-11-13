package com.example.air.repository.jpa;

import com.example.air.domain.Measurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeasurementJpaRepository extends JpaRepository<Measurement, Long> {

    Optional<Measurement> findByStationIdAndMeasureDate(Long stationId, LocalDate measureDate);

    List<Measurement> findByStationIdOrderByMeasureDateDesc(Long stationId, Pageable pageable);

    @Query("select m from Measurement m where (:sid is null or m.stationId = :sid)")
    Page<Measurement> search(@Param("sid") Long stationId, Pageable pageable);
}
