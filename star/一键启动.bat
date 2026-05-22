@echo off
chcp 65001 >nul
title 个人财务记账与分析系统 - 一键启动

REM ============================================================
REM 个人财务记账与分析系统 - 一键启动脚本
REM Author: qxw · Author-ID: 2501060122
REM 功能: 电脑重启后一键启动 MySQL + 后端 + 前端
REM ============================================================

echo ══════════════════════════════════════════════════════════
echo   个人财务记账与分析系统 · 一键启动
echo ══════════════════════════════════════════════════════════
echo.

REM --- 切换到项目根目录 ---
cd /d "%~dp0.."

REM --- 第一步: 检查 MySQL 服务 ---
echo [1/5] 检查 MySQL 服务...
sc query MySQL80 | find "RUNNING" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       MySQL80 服务已在运行 ✓
) else (
    echo       MySQL80 服务未运行,正在启动...
    net start MySQL80 >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo       MySQL80 服务启动成功 ✓
    ) else (
        echo       ✗ MySQL80 启动失败! 请检查 MySQL 安装和服务名
        echo       可尝试: sc query state= all 查找实际服务名
        pause
        exit /b 1
    )
)

REM --- 第二步: 初始化数据库(仅首次) ---
echo [2/5] 检查数据库 finance_db...
mysql -uroot -proot -e "USE finance_db;" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo       数据库不存在,正在初始化...
    mysql -uroot -proot < system\sql\01-init.sql >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo       数据库初始化成功 ✓ (7表+种子数据)
    ) else (
        echo       ✗ 数据库初始化失败! 请检查 MySQL root 密码
        echo       默认密码为 root,若已修改请编辑 system\sql\01-init.sql 适配
        pause
        exit /b 1
    )
) else (
    echo       数据库 finance_db 已存在 ✓
)

REM --- 第三步: 启动后端 ---
echo [3/5] 启动后端 (SpringBoot · 端口 8080)...
cd system\backend
start "后端-个人财务记账系统" cmd /k "mvn spring-boot:run"
cd /d "%~dp0.."
echo       后端启动命令已发出(新窗口) — 等待约 15 秒完成启动...
timeout /t 15 /nobreak >nul

REM --- 第四步: 启动前端 ---
echo [4/5] 启动前端 (Vue 3 · 端口 5173)...
cd system\frontend
if not exist node_modules (
    echo       首次运行,正在安装依赖...
    pnpm install
)
start "前端-个人财务记账系统" cmd /k "pnpm dev"
cd /d "%~dp0.."
echo       前端启动命令已发出(新窗口) — 等待约 10 秒完成启动...
timeout /t 10 /nobreak >nul

REM --- 第五步: 打开浏览器 ---
echo [5/5] 打开浏览器...
start http://localhost:5173

echo.
echo ══════════════════════════════════════════════════════════
echo   系统已启动! 请在新窗口中观察启动日志
echo   前端地址: http://localhost:5173
echo   后端地址: http://localhost:8080/api/health
echo   测试账号: zhangsan / 123456
echo   管理账号: admin / 123456
echo ══════════════════════════════════════════════════════════
echo.
echo 关闭系统: 直接关闭后端和前端命令行窗口即可
pause