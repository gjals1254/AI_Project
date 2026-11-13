#!/usr/bin/env bash
# AI 서버 실행 (macOS / Linux)
#   bash scripts/run-ai.sh trademark   # 8001
#   bash scripts/run-ai.sh stress      # 8002
#   bash scripts/run-ai.sh air         # 8003
set -e
cd "$(dirname "$0")/.."
case "$1" in
  trademark) dir=01-trademark-similarity; port=8001 ;;
  stress)    dir=02-parenting-stress;     port=8002 ;;
  air)       dir=03-air-quality;          port=8003 ;;
  *) echo "사용: $0 trademark|stress|air"; exit 1 ;;
esac
cd "$dir/ai-server"
exec ./.venv/bin/uvicorn main:app --host 0.0.0.0 --port "$port" --reload
