# ============================================================
# 个人财务记账与分析系统 - 一键启动 PowerShell 版
# Author: qxw · Author-ID: 2501060122
# 功能: 电脑重启后一键启动 MySQL + 后端 + 前端
# ============================================================

Write-Host "══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  个人财务记账与分析系统 · 一键启动 (PowerShell)" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 切换到项目根目录
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# --- 第一步: 检查 MySQL 服务 ---
Write-Host "[1/5] 检查 MySQL 服务..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if ($mysqlService -and $mysqlService.Status -eq 'Running') {
    Write-Host "      MySQL80 服务已在运行 ✓" -ForegroundColor Green
} else {
    Write-Host "      MySQL80 服务未运行,正在启动..." -ForegroundColor Yellow
    try {
        Start-Service -Name "MySQL80" -ErrorAction Stop
        Write-Host "      MySQL80 服务启动成功 ✓" -ForegroundColor Green
    } catch {
        Write-Host "      ✗ MySQL80 启动失败! 请检查 MySQL 安装和服务名" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
}

# --- 第二步: 初始化数据库 ---
Write-Host "[2/5] 检查数据库 finance_db..." -ForegroundColor Yellow
$dbCheck = & mysql -uroot -proot -e "USE finance_db;" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "      数据库不存在,正在初始化..." -ForegroundColor Yellow
    & mysql -uroot -proot -e "source system/sql/01-init.sql" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "      数据库初始化成功 ✓ (7表+种子数据)" -ForegroundColor Green
    } else {
        Write-Host "      ✗ 数据库初始化失败!" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
} else {
    Write-Host "      数据库 finance_db 已存在 ✓" -ForegroundColor Green
}

# --- 第三步: 启动后端 ---
Write-Host "[3/5] 启动后端 (SpringBoot · 端口 8080)..." -ForegroundColor Yellow
Set-Location "$projectRoot\system\backend"
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d `"$projectRoot\system\backend`" && mvn spring-boot:run" -WorkingDirectory "$projectRoot\system\backend"
Set-Location $projectRoot
Write-Host "      后端启动命令已发出 — 等待约 15 秒..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# --- 第四步: 启动前端 ---
Write-Host "[4/5] 启动前端 (Vue 3 · 端口 5173)..." -ForegroundColor Yellow
Set-Location "$projectRoot\system\frontend"
if (-not (Test-Path "node_modules")) {
    Write-Host "      首次运行,正在安装依赖..." -ForegroundColor Yellow
    & pnpm install
}
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d `"$projectRoot\system\frontend`" && pnpm dev" -WorkingDirectory "$projectRoot\system\frontend"
Set-Location $projectRoot
Write-Host "      前端启动命令已发出 — 等待约 10 秒..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# --- 第五步: 打开浏览器 ---
Write-Host "[5/5] 打开浏览器..." -ForegroundColor Yellow
Start-Process "http://localhost:5173"

Write-Host ""
Write-Host "══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  系统已启动!" -ForegroundColor Green
Write-Host "  前端地址: http://localhost:5173"
Write-Host "  后端地址: http://localhost:8080/api/health"
Write-Host "  测试账号: zhangsan / 123456"
Write-Host "  管理账号: admin / 123456"
Write-Host "══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "关闭系统: 直接关闭后端和前端命令行窗口即可"
Read-Host "按回车退出"