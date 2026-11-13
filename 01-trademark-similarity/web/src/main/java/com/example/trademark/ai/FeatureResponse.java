package com.example.trademark.ai;

/** FastAPI /api/v1/features 응답 */
public record FeatureResponse(String model, int dim, double[] vector, long elapsedMs) {
}
