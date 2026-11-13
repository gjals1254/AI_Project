package com.example.air.repository.mybatis;

import com.example.air.domain.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SQL: resources/mapper/StationMapper.xml */
@Mapper
public interface StationMapper {

    Station findById(@Param("id") Long id);

    List<Station> findAll(@Param("activeOnly") boolean activeOnly);

    int countByName(@Param("name") String name);

    int insert(Station station);

    int update(Station station);

    int deleteById(@Param("id") Long id);
}
