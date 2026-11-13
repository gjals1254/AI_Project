package com.example.trademark.repository;

import com.example.trademark.domain.Trademark;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 상표 저장소 추상화.
 * 구현체: JpaTrademarkStore(@Profile "jpa"), MybatisTrademarkStore(@Profile "mybatis")
 */
public interface TrademarkStore {

    /** id 가 없으면 INSERT, 있으면 UPDATE */
    Trademark save(Trademark trademark);

    Optional<Trademark> findById(Long id);

    boolean existsByApplicationNo(String applicationNo);

    /** keyword(상표명/출원인/출원번호) 검색, page 는 0부터 */
    List<Trademark> findPage(String keyword, int page, int size);

    long count(String keyword);

    /** 유사도 비교 대상: 같은 모델로 만든 벡터만, niceClass 가 null 이면 전체 */
    List<Trademark> findSearchTargets(String modelName, Integer niceClass);

    List<Trademark> findAll();

    List<Trademark> findByIds(Collection<Long> ids);

    long countWithoutVector();

    void deleteById(Long id);
}
