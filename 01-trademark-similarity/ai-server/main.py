"""
상표 이미지 특징 추출 AI 서버 (FastAPI)

실행:  uvicorn main:app --host 0.0.0.0 --port 8001
문서:  http://localhost:8001/docs
환경변수:
  FEATURE_BACKEND = auto | resnet | lite   (기본 auto)
"""
import logging
import os
import time

from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel

from feature_extractor import build_extractor, cosine_similarity, load_image

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

MAX_BYTES = 10 * 1024 * 1024
ALLOWED_TYPES = {"image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp", "application/octet-stream"}

app = FastAPI(title="Trademark Feature AI Server", version="1.0.0")
extractor = build_extractor(os.getenv("FEATURE_BACKEND", "auto"))


class FeatureResponse(BaseModel):
    model: str
    dim: int
    vector: list[float]
    elapsedMs: int


class SimilarityResponse(BaseModel):
    model: str
    similarity: float


async def _read_image(file: UploadFile):
    if file.content_type and file.content_type not in ALLOWED_TYPES:
        raise HTTPException(status_code=415, detail=f"지원하지 않는 형식입니다: {file.content_type}")
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="빈 파일입니다.")
    if len(data) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="파일이 10MB 를 초과합니다.")
    try:
        return load_image(data)
    except Exception:
        raise HTTPException(status_code=400, detail="이미지 파일을 읽을 수 없습니다.")


@app.get("/health")
def health():
    return {"status": "UP", "model": extractor.name, "dim": extractor.dim}


@app.post("/api/v1/features", response_model=FeatureResponse)
async def extract_features(file: UploadFile = File(...)):
    """이미지 1장 → L2 정규화된 특징 벡터"""
    image = await _read_image(file)
    started = time.perf_counter()
    vec = extractor.extract(image)
    return FeatureResponse(
        model=extractor.name,
        dim=int(vec.shape[0]),
        vector=[round(float(x), 6) for x in vec],
        elapsedMs=int((time.perf_counter() - started) * 1000),
    )


@app.post("/api/v1/similarity", response_model=SimilarityResponse)
async def compare(file1: UploadFile = File(...), file2: UploadFile = File(...)):
    """이미지 2장의 코사인 유사도 (테스트·시연용)"""
    a = extractor.extract(await _read_image(file1))
    b = extractor.extract(await _read_image(file2))
    return SimilarityResponse(model=extractor.name, similarity=round(cosine_similarity(a, b), 6))
