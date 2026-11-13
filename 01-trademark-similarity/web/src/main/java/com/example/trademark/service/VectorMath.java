package com.example.trademark.service;

import java.util.Locale;

/** 특징 벡터 직렬화와 코사인 유사도 계산 */
public final class VectorMath {

    private VectorMath() {
    }

    /** double[] → "0.012345,-0.2,..." (DB MEDIUMTEXT 저장용) */
    public static String encode(double[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.6f", vector[i]));
        }
        return sb.toString();
    }

    public static double[] decode(String text) {
        if (text == null || text.isBlank()) {
            return new double[0];
        }
        String[] parts = text.split(",");
        double[] v = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            v[i] = Double.parseDouble(parts[i].trim());
        }
        return v;
    }

    /**
     * 코사인 유사도 = (A·B) / (|A| × |B|), 범위 -1 ~ 1.
     * 차원이 다르면 비교할 수 없으므로 예외를 던진다.
     */
    public static double cosine(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("벡터 차원이 다릅니다: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
