package com.example.stress.repository.jpa;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.LevelCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AssessmentJpaRepository extends JpaRepository<Assessment, Long> {

    @Query("""
        select a from Assessment a
        where (:kw is null or a.respondentCode like concat('%', :kw, '%'))
          and (:level is null or a.stressLevel = :level)
        """)
    Page<Assessment> search(@Param("kw") String keyword, @Param("level") Integer level, Pageable pageable);

    @Query("""
        select count(a) from Assessment a
        where (:kw is null or a.respondentCode like concat('%', :kw, '%'))
          and (:level is null or a.stressLevel = :level)
        """)
    long countSearch(@Param("kw") String keyword, @Param("level") Integer level);

    List<Assessment> findByRespondentCodeOrderByCreatedAtAsc(String respondentCode);

    /** JPQL 생성자 표현식으로 DTO 직접 조회 */
    @Query("""
        select new com.example.stress.dto.LevelCount(a.stressLevel, count(a))
        from Assessment a
        where a.stressLevel is not null
        group by a.stressLevel
        order by a.stressLevel
        """)
    List<LevelCount> countByLevel();

    @Query("select avg(a.riskScore) from Assessment a")
    Double averageRiskScore();

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);
}
