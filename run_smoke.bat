@echo off
REM 路径计算(相对路径，无硬编码)
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%system\backend"
python api_smoke.py > api_smoke_result.txt 2>&1
type api_smoke_result.txt
