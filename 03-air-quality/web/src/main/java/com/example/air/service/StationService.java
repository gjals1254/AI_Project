package com.example.air.service;

import com.example.air.domain.Station;
import com.example.air.dto.StationForm;
import com.example.air.exception.AppExceptions.DuplicateException;
import com.example.air.exception.AppExceptions.NotFoundException;
import com.example.air.repository.StationStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 측정소 관리 (관리자) */
@Service
public class StationService {

    private final StationStore store;

    public StationService(StationStore store) {
        this.store = store;
    }

    @Transactional(readOnly = true)
    public List<Station> findAll(boolean activeOnly) {
        return store.findAll(activeOnly);
    }

    @Transactional(readOnly = true)
    public Station get(Long id) {
        return store.findById(id).orElseThrow(() -> new NotFoundException("측정소를 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Station create(StationForm form) {
        if (store.existsByName(form.getName().trim())) {
            throw new DuplicateException("이미 있는 측정소명입니다: " + form.getName());
        }
        Station s = new Station();
        form.applyTo(s);
        s.setCreatedAt(LocalDateTime.now());
        return store.save(s);
    }

    @Transactional
    public Station update(Long id, StationForm form) {
        Station s = get(id);
        String newName = form.getName().trim();
        if (!newName.equals(s.getName()) && store.existsByName(newName)) {
            throw new DuplicateException("이미 있는 측정소명입니다: " + newName);
        }
        form.applyTo(s);
        return store.save(s);
    }

    /** 측정소 삭제 시 측정·예측 데이터도 FK ON DELETE CASCADE 로 함께 삭제된다 */
    @Transactional
    public void delete(Long id) {
        get(id);
        store.deleteById(id);
    }
}
