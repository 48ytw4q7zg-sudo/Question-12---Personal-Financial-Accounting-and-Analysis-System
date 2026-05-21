@echo off
cd /d "C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main\system\backend"
python api_smoke.py > api_smoke_result.txt 2>&1
type api_smoke_result.txt