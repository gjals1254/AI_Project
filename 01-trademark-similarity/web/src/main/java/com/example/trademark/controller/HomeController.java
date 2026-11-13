package com.example.trademark.controller;

import com.example.trademark.ai.AiFeatureClient;
import com.example.trademark.repository.SearchHistoryStore;
import com.example.trademark.repository.TrademarkStore;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Arrays;

@Controller
public class HomeController {

    private final TrademarkStore trademarkStore;
    private final SearchHistoryStore historyStore;
    private final AiFeatureClient aiClient;
    private final Environment env;

    public HomeController(TrademarkStore trademarkStore, SearchHistoryStore historyStore,
                          AiFeatureClient aiClient, Environment env) {
        this.trademarkStore = trademarkStore;
        this.historyStore = historyStore;
        this.aiClient = aiClient;
        this.env = env;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("trademarkCount", trademarkStore.count(null));
        model.addAttribute("withoutVector", trademarkStore.countWithoutVector());
        model.addAttribute("searchCount", historyStore.count());
        model.addAttribute("searchesToday", historyStore.countSince(LocalDate.now().atStartOfDay()));
        model.addAttribute("recentTrademarks", trademarkStore.findPage(null, 0, 6));
        model.addAttribute("recentSearches", historyStore.findPage(0, 5));
        model.addAttribute("ai", aiClient.health());
        model.addAttribute("persistence", String.join(",", Arrays.asList(env.getActiveProfiles())));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
