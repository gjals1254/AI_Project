@echo off
REM 3개 AI 서버의 Python 가상환경 생성 + 패키지 설치 (Windows)
REM   사용: scripts\setup-python.bat   (프로젝트 루트에서 실행)
chcp 65001 > nul
cd /d %~dp0..
for %%p in (01-trademark-similarity 02-parenting-stress 03-air-quality) do (
  echo === %%p\ai-server ===
  pushd %%p\ai-server
  python -m venv .venv
  .venv\Scripts\python -m pip install -q --upgrade pip
  .venv\Scripts\pip install -q -r requirements.txt
  popd
)
echo 완료. ResNet50 을 쓰려면:
echo   cd 01-trademark-similarity\ai-server
echo   .venv\Scripts\pip install -r requirements-torch.txt --index-url https://download.pytorch.org/whl/cpu
