package com.example.trademark.dto;

import com.example.trademark.domain.Trademark;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 상표 등록/수정 폼 */
public class TrademarkForm {

    @NotBlank(message = "출원번호를 입력하세요.")
    @Size(max = 30)
    private String applicationNo;

    @NotBlank(message = "상표명을 입력하세요.")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String applicant;

    @Min(value = 1, message = "상품분류는 1~45류입니다.")
    @Max(value = 45, message = "상품분류는 1~45류입니다.")
    private Integer niceClass;

    @Size(max = 1000)
    private String description;

    public static TrademarkForm from(Trademark t) {
        TrademarkForm f = new TrademarkForm();
        f.setApplicationNo(t.getApplicationNo());
        f.setName(t.getName());
        f.setApplicant(t.getApplicant());
        f.setNiceClass(t.getNiceClass());
        f.setDescription(t.getDescription());
        return f;
    }

    public void applyTo(Trademark t) {
        t.setApplicationNo(applicationNo.trim());
        t.setName(name.trim());
        t.setApplicant(applicant);
        t.setNiceClass(niceClass);
        t.setDescription(description);
    }

    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }
    public Integer getNiceClass() { return niceClass; }
    public void setNiceClass(Integer niceClass) { this.niceClass = niceClass; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
