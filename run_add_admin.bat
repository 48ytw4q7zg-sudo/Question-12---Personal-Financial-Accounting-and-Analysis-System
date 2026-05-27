@echo off
REM 添加admin管理员用户(相对路径，无硬编码)
set "SCRIPT_DIR=%~dp0"
mysql -uroot -proot finance_db < "%SCRIPT_DIR%add_admin.sql"
pause
