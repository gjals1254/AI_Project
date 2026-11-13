package com.example.air.repository;

import com.example.air.domain.Station;

import java.util.List;
import java.util.Optional;

/** 측정소 저장소 (JpaStationStore / MybatisStationStore) */
public interface StationStore {

    Station save(Station station);

    Optional<Station> findById(Long id);

    List<Station> findAll(boolean activeOnly);

    boolean existsByName(String name);

    void deleteById(Long id);
}
