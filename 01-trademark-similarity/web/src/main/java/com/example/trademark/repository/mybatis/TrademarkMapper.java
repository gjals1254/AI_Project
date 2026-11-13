package com.example.trademark.repository.mybatis;

import com.example.trademark.domain.Trademark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** SQL 은 resources/mapper/TrademarkMapper.xml 에 있다. */
@Mapper
public interface TrademarkMapper {

    Trademark findById(@Param("id") Long id);

    int countByApplicationNo(@Param("applicationNo") String applicationNo);

    List<Trademark> findPage(@Param("keyword") String keyword,
                             @Param("offset") int offset,
                             @Param("size") int size);

    long count(@Param("keyword") String keyword);

    List<Trademark> findSearchTargets(@Param("modelName") String modelName,
                                      @Param("niceClass") Integer niceClass);

    List<Trademark> findAll();

    List<Trademark> findByIds(@Param("ids") Collection<Long> ids);

    long countWithoutVector();

    int insert(Trademark trademark);

    int update(Trademark trademark);

    int deleteById(@Param("id") Long id);
}
