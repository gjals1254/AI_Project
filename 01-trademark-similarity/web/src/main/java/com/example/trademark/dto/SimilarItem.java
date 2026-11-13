package com.example.trademark.dto;

import com.example.trademark.domain.Trademark;

/**
 * 유사 상표 1건.
 * trademark 는 검색 이후 삭제된 경우 null 일 수 있다.
 */
public record SimilarItem(int rank, Long trademarkId, Trademark trademark, double score, String level) {

    /** 0~100 % */
    public int percent() {
        return (int) Math.round(Math.max(0, score) * 100);
    }

    /** Bootstrap 색상 클래스 */
    public String levelColor() {
        return switch (level) {
            case "높음" -> "danger";
            case "보통" -> "warning";
            default -> "secondary";
        };
    }
}
