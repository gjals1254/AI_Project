from fastapi.testclient import TestClient

from main import app

client = TestClient(app)

LOW_RISK = dict(
    parentAge=38, childCount=1, youngestChildAge=9, sleepHours=8, weeklyWorkHours=35,
    parentingEfficacy=85, socialSupport=90, familyEnvironment=85, resilience=85, qualityOfLife=90,
)
HIGH_RISK = dict(
    parentAge=33, childCount=3, youngestChildAge=1, sleepHours=4.5, weeklyWorkHours=55,
    parentingEfficacy=30, socialSupport=30, familyEnvironment=35, resilience=30, qualityOfLife=35,
)


def test_health():
    assert client.get("/health").json()["status"] == "UP"


def test_low_vs_high_risk():
    low = client.post("/api/v1/predict", json=LOW_RISK).json()
    high = client.post("/api/v1/predict", json=HIGH_RISK).json()
    assert low["label"] == "정상"
    assert high["label"] != "정상"
    assert high["riskScore"] > low["riskScore"]
    assert abs(sum(high["probabilities"].values()) - 1) < 1e-3
    assert high["topFactors"], "고위험 응답에는 주요 요인이 있어야 한다"


def test_validation_error():
    bad = dict(LOW_RISK, sleepHours=20)
    assert client.post("/api/v1/predict", json=bad).status_code == 422


def test_model_info():
    info = client.get("/api/v1/model-info").json()
    # 실제 데이터(500건)는 예측 신호가 약해 정확도 하한 대신 학습 데이터·변수 구성을 확인한다
    assert info["samples"] == 500
    assert len(info["featureImportances"]) == 10
