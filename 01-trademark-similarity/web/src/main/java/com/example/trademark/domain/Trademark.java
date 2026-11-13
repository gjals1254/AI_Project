package com.example.trademark.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 상표 엔티티.
 * JPA 모드에서는 @Entity 로, MyBatis 모드에서는 resultType(POJO) 으로 같은 클래스를 사용한다.
 */
@Entity
@Table(name = "trademark")
public class Trademark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String applicationNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String applicant;

    private Integer niceClass;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String imagePath;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String featureVector;

    @Column(length = 50)
    private String modelName;

    private Integer vectorDim;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean hasVector() {
        return featureVector != null && !featureVector.isBlank();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getFeatureVector() { return featureVector; }
    public void setFeatureVector(String featureVector) { this.featureVector = featureVector; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getVectorDim() { return vectorDim; }
    public void setVectorDim(Integer vectorDim) { this.vectorDim = vectorDim; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
