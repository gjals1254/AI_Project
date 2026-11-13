package com.example.air.repository.jpa;

import com.example.air.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationJpaRepository extends JpaRepository<Station, Long> {

    List<Station> findByActiveTrueOrderByIdAsc();

    List<Station> findAllByOrderByIdAsc();

    boolean existsByName(String name);
}
