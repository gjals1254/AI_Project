package com.example.air.dto;

/** 등급별 예측 건수 (JPA 생성자 표현식 / MyBatis setter 매핑 겸용) */
public class GradeCount {

    private Integer grade;
    private Long count;

    public GradeCount() {
    }

    public GradeCount(Integer grade, Long count) {
        this.grade = grade;
        this.count = count;
    }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
