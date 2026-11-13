package com.example.air.repository.mybatis;

import com.example.air.domain.Station;
import com.example.air.repository.StationStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
public class MybatisStationStore implements StationStore {

    private final StationMapper mapper;

    public MybatisStationStore(StationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Station save(Station station) {
        if (station.getId() == null) {
            mapper.insert(station);
        } else {
            mapper.update(station);
        }
        return station;
    }

    @Override
    public Optional<Station> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    @Override
    public List<Station> findAll(boolean activeOnly) {
        return mapper.findAll(activeOnly);
    }

    @Override
    public boolean existsByName(String name) {
        return mapper.countByName(name) > 0;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
