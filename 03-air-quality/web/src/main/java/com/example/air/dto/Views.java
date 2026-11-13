package com.example.air.dto;

import com.example.air.domain.Measurement;
import com.example.air.domain.Prediction;
import com.example.air.domain.Station;

import java.time.LocalDate;
import java.util.List;

/** 화면 전용 모델 */
public final class Views {

    private Views() {
    }

    /** 대시보드 카드: 측정소 + 최근 측정 + 다음날 예보 */
    public record StationStatus(Station station, Measurement latest, Prediction forecast) {
    }

    /** 측정소 상세 차트 데이터 (날짜 축 공유) */
    public record StationChart(List<String> labels, List<Double> actualPm10, List<Double> actualPm25,
                               List<Double> predictedPm10, List<Double> predictedPm25) {
    }

    /** CSV 업로드 결과 */
    public record ImportResult(int success, int failed, List<String> errors) {
    }

    /** 공공데이터 API 수집 결과 */
    public record SyncResult(LocalDate date, int success, int failed, List<String> errors) {
    }
}
