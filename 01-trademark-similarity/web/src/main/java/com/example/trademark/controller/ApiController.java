package com.example.trademark.controller;

import com.example.trademark.dto.SearchResultView;
import com.example.trademark.dto.SimilarItem;
import com.example.trademark.exception.AppExceptions;
import com.example.trademark.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 외부 연동용 REST API (Postman 등으로 테스트) */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final SearchService searchService;

    public ApiController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** POST /api/v1/search  (multipart: image, niceClass?, topK?) */
    @PostMapping("/search")
    public Map<String, Object> search(@RequestParam("image") MultipartFile image,
                                      @RequestParam(required = false) Integer niceClass,
                                      @RequestParam(required = false) Integer topK,
                                      HttpServletRequest request) {
        Long id = searchService.search(image, niceClass, topK, request.getRemoteAddr());
        SearchResultView view = searchService.getResult(id);
        List<Map<String, Object>> items = view.items().stream().map(this::toMap).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("historyId", id);
        body.put("model", view.history().getModelName());
        body.put("results", items);
        return body;
    }

    private Map<String, Object> toMap(SimilarItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rank", i.rank());
        m.put("trademarkId", i.trademarkId());
        m.put("name", i.trademark() == null ? null : i.trademark().getName());
        m.put("applicationNo", i.trademark() == null ? null : i.trademark().getApplicationNo());
        m.put("imageUrl", i.trademark() == null ? null : "/uploads/" + i.trademark().getImagePath());
        m.put("score", Math.round(i.score() * 10000) / 10000.0);
        m.put("level", i.level());
        return m;
    }

    @ExceptionHandler(AppExceptions.InvalidFileException.class)
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AppExceptions.AiServerException.class)
    public ResponseEntity<Map<String, String>> aiDown(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}
