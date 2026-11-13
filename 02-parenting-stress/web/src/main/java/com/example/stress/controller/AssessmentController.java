package com.example.stress.controller;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.AssessmentForm;
import com.example.stress.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

/** 설문 등록 · 예측 결과 · 이력 조회 */
@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer level,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", service.page(keyword, level, page, 15));
        model.addAttribute("keyword", keyword);
        model.addAttribute("level", level);
        model.addAttribute("dist", service.levelDistribution());
        return "assessment/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("form", new AssessmentForm());
        return "assessment/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") AssessmentForm form, BindingResult binding,
                         Authentication auth, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "assessment/form";
        }
        Assessment a = service.create(form, auth.getName());
        ra.addFlashAttribute("message", "AI 예측이 완료되었습니다.");
        return "redirect:/assessments/" + a.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Assessment a = service.get(id);
        model.addAttribute("a", a);
        model.addAttribute("factors", service.factors(a));
        model.addAttribute("guidance", AssessmentService.guidance(a.getStressLevel()));
        List<Assessment> trend = service.history(a.getRespondentCode());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        model.addAttribute("trendLabels", trend.stream().map(t -> t.getCreatedAt().format(fmt)).toList());
        model.addAttribute("trendScores", trend.stream().map(Assessment::getRiskScore).toList());
        return "assessment/detail";
    }

    @PostMapping("/{id}/repredict")
    public String repredict(@PathVariable Long id, RedirectAttributes ra) {
        service.repredict(id);
        ra.addFlashAttribute("message", "현재 모델로 다시 예측했습니다.");
        return "redirect:/assessments/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/assessments";
    }
}
