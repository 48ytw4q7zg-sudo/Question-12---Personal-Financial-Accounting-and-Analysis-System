---
description: "Q-CRΩ∞Ω Engineering Runtime — 递归审查修复 + 项目健康观测 + 规范提交(qxw/2501060122)"
argument-hint: "[auto|health|commit <message>]"
---

# /Q-CR — Q-CRΩ∞Ω Engineering Runtime

你是自治工程调度器，负责：健康观测 → 审查修复循环 → 规范提交 → 收敛停机。
元数据：Author: qxw / Author-ID: 2501060122（所有 commit 强制嵌入）。

## 模式选择

根据用户传入的第一个参数选择模式：

| 参数 | 模式 | 说明 |
|---|---|---|
| `auto` | 递归审查修复闭环 | 健康观测 → 找问题 → Worker修复 → Verifier审查 → 循环直到通过 |
| `health` | 项目健康观测 | 编译 + 测试 + API + 数据库 + Git 五维检查 |
| `commit <msg>` | 规范提交 | 自动补全 Author/Author-ID/验证状态后提交 |

默认（无参数）= `health`。

---

## 模式一：`/Q-CR auto` — 递归审查修复闭环

### 流程

```
健康观测 → 发现待修复项 → 自动修复 → code-reviewer-be/fe 审查
                                              ↓
                                         审查通过? ──→ 否 → 继续修复 → 再审查(最多3轮)
                                              ↓
                                         审查通过 → 规范提交 → 健康复测 → 输出闭环报告
```

### 执行步骤

**Step 1: 健康观测**（执行下方"模式二"全部检查项，取得待修复清单）

**Step 2: 执行修复**（按严重度排序：高 → 中 → 低）：
- 每轮只修 1 个问题
- 修复后立即运行 `mvn compile` + 相关测试
- 若编译/测试失败 → 回滚该修复 → 标记 BLOCKED → 跳过

**Step 3: 审查**（调用 reviewer skill）：
- 后端改动 → 调用 `code-reviewer-be` 审查
- 前端改动 → 调用 `code-reviewer-fe` 审查
- 审查报告写入 `docs/对话记录/`

**Step 4: 判断**：
- 审查通过（无高/中严重度 issue）→ 进入 Step 5
- 审查不通过 → 回到 Step 2 修复新 issue → 最多重复 3 轮
- 同一问题 3 轮未通过 → 标记 BLOCKED → 输出升级报告 → 跳到下一个问题

**Step 5: 规范提交**（执行下方"模式三"）

**Step 6: 收敛检测**：
- 连续 3 轮无新增 issue → 输出 "Q-CR: 项目已收敛" → 停机
- 否则回到 Step 1

### 熔断规则
- 同一文件修改超过 5 次 → 冻结该文件，输出 `BLOCKED: <文件名>`
- 同一问题 3 轮未通过 → 输出升级报告 `docs/对话记录/Q-CR-escalation-<日期>.md`
- 编译/测试失败 → 立即回滚，不进入审查

---

## 模式二：`/Q-CR health` — 项目健康观测

执行以下 5 维检查，输出健康报告表格：

### 1. 编译健康
```bash
cd system/backend && mvn clean compile -q
cd system/frontend && pnpm build
```

### 2. 测试健康
```bash
cd system/backend && mvn test
```
统计通过率：Tests run / Failures / Errors / Skipped

### 3. API 健康
```bash
# 需后端正在运行
curl -s http://localhost:8080/api/health
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
curl -s -H "Authorization: Bearer <token>" http://localhost:8080/api/account
```

### 4. 数据库健康
```bash
mysql -u root -proot -e "SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db';"
```

### 5. Git 健康
```bash
git log --oneline -5
git status --short
```

### 输出格式

```
╔══════════════════════════════════════════════╗
║     Q-CRΩ∞Ω 项目健康报告  2026-XX-XX        ║
╠══════════════════════════════════════════════╣
║ 编译   │ backend: PASS │ frontend: PASS     ║
║ 测试   │ 37/0/0 PASS                        ║
║ API    │ /health=200 /login=200 /account=200║
║ 数据库 │ 6表 完整                            ║
║ Git    │ XX commits │ 未提交: N 文件         ║
╠══════════════════════════════════════════════╣
║ 综合   │ 🟢 HEALTHY / 🟡 WARNING / 🔴 CRIT  ║
╚══════════════════════════════════════════════╝
```

---

## 模式三：`/Q-CR commit <message>` — 规范提交

### 执行

1. 分析 `<message>` 或自动从 staged diff 推断 `type(scope): subject`
2. 从 `git diff --staged` 提取验证状态
3. 组装完整 commit body：

```
<type>(<scope>): <subject>

Author: qxw
Author-ID: 2501060122

Validation:
- compile: <PASS/FAIL>
- unit-tests: <N passed / M failed>
- review: <R-XX passed / pending>

Changes:
- <文件1>: <改动说明>
- <文件2>: <改动说明>
```

4. 校验规则：
   - ❌ 禁止: `temp`, `wip`, `fix again`, `try fix`, `final`, `test commit`
   - ❌ 主体超 50 字符 → 要求缩短
   - ❌ 缺 Author/Author-ID → 拒绝提交
5. 执行 `git commit -m "完整message"`
6. 输出 commit SHA

### scope 自动推断

| 改动路径 | scope |
|---|---|
| `backend/.../controller/` 或 `service/` | `p4-<模块>` |
| `frontend/src/views/` | `p5-<PageName>` |
| `frontend/src/api/` | `axios` |
| `frontend/src/components/` | `p5-components` |
| `docs/` | `docs` |
| `sql/` | `db` |
| `system/.claude/` | `rules` |

---

## 项目特定配置

- **项目路径**: `system/`
- **后端入口**: `system/backend/`
- **前端入口**: `system/frontend/`
- **测试账号**: zhangsan / 123456
- **数据库**: finance_db (MySQL root/root)
- **API 前缀**: `/api`
- **审查报告目录**: `system/docs/对话记录/`
- **状态文件**: `system/.claude/project-status.md`
- **CLAUDE.md**: 项目根目录规范文件
- **Commit 规范**: 详见 CLAUDE.md §四

## 熔断配置

| 参数 | 值 |
|---|---|
| 最大审查轮数 | 3 |
| 同文件最大修改次数 | 5 |
| 同问题最大重试 | 3 |
| 收敛连续无 issue 轮数 | 3 |

## 使用示例

```
/Q-CR health                          # 查看项目健康
/Q-CR auto                            # 启动自动修复循环
/Q-CR commit "fix(p4): 修复JWT过期"   # 规范提交
/Q-CR                                 # 默认=health
```
