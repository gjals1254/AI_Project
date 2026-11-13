package com.example.air.domain;

import java.util.List;

/**
 * 환경부 미세먼지 예보 등급 (일평균 µg/m³)
 *   PM10 : 좋음 0~30, 보통 31~80, 나쁨 81~150, 매우나쁨 151~
 *   PM2.5: 좋음 0~15, 보통 16~35, 나쁨 36~75, 매우나쁨 76~
 */
public final class AirGrade {

    public static final List<String> LABELS = List.of("", "좋음", "보통", "나쁨", "매우나쁨");
    private static final int[] PM10 = {30, 80, 150};
    private static final int[] PM25 = {15, 35, 75};

    private AirGrade() {
    }

    public static int pm10(double value) {
        return grade(value, PM10);
    }

    public static int pm25(double value) {
        return grade(value, PM25);
    }

    public static String label(int grade) {
        return grade >= 1 && grade <= 4 ? LABELS.get(grade) : "-";
    }

    /** 화면 색상 (에어코리아 관례: 파랑·초록·주황·빨강을 어두운 바탕에서 4.5:1 이상으로) */
    public static String color(int grade) {
        return switch (grade) {
            case 1 -> "#5aa9f0";
            case 2 -> "#46b38a";
            case 3 -> "#e0a33c";
            case 4 -> "#ef5f76";
            default -> "#8a8c92";
        };
    }

    private static int grade(double value, int[] bounds) {
        long v = Math.round(value);
        for (int i = 0; i < bounds.length; i++) {
            if (v <= bounds[i]) {
                return i + 1;
            }
        }
        return 4;
    }
}
