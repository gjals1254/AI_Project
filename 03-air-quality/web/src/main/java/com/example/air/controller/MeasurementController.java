package com.example.air.controller;

import com.example.air.domain.Prediction;
import com.example.air.dto.MeasurementForm;
import com.example.air.service.AirQualityService;
import com.example.air.service.StationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 측정 데이터 등록(→ 자동 예측) 및 목록 */
@Controller
@RequestMapping("/measurements")
public class MeasurementController {

    private final AirQualityService service;
    private final StationService stationService;

    public MeasurementController(AirQualityService service, StationService stationService) {
        this.service = service;
        this.stationService = stationService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long stationId,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", service.measurementPage(stationId, page, 20));
        model.addAttribute("stations", stationService.findAll(false));
        model.addAttribute("stationId", stationId);
        return "measurement/list";
    }

    @GetMapping("/new")
    public String form(@RequestParam(required = false) Long stationId, Model model) {
        MeasurementForm form = new MeasurementForm();
        form.setStationId(stationId);
        model.addAttribute("form", form);
        model.addAttribute("stations", stationService.findAll(true));
        return "measurement/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") MeasurementForm form, BindingResult binding,
                         Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("stations", stationService.findAll(true));
            return "measurement/form";
        }
        Prediction p = service.registerMeasurement(form);
        ra.addFlashAttribute("message",
            p.getTargetDate() + " 예보: " + p.getGradeLabel() + " (PM2.5 " + p.getPredPm25() + "㎍/㎥)");
        return "redirect:/stations/" + form.getStationId();
    }

    @PostMapping("/{id}/repredict")
    public String repredict(@PathVariable Long id, RedirectAttributes ra) {
        Prediction p = service.repredict(id);
        ra.addFlashAttribute("message", "재예측 완료: " + p.getGradeLabel());
        return "redirect:/stations/" + p.getStationId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.deleteMeasurement(id);
        ra.addFlashAttribute("message", "측정 데이터를 삭제했습니다.");
        return "redirect:/measurements";
    }
}
