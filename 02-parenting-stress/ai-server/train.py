"""
모델 학습 스크립트

  python train.py                 # data/parenting_stress.csv 로 학습
  python train.py --csv data.csv  # 다른 CSV 로 학습 (원본과 같은 snake_case 컬럼, psi_severity 포함)
"""
import argparse
from pathlib import Path

from stress_model import DATA_PATH, load_dataset, train

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", default=str(DATA_PATH), help="학습용 CSV 경로")
    args = parser.parse_args()

    result = train(load_dataset(args.csv), source=Path(args.csv).name)
    print(f"samples={result.n_samples} accuracy={result.accuracy:.4f} f1_macro={result.f1_macro:.4f}")
    print(result.report)
