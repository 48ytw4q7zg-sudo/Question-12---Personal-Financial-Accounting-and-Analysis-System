@echo off
chcp 65001 >nul
title 个人财务记账与分析系统 - 一键启动
color 0B

REM ============================================================
REM 个人财务记账与分析系统 - 一键启动脚本（Windows 重启后恢复）
REM Author: qxw · Author-ID: 2501060122
REM 版本: v2.0 · 2026-05-23
REM 功能: 电脑重启后一键启动 MySQL + 后端 + 前端 + 打开浏览器
REM 环境要求: JDK 21 + Maven 3.9 + Node.js 24 + pnpm 10 + MySQL 8.4
REM ============================================================

echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║   个人财务记账与分析系统 · 一键启动 v2.0                ║
echo ║   SpringBoot 3.5.14 + Vue 3.5.34 + MySQL 8.4 LTS       ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

REM --- 切换到项目根目录（star 的上一级） ---
cd /d "%~dp0.."
set "PROJECT_ROOT=%CD%"
echo [信息] 项目根目录: %PROJECT_ROOT%
echo.

REM ============================================================
REM 第 1 步: 检查并启动 MySQL 服务
REM ============================================================
echo ──────────────────────────────────────────────
echo [1/6] 检查 MySQL 服务...
echo ──────────────────────────────────────────────

REM 自动检测 MySQL 服务名（兼容 MySQL80 / MySQL84 / MySQL 等不同安装）
set "MYSQL_SERVICE="
sc query MySQL84 2>nul | find "SERVICE_NAME" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "MYSQL_SERVICE=MySQL84"
) else (
    sc query MySQL80 2>nul | find "SERVICE_NAME" >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set "MYSQL_SERVICE=MySQL80"
    ) else (
        sc query MySQL 2>nul | find "SERVICE_NAME" >nul 2>&1
        if %ERRORLEVEL% EQU 0 (
            set "MYSQL_SERVICE=MySQL"
        )
    )
)

if "%MYSQL_SERVICE%"=="" (
    echo [错误] 未检测到 MySQL 服务!
    echo        请确认已安装 MySQL 8.4 LTS
    echo        运行 "sc query state= all ^| findstr mysql" 查看实际服务名
    echo        然后在脚本中手动设置 MYSQL_SERVICE 变量
    pause
    exit /b 1
)

echo        检测到 MySQL 服务: %MYSQL_SERVICE%

sc query %MYSQL_SERVICE% | find "RUNNING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        %MYSQL_SERVICE% 服务已在运行 [OK]
) else (
    echo        %MYSQL_SERVICE% 服务未运行, 正在启动...
    net start %MYSQL_SERVICE% >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo        %MYSQL_SERVICE% 服务启动成功 [OK]
    ) else (
        echo [错误] %MYSQL_SERVICE% 启动失败! 请以管理员身份运行此脚本
        echo        或手动执行: net start %MYSQL_SERVICE%
        pause
        exit /b 1
    )
)
echo.

REM ============================================================
REM 第 2 步: 初始化数据库（首次运行时）
REM ============================================================
echo ──────────────────────────────────────────────
echo [2/6] 检查数据库 finance_db...
echo ──────────────────────────────────────────────

REM 尝试连接数据库（默认 root/root，兼容开发环境）
set "DB_USER=root"
set "DB_PASS=root"

mysql -u%DB_USER% -p%DB_PASS% -e "SELECT 1;" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo        默认密码 root 连接失败, 尝试空密码...
    set "DB_PASS="
    mysql -u%DB_USER% -e "SELECT 1;" >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo [错误] 无法连接 MySQL! 请确认:
        echo        1. MySQL root 密码是否为 root 或空
        echo        2. mysql 命令是否在 PATH 中
        echo        3. 修改脚本中的 DB_USER / DB_PASS 变量适配你的密码
        pause
        exit /b 1
    )
)

mysql -u%DB_USER% -p%DB_PASS% -e "USE finance_db;" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo        数据库 finance_db 不存在, 正在初始化...
    if "%DB_PASS%"=="" (
        mysql -u%DB_USER% < "%PROJECT_ROOT%\system\sql\01-init.sql" >nul 2>&1
    ) else (
        mysql -u%DB_USER% -p%DB_PASS% < "%PROJECT_ROOT%\system\sql\01-init.sql" >nul 2>&1
    )
    if %ERRORLEVEL% EQU 0 (
        echo        数据库初始化成功 [OK] (7表 + 种子数据 + 测试账号)
    ) else (
        echo [错误] 数据库初始化失败!
        echo        请手动执行: mysql -uroot -p ^< system\sql\01-init.sql
        pause
        exit /b 1
    )
) else (
    echo        数据库 finance_db 已存在 [OK]
)
echo.

REM ============================================================
REM 第 3 步: 设置后端环境变量
REM ============================================================
echo ──────────────────────────────────────────────
echo [3/6] 设置后端环境变量...
echo ──────────────────────────────────────────────

REM application.yml 要求 DB_PASSWORD 和 JWT_SECRET 必须通过环境变量注入
REM 开发环境使用以下默认值, 生产环境请修改
set "DB_USERNAME=%DB_USER%"
set "DB_PASSWORD=%DB_PASS%"
set "JWT_SECRET=dev-finance-jwt-secret-key-2026-qxw-2501060122-must-be-at-least-32-bytes"
set "JWT_EXPIRE=604800000"
set "SERVER_PORT=8080"
set "MYBATIS_LOG_IMPL=org.apache.ibatis.logging.stdout.StdOutImpl"
set "LOG_LEVEL=info"

echo        DB_USERNAME = %DB_USERNAME%
echo        DB_PASSWORD = *****(已隐藏)
echo        JWT_SECRET  = dev-*****(开发环境密钥, 已隐藏)
echo        SERVER_PORT = %SERVER_PORT%
echo        [OK] 环境变量已设置
echo.

REM ============================================================
REM 第 4 步: 启动后端（新窗口）
REM ============================================================
echo ──────────────────────────────────────────────
echo [4/6] 启动后端 (SpringBoot 3.5.14 · 端口 %SERVER_PORT%)...
echo ──────────────────────────────────────────────

REM 在新窗口中启动后端, 传递必要的环境变量
start "后端-个人财务记账系统(8080)" cmd /k "title 后端-SpringBoot-8080 && cd /d "%PROJECT_ROOT%\system\backend" && set DB_USERNAME=%DB_USERNAME% && set DB_PASSWORD=%DB_PASSWORD% && set JWT_SECRET=%JWT_SECRET% && set JWT_EXPIRE=%JWT_EXPIRE% && set SERVER_PORT=%SERVER_PORT% && set MYBATIS_LOG_IMPL=%MYBATIS_LOG_IMPL% && set LOG_LEVEL=%LOG_LEVEL% && echo [后端] 正在启动 SpringBoot... 首次启动需下载依赖, 请耐心等待 && mvn spring-boot:run"

echo        后端启动命令已发出(新窗口)
echo        等待约 20 秒让 SpringBoot 完成启动...
timeout /t 20 /nobreak >nul

REM 验证后端是否启动成功
curl -s http://localhost:%SERVER_PORT%/api/v1/health >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        后端健康检查通过 [OK]
) else (
    echo        [警告] 后端健康检查未通过, 可能仍在启动中
    echo        请查看后端窗口确认日志是否正常
    echo        首次启动可能需要更长时间下载 Maven 依赖
)
echo.

REM ============================================================
REM 第 5 步: 启动前端（新窗口）
REM ============================================================
echo ──────────────────────────────────────────────
echo [5/6] 启动前端 (Vue 3.5.34 · 端口 5173)...
echo ──────────────────────────────────────────────

cd /d "%PROJECT_ROOT%\system\frontend"
if not exist node_modules (
    echo        首次运行, 正在安装前端依赖 (pnpm install)...
    pnpm install
    if %ERRORLEVEL% NEQ 0 (
        echo [错误] pnpm install 失败! 请确认:
        echo        1. Node.js 24 LTS 已安装: node -v
        echo        2. pnpm 已安装: pnpm -v
        echo        3. 网络连接正常
        pause
        exit /b 1
    )
    echo        前端依赖安装完成 [OK]
)

start "前端-个人财务记账系统(5173)" cmd /k "title 前端-Vue3-5173 && cd /d "%PROJECT_ROOT%\system\frontend" && echo [前端] 正在启动 Vite 开发服务器... && pnpm dev"

cd /d "%PROJECT_ROOT%"
echo        前端启动命令已发出(新窗口)
echo        等待约 8 秒让 Vite 完成启动...
timeout /t 8 /nobreak >nul
echo        [OK]
echo.

REM ============================================================
REM 第 6 步: 打开浏览器
REM ============================================================
echo ──────────────────────────────────────────────
echo [6/6] 打开浏览器...
echo ──────────────────────────────────────────────

start "" "http://localhost:5173"
echo        浏览器已打开 [OK]
echo.

REM ============================================================
REM 启动完成 · 显示汇总信息
REM ============================================================
echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║                 系统启动完成!                            ║
echo ╠══════════════════════════════════════════════════════════╣
echo ║                                                          ║
echo ║   前端地址: http://localhost:5173                        ║
echo ║   后端地址: http://localhost:8080/api/v1/health          ║
echo ║                                                          ║
echo ║   测试账号(普通用户): zhangsan / 123456                  ║
echo ║   测试账号(管理员):   admin / 123456                     ║
echo ║                                                          ║
echo ║   如果登录失败, 请重新执行 system\sql\01-init.sql        ║
echo ║                                                          ║
echo ╠══════════════════════════════════════════════════════════╣
echo ║   关闭窗口说明:                                          ║
echo ║   - 关闭"后端"窗口 = 停止后端服务                        ║
echo ║   - 关闭"前端"窗口 = 停止前端服务                        ║
echo ║   - 关闭本窗口不影响后端和前端运行                       ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
pause
