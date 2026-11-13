package com.example.trademark.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** DB/AI 서버 없이 실행되는 단위 테스트: mvn test */
class VectorMathTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        double[] v = {0.2, 0.5, 0.1};
        assertEquals(1.0, VectorMath.cosine(v, v), 1e-9);
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        assertEquals(0.0, VectorMath.cosine(new double[]{1, 0}, new double[]{0, 1}), 1e-9);
    }

    @Test
    void oppositeVectorsHaveSimilarityMinusOne() {
        assertEquals(-1.0, VectorMath.cosine(new double[]{1, 2}, new double[]{-1, -2}), 1e-9);
    }

    @Test
    void zeroVectorReturnsZero() {
        assertEquals(0.0, VectorMath.cosine(new double[]{0, 0}, new double[]{1, 1}), 1e-9);
    }

    @Test
    void differentDimensionsThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> VectorMath.cosine(new double[]{1}, new double[]{1, 2}));
    }

    @Test
    void encodeDecodeRoundTrip() {
        double[] v = {0.123456, -0.5, 1.0};
        assertArrayEquals(v, VectorMath.decode(VectorMath.encode(v)), 1e-6);
    }
}
