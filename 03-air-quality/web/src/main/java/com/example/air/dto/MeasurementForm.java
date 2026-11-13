package com.example.air.dto;

import com.example.air.domain.Measurement;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 측정 데이터 입력 폼 (범위는 AI 서버 검증과 동일) */
public class MeasurementForm {

    @NotNull(message = "측정소를 선택하세요.")
    private Long stationId;

    @NotNull @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate measureDate = LocalDate.now();

    @NotNull @DecimalMin("0") @DecimalMax("1000") private Double pm10;
    @NotNull @DecimalMin("0") @DecimalMax("500") private Double pm25;
    @NotNull @DecimalMin("0") @DecimalMax("0.5") private Double o3;
    @NotNull @DecimalMin("0") @DecimalMax("0.5") private Double no2;
    @NotNull @DecimalMin("0") @DecimalMax("10") private Double co;
    @NotNull @DecimalMin("0") @DecimalMax("0.5") private Double so2;
    @NotNull @DecimalMin("-30") @DecimalMax("45") private Double temperature;
    @NotNull @DecimalMin("0") @DecimalMax("100") private Double humidity;
    @NotNull @DecimalMin("0") @DecimalMax("40") private Double windSpeed;
    @NotNull @DecimalMin("0") @DecimalMax("500") private Double precipitation = 0.0;

    public Measurement toEntity() {
        Measurement m = new Measurement();
        m.setStationId(stationId);
        m.setMeasureDate(measureDate);
        copyValuesTo(m);
        return m;
    }

    public void copyValuesTo(Measurement m) {
        m.setPm10(pm10);
        m.setPm25(pm25);
        m.setO3(o3);
        m.setNo2(no2);
        m.setCo(co);
        m.setSo2(so2);
        m.setTemperature(temperature);
        m.setHumidity(humidity);
        m.setWindSpeed(windSpeed);
        m.setPrecipitation(precipitation);
    }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public LocalDate getMeasureDate() { return measureDate; }
    public void setMeasureDate(LocalDate measureDate) { this.measureDate = measureDate; }
    public Double getPm10() { return pm10; }
    public void setPm10(Double pm10) { this.pm10 = pm10; }
    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }
    public Double getO3() { return o3; }
    public void setO3(Double o3) { this.o3 = o3; }
    public Double getNo2() { return no2; }
    public void setNo2(Double no2) { this.no2 = no2; }
    public Double getCo() { return co; }
    public void setCo(Double co) { this.co = co; }
    public Double getSo2() { return so2; }
    public void setSo2(Double so2) { this.so2 = so2; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    public Double getPrecipitation() { return precipitation; }
    public void setPrecipitation(Double precipitation) { this.precipitation = precipitation; }
}
