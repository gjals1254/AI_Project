package com.example.air.controller;

import com.example.air.domain.Prediction;
import com.example.air.service.AirQualityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** 측정소 상세: 실측/예측 추이 차트, 내일 예보, 영향 요인 */
@Controller
public class StationController {

    private final AirQualityService service;

    public StationController(AirQualityService service) {
        this.service = service;
    }

    @GetMapping("/stations/{id}")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "30") int days, Model model) {
        Prediction latest = service.latestPrediction(id);
        model.addAttribute("station", service.getStation(id));
        model.addAttribute("chart", service.chart(id, Math.min(Math.max(days, 7), 365)));
        model.addAttribute("days", days);
        model.addAttribute("forecast", latest);
        model.addAttribute("factors", service.factors(latest));
        model.addAttribute("advice", latest == null ? "" : AirQualityService.advice(latest.getGrade()));
        model.addAttribute("recent", service.recentMeasurements(id, 10));
        return "station/detail";
    }
}
