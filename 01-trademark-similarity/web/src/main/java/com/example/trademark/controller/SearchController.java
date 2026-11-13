package com.example.trademark.controller;

import com.example.trademark.dto.SearchResultView;
import com.example.trademark.exception.AppExceptions.InvalidFileException;
import com.example.trademark.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** 기능 4. 유사 상표 검색 / 기능 5. 결과 시각화 */
@Controller
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public String form() {
        return "search/form";
    }

    @PostMapping
    public String search(@RequestParam(value = "image", required = false) MultipartFile image,
                         @RequestParam(required = false) Integer niceClass,
                         @RequestParam(required = false) Integer topK,
                         HttpServletRequest request, Model model) {
        try {
            Long historyId = searchService.search(image, niceClass, topK, request.getRemoteAddr());
            return "redirect:/search/results/" + historyId;   // PRG 패턴
        } catch (InvalidFileException e) {
            model.addAttribute("error", e.getMessage());
            return "search/form";
        }
    }

    @GetMapping("/results/{historyId}")
    public String result(@PathVariable Long historyId, Model model) {
        SearchResultView view = searchService.getResult(historyId);
        model.addAttribute("view", view);
        return "search/result";
    }
}
