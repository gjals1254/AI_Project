package com.example.trademark.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 유사 상표 검색 이력 */
@Entity
@Table(name = "search_history")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String queryImagePath;

    private Integer niceClass;

    @Column(length = 50)
    private String modelName;

    private int resultCount;

    private Double topScore;

    private Long topTrademarkId;

    @Column(length = 45)
    private String clientIp;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQueryImagePath() { return queryImagePath; }
    public void setQueryImagePath(String queryImagePath) { this.queryImagePath = queryImagePath; }
    public Integer getNiceClass() { return niceClass; }
    public void setNiceClass(Integer niceClass) { this.niceClass = niceClass; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }
    public Double getTopScore() { return topScore; }
    public void setTopScore(Double topScore) { this.topScore = topScore; }
    public Long getTopTrademarkId() { return topTrademarkId; }
    public void setTopTrademarkId(Long topTrademarkId) { this.topTrademarkId = topTrademarkId; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public LocalDateTime getSearchedAt() { return searchedAt; }
    public void setSearchedAt(LocalDateTime searchedAt) { this.searchedAt = searchedAt; }
}
