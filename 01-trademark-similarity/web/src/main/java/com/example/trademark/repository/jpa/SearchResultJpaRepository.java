package com.example.trademark.repository.jpa;

import com.example.trademark.domain.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchResultJpaRepository extends JpaRepository<SearchResult, Long> {

    List<SearchResult> findByHistoryIdOrderByRankNoAsc(Long historyId);

    @Modifying
    @Query("delete from SearchResult r where r.historyId = :historyId")
    void deleteByHistoryId(@Param("historyId") Long historyId);
}
