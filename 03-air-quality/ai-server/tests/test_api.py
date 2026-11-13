from fastapi.testclient import TestClient

from air_model import PM10_BOUNDS, PM25_BOUNDS, grade
from main import app

client = TestClient(app)

CLEAN = dict(month=8, pm10=20, pm25=8, o3=0.04, no2=0.012, co=0.3, so2=0.002,
             temperature=27, humidity=80, windSpeed=5, precipitation=20)
POLLUTED = dict(month=3, pm10=160, pm25=85, o3=0.02, no2=0.045, co=0.8, so2=0.005,
                temperature=6, humidity=70, windSpeed=0.8, precipitation=0)


def test_grade_boundaries():
    assert grade(15, PM25_BOUNDS) == 1 and grade(16, PM25_BOUNDS) == 2
    assert grade(35, PM25_BOUNDS) == 2 and grade(36, PM25_BOUNDS) == 3
    assert grade(76, PM25_BOUNDS) == 4
    assert grade(30, PM10_BOUNDS) == 1 and grade(151, PM10_BOUNDS) == 4


def test_clean_vs_polluted():
    clean = client.post("/api/v1/predict", json=CLEAN).json()
    bad = client.post("/api/v1/predict", json=POLLUTED).json()
    assert clean["grade"] <= 2
    assert bad["grade"] >= 3
    assert bad["pm25"] > clean["pm25"]
    assert bad["grade"] == max(bad["pm10Grade"], bad["pm25Grade"])


def test_validation():
    assert client.post("/api/v1/predict", json=dict(CLEAN, month=13)).status_code == 422


def test_model_info():
    info = client.get("/api/v1/model-info").json()
    assert info["gradeAccuracy"] > 0.5
