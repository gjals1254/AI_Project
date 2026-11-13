"""
시간별 측정 CSV → 학습용 CSV (train.py --csv 형식) 변환

입력 컬럼: datetime,temperature,humidity,wind_speed,rainfall,PM10,PM2_5,O3,NO2,CO,SO2[,...]
변환 방식: 매 시각 t 마다
  특징 = 직전 24시간(t-23 ~ t) 평균  (강수량은 합계)  → 웹에서 넣는 '오늘 일평균'과 같은 의미
  정답 = 다음 24시간(t+1 ~ t+24) PM10 / PM2.5 평균   → '다음날 일평균'

  python prepare_hourly.py 미세먼지.csv train_data.csv
  python train.py --csv train_data.csv
"""
import sys

import pandas as pd

from air_model import FEATURE_NAMES, TARGETS

RENAME = {"wind_speed": "windSpeed", "rainfall": "precipitation", "PM10": "pm10", "PM2_5": "pm25",
          "O3": "o3", "NO2": "no2", "CO": "co", "SO2": "so2"}


def convert(hourly: pd.DataFrame) -> pd.DataFrame:
    df = hourly.rename(columns=RENAME)
    df["datetime"] = pd.to_datetime(df["datetime"])
    df = df.sort_values("datetime").set_index("datetime").asfreq("h")  # 빠진 시각은 NaN → 해당 창은 제외
    cols = [c for c in FEATURE_NAMES if c != "month"]
    past = df[cols].rolling(24, min_periods=24).mean()
    past["precipitation"] = df["precipitation"].rolling(24, min_periods=24).sum()
    # 다음 24시간 평균: 뒤집어서 rolling 후 한 칸 당김
    future = df[["pm10", "pm25"]][::-1].rolling(24, min_periods=24).mean()[::-1].shift(-1)
    out = past.assign(month=df.index.month, nextPm10=future["pm10"], nextPm25=future["pm25"])
    return out[FEATURE_NAMES + TARGETS].dropna().round(4).reset_index()


if __name__ == "__main__":
    _demo = pd.DataFrame({"datetime": pd.date_range("2025-01-01", periods=48, freq="h"),
                          **{k: 1.0 for k in RENAME}, "temperature": 0.0, "humidity": 50.0})
    _demo.loc[24:, "PM10"] = 3.0
    _r = convert(_demo)
    assert len(_r) == 1 and _r.pm10[0] == 1.0 and _r.nextPm10[0] == 3.0 and _r.precipitation[0] == 24.0

    src, dst = sys.argv[1], sys.argv[2]
    result = convert(pd.read_csv(src, encoding="utf-8-sig"))
    result.to_csv(dst, index=False)
    print(f"{len(result)}행 저장: {dst}")
