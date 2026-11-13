package com.example.air.controller;

import com.example.air.domain.Prediction;
import com.example.air.dto.MeasurementForm;
import com.example.air.exception.AppExceptions;
import com.example.air.service.AirQualityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API
 *  POST /api/v1/measurements            (Basic 인증 operator/operator1234) 측정 등록 + 예측
 *  GET  /api/v1/stations/{id}/forecast  최신 예보
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final AirQualityService service;

    public ApiController(AirQualityService service) {
        this.service = service;
    }

    @PostMapping("/measurements")
    public Map<String, Object> register(@Valid @RequestBody MeasurementForm form) {
        return toMap(service.registerMeasurement(form));
    }

    @GetMapping("/stations/{id}/forecast")
    public ResponseEntity<Map<String, Object>> forecast(@PathVariable Long id) {
        service.getStation(id);
        Prediction p = service.latestPrediction(id);
        return p == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(toMap(p));
    }

    private Map<String, Object> toMap(Prediction p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stationId", p.getStationId());
        m.put("targetDate", p.getTargetDate().toString());
        m.put("pm10", p.getPredPm10());
        m.put("pm25", p.getPredPm25());
        m.put("grade", p.getGrade());
        m.put("gradeLabel", p.getGradeLabel());
        m.put("advice", AirQualityService.advice(p.getGrade()));
        m.put("topFactors", service.factors(p));
        m.put("modelVersion", p.getModelVersion());
        return m;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        Map<String, Object> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(AppExceptions.NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AppExceptions.AiServerException.class)
    public ResponseEntity<Map<String, String>> aiDown(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}
