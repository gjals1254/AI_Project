@echo off
REM AI 서버 실행 (Windows)
REM   scripts\run-ai.bat trademark   (8001)
REM   scripts\run-ai.bat stress      (8002)
REM   scripts\run-ai.bat air         (8003)
chcp 65001 > nul
cd /d %~dp0..
if "%1"=="trademark" (set DIR=01-trademark-similarity& set PORT=8001)
if "%1"=="stress"    (set DIR=02-parenting-stress& set PORT=8002)
if "%1"=="air"       (set DIR=03-air-quality& set PORT=8003)
if "%DIR%"=="" (echo 사용: run-ai.bat trademark^|stress^|air & exit /b 1)
cd %DIR%\ai-server
.venv\Scripts\uvicorn main:app --host 0.0.0.0 --port %PORT% --reload
