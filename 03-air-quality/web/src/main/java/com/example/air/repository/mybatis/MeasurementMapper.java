package com.example.air.repository.mybatis;

import com.example.air.domain.Measurement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** SQL: resources/mapper/MeasurementMapper.xml */
@Mapper
public interface MeasurementMapper {

    Measurement findById(@Param("id") Long id);

    Measurement findByStationAndDate(@Param("stationId") Long stationId, @Param("date") LocalDate date);

    List<Measurement> findRecent(@Param("stationId") Long stationId, @Param("limit") int limit);

    List<Measurement> findPage(@Param("stationId") Long stationId,
                               @Param("offset") int offset, @Param("size") int size);

    long count(@Param("stationId") Long stationId);

    int insert(Measurement measurement);

    int update(Measurement measurement);

    int deleteById(@Param("id") Long id);
}
