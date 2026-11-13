package com.example.trademark.repository;

import com.example.trademark.domain.SearchHistory;
import com.example.trademark.domain.SearchResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 검색 이력 저장소 추상화 (JPA / MyBatis 구현) */
public interface SearchHistoryStore {

    SearchHistory save(SearchHistory history);

    void saveResults(List<SearchResult> results);

    Optional<SearchHistory> findById(Long id);

    List<SearchResult> findResults(Long historyId);

    List<SearchHistory> findPage(int page, int size);

    long count();

    long countSince(LocalDateTime from);

    void deleteById(Long id);
}
