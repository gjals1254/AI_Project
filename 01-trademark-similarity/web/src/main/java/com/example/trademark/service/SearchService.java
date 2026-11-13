package com.example.trademark.service;

import com.example.trademark.ai.AiFeatureClient;
import com.example.trademark.ai.FeatureResponse;
import com.example.trademark.config.AppProperties;
import com.example.trademark.domain.SearchHistory;
import com.example.trademark.domain.SearchResult;
import com.example.trademark.domain.Trademark;
import com.example.trademark.dto.PageResult;
import com.example.trademark.dto.SearchResultView;
import com.example.trademark.dto.SimilarItem;
import com.example.trademark.exception.AppExceptions.NotFoundException;
import com.example.trademark.repository.SearchHistoryStore;
import com.example.trademark.repository.TrademarkStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 기능 3. 코사인 유사도 산출
 * 기능 4. 유사 상표 검색 및 추천
 */
@Service
public class SearchService {

    private final TrademarkStore trademarkStore;
    private final SearchHistoryStore historyStore;
    private final AiFeatureClient aiClient;
    private final FileStorageService storage;
    private final AppProperties.Search config;

    public SearchService(TrademarkStore trademarkStore, SearchHistoryStore historyStore,
                         AiFeatureClient aiClient, FileStorageService storage, AppProperties props) {
        this.trademarkStore = trademarkStore;
        this.historyStore = historyStore;
        this.aiClient = aiClient;
        this.storage = storage;
        this.config = props.getSearch();
    }

    /** 순위가 매겨진 후보 (내부용) */
    record Scored(Trademark trademark, double score) {
    }

    /**
     * 이미지로 유사 상표 검색 → 이력 저장 → 이력 ID 반환
     */
    @Transactional
    public Long search(MultipartFile image, Integer niceClass, Integer topK, String clientIp) {
        String queryPath = storage.store(image, "query");
        FeatureResponse feature = aiClient.extract(storage.load(queryPath), image.getOriginalFilename());

        int k = (topK == null || topK < 1) ? config.getTopK() : Math.min(topK, 50);
        List<Scored> ranked = rank(feature.vector(), feature.model(), niceClass, k, null);

        SearchHistory h = new SearchHistory();
        h.setQueryImagePath(queryPath);
        h.setNiceClass(niceClass);
        h.setModelName(feature.model());
        h.setResultCount(ranked.size());
        h.setClientIp(clientIp);
        h.setSearchedAt(LocalDateTime.now());
        if (!ranked.isEmpty()) {
            h.setTopScore(ranked.get(0).score());
            h.setTopTrademarkId(ranked.get(0).trademark().getId());
        }
        historyStore.save(h);

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Scored s = ranked.get(i);
            results.add(new SearchResult(h.getId(), s.trademark().getId(), i + 1, s.score()));
        }
        historyStore.saveResults(results);
        return h.getId();
    }

    /** 등록된 상표와 비슷한 상표 추천 (AI 서버 호출 없이 저장된 벡터 사용) */
    @Transactional(readOnly = true)
    public List<SimilarItem> recommendFor(Trademark base, int k) {
        if (!base.hasVector()) {
            return List.of();
        }
        List<Scored> ranked = rank(VectorMath.decode(base.getFeatureVector()), base.getModelName(),
            null, k, base.getId());
        List<SimilarItem> items = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Scored s = ranked.get(i);
            items.add(new SimilarItem(i + 1, s.trademark().getId(), s.trademark(), s.score(), level(s.score())));
        }
        return items;
    }

    /**
     * 핵심 알고리즘: 같은 모델로 생성된 모든 벡터와 코사인 유사도를 계산하고 내림차순 상위 k개.
     * (데이터가 수십만 건 이상이면 FAISS, Milvus, pgvector 같은 벡터 인덱스로 교체 권장)
     */
    List<Scored> rank(double[] query, String modelName, Integer niceClass, int k, Long excludeId) {
        return trademarkStore.findSearchTargets(modelName, niceClass).stream()
            .filter(t -> excludeId == null || !excludeId.equals(t.getId()))
            .map(t -> {
                double[] v = VectorMath.decode(t.getFeatureVector());
                return v.length == query.length ? new Scored(t, VectorMath.cosine(query, v)) : null;
            })
            .filter(s -> s != null && s.score() >= config.getMinScore())
            .sorted(Comparator.comparingDouble(Scored::score).reversed())
            .limit(k)
            .toList();
    }

    public String level(double score) {
        if (score >= config.getHighThreshold()) {
            return "높음";
        }
        if (score >= config.getMediumThreshold()) {
            return "보통";
        }
        return "낮음";
    }

    /** 기능 5. 검색 결과 시각화용 데이터 */
    @Transactional(readOnly = true)
    public SearchResultView getResult(Long historyId) {
        SearchHistory h = historyStore.findById(historyId)
            .orElseThrow(() -> new NotFoundException("검색 이력을 찾을 수 없습니다. id=" + historyId));
        List<SearchResult> results = historyStore.findResults(historyId);
        Map<Long, Trademark> byId = trademarkStore
            .findByIds(results.stream().map(SearchResult::getTrademarkId).toList())
            .stream().collect(Collectors.toMap(Trademark::getId, Function.identity()));
        List<SimilarItem> items = results.stream()
            .map(r -> new SimilarItem(r.getRankNo(), r.getTrademarkId(), byId.get(r.getTrademarkId()),
                r.getScore(), level(r.getScore())))
            .toList();
        return new SearchResultView(h, items);
    }

    /** 기능 6. 관리자 - 검색 이력 */
    @Transactional(readOnly = true)
    public PageResult<SearchHistory> historyPage(int page, int size) {
        int safePage = Math.max(page, 0);
        return new PageResult<>(historyStore.findPage(safePage, size), safePage, size, historyStore.count());
    }

    @Transactional
    public void deleteHistory(Long id) {
        SearchHistory h = historyStore.findById(id)
            .orElseThrow(() -> new NotFoundException("검색 이력을 찾을 수 없습니다. id=" + id));
        historyStore.deleteById(id);
        storage.deleteQuietly(h.getQueryImagePath());
    }

    public long countSearchesSince(LocalDateTime from) {
        return historyStore.countSince(from);
    }

    public long countSearches() {
        return historyStore.count();
    }
}
