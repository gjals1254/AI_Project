package com.example.trademark.service;

import com.example.trademark.ai.AiFeatureClient;
import com.example.trademark.ai.FeatureResponse;
import com.example.trademark.domain.Trademark;
import com.example.trademark.dto.PageResult;
import com.example.trademark.dto.TrademarkForm;
import com.example.trademark.exception.AppExceptions.AiServerException;
import com.example.trademark.exception.AppExceptions.DuplicateException;
import com.example.trademark.exception.AppExceptions.NotFoundException;
import com.example.trademark.repository.TrademarkStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/** 기능 1. 상표 이미지 등록 및 관리 + 기능 2. 특징 벡터 생성 */
@Service
public class TrademarkService {

    private static final Logger log = LoggerFactory.getLogger(TrademarkService.class);

    private final TrademarkStore store;
    private final FileStorageService storage;
    private final AiFeatureClient aiClient;

    public TrademarkService(TrademarkStore store, FileStorageService storage, AiFeatureClient aiClient) {
        this.store = store;
        this.storage = storage;
        this.aiClient = aiClient;
    }

    /** 등록 결과. vectorMissing=true 이면 AI 서버 장애로 벡터 없이 저장된 것 */
    public record SaveResult(Trademark trademark, boolean vectorMissing) {
    }

    @Transactional(readOnly = true)
    public PageResult<Trademark> page(String keyword, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int safePage = Math.max(page, 0);
        return new PageResult<>(store.findPage(kw, safePage, size), safePage, size, store.count(kw));
    }

    @Transactional(readOnly = true)
    public Trademark get(Long id) {
        return store.findById(id).orElseThrow(() -> new NotFoundException("상표를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public SaveResult register(TrademarkForm form, MultipartFile image) {
        if (store.existsByApplicationNo(form.getApplicationNo().trim())) {
            throw new DuplicateException("이미 등록된 출원번호입니다: " + form.getApplicationNo());
        }
        String path = storage.store(image, "trademark");
        Trademark t = new Trademark();
        form.applyTo(t);
        t.setImagePath(path);
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        boolean vectorMissing = !applyFeature(t, storage.load(path), image.getOriginalFilename());
        return new SaveResult(store.save(t), vectorMissing);
    }

    @Transactional
    public SaveResult update(Long id, TrademarkForm form, MultipartFile newImage) {
        Trademark t = get(id);
        String newNo = form.getApplicationNo().trim();
        if (!newNo.equals(t.getApplicationNo()) && store.existsByApplicationNo(newNo)) {
            throw new DuplicateException("이미 등록된 출원번호입니다: " + newNo);
        }
        form.applyTo(t);
        boolean vectorMissing = false;
        if (newImage != null && !newImage.isEmpty()) {
            String oldPath = t.getImagePath();
            String path = storage.store(newImage, "trademark");
            t.setImagePath(path);
            vectorMissing = !applyFeature(t, storage.load(path), newImage.getOriginalFilename());
            storage.deleteQuietly(oldPath);
        }
        t.setUpdatedAt(LocalDateTime.now());
        return new SaveResult(store.save(t), vectorMissing);
    }

    @Transactional
    public void delete(Long id) {
        Trademark t = get(id);
        store.deleteById(id);
        storage.deleteQuietly(t.getImagePath());
    }

    /** 1건 벡터 재추출 */
    @Transactional
    public void reindex(Long id) {
        Trademark t = get(id);
        FeatureResponse f = aiClient.extract(storage.load(t.getImagePath()), t.getImagePath());
        setFeature(t, f);
        t.setUpdatedAt(LocalDateTime.now());
        store.save(t);
    }

    /** 전체 벡터 재추출 (모델 교체 시 사용). @return [성공, 실패] */
    public int[] reindexAll() {
        int ok = 0;
        int fail = 0;
        for (Trademark t : store.findAll()) {
            try {
                reindexOne(t);
                ok++;
            } catch (RuntimeException e) {
                log.warn("재추출 실패 id={} : {}", t.getId(), e.getMessage());
                fail++;
            }
        }
        return new int[]{ok, fail};
    }

    private void reindexOne(Trademark t) {
        FeatureResponse f = aiClient.extract(storage.load(t.getImagePath()), t.getImagePath());
        setFeature(t, f);
        t.setUpdatedAt(LocalDateTime.now());
        store.save(t);
    }

    /** AI 서버 호출. 실패해도 등록은 진행하고 false 를 반환한다. */
    private boolean applyFeature(Trademark t, byte[] bytes, String filename) {
        try {
            setFeature(t, aiClient.extract(bytes, filename));
            return true;
        } catch (AiServerException e) {
            log.warn("특징 추출 실패 → 벡터 없이 저장: {}", e.getMessage());
            t.setFeatureVector(null);
            t.setModelName(null);
            t.setVectorDim(null);
            return false;
        }
    }

    private void setFeature(Trademark t, FeatureResponse f) {
        t.setFeatureVector(VectorMath.encode(f.vector()));
        t.setModelName(f.model());
        t.setVectorDim(f.vector().length);
    }
}
