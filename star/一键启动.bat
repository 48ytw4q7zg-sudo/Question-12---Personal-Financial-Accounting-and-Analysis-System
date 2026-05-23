@echo off
REM ============================================================
REM   个人财务记账与分析系统 — 一键启动（Win10/Win11 兼容）
REM   Author: qxw / 2501060122
REM   Version: v4.0  / 2026-05-23
REM   说明: 启动顺序 = 环境检查 → 数据库自检/补丁 → 编译 → 启动后端 → 启动前端 → 浏览器打开
REM ============================================================

setlocal enabledelayedexpansion

REM ---------- 0. 控制台编码兼容 (Win10 默认 GBK / Win11 部分用户 UTF-8) ----------
chcp 65001 >nul 2>&1
if errorlevel 1 chcp 936 >nul 2>&1
title 个人财务记账与分析系统 - 启动器

REM ---------- 1. 项目根目录定位 (兼容含空格路径) ----------
set "ROOT=%~dp0.."
pushd "%ROOT%" >nul 2>&1
if errorlevel 1 (
    echo [FAIL] 无法切换到项目根目录: %ROOT%
    pause
    exit /b 1
)
set "ROOT=%CD%"

REM ---------- 2. 系统版本探测 (用于决定 timeout 兼容性) ----------
set "WIN_VER="
for /f "tokens=4-5 delims=. " %%i in ('ver') do set "WIN_VER=%%i.%%j"
echo.
echo ============================================================
echo   个人财务记账与分析系统 (qxw / 2501060122)
echo   Windows 内核版本: %WIN_VER% ^| 项目根: %ROOT%
echo ============================================================
echo.
echo [%date% %time%] 启动开始...

REM ============================================================
REM   STEP 1/6 - 环境检查 (Java / Maven / Node / pnpm / mysql)
REM ============================================================
echo.
echo ---- [1/6] 环境检查 ----

call :check_cmd java   "Java"      || goto :fatal
call :check_cmd mvn    "Maven"     || goto :fatal
call :check_cmd node   "Node.js"   || goto :fatal
call :check_cmd pnpm   "pnpm"      || goto :fatal
call :check_cmd mysql  "MySQL CLI" || (echo   [WARN] mysql.exe 不在 PATH，将跳过数据库自检步骤)

REM curl 在 Win10 1803+ / Win11 全版本均自带 (system32\curl.exe)
call :check_cmd curl   "curl"      || (echo   [WARN] 未发现 curl, 健康检查将退化为端口探测)

REM ============================================================
REM   STEP 2/6 - 数据库自检与补丁 (幂等; 缺失时自动初始化)
REM ============================================================
echo.
echo ---- [2/6] 数据库自检 ----
where mysql >nul 2>&1
if errorlevel 1 (
    echo   [SKIP] 未找到 mysql 可执行文件
) else (
    REM 用 -hlocalhost -P3306 强制走 TCP，避免 Win10 默认走 socket 失败
    mysql -uroot -proot -hlocalhost -P3306 -e "SELECT 1" >nul 2>&1
    if errorlevel 1 (
        echo   [WARN] root/root 连不上 MySQL，跳过自检
    ) else (
        REM 检查目标库是否存在
        mysql -uroot -proot -hlocalhost -P3306 -e "USE finance_db; SELECT 1" >nul 2>&1
        if errorlevel 1 (
            echo   [INIT] 数据库 finance_db 不存在，执行 sql\01-init.sql ...
            mysql -uroot -proot -hlocalhost -P3306 < "%ROOT%\sql\01-init.sql" >nul 2>&1
            if errorlevel 1 (
                echo   [FAIL] 数据库初始化失败 (检查 sql\01-init.sql)
                goto :fatal
            )
            echo   [OK] 数据库已初始化
        ) else (
            echo   [OK] finance_db 已存在
        )
        REM 应用增量补丁 (幂等: 若列已存在不会重复添加)
        if exist "%ROOT%\sql\fix_budget_alert.sql" (
            mysql -uroot -proot -hlocalhost -P3306 < "%ROOT%\sql\fix_budget_alert.sql" >nul 2>&1
            echo   [OK] budget_alert 表结构已对齐
        )
    )
)

REM ============================================================
REM   STEP 3/6 - 后端编译 (Maven)
REM ============================================================
echo.
echo ---- [3/6] 后端编译 ----
if not exist "%ROOT%\system\backend\pom.xml" (
    echo   [FAIL] 未找到 system\backend\pom.xml
    goto :fatal
)
pushd "%ROOT%\system\backend"
echo   编译中 (使用本地仓库, 静默)...
call mvn -q -B -DskipTests compile 1>nul 2>nul
if errorlevel 1 (
    echo   [WARN] 离线编译失败，尝试在线 mvn -U ...
    call mvn -B -U -DskipTests compile
    if errorlevel 1 (
        popd
        echo   [FAIL] 后端编译失败
        goto :fatal
    )
)
popd
echo   [OK] 后端编译通过

REM ============================================================
REM   STEP 4/6 - 启动后端 (Spring Boot, port 8080)
REM ============================================================
echo.
echo ---- [4/6] 启动后端 (端口 8080) ----
call :kill_port 8080

pushd "%ROOT%\system\backend"
echo   在新窗口启动 Spring Boot ...
REM start /D 指定工作目录, 显式 cmd /k 让窗口保持打开便于排错
start "Backend - Spring Boot" cmd /k "cd /d %ROOT%\system\backend && mvn -B spring-boot:run"
popd

echo   等待后端就绪 (最长 180 秒)...
set /a WAIT_BE=0
:wait_be
call :sleep 3
call :probe_health "http://localhost:8080/api/v1/health"
if !ERRORLEVEL! equ 0 (
    echo   [OK] 后端已就绪
    goto :be_done
)
set /a WAIT_BE+=1
if !WAIT_BE! lss 60 (
    echo   ...等待 !WAIT_BE!/60 ^(每轮 3 秒^)
    goto :wait_be
)
echo   [WARN] 后端启动超时, 请查看 "Backend - Spring Boot" 窗口排错
:be_done

REM ============================================================
REM   STEP 5/6 - 启动前端 (Vite, port 5173)
REM ============================================================
echo.
echo ---- [5/6] 启动前端 (端口 5173) ----
call :kill_port 5173

if not exist "%ROOT%\system\frontend\node_modules" (
    echo   首次启动: 安装前端依赖 (pnpm install)...
    pushd "%ROOT%\system\frontend"
    call pnpm install --reporter=default
    if errorlevel 1 (
        popd
        echo   [FAIL] pnpm install 失败
        goto :fatal
    )
    popd
)

pushd "%ROOT%\system\frontend"
echo   在新窗口启动 Vite ...
start "Frontend - Vite" cmd /k "cd /d %ROOT%\system\frontend && pnpm dev"
popd

echo   等待前端就绪 (最长 90 秒)...
set /a WAIT_FE=0
:wait_fe
call :sleep 2
call :probe_health "http://localhost:5173"
if !ERRORLEVEL! equ 0 (
    echo   [OK] 前端已就绪
    goto :fe_done
)
set /a WAIT_FE+=1
if !WAIT_FE! lss 45 (
    echo   ...等待 !WAIT_FE!/45 ^(每轮 2 秒^)
    goto :wait_fe
)
echo   [WARN] 前端启动超时, 请查看 "Frontend - Vite" 窗口排错
:fe_done

REM ============================================================
REM   STEP 6/6 - 完成与浏览器打开
REM ============================================================
echo.
echo ============================================================
echo   [SUCCESS] 系统已启动
echo ------------------------------------------------------------
echo   前端入口    : http://localhost:5173
echo   后端入口    : http://localhost:8080
echo   健康检查    : http://localhost:8080/api/v1/health
echo ------------------------------------------------------------
echo   测试账号    : zhangsan / 123456
echo   管理员账号  : admin    / 123456
echo ============================================================
echo.

REM 用 explorer 打开 (start 在 Win10 部分版本对 url 有 quirk; explorer 兼容性最好)
start "" "http://localhost:5173"

echo   两个新窗口分别承载 backend / frontend, 关闭它们即停止服务
echo   按任意键关闭本启动器窗口 (服务进程不受影响)
pause >nul
popd >nul 2>&1
endlocal
exit /b 0


REM ============================================================
REM                       工 具 函 数 区
REM ============================================================

REM ---------- check_cmd: 检查命令是否在 PATH 中 ----------
REM 参数: %1 = 可执行文件名, %2 = 显示名称
:check_cmd
where %~1 >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] 未发现 %~2 ^(请安装并加入 PATH^)
    exit /b 1
)
echo   [OK] %~2
exit /b 0

REM ---------- kill_port: 释放占用端口 ----------
REM 参数: %1 = 端口号
:kill_port
set "PORT=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
    echo   释放端口 %PORT% 占用 (PID=%%a)
    taskkill /F /PID %%a >nul 2>&1
)
exit /b 0

REM ---------- sleep: 跨版本休眠 (timeout 在某些 Win10 SKU 报错时退化为 ping) ----------
REM 参数: %1 = 秒数
:sleep
timeout /t %~1 /nobreak >nul 2>&1
if errorlevel 1 (
    REM Win10 部分 SKU + Cygwin shim 下 timeout 不可用, 用 ping 兜底
    ping -n %~1 127.0.0.1 >nul 2>&1
)
exit /b 0

REM ---------- probe_health: 探测 URL 是否可达 (curl 优先, 否则 powershell 兜底) ----------
REM 参数: %1 = URL
:probe_health
where curl >nul 2>&1
if not errorlevel 1 (
    curl -s -o nul -w "%%{http_code}" --max-time 3 %~1 2>nul | findstr /r "^[23]" >nul
    exit /b !ERRORLEVEL!
)
REM curl 不可用 → PowerShell Invoke-WebRequest 兜底 (Win10/Win11 默认自带)
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri '%~1' -TimeoutSec 3; if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 400) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
exit /b !ERRORLEVEL!

REM ---------- fatal: 致命错误退出 ----------
:fatal
echo.
echo ============================================================
echo   [FATAL] 启动失败, 请根据上方提示排错
echo ============================================================
pause
popd >nul 2>&1
endlocal
exit /b 1
