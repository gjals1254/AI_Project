package com.example.stress.controller;

import com.example.stress.ai.PredictionClient;
import com.example.stress.service.AssessmentService;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Controller
public class HomeController {

    private final PredictionClient client;
    private final AssessmentService service;
    private final Environment env;

    public HomeController(PredictionClient client, AssessmentService service, Environment env) {
        this.client = client;
        this.service = service;
        this.env = env;
    }

    @GetMapping("/")
    public String index(Model model, Authentication auth) {
        model.addAttribute("ai", client.health());
        model.addAttribute("modelInfo", client.modelInfo());
        model.addAttribute("persistence", String.join(",", env.getActiveProfiles()));
        // 평가 기록은 상담 정보이므로 로그인한 사용자에게만 집계·목록을 보여 준다
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            Map<String, Long> dist = service.levelDistribution();
            long total = dist.values().stream().mapToLong(Long::longValue).sum();
            model.addAttribute("dist", dist);
            model.addAttribute("total", total);
            model.addAttribute("avgRisk", service.averageRiskScore());
            model.addAttribute("thisWeek", service.countSince(
                LocalDate.now().minusDays(6).atStartOfDay().truncatedTo(ChronoUnit.DAYS)));
            model.addAttribute("recent", service.page(null, null, 0, 5).content());
            model.addAttribute("highRisk", service.page(null, 2, 0, 4).content());
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
