package com.example.air.repository.jpa;

import com.example.air.domain.Station;
import com.example.air.repository.StationStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaStationStore implements StationStore {

    private final StationJpaRepository repo;

    public JpaStationStore(StationJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Station save(Station station) {
        return repo.save(station);
    }

    @Override
    public Optional<Station> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<Station> findAll(boolean activeOnly) {
        return activeOnly ? repo.findByActiveTrueOrderByIdAsc() : repo.findAllByOrderByIdAsc();
    }

    @Override
    public boolean existsByName(String name) {
        return repo.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}
