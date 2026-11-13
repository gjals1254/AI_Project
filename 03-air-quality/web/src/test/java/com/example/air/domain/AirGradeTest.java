package com.example.air.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 환경부 등급 경계값 테스트 (DB/AI 서버 불필요) */
class AirGradeTest {

    @Test
    void pm25Boundaries() {
        assertEquals(1, AirGrade.pm25(15));
        assertEquals(2, AirGrade.pm25(16));
        assertEquals(2, AirGrade.pm25(35));
        assertEquals(3, AirGrade.pm25(36));
        assertEquals(3, AirGrade.pm25(75));
        assertEquals(4, AirGrade.pm25(76));
    }

    @Test
    void pm10Boundaries() {
        assertEquals(1, AirGrade.pm10(30));
        assertEquals(2, AirGrade.pm10(31));
        assertEquals(3, AirGrade.pm10(81));
        assertEquals(4, AirGrade.pm10(151));
    }

    @Test
    void overallGradeIsWorseOfTwo() {
        Measurement m = new Measurement();
        m.setPm10(25);   // 좋음
        m.setPm25(40);   // 나쁨
        assertEquals(3, m.grade());
        assertEquals("나쁨", m.gradeLabel());
    }
}
