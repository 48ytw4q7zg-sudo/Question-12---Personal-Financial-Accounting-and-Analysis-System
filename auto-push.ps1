# ============================================
# 自动提交并推送到 GitHub
# 每 5 小时由 Windows 任务计划程序触发
# ============================================

$projectDir = "C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main"
$logFile = Join-Path $projectDir "auto-push.log"

Set-Location $projectDir

function Write-Log($msg) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp | $msg" | Out-File -Append -FilePath $logFile -Encoding utf8
}

# 检查是否有变更
$status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Log "[跳过] 无文件变更"
    exit 0
}

# 自动判断 commit 类型
$changedFiles = git diff --name-only HEAD
$diffStat = git diff --stat HEAD
$message = ""

if ($changedFiles -match "docs/|\.md$") {
    $message = "docs: 自动同步文档变更"
} elseif ($changedFiles -match "src/main/java/.*controller/|src/main/java/.*service/") {
    $message = "feat: 自动同步后端业务代码"
} elseif ($changedFiles -match "src/main/java/.*entity/|src/main/java/.*mapper/") {
    $message = "feat: 自动同步数据层代码"
} elseif ($changedFiles -match "frontend/src/views/|frontend/src/components/") {
    $message = "feat: 自动同步前端页面代码"
} elseif ($changedFiles -match "frontend/src/api/|frontend/src/stores/") {
    $message = "feat: 自动同步前端 API/Store"
} elseif ($changedFiles -match "\.sql$") {
    $message = "feat: 自动同步数据库脚本"
} elseif ($changedFiles -match "\.yml$|\.json$|\.js$|config/") {
    $message = "chore: 自动同步配置变更"
} elseif ($changedFiles -match "\.css$|\.scss$|\.vue$") {
    $message = "style: 自动同步样式变更"
} else {
    $message = "chore: 自动同步项目变更"
}

# 执行 git 操作
try {
    git add . 2>&1 | Out-Null

    $commitResult = git commit -m $message 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Log "[跳过] commit 无变更: $commitResult"
        exit 0
    }

    $hash = git log -1 --format="%h"
    Write-Log "[提交] $hash $message"

    $pushResult = git push origin master 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Log "[推送] 成功 → origin/master"
    } else {
        Write-Log "[失败] 推送失败: $pushResult"
    }
} catch {
    Write-Log "[异常] $_"
}
