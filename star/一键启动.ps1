# ============================================================
# 个人财务记账与分析系统 - 一键启动 PowerShell 版
# Author: qxw · Author-ID: 2501060122
# 版本: v2.0 · 2026-05-23
# 功能: 电脑重启后一键启动 MySQL + 后端 + 前端 + 打开浏览器
# 用法: 右键 → 使用 PowerShell 运行, 或在 PowerShell 中执行本脚本
# ============================================================

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   个人财务记账与分析系统 · 一键启动 v2.0 (PowerShell)   ║" -ForegroundColor Cyan
Write-Host "║   SpringBoot 3.5.14 + Vue 3.5.34 + MySQL 8.4 LTS       ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 切换到项目根目录（star 的上一级）
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
Write-Host "[信息] 项目根目录: $projectRoot" -ForegroundColor Gray
Write-Host ""

# ============================================================
# 第 1 步: 检查并启动 MySQL 服务
# ============================================================
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "[1/6] 检查 MySQL 服务..." -ForegroundColor Yellow
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray

# 自动检测 MySQL 服务名（兼容 MySQL80 / MySQL84 / MySQL 等不同安装）
$mysqlService = $null
foreach ($svcName in @("MySQL84", "MySQL80", "MySQL")) {
    $svc = Get-Service -Name $svcName -ErrorAction SilentlyContinue
    if ($svc) {
        $mysqlService = $svc
        break
    }
}

if (-not $mysqlService) {
    Write-Host "[错误] 未检测到 MySQL 服务!" -ForegroundColor Red
    Write-Host "       运行 'Get-Service *mysql*' 查看实际服务名" -ForegroundColor Red
    Read-Host "按回车退出"
    exit 1
}

Write-Host "       检测到 MySQL 服务: $($mysqlService.Name)" -ForegroundColor Green

if ($mysqlService.Status -eq 'Running') {
    Write-Host "       $($mysqlService.Name) 服务已在运行 [OK]" -ForegroundColor Green
} else {
    Write-Host "       $($mysqlService.Name) 服务未运行, 正在启动..." -ForegroundColor Yellow
    try {
        Start-Service -Name $mysqlService.Name -ErrorAction Stop
        Write-Host "       $($mysqlService.Name) 服务启动成功 [OK]" -ForegroundColor Green
    } catch {
        Write-Host "[错误] $($mysqlService.Name) 启动失败!" -ForegroundColor Red
        Write-Host "       请以管理员身份运行 PowerShell 后重试" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
}
Write-Host ""

# ============================================================
# 第 2 步: 初始化数据库（首次运行时）
# ============================================================
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "[2/6] 检查数据库 finance_db..." -ForegroundColor Yellow
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray

$dbUser = "root"
$dbPass = "root"

# 尝试默认密码连接
$testResult = & mysql -u$dbUser -p$dbPass -e "SELECT 1;" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "       默认密码 root 连接失败, 尝试空密码..." -ForegroundColor Yellow
    $dbPass = ""
    $testResult = & mysql -u$dbUser -e "SELECT 1;" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 无法连接 MySQL!" -ForegroundColor Red
        Write-Host "       请修改脚本中的 dbUser / dbPass 变量" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
}

# 检查数据库是否存在
$dbCheck = & mysql -u$dbUser -p$dbPass -e "USE finance_db;" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "       数据库 finance_db 不存在, 正在初始化..." -ForegroundColor Yellow
    $sqlFile = "$projectRoot\system\sql\01-init.sql"
    if ($dbPass) {
        & mysql -u$dbUser -p$dbPass -e "source $sqlFile" 2>&1 | Out-Null
    } else {
        & mysql -u$dbUser -e "source $sqlFile" 2>&1 | Out-Null
    }
    if ($LASTEXITCODE -eq 0) {
        Write-Host "       数据库初始化成功 [OK] (7表 + 种子数据 + 测试账号)" -ForegroundColor Green
    } else {
        Write-Host "[错误] 数据库初始化失败!" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
} else {
    Write-Host "       数据库 finance_db 已存在 [OK]" -ForegroundColor Green
}
Write-Host ""

# ============================================================
# 第 3 步: 设置后端环境变量
# ============================================================
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "[3/6] 设置后端环境变量..." -ForegroundColor Yellow
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray

# application.yml 要求 DB_PASSWORD 和 JWT_SECRET 必须通过环境变量注入
$env:DB_USERNAME = $dbUser
$env:DB_PASSWORD = $dbPass
$env:JWT_SECRET = "dev-finance-jwt-secret-key-2026-qxw-2501060122-must-be-at-least-32-bytes"
$env:JWT_EXPIRE = "604800000"
$env:SERVER_PORT = "8080"
$env:MYBATIS_LOG_IMPL = "org.apache.ibatis.logging.stdout.StdOutImpl"
$env:LOG_LEVEL = "info"

Write-Host "       DB_USERNAME = $dbUser" -ForegroundColor Green
Write-Host "       DB_PASSWORD = *****(已隐藏)" -ForegroundColor Green
Write-Host "       JWT_SECRET  = dev-*****(开发环境密钥, 已隐藏)" -ForegroundColor Green
Write-Host "       SERVER_PORT = $env:SERVER_PORT" -ForegroundColor Green
Write-Host "       [OK] 环境变量已设置" -ForegroundColor Green
Write-Host ""

# ============================================================
# 第 4 步: 启动后端（新窗口）
# ============================================================
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "[4/6] 启动后端 (SpringBoot 3.5.14 · 端口 $env:SERVER_PORT)..." -ForegroundColor Yellow
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray

$backendDir = "$projectRoot\system\backend"
$backendCmd = "cd /d `"$backendDir`" && set DB_USERNAME=$env:DB_USERNAME && set DB_PASSWORD=$env:DB_PASSWORD && set JWT_SECRET=$env:JWT_SECRET && set JWT_EXPIRE=$env:JWT_EXPIRE && set SERVER_PORT=$env:SERVER_PORT && set MYBATIS_LOG_IMPL=$env:MYBATIS_LOG_IMPL && set LOG_LEVEL=$env:LOG_LEVEL && echo [后端] 正在启动 SpringBoot... 首次启动需下载依赖 && mvn spring-boot:run"

Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "title 后端-SpringBoot-8080 && $backendCmd" -WorkingDirectory $backendDir

Write-Host "       后端启动命令已发出(新窗口)" -ForegroundColor Green
Write-Host "       等待约 20 秒让 SpringBoot 完成启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

# 验证后端健康
try {
    $healthCheck = Invoke-WebRequest -Uri "http://localhost:$env:SERVER_PORT/api/v1/health" -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($healthCheck.StatusCode -eq 200) {
        Write-Host "       后端健康检查通过 [OK]" -ForegroundColor Green
    }
} catch {
    Write-Host "       [警告] 后端健康检查未通过, 可能仍在启动中" -ForegroundColor Yellow
    Write-Host "       请查看后端窗口确认日志是否正常" -ForegroundColor Yellow
}
Write-Host ""

# ============================================================
# 第 5 步: 启动前端（新窗口）
# ============================================================
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "[5/6] 启动前端 (Vue 3.5.34 · 端口 5173)..." -ForegroundColor Yellow
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray

$frontendDir = "$projectRoot\system\frontend"
Set-Location $frontendDir

if (-not (Test-Path "node_modules")) {
    Write-Host "       首次运行, 正在安装前端依赖 (pnpm install)..." -ForegroundColor Yellow
    & pnpm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] pnpm install 失败!" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
    Write-Host "       前端依赖安装完成 [OK]" -ForegroundColor Green
}

Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "title 前端-Vue3-5173 && cd /d `"$frontendDir`" && echo [前端] 正在启动 Vite 开发服务器... && pnpm dev" -WorkingDirectory $frontendDir

Set-Location $projectRoot
Write-Host "       前端启动命令已发出(新窗口)" -ForegroundColor Green
Write-Host "       等待约 8 秒让 Vite 完成启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 8
Write-Host "       [OK]" -ForegroundColor Green
Write-Host ""

# ============================================================
# 第 6 步: 打开浏览器
# ============================================================
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "[6/6] 打开浏览器..." -ForegroundColor Yellow
Write-Host "──────────────────────────────────────────────" -ForegroundColor DarkGray

Start-Process "http://localhost:5173"
Write-Host "       浏览器已打开 [OK]" -ForegroundColor Green
Write-Host ""

# ============================================================
# 启动完成 · 显示汇总信息
# ============================================================
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                 系统启动完成!                            ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║                                                          ║"
Write-Host "║   前端地址: http://localhost:5173                        ║" -ForegroundColor White
Write-Host "║   后端地址: http://localhost:8080/api/v1/health          ║" -ForegroundColor White
Write-Host "║                                                          ║"
Write-Host "║   测试账号(普通用户): zhangsan / 123456                  ║" -ForegroundColor Yellow
Write-Host "║   测试账号(管理员):   admin / 123456                     ║" -ForegroundColor Yellow
Write-Host "║                                                          ║"
Write-Host "║   如果登录失败, 请重新执行 system\sql\01-init.sql        ║" -ForegroundColor DarkGray
Write-Host "║                                                          ║"
Write-Host "╠══════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║   关闭窗口说明:                                          ║"
Write-Host "║   - 关闭'后端'窗口 = 停止后端服务                        ║"
Write-Host "║   - 关闭'前端'窗口 = 停止前端服务                        ║"
Write-Host "║   - 关闭本窗口不影响后端和前端运行                       ║"
Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Read-Host "按回车退出"
