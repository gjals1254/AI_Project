package com.example.trademark.controller;

import com.example.trademark.ai.AiFeatureClient;
import com.example.trademark.repository.TrademarkStore;
import com.example.trademark.service.SearchService;
import com.example.trademark.service.TrademarkService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;

/** 기능 6. 관리자: 상표 데이터 관리, 검색 이력 조회, 시스템 운영 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TrademarkService trademarkService;
    private final SearchService searchService;
    private final TrademarkStore trademarkStore;
    private final AiFeatureClient aiClient;
    private final Environment env;

    public AdminController(TrademarkService trademarkService, SearchService searchService,
                           TrademarkStore trademarkStore, AiFeatureClient aiClient, Environment env) {
        this.trademarkService = trademarkService;
        this.searchService = searchService;
        this.trademarkStore = trademarkStore;
        this.aiClient = aiClient;
        this.env = env;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("trademarkCount", trademarkStore.count(null));
        model.addAttribute("missingVectorCount", trademarkStore.countWithoutVector());
        model.addAttribute("searchCount", searchService.countSearches());
        model.addAttribute("todaySearchCount", searchService.countSearchesSince(LocalDate.now().atStartOfDay()));
        model.addAttribute("ai", aiClient.health());
        model.addAttribute("persistence", String.join(",", Arrays.asList(env.getActiveProfiles())));
        model.addAttribute("recent", searchService.historyPage(0, 5).content());
        return "admin/dashboard";
    }

    @GetMapping("/trademarks")
    public String trademarks(@RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", trademarkService.page(keyword, page, 20));
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentModel", aiClient.currentModel());
        return "admin/trademarks";
    }

    @PostMapping("/trademarks/{id}/reindex")
    public String reindex(@PathVariable Long id, RedirectAttributes ra) {
        trademarkService.reindex(id);
        ra.addFlashAttribute("message", "id=" + id + " 벡터를 다시 추출했습니다.");
        return "redirect:/admin/trademarks";
    }

    @PostMapping("/trademarks/reindex-all")
    public String reindexAll(RedirectAttributes ra) {
        int[] r = trademarkService.reindexAll();
        ra.addFlashAttribute("message", "전체 재추출 완료: 성공 " + r[0] + "건, 실패 " + r[1] + "건");
        return "redirect:/admin/trademarks";
    }

    @PostMapping("/trademarks/{id}/delete")
    public String deleteTrademark(@PathVariable Long id, RedirectAttributes ra) {
        trademarkService.delete(id);
        ra.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/admin/trademarks";
    }

    @GetMapping("/history")
    public String history(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", searchService.historyPage(page, 20));
        return "admin/history";
    }

    @GetMapping("/history/{id}")
    public String historyDetail(@PathVariable Long id, Model model) {
        model.addAttribute("view", searchService.getResult(id));
        model.addAttribute("adminView", true);
        return "search/result";
    }

    @PostMapping("/history/{id}/delete")
    public String deleteHistory(@PathVariable Long id, RedirectAttributes ra) {
        searchService.deleteHistory(id);
        ra.addFlashAttribute("message", "검색 이력을 삭제했습니다.");
        return "redirect:/admin/history";
    }
}
