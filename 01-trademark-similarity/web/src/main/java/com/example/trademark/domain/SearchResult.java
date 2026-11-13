package com.example.trademark.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 검색 1건에 대한 순위별 결과 (연관관계 없이 ID 로만 참조 → JPA/MyBatis 매핑을 동일하게 유지) */
@Entity
@Table(name = "search_result")
public class SearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long historyId;

    private Long trademarkId;

    private int rankNo;

    private double score;

    public SearchResult() {
    }

    public SearchResult(Long historyId, Long trademarkId, int rankNo, double score) {
        this.historyId = historyId;
        this.trademarkId = trademarkId;
        this.rankNo = rankNo;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getHistoryId() { return historyId; }
    public void setHistoryId(Long historyId) { this.historyId = historyId; }
    public Long getTrademarkId() { return trademarkId; }
    public void setTrademarkId(Long trademarkId) { this.trademarkId = trademarkId; }
    public int getRankNo() { return rankNo; }
    public void setRankNo(int rankNo) { this.rankNo = rankNo; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
