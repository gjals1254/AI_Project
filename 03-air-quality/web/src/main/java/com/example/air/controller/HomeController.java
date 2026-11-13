package com.example.air.controller;

import com.example.air.ai.AirPredictionClient;
import com.example.air.service.AirQualityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 메인 대시보드: 측정소별 내일 예보 */
@Controller
public class HomeController {

    private final AirQualityService service;
    private final AirPredictionClient client;

    public HomeController(AirQualityService service, AirPredictionClient client) {
        this.service = service;
        this.client = client;
    }

    @GetMapping("/")
    public String index(Model model) {
        var statuses = service.dashboard();
        model.addAttribute("statuses", statuses);
        // 측정소별 최근 14일(어제까지) 실측 등급 띠: 날짜 칸마다 그날 측정값, 없으면 null(빈 칸)
        java.time.LocalDate end = java.time.LocalDate.now().minusDays(1);
        java.time.LocalDate start = end.minusDays(13);
        java.util.Map<Long, java.util.List<com.example.air.domain.Measurement>> strips = new java.util.HashMap<>();
        for (var s : statuses) {
            java.util.Map<java.time.LocalDate, com.example.air.domain.Measurement> byDate = new java.util.HashMap<>();
            for (var m : service.recentMeasurements(s.station().getId(), 14)) {
                byDate.put(m.getMeasureDate(), m);
            }
            java.util.List<com.example.air.domain.Measurement> days = new java.util.ArrayList<>();
            for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                days.add(byDate.get(d));
            }
            strips.put(s.station().getId(), days);
        }
        model.addAttribute("strips", strips);
        model.addAttribute("stripStart", start);
        model.addAttribute("ai", client.health());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
