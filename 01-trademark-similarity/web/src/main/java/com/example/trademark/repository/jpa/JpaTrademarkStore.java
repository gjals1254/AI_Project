package com.example.trademark.repository.jpa;

import com.example.trademark.domain.Trademark;
import com.example.trademark.repository.TrademarkStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaTrademarkStore implements TrademarkStore {

    private final TrademarkJpaRepository repository;

    public JpaTrademarkStore(TrademarkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Trademark save(Trademark trademark) {
        return repository.save(trademark);
    }

    @Override
    public Optional<Trademark> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public boolean existsByApplicationNo(String applicationNo) {
        return repository.existsByApplicationNo(applicationNo);
    }

    @Override
    public List<Trademark> findPage(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return repository.search(keyword, pageable).getContent();
    }

    @Override
    public long count(String keyword) {
        return repository.countSearch(keyword);
    }

    @Override
    public List<Trademark> findSearchTargets(String modelName, Integer niceClass) {
        return repository.findSearchTargets(modelName, niceClass);
    }

    @Override
    public List<Trademark> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public List<Trademark> findByIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : repository.findAllById(ids);
    }

    @Override
    public long countWithoutVector() {
        return repository.countByFeatureVectorIsNull();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
