package com.example.air.service;

import com.example.air.domain.Station;
import com.example.air.dto.MeasurementForm;
import com.example.air.dto.Views.SyncResult;
import com.example.air.openapi.OpenApiClient;
import com.example.air.openapi.OpenApiClient.AirDaily;
import com.example.air.openapi.OpenApiClient.WeatherDaily;
import com.example.air.openapi.OpenMeteoClient;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 측정소별 대기오염·기상 일평균을 받아 측정값으로 저장하고 예측까지 실행.
 * app.sync.source = openmeteo(키 불필요, 위경도·모델 추정값) | publicdata(에어코리아·기상청 실측, 인증키 필요)
 */
@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final StationService stationService;
    private final OpenApiClient api;
    private final OpenMeteoClient meteo;
    private final AirQualityService airQualityService;
    private final Validator validator;
    private final boolean openMeteo;

    public DataSyncService(StationService stationService, OpenApiClient api, OpenMeteoClient meteo,
                           AirQualityService airQualityService, Validator validator,
                           @Value("${app.sync.source:openmeteo}") String source) {
        this.stationService = stationService;
        this.api = api;
        this.meteo = meteo;
        this.airQualityService = airQualityService;
        this.validator = validator;
        this.openMeteo = !"publicdata".equalsIgnoreCase(source);
    }

    /** Open-Meteo 는 키가 필요 없고, 공공데이터는 인증키가 있어야 수집 가능 */
    public boolean ready() {
        return openMeteo || api.hasKey();
    }

    public String sourceName() {
        return openMeteo ? "Open-Meteo (모델 추정값)" : "에어코리아 + 기상청";
    }

    /** 매일 정해진 시각에 어제치 수집 (기상청 일자료는 D-1 까지 제공) */
    @Scheduled(cron = "${app.sync.cron:0 0 11 * * *}")
    public void syncYesterday() {
        if (!ready()) {
            log.warn("공공데이터 인증키 미설정 - 자동 수집 건너뜀 (app.openapi.service-key)");
            return;
        }
        sync(LocalDate.now().minusDays(1));
    }

    public SyncResult sync(LocalDate date) {
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (Station s : stationService.findAll(true)) {
            if (!mapped(s)) {
                continue;
            }
            try {
                airQualityService.registerMeasurement(toForm(s, date));
                ok++;
            } catch (RuntimeException e) {
                errors.add(s.getName() + ": " + e.getMessage());
                log.warn("API 수집 실패 {} {}: {}", s.getName(), date, e.getMessage());
            }
        }
        log.info("API 수집 {}: 성공 {}건, 실패 {}건", date, ok, errors.size());
        return new SyncResult(date, ok, errors.size(), errors);
    }

    private boolean mapped(Station s) {
        return openMeteo ? s.getLatitude() != null && s.getLongitude() != null
            : s.getAirkoreaName() != null && s.getKmaStnId() != null;
    }

    private MeasurementForm toForm(Station s, LocalDate date) {
        AirDaily a = openMeteo ? meteo.fetchAir(s.getLatitude(), s.getLongitude(), date)
            : api.fetchAir(s.getAirkoreaName(), date);
        WeatherDaily w = openMeteo ? meteo.fetchWeather(s.getLatitude(), s.getLongitude(), date)
            : api.fetchWeather(s.getKmaStnId(), date);
        MeasurementForm f = new MeasurementForm();
        f.setStationId(s.getId());
        f.setMeasureDate(date);
        f.setPm10(require(a.pm10(), "PM10"));
        f.setPm25(require(a.pm25(), "PM2.5"));
        f.setO3(require(a.o3(), "O3"));
        f.setNo2(require(a.no2(), "NO2"));
        f.setCo(require(a.co(), "CO"));
        f.setSo2(require(a.so2(), "SO2"));
        f.setTemperature(w.temperature());
        f.setHumidity(w.humidity());
        f.setWindSpeed(w.windSpeed());
        f.setPrecipitation(w.precipitation());
        for (ConstraintViolation<MeasurementForm> v : validator.validate(f)) {
            throw new IllegalArgumentException(v.getPropertyPath() + " " + v.getMessage());
        }
        return f;
    }

    private static double require(Double v, String name) {
        if (v == null) {
            throw new IllegalStateException(name + " 결측 (그날 측정값 없음)");
        }
        return v;
    }
}
