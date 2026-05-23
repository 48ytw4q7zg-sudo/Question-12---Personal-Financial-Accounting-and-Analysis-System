@echo off
chcp 65001 >nul
title 个人财务记账与分析系统 - 一键启动
color 0A

REM ============================================
REM 系统配置（使用相对路径）
REM ============================================
REM 获取脚本所在目录的父目录作为项目根目录
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set BACKEND_DIR=%PROJECT_ROOT%\system\backend
set FRONTEND_DIR=%PROJECT_ROOT%\system\frontend
set SQL_FILE=%PROJECT_ROOT%\system\sql\01-init.sql

set DB_USER=root
set DB_PASS=root
set DB_NAME=finance_db

set BACKEND_PORT=8080
set FRONTEND_PORT=5173

REM 创建日志目录
if not exist "%PROJECT_ROOT%\logs" mkdir "%PROJECT_ROOT%\logs"

REM ============================================
REM 阶段1：环境检测
REM ============================================
echo ============================================
echo  阶段 1/6：环境依赖检测
echo ============================================
echo.

set MISSING_DEPS=0

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FATAL] Java 未安装！请安装 JDK 21
    echo 下载地址：https://adoptium.net/
    set /A MISSING_DEPS+=1
) else (
    java -version 2>&1 | findstr "21" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo [WARN] Java 版本可能不是 21，请确认
    ) else (
        echo [OK] Java 已安装
    )
)

where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FATAL] Maven 未安装！请安装 Maven 3.9+
    echo 下载地址：https://maven.apache.org/
    set /A MISSING_DEPS+=1
) else (
    echo [OK] Maven 已安装
)

where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FATAL] Node.js 未安装！请安装 Node.js 24 LTS
    echo 下载地址：https://nodejs.org/
    set /A MISSING_DEPS+=1
) else (
    echo [OK] Node.js 已安装
)

where pnpm >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] pnpm 未安装，将自动安装
    call npm install -g pnpm@10
) else (
    echo [OK] pnpm 已安装
)

where mysql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FATAL] MySQL 未安装！请安装 MySQL 8.4
    echo 下载地址：https://dev.mysql.com/downloads/
    set /A MISSING_DEPS+=1
) else (
    echo [OK] MySQL 已安装
)

if %MISSING_DEPS% GTR 0 (
    echo.
    echo 检测到 %MISSING_DEPS% 个必需依赖缺失，请安装后重试！
    pause
    exit /b 1
)

echo.
echo ============================================
echo  阶段 2/6：数据库初始化
echo ============================================
echo.

mysql -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME%;" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] 数据库连接失败，请确认 MySQL 服务已启动
    echo 将跳过数据库初始化，如需初始化请手动执行：
    echo mysql -u%DB_USER% -p%DB_PASS% ^< "%SQL_FILE%"
) else (
    mysql -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%SQL_FILE%"
    if %ERRORLEVEL% EQU 0 (
        echo [OK] 数据库初始化成功
    ) else (
        echo [WARN] 数据库初始化可能已存在，跳过
    )
)

echo.
echo ============================================
echo  阶段 3/6：后端编译检查
echo ============================================
echo.

cd /d "%BACKEND_DIR%"
if exist target\classes (
    echo [OK] 后端已编译，跳过
) else (
    echo 正在编译后端...
    call mvn.cmd clean compile -DskipTests > "%PROJECT_ROOT%\logs\backend-compile.log" 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] 后端编译失败！
        echo 详细日志："%PROJECT_ROOT%\logs\backend-compile.log"
        notepad "%PROJECT_ROOT%\logs\backend-compile.log"
        pause
        exit /b 1
    )
    echo [OK] 后端编译成功
)

echo.
echo ============================================
echo  阶段 4/6：启动后端服务 (端口 %BACKEND_PORT%)
echo ============================================
echo.

cd /d "%BACKEND_DIR%"

REM 检查端口是否被占用
netstat -ano | findstr ":%BACKEND_PORT%" >nul
if %ERRORLEVEL% EQU 0 (
    echo [WARN] 端口 %BACKEND_PORT% 已被占用，尝试终止进程...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%BACKEND_PORT%"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

REM 启动后端（新窗口）
start "Finance Backend" cmd /k "cd /d %BACKEND_DIR% && mvn.cmd spring-boot:run"

echo 等待后端启动...
timeout /t 5 /nobreak >nul

REM 检查后端是否启动成功
set BACKEND_STARTED=0
for /l %%i in (1,1,36) do (
    curl -s http://localhost:%BACKEND_PORT%/api/v1/health >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set BACKEND_STARTED=1
        echo [OK] 后端服务启动成功
        goto backend_started
    )
    timeout /t 5 /nobreak >nul
    echo 等待中... (%%i/36)
)
echo [ERROR] 后端启动超时，请检查日志
pause
exit /b 1

:backend_started

echo.
echo ============================================
echo  阶段 5/6：启动前端服务 (端口 %FRONTEND_PORT%)
echo ============================================
echo.

cd /d "%FRONTEND_DIR%"

REM 检查依赖
if not exist node_modules (
    echo 正在安装前端依赖...
    call pnpm.CMD install > "%PROJECT_ROOT%\logs\frontend-install.log" 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] 前端依赖安装失败！
        echo 详细日志："%PROJECT_ROOT%\logs\frontend-install.log"
        notepad "%PROJECT_ROOT%\logs\frontend-install.log"
        pause
        exit /b 1
    )
    echo [OK] 前端依赖安装成功
)

REM 检查端口是否被占用
netstat -ano | findstr ":%FRONTEND_PORT%" >nul
if %ERRORLEVEL% EQU 0 (
    echo [WARN] 端口 %FRONTEND_PORT% 已被占用，尝试终止进程...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%FRONTEND_PORT%"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

REM 启动前端（新窗口）
start "Finance Frontend" cmd /k "cd /d %FRONTEND_DIR% && pnpm.CMD dev"

echo 等待前端启动...
timeout /t 3 /nobreak >nul

REM 检查前端是否启动成功
set FRONTEND_STARTED=0
for /l %%i in (1,1,18) do (
    curl -s http://localhost:%FRONTEND_PORT%/ >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set FRONTEND_STARTED=1
        echo [OK] 前端服务启动成功
        goto frontend_started
    )
    timeout /t 5 /nobreak >nul
    echo 等待中... (%%i/18)
)
echo [ERROR] 前端启动超时，请检查日志
pause
exit /b 1

:frontend_started

echo.
echo ============================================
echo  阶段 6/6：打开浏览器
echo ============================================
echo.

timeout /t 3 /nobreak >nul
start "" "http://localhost:%FRONTEND_PORT%"

echo.
echo ============================================
echo  系统启动完成！
echo ============================================
echo.
echo 后端服务：http://localhost:%BACKEND_PORT%
echo 前端服务：http://localhost:%FRONTEND_PORT%
echo.
echo 测试账号：
echo   普通用户：zhangsan / 123456
echo   管理员：admin / 000000
echo.
echo 关闭此窗口不会影响服务运行
echo 要停止服务，请关闭对应的终端窗口
echo.
pause