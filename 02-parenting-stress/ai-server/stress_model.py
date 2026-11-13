"""
양육 스트레스 노출군 정신건강 스트레스 수준 예측 모델

- 입력: 부모/가구/생활시간 5개 + 심리·사회 척도 점수(0~100) 5개 = 10개 변수
- 출력: 스트레스 수준 3단계 (0 정상, 1 주의, 2 고위험) + 확률 + 위험점수(0~100) + 주요 영향 요인

※ 학습 데이터: data/parenting_stress.csv (500명)
  정답 = psi_severity(양육스트레스 지수 등급 0~3) → 0·1 정상, 2 주의, 3 고위험
  psi/pss/hdrs/hars 점수와 등급, 진단·위험 플래그는 결과 지표라 입력에서 제외한다(정답 누수 방지).
  이 모델의 결과는 진단이 아니며 선별(screening) 참고용이다.
"""
from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report, f1_score
from sklearn.model_selection import train_test_split

DATA_PATH = Path(__file__).parent / "data" / "parenting_stress.csv"
MODEL_DIR = Path(__file__).parent / "model"
MODEL_PATH = MODEL_DIR / "stress_model.joblib"
META_PATH = MODEL_DIR / "metadata.json"

LABELS = ["정상", "주의", "고위험"]

# API(JSON) 필드명, 한국어 이름, 허용 범위, CSV 컬럼명
FEATURES: list[tuple[str, str, float, float, str]] = [
    ("parentAge", "부모 나이", 18, 70, "age"),
    ("childCount", "자녀 수", 1, 6, "children_count"),
    ("youngestChildAge", "막내 자녀 나이", 0, 18, "youngest_child_age"),
    ("sleepHours", "하루 수면시간", 3, 10, "sleep_hours"),
    ("weeklyWorkHours", "주당 근로시간", 0, 80, "work_hours_week"),
    ("parentingEfficacy", "양육 효능감", 0, 100, "parenting_efficacy"),
    ("socialSupport", "사회적 지지", 0, 100, "social_support"),
    ("familyEnvironment", "가족 환경", 0, 100, "family_environment"),
    ("resilience", "회복탄력성", 0, 100, "resilience"),
    ("qualityOfLife", "삶의 질", 0, 100, "quality_of_life"),
]
FEATURE_NAMES = [f[0] for f in FEATURES]
FEATURE_LABELS = {f[0]: f[1] for f in FEATURES}
CSV_COLUMNS = {f[4]: f[0] for f in FEATURES}
PSI_TO_LEVEL = {0: 0, 1: 0, 2: 1, 3: 2}  # psi_severity 0~3 → 정상/주의/고위험


def load_dataset(path: Path | str = DATA_PATH) -> pd.DataFrame:
    """원본 CSV(snake_case) → 모델 컬럼(FEATURE_NAMES + stressLevel)"""
    raw = pd.read_csv(path, encoding="utf-8-sig")
    df = raw.rename(columns=CSV_COLUMNS)
    if "stressLevel" not in df.columns and "psi_severity" in df.columns:
        df["stressLevel"] = df["psi_severity"].map(PSI_TO_LEVEL)
    return df


@dataclass
class TrainResult:
    accuracy: float
    f1_macro: float
    n_samples: int
    report: str


def train(df: pd.DataFrame | None = None, source: str = DATA_PATH.name) -> TrainResult:
    if df is None:
        df = load_dataset()
    missing = [c for c in FEATURE_NAMES + ["stressLevel"] if c not in df.columns]
    if missing:
        raise ValueError(f"CSV 에 없는 컬럼: {missing}")

    X, y = df[FEATURE_NAMES].astype(float), df["stressLevel"].astype(int)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    model = RandomForestClassifier(
        n_estimators=300, max_depth=12, min_samples_leaf=3, class_weight="balanced", random_state=42, n_jobs=-1
    )
    model.fit(X_train, y_train)
    pred = model.predict(X_test)
    acc, f1 = accuracy_score(y_test, pred), f1_score(y_test, pred, average="macro")

    MODEL_DIR.mkdir(exist_ok=True)
    version = datetime.now().strftime("rf-%Y%m%d-%H%M%S")
    joblib.dump({"model": model, "reference": X.median().to_dict(), "version": version}, MODEL_PATH)
    importances = sorted(
        ({"feature": n, "label": FEATURE_LABELS[n], "importance": round(float(v), 4)}
         for n, v in zip(FEATURE_NAMES, model.feature_importances_)),
        key=lambda d: -d["importance"],
    )
    meta = {
        "version": version,
        "algorithm": "RandomForestClassifier",
        "trainedAt": datetime.now().isoformat(timespec="seconds"),
        "dataSource": source,
        "samples": int(len(df)),
        "accuracy": round(float(acc), 4),
        "f1Macro": round(float(f1), 4),
        "classDistribution": {LABELS[k]: int(v) for k, v in y.value_counts().sort_index().items()},
        "featureImportances": importances,
    }
    META_PATH.write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
    return TrainResult(acc, f1, len(df), classification_report(y_test, pred, target_names=LABELS))


class StressPredictor:
    def __init__(self) -> None:
        if not MODEL_PATH.exists():
            train()
        self.reload()

    def reload(self) -> None:
        bundle = joblib.load(MODEL_PATH)
        self.model: RandomForestClassifier = bundle["model"]
        self.reference: dict = bundle["reference"]
        self.version: str = bundle["version"]

    @staticmethod
    def _risk_score(proba: np.ndarray) -> np.ndarray:
        """위험점수 0~100 = P(주의)×50 + P(고위험)×100"""
        return proba[:, 1] * 50 + proba[:, 2] * 100

    def predict(self, values: dict) -> dict:
        row = pd.DataFrame([[float(values[n]) for n in FEATURE_NAMES]], columns=FEATURE_NAMES)
        proba = self.model.predict_proba(row)
        score = float(self._risk_score(proba)[0])
        level = int(np.argmax(proba[0]))

        # 지역 설명: 변수 하나를 표본 중앙값으로 바꿨을 때 위험점수가 얼마나 내려가는가
        variants = pd.concat([row] * len(FEATURE_NAMES), ignore_index=True)
        for i, name in enumerate(FEATURE_NAMES):
            variants.loc[i, name] = self.reference[name]
        impacts = score - self._risk_score(self.model.predict_proba(variants))
        factors = [
            {
                "feature": name,
                "label": FEATURE_LABELS[name],
                "value": values[name],
                "reference": round(float(self.reference[name]), 2),
                "impact": round(float(impact), 2),
            }
            for name, impact in zip(FEATURE_NAMES, impacts)
            if impact > 0.5
        ]
        factors.sort(key=lambda f: -f["impact"])

        return {
            "level": level,
            "label": LABELS[level],
            "riskScore": round(score, 1),
            "probabilities": {LABELS[i]: round(float(p), 4) for i, p in enumerate(proba[0])},
            "topFactors": factors[:5],
            "modelVersion": self.version,
        }


def load_metadata() -> dict:
    return json.loads(META_PATH.read_text(encoding="utf-8")) if META_PATH.exists() else {}
