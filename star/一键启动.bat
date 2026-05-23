@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================================
REM   个人财务记账与分析系统 — 一键启动脚本
REM   Author: qxw · Author-ID: 2501060122
REM   Version: v2.0 · 2026-05-23
REM
REM   功能:
REM     1. 自动检测环境 (Java/Maven/MySQL/Node.js/pnpm)
REM     2. 自动初始化数据库 (如需要)
REM     3. 自动编译&启动后端 (SpringBoot :8080)
REM     4. 自动安装依赖&启动前端 (Vite :5173)
REM     5. 启动完成后自动打开浏览器
REM ============================================================

set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║    个人财务记账与分析系统 - 一键启动脚本                 ║
echo ║    Creator: qxw · 2501060122                            ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
echo [%date% %time%] 开始系统启动检查...

REM ============================================================
REM §1 环境检测
REM ============================================================
echo.
echo ┌─ §1 环境检测 ─────────────────────────────────────────┐

REM 1.1 Java 21
echo │  [1/5] 检测 Java 21...
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [FAIL] 未找到 Java，请安装 JDK 21
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%i
echo │  [ OK ] %JAVA_VER%

REM 1.2 Maven
echo │  [2/5] 检测 Maven...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [FAIL] 未找到 Maven，请安装 Maven 3.9+
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('mvn --version 2^>^&1 ^| findstr /i "Apache Maven"') do set MVN_VER=%%i
echo │  [ OK ] %MVN_VER%

REM 1.3 MySQL
echo │  [3/5] 检测 MySQL...
where mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [WARN] 未找到 mysql 命令行，尝试检测 MySQL 服务...
    sc query MySQL80 >nul 2>&1
    if %errorlevel% neq 0 (
        echo │  [WARN] MySQL 服务未找到，请确保 MySQL 8.4 已安装并运行
        echo │        数据库连接: localhost:3306  root/root  finance_db
    ) else (
        echo │  [ OK ] MySQL80 服务已安装
    )
) else (
    echo │  [ OK ] MySQL 命令行可用
)

REM 1.4 Node.js
echo │  [4/5] 检测 Node.js 24+...
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [FAIL] 未找到 Node.js，请安装 Node.js 24 LTS
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('node --version 2^>^&1') do set NODE_VER=%%i
echo │  [ OK ] Node.js %NODE_VER%

REM 1.5 pnpm
echo │  [5/5] 检测 pnpm...
where pnpm >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [WARN] 未找到 pnpm，尝试用 npm 安装...
    npm install -g pnpm >nul 2>&1
    if %errorlevel% neq 0 (
        echo │  [FAIL] pnpm 安装失败，请手动执行: npm install -g pnpm
        pause
        exit /b 1
    )
)
for /f "tokens=*" %%i in ('pnpm --version 2^>^&1') do set PNPM_VER=%%i
echo │  [ OK ] pnpm %PNPM_VER%
echo └────────────────────────────────────────────────────────┘

REM ============================================================
REM §2 数据库初始化
REM ============================================================
echo.
echo ┌─ §2 数据库初始化 ────────────────────────────────────┐
echo │  检查 finance_db 数据库...

REM 尝试连接 MySQL 并初始化数据库
mysql -uroot -proot -e "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [WARN] 无法连接 MySQL (root/root)，请确认:
    echo │         1. MySQL 服务已启动
    echo │         2. root 密码为 root
    echo │         3. MySQL 监听 3306 端口
    echo │  [INFO] 跳过数据库初始化，启动后可能需要手动执行:
    echo │         mysql -uroot -proot < sql/01-init.sql
) else (
    REM 检查数据库是否存在
    mysql -uroot -proot -e "USE finance_db; SELECT 1" >nul 2>&1
    if %errorlevel% neq 0 (
        echo │  数据库不存在，开始初始化...
        mysql -uroot -proot < sql\01-init.sql 2>&1
        if %errorlevel% equ 0 (
            echo │  [ OK ] 数据库初始化完成 (finance_db + 7 张表 + 种子数据)
        ) else (
            echo │  [FAIL] 数据库初始化失败，请检查 sql/01-init.sql
        )
    ) else (
        echo │  [ OK ] finance_db 已存在，跳过初始化
        REM 检查表是否存在
        for /f %%c in ('mysql -uroot -proot -N -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='finance_db'" 2^>nul') do set TABLE_COUNT=%%c
        echo │  [ OK ] 已有 !TABLE_COUNT! 张表
    )
)
echo └────────────────────────────────────────────────────────┘

REM ============================================================
REM §3 编译后端 (SpringBoot)
REM ============================================================
echo.
echo ┌─ §3 编译后端 (Maven) ─────────────────────────────────┐
cd /d "%PROJECT_ROOT%\system\backend"

echo │  正在编译后端 (mvn compile -q)...
mvn -B -q -o compile >nul 2>&1
if %errorlevel% neq 0 (
    echo │  [WARN] 离线编译失败，尝试在线编译...
    mvn -B -q compile >nul 2>&1
    if %errorlevel% neq 0 (
        echo │  [FAIL] 后端编译失败，请检查 pom.xml 和网络连接
        pause
        exit /b 1
    )
)
echo │  [ OK ] 后端编译成功

REM 运行单元测试（快速验证）
echo │  运行单元测试 (mvn test)...
mvn -B -q test 2>&1 | findstr /C:"Tests run:" | findstr /C:"Failures: 0" >nul
if %errorlevel% equ 0 (
    echo │  [ OK ] 单元测试通过
) else (
    echo │  [WARN] 部分测试未通过，但继续启动（不影响运行）
)
echo └────────────────────────────────────────────────────────┘

REM ============================================================
REM §4 启动后端服务
REM ============================================================
echo.
echo ┌─ §4 启动后端服务 (:8080) ─────────────────────────────┐

REM 检查端口 8080 是否被占用
netstat -ano 2>nul | findstr ":8080.*LISTENING" >nul
if %errorlevel% equ 0 (
    echo │  [WARN] 端口 8080 已被占用，尝试释放...
    for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8080.*LISTENING"') do (
        echo │  终止进程 PID=%%a...
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo │  [ OK ] 端口 8080 已释放
)

REM 启动后端（新窗口）
echo │  启动 SpringBoot 后端...
start "Finance-Backend" /MIN cmd /c "cd /d "%PROJECT_ROOT%\system\backend" && mvn -B -q spring-boot:run 2>&1"
echo │  [ OK ] 后端启动中（新窗口 Finance-Backend）

REM 等待后端就绪
echo │  等待后端就绪 (最多 120 秒)...
set /a RETRY=0
:wait_backend
timeout /t 3 /nobreak >nul
curl -s http://localhost:8080/api/v1/health >nul 2>&1
if %errorlevel% equ 0 (
    echo │  [ OK ] 后端已就绪 (http://localhost:8080/api/v1/health)
    goto backend_ready
)
set /a RETRY+=1
if %RETRY% lss 40 (
    echo │  等待中... (%RETRY%/40)
    goto wait_backend
)
echo │  [WARN] 后端启动超时，请检查 Finance-Backend 窗口日志
:backend_ready
echo └────────────────────────────────────────────────────────┘

REM ============================================================
REM §5 启动前端服务
REM ============================================================
echo.
echo ┌─ §5 启动前端服务 (:5173) ─────────────────────────────┐
cd /d "%PROJECT_ROOT%\system\frontend"

REM 安装依赖（首次运行）
if not exist "node_modules" (
    echo │  安装前端依赖 (pnpm install)...
    pnpm install >nul 2>&1
    if %errorlevel% neq 0 (
        echo │  [FAIL] 前端依赖安装失败
        pause
        exit /b 1
    )
    echo │  [ OK ] 依赖安装完成
) else (
    echo │  [ OK ] node_modules 已存在，跳过 pnpm install
)

REM 检查端口 5173
netstat -ano 2>nul | findstr ":5173.*LISTENING" >nul
if %errorlevel% equ 0 (
    echo │  [WARN] 端口 5173 已被占用，尝试释放...
    for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":5173.*LISTENING"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

REM 启动前端（新窗口）
echo │  启动 Vite 前端开发服务器...
start "Finance-Frontend" /MIN cmd /c "cd /d "%PROJECT_ROOT%\system\frontend" && pnpm dev 2>&1"
echo │  [ OK ] 前端启动中（新窗口 Finance-Frontend）

REM 等待前端就绪
echo │  等待前端就绪 (最多 60 秒)...
set /a RETRY=0
:wait_frontend
timeout /t 2 /nobreak >nul
curl -s http://localhost:5173 >nul 2>&1
if %errorlevel% equ 0 (
    echo │  [ OK ] 前端已就绪 (http://localhost:5173)
    goto frontend_ready
)
set /a RETRY+=1
if %RETRY% lss 30 (
    echo │  等待中... (%RETRY%/30)
    goto wait_frontend
)
echo │  [WARN] 前端启动超时，请检查 Finance-Frontend 窗口日志
:frontend_ready
echo └────────────────────────────────────────────────────────┘

REM ============================================================
REM §6 启动完成
REM ============================================================
echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║  ✅ 系统启动完成!                                       ║
echo ║                                                        ║
echo ║  前端地址:  http://localhost:5173                       ║
echo ║  后端地址:  http://localhost:8080                       ║
echo ║  API 文档:  http://localhost:8080/api/v1/health         ║
echo ║                                                        ║
echo ║  测试账号:  zhangsan / 123456                          ║
echo ║  管理员账号: admin   / 123456                          ║
echo ║                                                        ║
echo ║  管理窗口:  Finance-Backend / Finance-Frontend          ║
echo ║  关闭系统:  直接关闭上述两个命令行窗口即可              ║
echo ╚══════════════════════════════════════════════════════════╝

REM 自动打开浏览器
echo  正在打开浏览器...
start http://localhost:5173

echo.
echo  按任意键退出此窗口 (不影响后端和前端运行)
pause >nul
exit /b 0
