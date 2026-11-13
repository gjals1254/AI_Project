package com.example.stress.service;

import com.example.stress.domain.Assessment;
import com.example.stress.dto.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DB/AI 서버 없이 실행되는 단위 테스트 */
class AssessmentServiceTest {

    @Test
    void guidanceForHighRiskMentionsCounseling() {
        assertTrue(AssessmentService.guidance(2).contains("상담"));
        assertEquals("", AssessmentService.guidance(null));
    }

    @Test
    void levelColorMapping() {
        Assessment a = new Assessment();
        assertEquals("secondary", a.levelColor());
        a.setStressLevel(0);
        assertEquals("success", a.levelColor());
        a.setStressLevel(2);
        assertEquals("danger", a.levelColor());
    }

    @Test
    void pageResultMath() {
        PageResult<Integer> p = new PageResult<>(List.of(1, 2), 0, 15, 31);
        assertEquals(3, p.totalPages());
        assertFalse(p.hasPrev());
        assertTrue(p.hasNext());
    }
}
