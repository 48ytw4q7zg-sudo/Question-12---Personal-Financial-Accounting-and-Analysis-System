@echo off
chcp 65001 >nul
title 个人财务记账与分析系统 - 一键启动

:: ============================================================
:: 个人财务记账与分析系统 · 一键启动脚本
:: 电脑重启后双击此文件即可启动完整系统
:: 依赖: MySQL 8.4 + JDK 21+ + Maven 3.9+ + Node.js 22+ + pnpm 10+
:: ============================================================

echo.
echo  ========================================
echo  个人财务记账与分析系统 - 一键启动
echo  ========================================
echo.

:: ---- 设置项目根目录(自动检测) ----
set "PROJECT_ROOT=%~dp0.."
set "PROJECT_ROOT=%PROJECT_ROOT:\star\=%"

:: ---- 检查 MySQL 服务 ----
echo  [1/5] 检查 MySQL 服务...
net start MySQL80 >nul 2>&1
if %errorlevel% equ 0 (
    echo       MySQL 服务已启动
) else (
    sc query MySQL80 | find "RUNNING" >nul 2>&1
    if %errorlevel% equ 0 (
        echo       MySQL 服务已在运行
    ) else (
        echo       [警告] MySQL 服务未运行,尝试启动...
        net start MySQL80 >nul 2>&1
        if %errorlevel% neq 0 (
            echo       [错误] 无法启动 MySQL,请手动启动后重试!
            echo       命令: net start MySQL80
            pause
            exit /b 1
        )
        echo       MySQL 服务启动成功
    )
)

:: ---- 等待 MySQL 就绪 ----
echo       等待 MySQL 就绪(5秒)...
timeout /t 5 /nobreak >nul

:: ---- 初始化数据库(仅首次) ----
echo  [2/5] 初始化数据库...
mysql -uroot -proot -e "USE finance_db;" >nul 2>&1
if %errorlevel% neq 0 (
    echo       数据库不存在,执行初始化脚本...
    mysql -uroot -proot < "%PROJECT_ROOT%\system\sql\01-init.sql" 2>nul
    if %errorlevel% neq 0 (
        echo       [错误] 数据库初始化失败! 请检查 MySQL root 密码是否为 root
        pause
        exit /b 1
    )
    echo       数据库初始化完成(7表+种子数据)
) else (
    echo       数据库已存在,跳过初始化
)

:: ---- 启动后端(SpringBoot) ----
echo  [3/5] 启动后端服务(SpringBoot:8080)...
cd /d "%PROJECT_ROOT%\system\backend"
start "后端-SpringBoot-8080" cmd /k "mvn spring-boot:run -q"
echo       后端启动中,等待就绪(约15秒)...
echo       可在新窗口查看启动日志

:: ---- 等待后端就绪 ----
set READY=0
for /L %%i in (1,1,30) do (
    if %READY% equ 0 (
        curl -s -o nul -w "" http://localhost:8080/api/health >nul 2>&1
        if %errorlevel% equ 0 (
            set READY=1
            echo       后端已就绪! (%%i秒)
        )
        timeout /t 1 /nobreak >nul
    )
)
if %READY% equ 0 (
    echo       [提示] 后端可能仍在启动中,请等待新窗口日志显示 "Started Application"
)

:: ---- 启动前端(Vite) ----
echo  [4/5] 启动前端服务(Vite:5173)...
cd /d "%PROJECT_ROOT%\system\frontend"

:: 检查 node_modules 是否存在
if not exist "node_modules" (
    echo       首次运行,安装前端依赖...
    call pnpm install
    if %errorlevel% neq 0 (
        echo       [错误] 前端依赖安装失败!
        pause
        exit /b 1
    )
)

start "前端-Vite-5173" cmd /k "pnpm dev"
echo       前端启动中(约3秒)...

:: ---- 完成 ----
echo  [5/5] 启动完成!
echo.
echo  ========================================
echo  系统已启动!
echo.
echo  前端地址: http://localhost:5173
echo  后端地址: http://localhost:8080
echo.
echo  测试账号:
echo    普通用户: zhangsan / 123456
echo    管理员:   admin / 123456
echo    普通用户: lisi / 123456
echo.
echo  关闭系统: 关闭后端和前端的命令窗口即可
echo  ========================================
echo.

:: 自动打开浏览器
timeout /t 5 /nobreak >nul
start http://localhost:5173

pause