package com.example.stress.repository.mybatis;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.LevelCount;
import com.example.stress.repository.AssessmentStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
public class MybatisAssessmentStore implements AssessmentStore {

    private final AssessmentMapper mapper;

    public MybatisAssessmentStore(AssessmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Assessment save(Assessment assessment) {
        if (assessment.getId() == null) {
            mapper.insert(assessment);
        } else {
            mapper.update(assessment);
        }
        return assessment;
    }

    @Override
    public Optional<Assessment> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    @Override
    public List<Assessment> findPage(String keyword, Integer level, int page, int size) {
        return mapper.findPage(keyword, level, page * size, size);
    }

    @Override
    public long count(String keyword, Integer level) {
        return mapper.count(keyword, level);
    }

    @Override
    public List<Assessment> findByRespondentCode(String respondentCode) {
        return mapper.findByRespondentCode(respondentCode);
    }

    @Override
    public List<LevelCount> countByLevel() {
        return mapper.countByLevel();
    }

    @Override
    public Double averageRiskScore() {
        return mapper.averageRiskScore();
    }

    @Override
    public long countSince(LocalDateTime from) {
        return mapper.countSince(from);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
