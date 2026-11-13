package com.example.stress.repository;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.LevelCount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 평가 저장소 추상화 (JpaAssessmentStore / MybatisAssessmentStore) */
public interface AssessmentStore {

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(Long id);

    /** keyword: 대상자 코드 부분일치, level: null 이면 전체 */
    List<Assessment> findPage(String keyword, Integer level, int page, int size);

    long count(String keyword, Integer level);

    /** 같은 대상자의 이전 평가 (추이 비교) */
    List<Assessment> findByRespondentCode(String respondentCode);

    List<LevelCount> countByLevel();

    Double averageRiskScore();

    long countSince(LocalDateTime from);

    void deleteById(Long id);
}
