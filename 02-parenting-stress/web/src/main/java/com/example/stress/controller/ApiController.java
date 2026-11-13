package com.example.stress.controller;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.AssessmentForm;
import com.example.stress.exception.AppExceptions;
import com.example.stress.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

/** REST API (Basic 인증: counselor / counselor1234) */
@RestController
@RequestMapping("/api/v1/assessments")
public class ApiController {

    private final AssessmentService service;

    public ApiController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody AssessmentForm form, Authentication auth) {
        return toMap(service.create(form, auth.getName()));
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return toMap(service.get(id));
    }

    private Map<String, Object> toMap(Assessment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("respondentCode", a.getRespondentCode());
        m.put("level", a.getStressLevel());
        m.put("label", a.getStressLabel());
        m.put("riskScore", a.getRiskScore());
        Map<String, Double> prob = new LinkedHashMap<>();
        prob.put("정상", a.getProbNormal());
        prob.put("주의", a.getProbCaution());
        prob.put("고위험", a.getProbHigh());
        m.put("probabilities", prob);
        m.put("topFactors", service.factors(a));
        m.put("guidance", AssessmentService.guidance(a.getStressLevel()));
        m.put("modelVersion", a.getModelVersion());
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
