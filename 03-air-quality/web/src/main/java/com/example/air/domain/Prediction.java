package com.example.air.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 다음날 대기질 예측 결과 */
@Entity
@Table(name = "prediction")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long stationId;
    private Long measurementId;
    private LocalDate targetDate;
    private double predPm10;
    private double predPm25;
    // 숫자 뒤 대문자는 기본 네이밍 전략이 '_' 를 넣지 않아(pm10grade) 직접 지정
    @Column(name = "pm10_grade")
    private int pm10Grade;
    @Column(name = "pm25_grade")
    private int pm25Grade;
    private int grade;
    @Column(length = 10)
    private String gradeLabel;
    @Column(columnDefinition = "TEXT")
    private String topFactors;
    @Column(length = 50)
    private String modelVersion;
    private LocalDateTime createdAt;

    public String color() {
        return AirGrade.color(grade);
    }

    public String pm10Color() {
        return AirGrade.color(pm10Grade);
    }

    public String pm25Color() {
        return AirGrade.color(pm25Grade);
    }

    public String pm10Label() {
        return AirGrade.label(pm10Grade);
    }

    public String pm25Label() {
        return AirGrade.label(pm25Grade);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public Long getMeasurementId() { return measurementId; }
    public void setMeasurementId(Long measurementId) { this.measurementId = measurementId; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public double getPredPm10() { return predPm10; }
    public void setPredPm10(double predPm10) { this.predPm10 = predPm10; }
    public double getPredPm25() { return predPm25; }
    public void setPredPm25(double predPm25) { this.predPm25 = predPm25; }
    public int getPm10Grade() { return pm10Grade; }
    public void setPm10Grade(int pm10Grade) { this.pm10Grade = pm10Grade; }
    public int getPm25Grade() { return pm25Grade; }
    public void setPm25Grade(int pm25Grade) { this.pm25Grade = pm25Grade; }
    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }
    public String getGradeLabel() { return gradeLabel; }
    public void setGradeLabel(String gradeLabel) { this.gradeLabel = gradeLabel; }
    public String getTopFactors() { return topFactors; }
    public void setTopFactors(String topFactors) { this.topFactors = topFactors; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
