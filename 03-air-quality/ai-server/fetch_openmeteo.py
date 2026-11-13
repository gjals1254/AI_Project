"""
Open-Meteo 과거 자료(키 불필요)로 학습용 CSV 만들기 — 웹의 자동 수집과 같은 출처·같은 단위

  python fetch_openmeteo.py data/openmeteo_train.csv 2024-11-01 2026-11-11
  python train.py --csv data/openmeteo_train.csv

측정소별로 시간별 대기질 → 일평균(가스는 ppm 환산), 기상 일자료를 받아
'오늘 값 → 다음날 PM10/PM2.5' 행을 만든다. 날짜가 비면 그 이어지는 쌍은 만들지 않는다.
"""
import json
import sys
import time
import urllib.parse
import urllib.request

import pandas as pd

from air_model import FEATURE_NAMES, TARGETS

# shortcut: 웹 DB 기본 측정소 5곳 좌표를 고정, 측정소가 늘면 DB 에서 읽도록 바꾼다
STATIONS = {"중구": (37.5643, 126.9750), "강남구": (37.5175, 127.0473),
            "수원": (37.2636, 127.0286), "부산 연산동": (35.1858, 129.0819),
            "대구 수창동": (35.8714, 128.5911)}
AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
WEATHER_URL = "https://archive-api.open-meteo.com/v1/archive"
MOLAR_VOLUME = 24.45  # 25℃·1기압 — 웹 OpenMeteoClient 와 같은 환산
GASES = {"ozone": ("o3", 48.00), "nitrogen_dioxide": ("no2", 46.01),
         "carbon_monoxide": ("co", 28.01), "sulphur_dioxide": ("so2", 64.07)}


def get(url: str, params: dict) -> dict:
    with urllib.request.urlopen(url + "?" + urllib.parse.urlencode(params), timeout=60) as r:
        return json.load(r)


def daily_air(hourly: dict) -> pd.DataFrame:
    h = pd.DataFrame(hourly)
    h["date"] = pd.to_datetime(h.pop("time")).dt.date
    d = h.groupby("date").mean()  # NaN(결측 시간)은 평균에서 제외
    out = pd.DataFrame({"pm10": d["pm10"], "pm25": d["pm2_5"]})
    for src, (col, mw) in GASES.items():
        out[col] = d[src] * MOLAR_VOLUME / (mw * 1000)
    return out


def daily_weather(daily: dict) -> pd.DataFrame:
    d = pd.DataFrame(daily)
    d["date"] = pd.to_datetime(d.pop("time")).dt.date
    d = d.set_index("date").rename(columns={"temperature_2m_mean": "temperature", "relative_humidity_2m_mean": "humidity",
                                            "wind_speed_10m_mean": "windSpeed", "precipitation_sum": "precipitation"})
    d["precipitation"] = d["precipitation"].fillna(0)
    return d


def pairs(day: pd.DataFrame) -> pd.DataFrame:
    """오늘 행 + 다음날(정확히 하루 뒤) PM → 학습 행"""
    day = day.sort_index()
    nxt = day[["pm10", "pm25"]].copy()
    nxt.index = [d - pd.Timedelta(days=1) for d in nxt.index]
    out = day.join(nxt.rename(columns={"pm10": "nextPm10", "pm25": "nextPm25"}), how="inner")
    out["month"] = [d.month for d in out.index]
    return out[FEATURE_NAMES + TARGETS].dropna()


def station_rows(lat: float, lon: float, start: str, end: str) -> pd.DataFrame:
    common = {"latitude": lat, "longitude": lon, "start_date": start, "end_date": end, "timezone": "Asia/Seoul"}
    air = get(AIR_URL, {**common, "hourly": "pm10,pm2_5," + ",".join(GASES)})["hourly"]
    wx = get(WEATHER_URL, {**common, "wind_speed_unit": "ms", "daily": "temperature_2m_mean,relative_humidity_2m_mean,"
                                                                      "wind_speed_10m_mean,precipitation_sum"})["daily"]
    return pairs(daily_air(air).join(daily_weather(wx), how="inner"))


def build(start: str, end: str) -> pd.DataFrame:
    frames = []
    for name, (lat, lon) in STATIONS.items():
        rows = station_rows(lat, lon, start, end)
        print(f"{name}: {len(rows)}행")
        frames.append(rows.assign(station=name))
        time.sleep(1)  # 무료 API 예의상 간격
    return pd.concat(frames).rename_axis("date").reset_index()


if __name__ == "__main__":
    _d = daily_air({"time": ["2025-01-01T00:00", "2025-01-01T01:00", "2025-01-02T00:00"], "pm10": [10, None, 30],
                    "pm2_5": [5, 7, 9], "ozone": [48, 48, 48], "nitrogen_dioxide": [46.01] * 3,
                    "carbon_monoxide": [28.01] * 3, "sulphur_dioxide": [64.07] * 3})
    assert _d.pm10.iloc[0] == 10 and _d.pm25.iloc[0] == 6 and abs(_d.o3.iloc[0] - 0.02445) < 1e-9
    _w = pd.DataFrame({"temperature": [1, 2], "humidity": [50, 60], "windSpeed": [1, 1], "precipitation": [0, 0]},
                      index=list(_d.index))
    _p = pairs(_d.join(_w))
    assert len(_p) == 1 and _p.nextPm10.iloc[0] == 30 and _p.month.iloc[0] == 1

    dst, start, end = sys.argv[1], sys.argv[2], sys.argv[3]
    result = build(start, end)
    result.to_csv(dst, index=False)
    print(f"{len(result)}행 저장: {dst}")
