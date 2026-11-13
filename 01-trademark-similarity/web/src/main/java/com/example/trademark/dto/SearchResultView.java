package com.example.trademark.dto;

import com.example.trademark.domain.SearchHistory;

import java.util.List;

/** 검색 결과 화면 모델 */
public record SearchResultView(SearchHistory history, List<SimilarItem> items) {

    public List<String> chartLabels() {
        return items.stream()
            .map(i -> i.rank() + ". " + (i.trademark() == null ? "(삭제됨)" : i.trademark().getName()))
            .toList();
    }

    public List<Integer> chartScores() {
        return items.stream().map(SimilarItem::percent).toList();
    }
}
