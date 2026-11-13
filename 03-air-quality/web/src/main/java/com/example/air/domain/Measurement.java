package com.example.air.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 측정소별 일평균 측정 데이터 (대기오염 + 기상) */
@Entity
@Table(name = "measurement")
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long stationId;
    private LocalDate measureDate;
    private double pm10;
    private double pm25;
    private double o3;
    private double no2;
    private double co;
    private double so2;
    private double temperature;
    private double humidity;
    private double windSpeed;
    private double precipitation;
    private LocalDateTime createdAt;

    public int pm10Grade() {
        return AirGrade.pm10(pm10);
    }

    public int pm25Grade() {
        return AirGrade.pm25(pm25);
    }

    public int grade() {
        return Math.max(pm10Grade(), pm25Grade());
    }

    public String gradeLabel() {
        return AirGrade.label(grade());
    }

    public String color() {
        return AirGrade.color(grade());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public LocalDate getMeasureDate() { return measureDate; }
    public void setMeasureDate(LocalDate measureDate) { this.measureDate = measureDate; }
    public double getPm10() { return pm10; }
    public void setPm10(double pm10) { this.pm10 = pm10; }
    public double getPm25() { return pm25; }
    public void setPm25(double pm25) { this.pm25 = pm25; }
    public double getO3() { return o3; }
    public void setO3(double o3) { this.o3 = o3; }
    public double getNo2() { return no2; }
    public void setNo2(double no2) { this.no2 = no2; }
    public double getCo() { return co; }
    public void setCo(double co) { this.co = co; }
    public double getSo2() { return so2; }
    public void setSo2(double so2) { this.so2 = so2; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public double getPrecipitation() { return precipitation; }
    public void setPrecipitation(double precipitation) { this.precipitation = precipitation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
