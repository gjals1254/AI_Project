package com.example.trademark.repository.mybatis;

import com.example.trademark.domain.Trademark;
import com.example.trademark.repository.TrademarkStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
public class MybatisTrademarkStore implements TrademarkStore {

    private final TrademarkMapper mapper;

    public MybatisTrademarkStore(TrademarkMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Trademark save(Trademark trademark) {
        if (trademark.getId() == null) {
            mapper.insert(trademark);   // useGeneratedKeys 로 id 가 채워진다
        } else {
            mapper.update(trademark);
        }
        return trademark;
    }

    @Override
    public Optional<Trademark> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    @Override
    public boolean existsByApplicationNo(String applicationNo) {
        return mapper.countByApplicationNo(applicationNo) > 0;
    }

    @Override
    public List<Trademark> findPage(String keyword, int page, int size) {
        return mapper.findPage(keyword, page * size, size);
    }

    @Override
    public long count(String keyword) {
        return mapper.count(keyword);
    }

    @Override
    public List<Trademark> findSearchTargets(String modelName, Integer niceClass) {
        return mapper.findSearchTargets(modelName, niceClass);
    }

    @Override
    public List<Trademark> findAll() {
        return mapper.findAll();
    }

    @Override
    public List<Trademark> findByIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : mapper.findByIds(ids);
    }

    @Override
    public long countWithoutVector() {
        return mapper.countWithoutVector();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
