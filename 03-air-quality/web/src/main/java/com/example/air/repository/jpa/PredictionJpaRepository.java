package com.example.air.repository.jpa;

import com.example.air.domain.Prediction;
import com.example.air.dto.GradeCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PredictionJpaRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findFirstByMeasurementIdOrderByIdDesc(Long measurementId);

    List<Prediction> findByStationIdOrderByTargetDateDesc(Long stationId, Pageable pageable);

    @Query("""
        select p from Prediction p
        where (:sid is null or p.stationId = :sid)
          and (:grade is null or p.grade = :grade)
        """)
    Page<Prediction> search(@Param("sid") Long stationId, @Param("grade") Integer grade, Pageable pageable);

    /** JPQL 생성자 표현식으로 DTO 직접 조회 */
    @Query("""
        select new com.example.air.dto.GradeCount(p.grade, count(p))
        from Prediction p
        group by p.grade
        order by p.grade
        """)
    List<GradeCount> countByGrade();

    @Transactional
    @Modifying
    @Query("delete from Prediction p where p.measurementId = :mid")
    void deleteByMeasurementId(@Param("mid") Long measurementId);
}
