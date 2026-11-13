package com.example.trademark.repository.mybatis;

import com.example.trademark.domain.SearchHistory;
import com.example.trademark.domain.SearchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SearchHistoryMapper {

    int insertHistory(SearchHistory history);

    int insertResults(@Param("results") List<SearchResult> results);

    SearchHistory findById(@Param("id") Long id);

    List<SearchResult> findResults(@Param("historyId") Long historyId);

    List<SearchHistory> findPage(@Param("offset") int offset, @Param("size") int size);

    long count();

    long countSince(@Param("from") LocalDateTime from);

    int deleteResults(@Param("historyId") Long historyId);

    int deleteById(@Param("id") Long id);
}
