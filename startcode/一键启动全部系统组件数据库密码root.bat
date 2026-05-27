@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title 个人财务记账与分析系统 - 一键启动
color 0A

echo.
echo ============================================
echo  个人财务记账与分析系统 - 正在启动...
echo ============================================
echo.

REM ============================================
REM 路径计算
REM ============================================
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "BACKEND_DIR=%PROJECT_ROOT%\system\backend"
set "FRONTEND_DIR=%PROJECT_ROOT%\system\frontend"
set "SQL_FILE=%PROJECT_ROOT%\system\sql\01-init.sql"
set "DB_USER=root"
set "DB_PASS=root"
set "DB_NAME=finance_db"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"

REM ============================================
REM 路径验证
REM ============================================
echo [INFO] 项目根目录: %PROJECT_ROOT%

if not exist "%BACKEND_DIR%" (
    echo [ERROR] 后端目录不存在: %BACKEND_DIR%
    echo 请确认 bat 文件放在 startcode 文件夹内
    echo 且项目目录结构完整（system\backend）
    goto :fail
)
if not exist "%FRONTEND_DIR%" (
    echo [ERROR] 前端目录不存在: %FRONTEND_DIR%
    echo 请确认项目目录结构完整（system\frontend）
    goto :fail
)
echo [OK] 目录结构验证通过
echo.

if not exist "%PROJECT_ROOT%\logs" mkdir "%PROJECT_ROOT%\logs"

REM ============================================
REM 环境检测
REM ============================================
echo ============================================
echo  环境依赖检测
echo ============================================
echo.

set "MISSING=0"

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请安装 JDK 21
    echo 下载: https://adoptium.net/
    set /A MISSING+=1
) else (
    echo [OK] Java
)

set "MVN_CMD=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
    if exist "%BACKEND_DIR%\mvnw.cmd" (
        set "MVN_CMD=%BACKEND_DIR%\mvnw.cmd"
        echo [OK] Maven Wrapper
    ) else (
        echo [ERROR] 未找到 Maven，请安装 3.9+ 版本
        echo 下载: https://maven.apache.org/
        set /A MISSING+=1
    )
) else (
    echo [OK] Maven
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Node.js，请安装 24 LTS
    echo 下载: https://nodejs.org/
    set /A MISSING+=1
) else (
    echo [OK] Node.js
)

where pnpm >nul 2>&1
if errorlevel 1 (
    echo [INFO] 正在安装 pnpm...
    call npm install -g pnpm@10 >nul 2>&1
    where pnpm >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] pnpm 安装失败，请手动执行: npm install -g pnpm@10
        set /A MISSING+=1
    ) else (
        echo [OK] pnpm (已自动安装)
    )
) else (
    echo [OK] pnpm
)

where mysql >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 MySQL，请安装 8.4
    echo 下载: https://dev.mysql.com/downloads/
    set /A MISSING+=1
) else (
    echo [OK] MySQL
)

where curl >nul 2>&1
if errorlevel 1 (
    echo [WARN] 未找到 curl，健康检查将用替代方案
) else (
    echo [OK] curl
)

if !MISSING! GTR 0 (
    echo.
    echo 缺少 !MISSING! 个必需依赖，无法继续
    goto :fail
)

echo.
echo 所有依赖检查通过
echo.

REM ============================================
REM 数据库初始化
REM ============================================
echo ============================================
echo  数据库初始化
echo ============================================
echo.

mysql -u%DB_USER% -p%DB_PASS% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 数据库连接失败！
    echo.
    echo 可能原因:
    echo   1. MySQL 未安装或未启动（服务名通常为 MySQL80 / MySQL84）
    echo   2. root 密码不是 %DB_PASS%（请修改 bat 文件中的 DB_PASS 变量）
    echo   3. MySQL 未加入 PATH（运行 mysql --version 验证）
    echo.
    echo 后续步骤需要数据库支持，无法继续
    goto :fail
) else (
    echo [OK] 数据库连接成功
    mysql -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME%;" >nul 2>&1
    if exist "%SQL_FILE%" (
        mysql -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%SQL_FILE%" >nul 2>&1
        echo [OK] 数据表初始化完成
    ) else (
        echo [WARN] SQL 文件不存在: %SQL_FILE%
    )
)

echo.

REM ============================================
REM 后端编译
REM ============================================
echo ============================================
echo  后端编译
echo ============================================
echo.

cd /d "%BACKEND_DIR%"
if exist target\classes (
    echo [OK] 已编译，跳过
) else (
    echo 正在编译（首次需几分钟）...
    call %MVN_CMD% clean compile -DskipTests > "%PROJECT_ROOT%\logs\backend-compile.log" 2>&1
    if errorlevel 1 (
        echo [ERROR] 编译失败，日志: %PROJECT_ROOT%\logs\backend-compile.log
        goto :fail
    )
    echo [OK] 编译完成
)

echo.

REM ============================================
REM 启动后端
REM ============================================
echo ============================================
echo  启动后端服务（端口 %BACKEND_PORT%）
echo ============================================
echo.

cd /d "%BACKEND_DIR%"

REM 清理端口占用（用 findstr 精确匹配，兼容中英文 Windows）
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%BACKEND_PORT%" ^| findstr "LISTENING"') do (
    echo [INFO] 清理端口 %BACKEND_PORT% 上的进程 %%p
    taskkill /F /PID %%p >nul 2>&1
)
timeout /t 2 /nobreak >nul

start "Finance Backend" cmd /k "cd /d %BACKEND_DIR% && %MVN_CMD% spring-boot:run"

echo 等待后端启动...
timeout /t 10 /nobreak >nul

set "BACKEND_OK=0"
for /l %%i in (1,1,24) do (
    curl -s http://localhost:%BACKEND_PORT%/api/v1/health >nul 2>&1
    if not errorlevel 1 (
        set "BACKEND_OK=1"
        goto :backend_on
    )
    timeout /t 5 /nobreak >nul
    echo   等待中... %%i/24
)
echo [ERROR] 后端启动超时，请检查 Finance Backend 窗口
goto :fail

:backend_on
echo [OK] 后端已启动
echo.

REM ============================================
REM 启动前端
REM ============================================
echo ============================================
echo  启动前端服务（端口 %FRONTEND_PORT%）
echo ============================================
echo.

cd /d "%FRONTEND_DIR%"

if not exist node_modules (
    echo 安装前端依赖（首次需几分钟）...
    call pnpm install > "%PROJECT_ROOT%\logs\frontend-install.log" 2>&1
    if errorlevel 1 (
        echo [ERROR] 依赖安装失败，日志: %PROJECT_ROOT%\logs\frontend-install.log
        goto :fail
    )
    echo [OK] 依赖安装完成
)

REM 清理端口占用（用 findstr 精确匹配，兼容中英文 Windows）
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%FRONTEND_PORT%" ^| findstr "LISTENING"') do (
    echo [INFO] 清理端口 %FRONTEND_PORT% 上的进程 %%p
    taskkill /F /PID %%p >nul 2>&1
)
timeout /t 2 /nobreak >nul

start "Finance Frontend" cmd /k "cd /d %FRONTEND_DIR% && pnpm dev"

echo 等待前端启动...
timeout /t 8 /nobreak >nul

set "FRONTEND_OK=0"
for /l %%i in (1,1,18) do (
    curl -s http://localhost:%FRONTEND_PORT%/ >nul 2>&1
    if not errorlevel 1 (
        set "FRONTEND_OK=1"
        goto :frontend_on
    )
    timeout /t 5 /nobreak >nul
    echo   等待中... %%i/18
)
echo [ERROR] 前端启动超时，请检查 Finance Frontend 窗口
goto :fail

:frontend_on
echo [OK] 前端已启动
echo.

REM ============================================
REM 打开浏览器
REM ============================================
timeout /t 2 /nobreak >nul
start "" "http://localhost:%FRONTEND_PORT%"

echo ============================================
echo  系统启动完成！
echo ============================================
echo.
echo 后端: http://localhost:%BACKEND_PORT%
echo 前端: http://localhost:%FRONTEND_PORT%
echo.
echo 测试账号:
echo   普通用户: zhangsan / 123456
echo   管理员: admin / 000000
echo.
echo 关闭此窗口不影响已启动的服务
echo.
pause
exit /b 0

:fail
echo.
echo ============================================
echo  启动失败，请检查上方错误信息
echo ============================================
echo.
pause
exit /b 1
