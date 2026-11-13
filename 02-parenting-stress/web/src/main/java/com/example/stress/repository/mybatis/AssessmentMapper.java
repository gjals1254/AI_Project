package com.example.stress.repository.mybatis;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.LevelCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** SQL: resources/mapper/AssessmentMapper.xml */
@Mapper
public interface AssessmentMapper {

    Assessment findById(@Param("id") Long id);

    List<Assessment> findPage(@Param("keyword") String keyword, @Param("level") Integer level,
                              @Param("offset") int offset, @Param("size") int size);

    long count(@Param("keyword") String keyword, @Param("level") Integer level);

    List<Assessment> findByRespondentCode(@Param("respondentCode") String respondentCode);

    List<LevelCount> countByLevel();

    Double averageRiskScore();

    long countSince(@Param("from") LocalDateTime from);

    int insert(Assessment assessment);

    int update(Assessment assessment);

    int deleteById(@Param("id") Long id);
}
