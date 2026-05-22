# ============================================================
# 个人财务记账与分析系统 · PowerShell 一键启动脚本
# 电脑重启后运行此文件即可启动完整系统
# 依赖: MySQL 8.4 + JDK 21+ + Maven 3.9+ + Node.js 22+ + pnpm 10+
# ============================================================

$Host.UI.RawUI.WindowTitle = "个人财务记账与分析系统 - 一键启动"

# 项目根目录(自动检测)
$ProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

Write-Host ""
Write-Host "  ========================================" -ForegroundColor Cyan
Write-Host "  个人财务记账与分析系统 - 一键启动" -ForegroundColor Cyan
Write-Host "  ========================================" -ForegroundColor Cyan
Write-Host ""

# ---- [1/5] 检查 MySQL 服务 ----
Write-Host "  [1/5] 检查 MySQL 服务..." -ForegroundColor Yellow

$mysqlService = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if ($null -eq $mysqlService) {
    Write-Host "      [错误] 未找到 MySQL80 服务!" -ForegroundColor Red
    Write-Host "      请确认 MySQL 8.4 已安装" -ForegroundColor Red
    Read-Host "按回车退出"
    exit 1
}

if ($mysqlService.Status -ne 'Running') {
    Write-Host "      MySQL 未运行,正在启动..." -ForegroundColor Yellow
    try {
        Start-Service -Name "MySQL80" -ErrorAction Stop
        Write-Host "      MySQL 服务启动成功" -ForegroundColor Green
    } catch {
        Write-Host "      [错误] 无法启动 MySQL: $_" -ForegroundColor Red
        Read-Host "按回车退出"
        exit 1
    }
} else {
    Write-Host "      MySQL 服务已在运行" -ForegroundColor Green
}

# 等待 MySQL 就绪
Write-Host "      等待 MySQL 就绪(5秒)..." -ForegroundColor Gray
Start-Sleep -Seconds 5

# ---- [2/5] 初始化数据库 ----
Write-Host "  [2/5] 初始化数据库..." -ForegroundColor Yellow

$checkDb = & mysql -uroot -proot -e "USE finance_db;" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "      数据库不存在,执行初始化脚本..." -ForegroundColor Yellow
    $initResult = & mysql -uroot -proot -e "source $ProjectRoot\system\sql\01-init.sql" 2>&1
    if ($LASTEXITCODE -ne 0) {
        # 用 -e source 不行时用管道方式
        Get-Content "$ProjectRoot\system\sql\01-init.sql" | & mysql -uroot -proot 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "      [错误] 数据库初始化失败! 请检查 MySQL root 密码" -ForegroundColor Red
            Read-Host "按回车退出"
            exit 1
        }
    }
    Write-Host "      数据库初始化完成(7表+种子数据)" -ForegroundColor Green
} else {
    Write-Host "      数据库已存在,跳过初始化" -ForegroundColor Green
}

# ---- [3/5] 启动后端 ----
Write-Host "  [3/5] 启动后端服务(SpringBoot:8080)..." -ForegroundColor Yellow

$backendDir = "$ProjectRoot\system\backend"
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d `"$backendDir`" && mvn spring-boot:run" -WorkingDirectory $backendDir
Write-Host "      后端启动中,等待就绪(约15-20秒)..." -ForegroundColor Gray

# 等待后端 /api/health 可达
$ready = $false
for ($i = 1; $i -le 40; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $ready = $true
            Write-Host "      后端已就绪! ($i秒)" -ForegroundColor Green
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}
if (-not $ready) {
    Write-Host "      [提示] 后端可能仍在启动中,请查看后端窗口日志" -ForegroundColor Yellow
}

# ---- [4/5] 启动前端 ----
Write-Host "  [4/5] 启动前端服务(Vite:5173)..." -ForegroundColor Yellow

$frontendDir = "$ProjectRoot\system\frontend"
if (-not (Test-Path "$frontendDir\node_modules")) {
    Write-Host "      首次运行,安装前端依赖..." -ForegroundColor Yellow
    Push-Location $frontendDir
    & pnpm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "      [错误] 前端依赖安装失败!" -ForegroundColor Red
        Pop-Location
        Read-Host "按回车退出"
        exit 1
    }
    Pop-Location
}

Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "cd /d `"$frontendDir`" && pnpm dev" -WorkingDirectory $frontendDir
Write-Host "      前端启动中(约3秒)..." -ForegroundColor Gray

Start-Sleep -Seconds 3

# ---- [5/5] 完成 ----
Write-Host "  [5/5] 启动完成!" -ForegroundColor Green
Write-Host ""
Write-Host "  ========================================" -ForegroundColor Cyan
Write-Host "  系统已启动!" -ForegroundColor Green
Write-Host ""
Write-Host "  前端地址: http://localhost:5173" -ForegroundColor White
Write-Host "  后端地址: http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "  测试账号:" -ForegroundColor White
Write-Host "    普通用户: zhangsan / 123456" -ForegroundColor White
Write-Host "    管理员:   admin    / 123456" -ForegroundColor White
Write-Host "    普通用户: lisi     / 123456" -ForegroundColor White
Write-Host ""
Write-Host "  关闭系统: 关闭后端和前端的命令窗口即可" -ForegroundColor Gray
Write-Host "  ========================================" -ForegroundColor Cyan
Write-Host ""

# 自动打开浏览器
Start-Process "http://localhost:5173"

Read-Host "按回车退出此窗口(后端/前端窗口保持运行)"