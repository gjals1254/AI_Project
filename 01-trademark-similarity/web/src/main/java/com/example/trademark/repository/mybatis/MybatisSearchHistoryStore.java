package com.example.trademark.repository.mybatis;

import com.example.trademark.domain.SearchHistory;
import com.example.trademark.domain.SearchResult;
import com.example.trademark.repository.SearchHistoryStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
public class MybatisSearchHistoryStore implements SearchHistoryStore {

    private final SearchHistoryMapper mapper;

    public MybatisSearchHistoryStore(SearchHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SearchHistory save(SearchHistory history) {
        mapper.insertHistory(history);
        return history;
    }

    @Override
    public void saveResults(List<SearchResult> results) {
        if (!results.isEmpty()) {
            mapper.insertResults(results);
        }
    }

    @Override
    public Optional<SearchHistory> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    @Override
    public List<SearchResult> findResults(Long historyId) {
        return mapper.findResults(historyId);
    }

    @Override
    public List<SearchHistory> findPage(int page, int size) {
        return mapper.findPage(page * size, size);
    }

    @Override
    public long count() {
        return mapper.count();
    }

    @Override
    public long countSince(LocalDateTime from) {
        return mapper.countSince(from);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        mapper.deleteResults(id);
        mapper.deleteById(id);
    }
}
