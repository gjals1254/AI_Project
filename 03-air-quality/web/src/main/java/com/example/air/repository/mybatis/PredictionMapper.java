package com.example.air.repository.mybatis;

import com.example.air.domain.Prediction;
import com.example.air.dto.GradeCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SQL: resources/mapper/PredictionMapper.xml */
@Mapper
public interface PredictionMapper {

    Prediction findByMeasurementId(@Param("measurementId") Long measurementId);

    List<Prediction> findRecent(@Param("stationId") Long stationId, @Param("limit") int limit);

    List<Prediction> findPage(@Param("stationId") Long stationId, @Param("grade") Integer grade,
                              @Param("offset") int offset, @Param("size") int size);

    long count(@Param("stationId") Long stationId, @Param("grade") Integer grade);

    List<GradeCount> countByGrade();

    int insert(Prediction prediction);

    int deleteByMeasurementId(@Param("measurementId") Long measurementId);
}
