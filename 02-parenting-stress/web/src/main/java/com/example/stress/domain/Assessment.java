package com.example.stress.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 스트레스 평가 1건 = 설문 입력값 + AI 예측 결과 */
@Entity
@Table(name = "assessment")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String respondentCode;

    // ---- 설문 입력값 ----
    private int parentAge;
    private int childCount;
    private int youngestChildAge;
    private double sleepHours;
    private double weeklyWorkHours;
    private int parentingEfficacy;
    private int socialSupport;
    private int familyEnvironment;
    private int resilience;
    private int qualityOfLife;

    // ---- AI 예측 결과 ----
    private Integer stressLevel;
    @Column(length = 10)
    private String stressLabel;
    private Double riskScore;
    private Double probNormal;
    private Double probCaution;
    private Double probHigh;
    @Column(columnDefinition = "TEXT")
    private String topFactors;
    @Column(length = 50)
    private String modelVersion;

    @Column(length = 500)
    private String memo;
    @Column(length = 50)
    private String createdBy;
    private LocalDateTime createdAt;

    public boolean isPredicted() {
        return stressLevel != null;
    }

    /** Bootstrap 색상 */
    public String levelColor() {
        if (stressLevel == null) {
            return "secondary";
        }
        return switch (stressLevel) {
            case 0 -> "success";
            case 1 -> "warning";
            default -> "danger";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRespondentCode() { return respondentCode; }
    public void setRespondentCode(String respondentCode) { this.respondentCode = respondentCode; }
    public int getParentAge() { return parentAge; }
    public void setParentAge(int parentAge) { this.parentAge = parentAge; }
    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }
    public int getYoungestChildAge() { return youngestChildAge; }
    public void setYoungestChildAge(int youngestChildAge) { this.youngestChildAge = youngestChildAge; }
    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }
    public double getWeeklyWorkHours() { return weeklyWorkHours; }
    public void setWeeklyWorkHours(double weeklyWorkHours) { this.weeklyWorkHours = weeklyWorkHours; }
    public int getParentingEfficacy() { return parentingEfficacy; }
    public void setParentingEfficacy(int parentingEfficacy) { this.parentingEfficacy = parentingEfficacy; }
    public int getSocialSupport() { return socialSupport; }
    public void setSocialSupport(int socialSupport) { this.socialSupport = socialSupport; }
    public int getFamilyEnvironment() { return familyEnvironment; }
    public void setFamilyEnvironment(int familyEnvironment) { this.familyEnvironment = familyEnvironment; }
    public int getResilience() { return resilience; }
    public void setResilience(int resilience) { this.resilience = resilience; }
    public int getQualityOfLife() { return qualityOfLife; }
    public void setQualityOfLife(int qualityOfLife) { this.qualityOfLife = qualityOfLife; }
    public Integer getStressLevel() { return stressLevel; }
    public void setStressLevel(Integer stressLevel) { this.stressLevel = stressLevel; }
    public String getStressLabel() { return stressLabel; }
    public void setStressLabel(String stressLabel) { this.stressLabel = stressLabel; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public Double getProbNormal() { return probNormal; }
    public void setProbNormal(Double probNormal) { this.probNormal = probNormal; }
    public Double getProbCaution() { return probCaution; }
    public void setProbCaution(Double probCaution) { this.probCaution = probCaution; }
    public Double getProbHigh() { return probHigh; }
    public void setProbHigh(Double probHigh) { this.probHigh = probHigh; }
    public String getTopFactors() { return topFactors; }
    public void setTopFactors(String topFactors) { this.topFactors = topFactors; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
