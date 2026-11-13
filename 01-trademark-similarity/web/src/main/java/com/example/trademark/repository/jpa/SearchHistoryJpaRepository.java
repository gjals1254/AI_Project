package com.example.trademark.repository.jpa;

import com.example.trademark.domain.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SearchHistoryJpaRepository extends JpaRepository<SearchHistory, Long> {

    long countBySearchedAtGreaterThanEqual(LocalDateTime from);
}
