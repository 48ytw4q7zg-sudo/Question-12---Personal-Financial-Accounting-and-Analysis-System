@echo off
chcp 65001 >nul
echo === H4 API Smoke: 28 Endpoint Live Probe ===
echo.

REM Step 1: Add admin user to DB
echo [1/4] Adding admin user...
mysql -uroot -proot finance_db --execute="INSERT IGNORE INTO user (username, password, role) VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1);"
echo Done.

REM Step 2: Run Python API smoke test
echo [2/4] Running API smoke test...
cd /d "C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main\system\backend"
python api_smoke.py
echo.

echo [3/4] All done. Check output above for results.
echo.
echo [4/4] If admin login shows code=1002, run this manually:
echo   mysql -uroot -proot finance_db -e "SELECT id,username,role FROM user;"
echo   mysql -uroot -proot finance_db -e "INSERT IGNORE INTO user (username,password,role) VALUES ('admin','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',1);"
pause