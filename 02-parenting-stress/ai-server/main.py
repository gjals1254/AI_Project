"""
양육 스트레스 예측 AI 서버 (FastAPI)

실행:  uvicorn main:app --host 0.0.0.0 --port 8002
문서:  http://localhost:8002/docs
"""
import logging

from fastapi import FastAPI
from pydantic import BaseModel, Field

from stress_model import StressPredictor, load_metadata, train

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="Parenting Stress AI Server", version="1.0.0")
predictor = StressPredictor()


class PredictRequest(BaseModel):
    parentAge: int = Field(ge=18, le=70)
    childCount: int = Field(ge=1, le=6)
    youngestChildAge: int = Field(ge=0, le=18)
    sleepHours: float = Field(ge=3, le=10)
    weeklyWorkHours: float = Field(ge=0, le=80)
    parentingEfficacy: int = Field(ge=0, le=100)
    socialSupport: int = Field(ge=0, le=100)
    familyEnvironment: int = Field(ge=0, le=100)
    resilience: int = Field(ge=0, le=100)
    qualityOfLife: int = Field(ge=0, le=100)


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
    """data/parenting_stress.csv 로 재학습 (다른 CSV 는 train.py --csv 사용)"""
    result = train()
    predictor.reload()
    return {"version": predictor.version, "accuracy": round(result.accuracy, 4), "f1Macro": round(result.f1_macro, 4)}
