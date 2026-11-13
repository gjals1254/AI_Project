"""
미세먼지 대기질 등급 예측 모델 (다음날 PM10 / PM2.5 일평균 농도 회귀 → 등급 변환)

등급 기준 (환경부, 일평균 µg/m³)
  PM10 : 좋음 0~30, 보통 31~80, 나쁨 81~150, 매우나쁨 151~
  PM2.5: 좋음 0~15, 보통 16~35, 나쁨 36~75, 매우나쁨 76~
통합 등급은 두 등급 중 나쁜 쪽을 따른다.

※ 학습 데이터 주의
  generate_synthetic() 은 계절성·전날 농도 지속성·풍속/강수 세정 효과를 반영한 '합성 데이터'다.
  실제 운영 시에는 에어코리아(공공데이터포털 대기오염정보 API)와 기상청 ASOS 일자료를 날짜·측정소로
  결합해 같은 컬럼 이름의 CSV 로 만든 뒤 `python train.py --csv data.csv` 로 재학습한다.
"""
from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import accuracy_score, mean_absolute_error
from sklearn.model_selection import train_test_split

MODEL_DIR = Path(__file__).parent / "model"
MODEL_PATH = MODEL_DIR / "air_model.joblib"
META_PATH = MODEL_DIR / "metadata.json"

FEATURES = [
    ("month", "월(계절)"),
    ("pm10", "오늘 PM10"),
    ("pm25", "오늘 PM2.5"),
    ("o3", "오존(ppm)"),
    ("no2", "이산화질소(ppm)"),
    ("co", "일산화탄소(ppm)"),
    ("so2", "아황산가스(ppm)"),
    ("temperature", "기온(℃)"),
    ("humidity", "습도(%)"),
    ("windSpeed", "풍속(m/s)"),
    ("precipitation", "강수량(mm)"),
]
FEATURE_NAMES = [f[0] for f in FEATURES]
FEATURE_LABELS = dict(FEATURES)
TARGETS = ["nextPm10", "nextPm25"]

GRADE_LABELS = {1: "좋음", 2: "보통", 3: "나쁨", 4: "매우나쁨"}
PM10_BOUNDS = (30, 80, 150)
PM25_BOUNDS = (15, 35, 75)


def grade(value: float, bounds: tuple[int, int, int]) -> int:
    v = round(value)
    for i, upper in enumerate(bounds, start=1):
        if v <= upper:
            return i
    return 4


SEASON = {1: 1.35, 2: 1.45, 3: 1.6, 4: 1.3, 5: 1.0, 6: 0.75, 7: 0.55, 8: 0.5, 9: 0.6, 10: 0.8, 11: 1.1, 12: 1.3}
TEMP = {1: -2, 2: 1, 3: 7, 4: 13, 5: 18, 6: 23, 7: 26, 8: 27, 9: 22, 10: 15, 11: 8, 12: 1}


def generate_synthetic(n: int = 8000, seed: int = 7, months: np.ndarray | None = None) -> pd.DataFrame:
    """months 를 주면 해당 월로 생성 (샘플 CSV 를 실제 날짜에 맞출 때 사용)"""
    rng = np.random.default_rng(seed)
    month = np.asarray(months) if months is not None else rng.integers(1, 13, n)
    s = np.vectorize(SEASON.get)(month)
    base25 = 22 * s
    pm25 = np.clip(rng.lognormal(np.log(base25), 0.45), 3, 180)
    dust = (np.isin(month, [3, 4, 5]) & (rng.random(n) < 0.08)) * rng.uniform(60, 200, n)  # 황사
    pm10 = np.clip(pm25 * rng.uniform(1.4, 2.1, n) + dust, 5, 400)
    rainy = rng.random(n) < np.where(np.isin(month, [6, 7, 8]), 0.45, 0.18)
    precipitation = np.where(rainy, rng.gamma(1.5, 6, n), 0).round(1)
    wind = np.clip(rng.gamma(3, 0.9, n), 0.3, 10).round(1)
    humidity = np.clip(rng.normal(60 + 15 * rainy, 13, n), 15, 100).round()
    temperature = (np.vectorize(TEMP.get)(month) + rng.normal(0, 3.5, n)).round(1)
    no2 = np.clip(0.012 + pm25 * 0.0004 + rng.normal(0, 0.006, n), 0.003, 0.09).round(3)
    co = np.clip(0.25 + pm25 * 0.006 + rng.normal(0, 0.08, n), 0.1, 1.5).round(2)
    so2 = np.clip(0.0025 + pm25 * 0.00003 + rng.normal(0, 0.0008, n), 0.001, 0.012).round(4)
    o3 = np.clip(0.02 + 0.0012 * (temperature - 10) + rng.normal(0, 0.008, n), 0.003, 0.11).round(3)

    # 다음날 농도: 전날 지속성 + 계절 기저 - 풍속·강수 세정 + 고습 정체
    wash = (1 - 0.06 * (wind - 2.5)) * np.where(precipitation > 1, 0.65, 1.0)
    stagnation = 1 + 0.004 * (humidity - 60)
    next25 = (0.55 * pm25 + 0.45 * base25) * wash * stagnation * rng.lognormal(0, 0.18, n)
    next10 = (0.55 * pm10 + 0.45 * base25 * 1.75) * wash * rng.lognormal(0, 0.2, n)

    return pd.DataFrame(
        {
            "month": month, "pm10": pm10.round(), "pm25": pm25.round(), "o3": o3, "no2": no2, "co": co,
            "so2": so2, "temperature": temperature, "humidity": humidity, "windSpeed": wind,
            "precipitation": precipitation,
            "nextPm10": np.clip(next10, 3, 500).round(), "nextPm25": np.clip(next25, 2, 300).round(),
        }
    )


def train(df: pd.DataFrame | None = None, source: str = "synthetic") -> dict:
    if df is None:
        df = generate_synthetic()
    missing = [c for c in FEATURE_NAMES + TARGETS if c not in df.columns]
    if missing:
        raise ValueError(f"CSV 에 없는 컬럼: {missing}")
    X, y = df[FEATURE_NAMES].astype(float), df[TARGETS].astype(float)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
    model = RandomForestRegressor(n_estimators=250, max_depth=14, min_samples_leaf=4, random_state=42, n_jobs=-1)
    model.fit(X_tr, y_tr)
    pred = model.predict(X_te)

    true_grade = [max(grade(a, PM10_BOUNDS), grade(b, PM25_BOUNDS)) for a, b in y_te.values]
    pred_grade = [max(grade(a, PM10_BOUNDS), grade(b, PM25_BOUNDS)) for a, b in pred]
    version = datetime.now().strftime("rf-air-%Y%m%d-%H%M%S")
    MODEL_DIR.mkdir(exist_ok=True)
    joblib.dump({"model": model, "reference": X.median().to_dict(), "version": version}, MODEL_PATH)
    meta = {
        "version": version,
        "algorithm": "RandomForestRegressor (multi-output: PM10, PM2.5)",
        "trainedAt": datetime.now().isoformat(timespec="seconds"),
        "dataSource": source,
        "samples": int(len(df)),
        "maePm10": round(float(mean_absolute_error(y_te["nextPm10"], pred[:, 0])), 2),
        "maePm25": round(float(mean_absolute_error(y_te["nextPm25"], pred[:, 1])), 2),
        "gradeAccuracy": round(float(accuracy_score(true_grade, pred_grade)), 4),
        "featureImportances": sorted(
            ({"feature": n, "label": FEATURE_LABELS[n], "importance": round(float(v), 4)}
             for n, v in zip(FEATURE_NAMES, model.feature_importances_)),
            key=lambda d: -d["importance"],
        ),
    }
    META_PATH.write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
    return meta


class AirPredictor:
    def __init__(self) -> None:
        if not MODEL_PATH.exists():
            train()
        self.reload()

    def reload(self) -> None:
        bundle = joblib.load(MODEL_PATH)
        self.model: RandomForestRegressor = bundle["model"]
        self.reference: dict = bundle["reference"]
        self.version: str = bundle["version"]

    def predict(self, values: dict) -> dict:
        row = pd.DataFrame([[float(values[n]) for n in FEATURE_NAMES]], columns=FEATURE_NAMES)
        pm10, pm25 = (float(v) for v in self.model.predict(row)[0])
        g10, g25 = grade(pm10, PM10_BOUNDS), grade(pm25, PM25_BOUNDS)
        overall = max(g10, g25)

        # 지역 설명: 변수 하나를 표본 중앙값으로 바꿨을 때 다음날 PM2.5 가 얼마나 변하는가
        variants = pd.concat([row] * len(FEATURE_NAMES), ignore_index=True)
        for i, name in enumerate(FEATURE_NAMES):
            variants.loc[i, name] = self.reference[name]
        delta = pm25 - self.model.predict(variants)[:, 1]
        factors = sorted(
            (
                {"feature": n, "label": FEATURE_LABELS[n], "value": values[n],
                 "reference": round(float(self.reference[n]), 3), "impact": round(float(d), 2)}
                for n, d in zip(FEATURE_NAMES, delta)
                if abs(d) >= 0.5
            ),
            key=lambda f: -abs(f["impact"]),
        )
        return {
            "pm10": round(pm10, 1),
            "pm25": round(pm25, 1),
            "pm10Grade": g10,
            "pm25Grade": g25,
            "grade": overall,
            "gradeLabel": GRADE_LABELS[overall],
            "topFactors": factors[:5],
            "modelVersion": self.version,
        }


def load_metadata() -> dict:
    return json.loads(META_PATH.read_text(encoding="utf-8")) if META_PATH.exists() else {}
