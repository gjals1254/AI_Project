package com.example.stress.controller;

import com.example.stress.ai.PredictionClient;
import com.example.stress.service.AssessmentService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

/** 관리자: 통계 대시보드, 모델 정보, 재학습 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AssessmentService service;
    private final PredictionClient client;
    private final Environment env;

    public AdminController(AssessmentService service, PredictionClient client, Environment env) {
        this.service = service;
        this.client = client;
        this.env = env;
    }

    @GetMapping
    public String dashboard(Model model) {
        Map<String, Long> dist = service.levelDistribution();
        model.addAttribute("distribution", dist);
        model.addAttribute("total", dist.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("today", service.countSince(LocalDate.now().atStartOfDay()));
        model.addAttribute("avgRisk", service.averageRiskScore());
        model.addAttribute("modelInfo", client.modelInfo());
        model.addAttribute("ai", client.health());
        model.addAttribute("persistence", String.join(",", env.getActiveProfiles()));
        return "admin/dashboard";
    }

    @PostMapping("/retrain")
    public String retrain(RedirectAttributes ra) {
        Map<String, Object> r = client.retrain();
        ra.addFlashAttribute("message", "재학습 완료: " + r);
        return "redirect:/admin";
    }
}
