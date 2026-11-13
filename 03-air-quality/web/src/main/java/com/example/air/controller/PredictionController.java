package com.example.air.controller;

import com.example.air.domain.Station;
import com.example.air.service.AirQualityService;
import com.example.air.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 예측 이력 조회 */
@Controller
public class PredictionController {

    private final AirQualityService service;
    private final StationService stationService;

    public PredictionController(AirQualityService service, StationService stationService) {
        this.service = service;
        this.stationService = stationService;
    }

    @GetMapping("/predictions")
    public String list(@RequestParam(required = false) Long stationId,
                       @RequestParam(required = false) Integer grade,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Map<Long, String> stationNames = stationService.findAll(false).stream()
            .collect(Collectors.toMap(Station::getId, Station::getName));
        model.addAttribute("result", service.predictionPage(stationId, grade, page, 20));
        model.addAttribute("stations", stationService.findAll(false));
        model.addAttribute("stationNames", stationNames);
        model.addAttribute("stationId", stationId);
        model.addAttribute("grade", grade);
        return "prediction/list";
    }
}
