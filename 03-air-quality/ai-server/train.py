"""
  python train.py                       # 합성 데이터로 학습
  python train.py --csv data.csv        # 실제 데이터(에어코리아+기상청 결합)로 학습
  python train.py --export-sample out.csv --days 60
        # Spring 관리자 화면 CSV 업로드용 측정 데이터 샘플 생성 (date 컬럼 포함, next* 컬럼 제외)
"""
import argparse
import json
from datetime import date, timedelta

import pandas as pd

from air_model import FEATURE_NAMES, generate_synthetic, train

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--csv")
    p.add_argument("--export-sample")
    p.add_argument("--days", type=int, default=60)
    args = p.parse_args()

    if args.export_sample:
        start = date.today() - timedelta(days=args.days)
        days = [start + timedelta(days=i) for i in range(args.days)]
        df = generate_synthetic(args.days, seed=11, months=[d.month for d in days])
        df.insert(0, "date", [d.isoformat() for d in days])
        cols = ["date"] + [c for c in FEATURE_NAMES if c != "month"]
        df[cols].to_csv(args.export_sample, index=False)
        print("저장:", args.export_sample)
    else:
        meta = train(pd.read_csv(args.csv) if args.csv else None, source=args.csv or "synthetic")
        print(json.dumps({k: v for k, v in meta.items() if k != "featureImportances"}, ensure_ascii=False, indent=2))
