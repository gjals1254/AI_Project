package com.example.trademark.controller;

import com.example.trademark.domain.Trademark;
import com.example.trademark.dto.TrademarkForm;
import com.example.trademark.exception.AppExceptions.DuplicateException;
import com.example.trademark.exception.AppExceptions.InvalidFileException;
import com.example.trademark.service.SearchService;
import com.example.trademark.service.TrademarkService;
import com.example.trademark.service.TrademarkService.SaveResult;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 기능 1. 상표 이미지 등록 및 관리 */
@Controller
@RequestMapping("/trademarks")
public class TrademarkController {

    private static final int PAGE_SIZE = 12;

    private final TrademarkService trademarkService;
    private final SearchService searchService;

    public TrademarkController(TrademarkService trademarkService, SearchService searchService) {
        this.trademarkService = trademarkService;
        this.searchService = searchService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", trademarkService.page(keyword, page, PAGE_SIZE));
        model.addAttribute("keyword", keyword);
        return "trademark/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Trademark t = trademarkService.get(id);
        model.addAttribute("trademark", t);
        model.addAttribute("recommendations", searchService.recommendFor(t, 6));
        return "trademark/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new TrademarkForm());
        model.addAttribute("mode", "create");
        return "trademark/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") TrademarkForm form, BindingResult binding,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Model model, RedirectAttributes ra) {
        model.addAttribute("mode", "create");
        if (binding.hasErrors()) {
            return "trademark/form";
        }
        try {
            SaveResult r = trademarkService.register(form, image);
            flash(ra, r, "상표가 등록되었습니다.");
            return "redirect:/trademarks/" + r.trademark().getId();
        } catch (DuplicateException e) {
            binding.rejectValue("applicationNo", "duplicate", e.getMessage());
        } catch (InvalidFileException e) {
            model.addAttribute("imageError", e.getMessage());
        }
        return "trademark/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Trademark t = trademarkService.get(id);
        model.addAttribute("form", TrademarkForm.from(t));
        model.addAttribute("trademark", t);
        model.addAttribute("mode", "edit");
        return "trademark/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") TrademarkForm form, BindingResult binding,
                       @RequestParam(value = "image", required = false) MultipartFile image,
                       Model model, RedirectAttributes ra) {
        model.addAttribute("mode", "edit");
        model.addAttribute("trademark", trademarkService.get(id));
        if (binding.hasErrors()) {
            return "trademark/form";
        }
        try {
            SaveResult r = trademarkService.update(id, form, image);
            flash(ra, r, "수정되었습니다.");
            return "redirect:/trademarks/" + id;
        } catch (DuplicateException e) {
            binding.rejectValue("applicationNo", "duplicate", e.getMessage());
        } catch (InvalidFileException e) {
            model.addAttribute("imageError", e.getMessage());
        }
        return "trademark/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        trademarkService.delete(id);
        ra.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/trademarks";
    }

    private void flash(RedirectAttributes ra, SaveResult r, String ok) {
        ra.addFlashAttribute("message", ok);
        if (r.vectorMissing()) {
            ra.addFlashAttribute("warning",
                "AI 서버에 연결하지 못해 특징 벡터 없이 저장했습니다. 관리자 화면에서 '벡터 재추출'을 실행하세요.");
        }
    }
}
