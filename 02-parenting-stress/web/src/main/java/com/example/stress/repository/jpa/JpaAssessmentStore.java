package com.example.stress.repository.jpa;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.LevelCount;
import com.example.stress.repository.AssessmentStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaAssessmentStore implements AssessmentStore {

    private final AssessmentJpaRepository repository;

    public JpaAssessmentStore(AssessmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Assessment save(Assessment assessment) {
        return repository.save(assessment);
    }

    @Override
    public Optional<Assessment> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Assessment> findPage(String keyword, Integer level, int page, int size) {
        return repository.search(keyword, level, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
            .getContent();
    }

    @Override
    public long count(String keyword, Integer level) {
        return repository.countSearch(keyword, level);
    }

    @Override
    public List<Assessment> findByRespondentCode(String respondentCode) {
        return repository.findByRespondentCodeOrderByCreatedAtAsc(respondentCode);
    }

    @Override
    public List<LevelCount> countByLevel() {
        return repository.countByLevel();
    }

    @Override
    public Double averageRiskScore() {
        return repository.averageRiskScore();
    }

    @Override
    public long countSince(LocalDateTime from) {
        return repository.countByCreatedAtGreaterThanEqual(from);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
