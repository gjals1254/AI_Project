package com.example.trademark.repository.jpa;

import com.example.trademark.domain.Trademark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Spring Data JPA 리포지토리 (메서드 이름 쿼리 + JPQL) */
public interface TrademarkJpaRepository extends JpaRepository<Trademark, Long> {

    boolean existsByApplicationNo(String applicationNo);

    @Query("""
        select t from Trademark t
        where :kw is null
           or t.name like concat('%', :kw, '%')
           or t.applicant like concat('%', :kw, '%')
           or t.applicationNo like concat('%', :kw, '%')
        """)
    Page<Trademark> search(@Param("kw") String keyword, Pageable pageable);

    @Query("""
        select count(t) from Trademark t
        where :kw is null
           or t.name like concat('%', :kw, '%')
           or t.applicant like concat('%', :kw, '%')
           or t.applicationNo like concat('%', :kw, '%')
        """)
    long countSearch(@Param("kw") String keyword);

    @Query("""
        select t from Trademark t
        where t.featureVector is not null
          and t.modelName = :model
          and (:nc is null or t.niceClass = :nc)
        """)
    List<Trademark> findSearchTargets(@Param("model") String modelName, @Param("nc") Integer niceClass);

    long countByFeatureVectorIsNull();
}
