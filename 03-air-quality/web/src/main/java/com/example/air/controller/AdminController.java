package com.example.air.controller;

import com.example.air.ai.AirPredictionClient;
import com.example.air.dto.StationForm;
import com.example.air.dto.Views.ImportResult;
import com.example.air.exception.AppExceptions.DuplicateException;
import com.example.air.service.AirQualityService;
import com.example.air.dto.Views.SyncResult;
import com.example.air.service.DataSyncService;
import com.example.air.service.StationService;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** 관리자: 모델 관리, 측정소 관리, CSV 일괄 등록 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AirQualityService service;
    private final StationService stationService;
    private final AirPredictionClient client;
    private final Environment env;
    private final DataSyncService dataSync;

    public AdminController(AirQualityService service, StationService stationService,
                           AirPredictionClient client, Environment env, DataSyncService dataSync) {
        this.dataSync = dataSync;
        this.service = service;
        this.stationService = stationService;
        this.client = client;
        this.env = env;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("distribution", service.gradeDistribution());
        model.addAttribute("modelInfo", client.modelInfo());
        model.addAttribute("ai", client.health());
        model.addAttribute("persistence", String.join(",", env.getActiveProfiles()));
        model.addAttribute("stationCount", stationService.findAll(false).size());
        model.addAttribute("measurementCount", service.measurementPage(null, 0, 1).total());
        model.addAttribute("syncSource", dataSync.sourceName());
        return "admin/dashboard";
    }

    /** 공공데이터 API 수동 수집 (날짜 없으면 어제) */
    @PostMapping("/sync")
    public String sync(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       RedirectAttributes ra) {
        if (!dataSync.ready()) {
            ra.addFlashAttribute("warning", "공공데이터 인증키(app.openapi.service-key)가 설정되지 않았습니다. web/config/application.yml 에 넣고 웹을 재시작하세요.");
            return "redirect:/admin";
        }
        SyncResult r = dataSync.sync(date == null ? LocalDate.now().minusDays(1) : date);
        ra.addFlashAttribute("syncResult", r);
        ra.addFlashAttribute(r.failed() == 0 ? "message" : "warning",
            "API 수집 완료(" + r.date() + ", " + dataSync.sourceName() + "): 성공 " + r.success() + "건, 실패 " + r.failed() + "건");
        return "redirect:/admin";
    }

    @PostMapping("/retrain")
    public String retrain(RedirectAttributes ra) {
        Map<String, Object> r = client.retrain();
        ra.addFlashAttribute("message", "재학습 완료: " + r);
        return "redirect:/admin";
    }

    // ---------------- 측정소 관리
    @GetMapping("/stations")
    public String stations(Model model) {
        model.addAttribute("stations", stationService.findAll(false));
        return "admin/stations";
    }

    @GetMapping("/stations/new")
    public String newStation(Model model) {
        model.addAttribute("form", new StationForm());
        model.addAttribute("stationId", null);
        return "admin/station-form";
    }

    @PostMapping("/stations/new")
    public String createStation(@Valid @ModelAttribute("form") StationForm form, BindingResult binding,
                                RedirectAttributes ra) {
        if (!binding.hasErrors()) {
            try {
                stationService.create(form);
                ra.addFlashAttribute("message", "측정소를 등록했습니다.");
                return "redirect:/admin/stations";
            } catch (DuplicateException e) {
                binding.rejectValue("name", "duplicate", e.getMessage());
            }
        }
        return "admin/station-form";
    }

    @GetMapping("/stations/{id}/edit")
    public String editStation(@PathVariable Long id, Model model) {
        model.addAttribute("form", StationForm.from(stationService.get(id)));
        model.addAttribute("stationId", id);
        return "admin/station-form";
    }

    @PostMapping("/stations/{id}/edit")
    public String updateStation(@PathVariable Long id, @Valid @ModelAttribute("form") StationForm form,
                                BindingResult binding, Model model, RedirectAttributes ra) {
        model.addAttribute("stationId", id);
        if (!binding.hasErrors()) {
            try {
                stationService.update(id, form);
                ra.addFlashAttribute("message", "수정했습니다.");
                return "redirect:/admin/stations";
            } catch (DuplicateException e) {
                binding.rejectValue("name", "duplicate", e.getMessage());
            }
        }
        return "admin/station-form";
    }

    @PostMapping("/stations/{id}/delete")
    public String deleteStation(@PathVariable Long id, RedirectAttributes ra) {
        stationService.delete(id);
        ra.addFlashAttribute("message", "측정소와 관련 데이터를 삭제했습니다.");
        return "redirect:/admin/stations";
    }

    // ---------------- CSV 일괄 등록
    @GetMapping("/import")
    public String importForm(Model model) {
        model.addAttribute("stations", stationService.findAll(true));
        model.addAttribute("columns", String.join(",", AirQualityService.CSV_COLUMNS));
        return "admin/import";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam Long stationId, @RequestParam("file") MultipartFile file,
                            RedirectAttributes ra) {
        ImportResult r = service.importCsv(stationId, file);
        ra.addFlashAttribute("result", r);
        ra.addFlashAttribute("message", "CSV 등록 완료: 성공 " + r.success() + "건, 실패 " + r.failed() + "건");
        return "redirect:/admin/import";
    }
}
