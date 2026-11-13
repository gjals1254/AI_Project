package com.example.trademark.repository.jpa;

import com.example.trademark.domain.SearchHistory;
import com.example.trademark.domain.SearchResult;
import com.example.trademark.repository.SearchHistoryStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaSearchHistoryStore implements SearchHistoryStore {

    private final SearchHistoryJpaRepository historyRepository;
    private final SearchResultJpaRepository resultRepository;

    public JpaSearchHistoryStore(SearchHistoryJpaRepository historyRepository,
                                 SearchResultJpaRepository resultRepository) {
        this.historyRepository = historyRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    public SearchHistory save(SearchHistory history) {
        return historyRepository.save(history);
    }

    @Override
    public void saveResults(List<SearchResult> results) {
        resultRepository.saveAll(results);
    }

    @Override
    public Optional<SearchHistory> findById(Long id) {
        return historyRepository.findById(id);
    }

    @Override
    public List<SearchResult> findResults(Long historyId) {
        return resultRepository.findByHistoryIdOrderByRankNoAsc(historyId);
    }

    @Override
    public List<SearchHistory> findPage(int page, int size) {
        return historyRepository
            .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
            .getContent();
    }

    @Override
    public long count() {
        return historyRepository.count();
    }

    @Override
    public long countSince(LocalDateTime from) {
        return historyRepository.countBySearchedAtGreaterThanEqual(from);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        resultRepository.deleteByHistoryId(id);
        historyRepository.deleteById(id);
    }
}
