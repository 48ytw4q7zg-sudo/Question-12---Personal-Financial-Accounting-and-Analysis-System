@echo off
REM ============================================================
REM   个人财务记账与分析系统 — 一键启动 v5.2
REM   兼容: Windows 7/8/8.1/10/11 (含 Server 2012-2022)
REM   Author: qxw / 2501060122
REM   Date  : 2026-05-23
REM
REM   v5.2 vs v5.1: -WorkingDirectory 取代 cd /d 内联命令,彻底消除路径双引号转义问题
REM   v5.1 vs v5.0: ps命令内联(取消 ^ 续行歧义), mvn.cmd+pnpm.CMD 显式调用
REM
REM   设计:
REM     后端 → PowerShell Start-Process mvn.cmd spring-boot:run (WorkingDirectory=system/backend)
REM     前端 → PowerShell Start-Process pnpm.CMD dev         (WorkingDirectory=system/frontend)
REM     日志 → logs/backend-startup.log 时带时间戳,失败自动弹出记事本
REM     探测 → 纯 PowerShell Invoke-WebRequest,无 timeout/curl 依赖
REM ============================================================

setlocal enabledelayedexpansion

REM ---------- 0. 控制台编码 ----------
chcp 65001 >nul 2>&1
title 个人财务记账与分析系统 - 启动器 v5.2

REM ---------- 1. 项目根定位 ----------
set "ROOT=%~dp0.."
pushd "%ROOT%" >nul
if errorlevel 1 (
    echo [FATAL] 无法切换到项目根: %ROOT%
    pause
    exit /b 1
)
set "ROOT=%CD%"

REM ---------- 2. 日志目录 ----------
if not exist "%ROOT%\logs" mkdir "%ROOT%\logs" >nul 2>&1
set "BE_LOG=%ROOT%\logs\backend-startup.log"
set "BE_ERR=%ROOT%\logs\backend-error.log"
set "FE_LOG=%ROOT%\logs\frontend-startup.log"
set "FE_ERR=%ROOT%\logs\frontend-error.log"
del /Q "%BE_LOG%" "%BE_ERR%" "%FE_LOG%" "%FE_ERR%" >nul 2>&1

REM ---------- 3. Banner ----------
echo.
echo ============================================================
echo   个人财务记账与分析系统  (qxw / 2501060122)
echo   版本: v5.2  ^|  2026-05-23
echo   项目根: %ROOT%
echo   日志目录: %ROOT%\logs
echo ============================================================
echo.

REM ============================================================
REM   STEP 1/6 — 环境检查
REM ============================================================
echo ---- [1/6] 环境检查 ----
call :need java   "Java"     || goto :fatal
call :need mvn    "Maven"    || goto :fatal
call :need node   "Node.js"  || goto :fatal
call :need pnpm   "pnpm"     || goto :fatal
call :soft mysql  "MySQL CLI"
echo.

REM ============================================================
REM   STEP 2/6 — 数据库自检 + 补丁
REM ============================================================
echo ---- [2/6] 数据库自检 ----
where mysql >nul 2>&1
if errorlevel 1 (
    echo   [SKIP] 未找到 mysql, 假设数据库已就绪
) else (
    mysql -uroot -proot -hlocalhost -P3306 -e "SELECT 1" >nul 2>&1
    if errorlevel 1 (
        echo   [WARN] 无法连接 MySQL (root/root), 跳过自检
    ) else (
        mysql -uroot -proot -hlocalhost -P3306 -e "USE finance_db" >nul 2>&1
        if errorlevel 1 (
            echo   [INIT] 初始化数据库 finance_db ...
            mysql -uroot -proot -hlocalhost -P3306 < "%ROOT%\sql\01-init.sql" >"%ROOT%\logs\db-init.log" 2>&1
            if errorlevel 1 (
                echo   [FAIL] 初始化失败, 详见 %ROOT%\logs\db-init.log
                start "" notepad.exe "%ROOT%\logs\db-init.log"
                goto :fatal
            )
            echo   [OK] 数据库已初始化
        ) else (
            echo   [OK] finance_db 已存在
        )
        if exist "%ROOT%\sql\fix_budget_alert.sql" (
            mysql -uroot -proot -hlocalhost -P3306 < "%ROOT%\sql\fix_budget_alert.sql" >nul 2>&1
            echo   [OK] budget_alert 表结构补丁已应用
        )
    )
)
echo.

REM ============================================================
REM   STEP 3/6 — 后端预编译
REM ============================================================
echo ---- [3/6] 后端编译 ----
if not exist "%ROOT%\system\backend\pom.xml" (
    echo   [FATAL] 未找到 system\backend\pom.xml
    goto :fatal
)
pushd "%ROOT%\system\backend"
echo   编译中, 首次可能需 1-3 分钟下载依赖...
call mvn -B -q -DskipTests compile >"%ROOT%\logs\mvn-compile.log" 2>&1
if errorlevel 1 (
    echo   [WARN] 离线编译失败, 尝试 mvn -U 在线编译...
    call mvn -B -U -DskipTests compile >>"%ROOT%\logs\mvn-compile.log" 2>&1
    if errorlevel 1 (
        popd
        echo   [FAIL] 后端编译失败, 详见 %ROOT%\logs\mvn-compile.log
        start "" notepad.exe "%ROOT%\logs\mvn-compile.log"
        goto :fatal
    )
)
popd
echo   [OK] 后端编译通过
echo.

REM ============================================================
REM   STEP 4/6 — 启动后端 (8080)
REM ============================================================
echo ---- [4/6] 启动后端 (端口 8080) ----
call :kill_port 8080

REM v5.2: -WorkingDirectory 设置工作目录,命令中无路径双引号,消除转义风险
REM 显式 mvn.cmd — Windows 上 mvn 是 mvn.cmd 包装,cmd 子环境可能找不到无后缀版本
echo   启动后端 (后台最小化) ...
powershell -NoProfile -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','echo [%date% %time%] Backend start && mvn.cmd -B spring-boot:run' -WorkingDirectory '%ROOT%\system\backend' -WindowStyle Minimized -RedirectStandardOutput '%BE_LOG%' -RedirectStandardError '%BE_ERR%'"
if errorlevel 1 (
    echo   [FAIL] 后端启动命令调用失败
    goto :fatal
)

echo   等待后端就绪 (最长 180 秒, 每 3 秒探测)...
set /a BE_TRY=0
:wait_be
call :ps_sleep 3
call :probe "http://localhost:8080/api/v1/health"
if !ERRORLEVEL! equ 0 (
    echo   [OK] 后端已就绪 (http://localhost:8080)
    goto :be_done
)
set /a BE_TRY+=1
if !BE_TRY! lss 60 (
    echo   ...等待 !BE_TRY!/60
    goto :wait_be
)
echo   [FAIL] 后端启动超时 (180s)
echo   日志: %BE_LOG%  ^|  错误: %BE_ERR%
start "" notepad.exe "%BE_LOG%"
goto :fatal
:be_done
echo.

REM ============================================================
REM   STEP 5/6 — 启动前端 (5173)
REM ============================================================
echo ---- [5/6] 启动前端 (端口 5173) ----
call :kill_port 5173

if not exist "%ROOT%\system\frontend\node_modules" (
    echo   首次运行, 安装前端依赖 (可能需 1-2 分钟)...
    pushd "%ROOT%\system\frontend"
    call pnpm install >"%ROOT%\logs\pnpm-install.log" 2>&1
    if errorlevel 1 (
        popd
        echo   [FAIL] pnpm install 失败, 详见 %ROOT%\logs\pnpm-install.log
        start "" notepad.exe "%ROOT%\logs\pnpm-install.log"
        goto :fatal
    )
    popd
    echo   [OK] 依赖安装完成
)

REM v5.2: 显式 pnpm.CMD — Windows 上 pnpm 是 sh 脚本, 只有 git-bash 能执行; cmd 必须 .CMD
echo   启动前端 (后台最小化) ...
powershell -NoProfile -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','pnpm.CMD dev' -WorkingDirectory '%ROOT%\system\frontend' -WindowStyle Minimized -RedirectStandardOutput '%FE_LOG%' -RedirectStandardError '%FE_ERR%'"
if errorlevel 1 (
    echo   [FAIL] 前端启动命令调用失败
    goto :fatal
)

echo   等待前端就绪 (最长 90 秒, 每 2 秒探测)...
set /a FE_TRY=0
:wait_fe
call :ps_sleep 2
call :probe "http://localhost:5173"
if !ERRORLEVEL! equ 0 (
    echo   [OK] 前端已就绪 (http://localhost:5173)
    goto :fe_done
)
set /a FE_TRY+=1
if !FE_TRY! lss 45 (
    echo   ...等待 !FE_TRY!/45
    goto :wait_fe
)
echo   [FAIL] 前端启动超时 (90s)
echo   日志: %FE_LOG%  ^|  错误: %FE_ERR%
start "" notepad.exe "%FE_LOG%"
goto :fatal
:fe_done
echo.

REM ============================================================
REM   STEP 6/6 — 完成, 打开浏览器
REM ============================================================
echo ============================================================
echo   [SUCCESS] 系统启动完成
echo ------------------------------------------------------------
echo   前端入口      : http://localhost:5173
echo   后端入口      : http://localhost:8080
echo   健康检查      : http://localhost:8080/api/v1/health
echo ------------------------------------------------------------
echo   测试账号      : zhangsan / 123456
echo   管理员账号    : admin    / 123456
echo ------------------------------------------------------------
echo   后端日志      : %BE_LOG%
echo   前端日志      : %FE_LOG%
echo ============================================================
echo.

start "" "http://localhost:5173"

echo   后端/前端在后台最小化窗口中运行
echo   关闭这些窗口即可停止服务
echo   按任意键关闭本启动器 (不影响后台服务)
pause >nul
popd >nul
endlocal
exit /b 0


REM ============================================================
REM                      工 具 函 数 区
REM ============================================================

:need
where %~1 >nul 2>&1
if errorlevel 1 (
    echo   [FAIL] 未发现 %~2 (请安装并加入 PATH)
    exit /b 1
)
echo   [OK] %~2
exit /b 0

:soft
where %~1 >nul 2>&1
if errorlevel 1 (
    echo   [WARN] 未发现 %~2 (可选, 跳过)
) else (
    echo   [OK] %~2
)
exit /b 0

:kill_port
set "PORT=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
    echo   释放端口 %PORT% 占用 PID=%%a
    taskkill /F /PID %%a >nul 2>&1
)
exit /b 0

:ps_sleep
powershell -NoProfile -Command "Start-Sleep -Seconds %~1" >nul 2>&1
exit /b 0

:probe
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri '%~1' -TimeoutSec 3; if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
exit /b !ERRORLEVEL!

:fatal
echo.
echo ============================================================
echo   [FATAL] 启动失败
echo ------------------------------------------------------------
echo   日志目录: %ROOT%\logs\
echo     - mvn-compile.log      (后端编译)
echo     - backend-startup.log  (后端 stdout)
echo     - backend-error.log    (后端 stderr)
echo     - pnpm-install.log     (前端依赖)
echo     - frontend-startup.log (前端 stdout)
echo     - frontend-error.log   (前端 stderr)
echo     - db-init.log          (数据库初始化)
echo ============================================================
pause
popd >nul 2>&1
endlocal
exit /b 1
