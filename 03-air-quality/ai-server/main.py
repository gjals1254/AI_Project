"""
미세먼지 대기질 등급 예측 AI 서버 (FastAPI)

실행:  uvicorn main:app --host 0.0.0.0 --port 8003
문서:  http://localhost:8003/docs
"""
import logging

from fastapi import FastAPI
from pydantic import BaseModel, Field

from pathlib import Path

from air_model import AirPredictor, load_metadata, train

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="Air Quality AI Server", version="1.0.0")
predictor = AirPredictor()


class PredictRequest(BaseModel):
    month: int = Field(ge=1, le=12)
    pm10: float = Field(ge=0, le=1000, description="오늘 PM10 일평균(µg/m³)")
    pm25: float = Field(ge=0, le=500, description="오늘 PM2.5 일평균(µg/m³)")
    o3: float = Field(ge=0, le=0.5)
    no2: float = Field(ge=0, le=0.5)
    co: float = Field(ge=0, le=10)
    so2: float = Field(ge=0, le=0.5)
    temperature: float = Field(ge=-30, le=45)
    humidity: float = Field(ge=0, le=100)
    windSpeed: float = Field(ge=0, le=40)
    precipitation: float = Field(ge=0, le=500)


TRAIN_CSV = Path(__file__).parent / "data" / "openmeteo_train.csv"


@app.get("/health")
def health():
    return {"status": "UP", "model": predictor.version}


@app.post("/api/v1/predict")
def predict(req: PredictRequest):
    return predictor.predict(req.model_dump())


@app.get("/api/v1/model-info")
def model_info():
    return load_metadata()


@app.post("/api/v1/train")
def retrain():
    """Open-Meteo 최근 2년(어제까지)을 새로 받아 재학습 — 웹 자동 수집과 같은 출처·단위"""
    from datetime import date, timedelta

    from fetch_openmeteo import build

    end = date.today() - timedelta(days=1)
    df = build((end - timedelta(days=730)).isoformat(), end.isoformat())
    df.to_csv(TRAIN_CSV, index=False)
    meta = train(df, source=f"Open-Meteo {df['date'].min()}~{df['date'].max()} ({TRAIN_CSV.name})")
    predictor.reload()
    return {"version": meta["version"], "maePm25": meta["maePm25"], "gradeAccuracy": meta["gradeAccuracy"]}
