package com.example.stress.dto;

import com.example.stress.domain.Assessment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 설문 입력 폼 (범위는 AI 서버 검증 범위와 동일) */
public class AssessmentForm {

    @NotBlank(message = "대상자 코드를 입력하세요.")
    @Size(max = 50)
    private String respondentCode;

    @NotNull @Min(18) @Max(70)
    private Integer parentAge;

    @NotNull @Min(1) @Max(6)
    private Integer childCount;

    @NotNull @Min(0) @Max(18)
    private Integer youngestChildAge;

    @NotNull @DecimalMin("3") @DecimalMax("10")
    private Double sleepHours;

    @NotNull @DecimalMin("0") @DecimalMax("80")
    private Double weeklyWorkHours;

    @NotNull @Min(0) @Max(100)
    private Integer parentingEfficacy;

    @NotNull @Min(0) @Max(100)
    private Integer socialSupport;

    @NotNull @Min(0) @Max(100)
    private Integer familyEnvironment;

    @NotNull @Min(0) @Max(100)
    private Integer resilience;

    @NotNull @Min(0) @Max(100)
    private Integer qualityOfLife;

    @Size(max = 500)
    private String memo;

    public Assessment toEntity() {
        Assessment a = new Assessment();
        a.setRespondentCode(respondentCode.trim());
        a.setParentAge(parentAge);
        a.setChildCount(childCount);
        a.setYoungestChildAge(youngestChildAge);
        a.setSleepHours(sleepHours);
        a.setWeeklyWorkHours(weeklyWorkHours);
        a.setParentingEfficacy(parentingEfficacy);
        a.setSocialSupport(socialSupport);
        a.setFamilyEnvironment(familyEnvironment);
        a.setResilience(resilience);
        a.setQualityOfLife(qualityOfLife);
        a.setMemo(memo);
        return a;
    }

    public String getRespondentCode() { return respondentCode; }
    public void setRespondentCode(String respondentCode) { this.respondentCode = respondentCode; }
    public Integer getParentAge() { return parentAge; }
    public void setParentAge(Integer parentAge) { this.parentAge = parentAge; }
    public Integer getChildCount() { return childCount; }
    public void setChildCount(Integer childCount) { this.childCount = childCount; }
    public Integer getYoungestChildAge() { return youngestChildAge; }
    public void setYoungestChildAge(Integer youngestChildAge) { this.youngestChildAge = youngestChildAge; }
    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double sleepHours) { this.sleepHours = sleepHours; }
    public Double getWeeklyWorkHours() { return weeklyWorkHours; }
    public void setWeeklyWorkHours(Double weeklyWorkHours) { this.weeklyWorkHours = weeklyWorkHours; }
    public Integer getParentingEfficacy() { return parentingEfficacy; }
    public void setParentingEfficacy(Integer parentingEfficacy) { this.parentingEfficacy = parentingEfficacy; }
    public Integer getSocialSupport() { return socialSupport; }
    public void setSocialSupport(Integer socialSupport) { this.socialSupport = socialSupport; }
    public Integer getFamilyEnvironment() { return familyEnvironment; }
    public void setFamilyEnvironment(Integer familyEnvironment) { this.familyEnvironment = familyEnvironment; }
    public Integer getResilience() { return resilience; }
    public void setResilience(Integer resilience) { this.resilience = resilience; }
    public Integer getQualityOfLife() { return qualityOfLife; }
    public void setQualityOfLife(Integer qualityOfLife) { this.qualityOfLife = qualityOfLife; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
