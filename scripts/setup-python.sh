#!/usr/bin/env bash
# 3개 AI 서버의 Python 가상환경 생성 + 패키지 설치 (macOS / Linux)
#   사용: bash scripts/setup-python.sh
set -e
cd "$(dirname "$0")/.."
for p in 01-trademark-similarity 02-parenting-stress 03-air-quality; do
  echo "=== $p/ai-server ==="
  (cd "$p/ai-server" && python3 -m venv .venv && ./.venv/bin/pip install -q --upgrade pip && ./.venv/bin/pip install -q -r requirements.txt)
done
echo "완료. ResNet50 딥러닝 추출기를 쓰려면 추가로:"
echo "  cd 01-trademark-similarity/ai-server && ./.venv/bin/pip install -r requirements-torch.txt --index-url https://download.pytorch.org/whl/cpu"
