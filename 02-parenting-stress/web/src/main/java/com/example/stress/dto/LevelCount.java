package com.example.stress.dto;

/**
 * 수준별 건수 (통계).
 * JPA 는 생성자 표현식(select new ...)으로, MyBatis 는 기본 생성자 + setter 로 채운다.
 */
public class LevelCount {

    private Integer level;
    private Long count;

    public LevelCount() {
    }

    public LevelCount(Integer level, Long count) {
        this.level = level;
        this.count = count;
    }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
