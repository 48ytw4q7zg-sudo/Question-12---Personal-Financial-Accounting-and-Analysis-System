---
description: "Q-CRΩ∞Ω — 一键全流程: 健康→修复→审查→提交→复测 无缝循环 (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 全自动工程闭环

> **一个命令完成全部工作。** 无需选择模式、无需手动分步。

你是全自动工程调度器。每次被调用时，按以下固定流程执行，不停机直到项目收敛或无待修复问题。

元数据（所有 commit 强制嵌入）：Author: qxw / Author-ID: 2501060122

---

## 固定执行流程

```
/Q-CR 被调用
    │
    ▼
┌─────────────────────────────────────────────┐
│ STEP 1  五维健康观测                         │
│  编译 · 测试 · API · 数据库 · Git            │
│  输出健康报告表格                             │
└─────────────────────────────────────────────┘
    │
    ├── 🟢 全部健康 + 无待修复项 → 输出 "项目已收敛" → 停机
    │
    └── 🟡/🔴 有问题
            │
            ▼
┌─────────────────────────────────────────────┐
│ STEP 2  自动修复                             │
│  按严重度排序(高→中→低)，每次只修1个问题       │
│  修复后立即 mvn compile + 相关测试            │
│  编译/测试失败 → 回滚 → 标记 BLOCKED → 跳过   │
└─────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────┐
│ STEP 3  审查                                 │
│  后端改动 → 调用 code-reviewer-be             │
│  前端改动 → 调用 code-reviewer-fe             │
│  审查报告写入 docs/对话记录/                   │
└─────────────────────────────────────────────┘
            │
            ├── 审查通过(无高/中issue) → STEP 4
            │
            └── 审查不通过 → STEP 2(最多3轮)
                    │
                    └── 3轮仍不通过 → BLOCKED → 升级报告 → STEP 4
            │
            ▼
┌─────────────────────────────────────────────┐
│ STEP 4  规范提交                             │
│  自动生成含 Author/Author-ID 的 commit        │
│  git add + commit                            │
└─────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────┐
│ STEP 5  收敛检测                             │
│  连续3轮无新issue → 输出收敛证明 → 停机       │
│  否则 → 回到 STEP 1 继续                      │
└─────────────────────────────────────────────┘
```

---

## STEP 1: 五维健康观测

同时执行以下 5 项检查，生成健康报告：

### 1.1 编译健康
```bash
cd system/backend && mvn clean compile
cd system/frontend && pnpm build
```

### 1.2 测试健康
```bash
cd system/backend && mvn test
```
提取：Tests run / Failures / Errors / Skipped

### 1.3 API 健康（若后端未运行则先启动）
```bash
curl -s http://localhost:8080/api/health
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```
用返回的 token 调用 `/api/account`、`/api/statistics/monthly?year=2026&month=5`

### 1.4 数据库健康
```bash
mysql -u root -proot -e "SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db';"
```

### 1.5 Git 健康
```bash
git log --oneline -5
git status --short
```
检查未提交文件数、当前分支、与 remote 的差异。

### 输出格式

```
╔══════════════════════════════════════════════════════╗
║         Q-CRΩ∞Ω 项目健康报告  YYYY-MM-DD             ║
╠══════════════════════════════════════════════════════╣
║ 编译   │ backend: PASS/FAIL  │ frontend: PASS/FAIL   ║
║ 测试   │ N run / X fail / Y error / Z skip           ║
║ API    │ /health=XXX  /login=XXX  /account=XXX       ║
║ 数据库 │ finance_db: N 表                             ║
║ Git    │ N commits  │ 未提交: M 文件                  ║
╠══════════════════════════════════════════════════════╣
║ 综合   │ 🟢 HEALTHY / 🟡 WARNING / 🔴 CRITICAL       ║
╚══════════════════════════════════════════════════════╝
```

### 待修复清单

从以下来源汇总待修复项：
1. 编译/测试失败的具体错误
2. 未提交的代码改动（git status）
3. 代码中的 `R-XX-issue` 未标记"已修复"
4. API 返回非 200 的端点
5. 数据库表缺失或数据异常
6. 上次审查遗留的 issue

无待修复项 + 综合 🟢 → 跳至 STEP 5 收敛停机。

---

## STEP 2: 自动修复

按严重度处理待修复清单，每轮只修 1 个：

| 严重度 | 类型 | 例子 |
|---|---|---|
| 🔴 高 | 编译失败、测试失败、API 5xx、SQL 注入 | 优先修复 |
| 🟡 中 | 未提交代码、R-XX issue 未闭环、N+1 | 次优先 |
| 🟢 低 | 命名风格、注释格式、代码风格 | 最后 |

**修复约束**：
- 修改前先读文件，理解上下文
- 修改后立即运行编译 + 相关测试
- 编译/测试失败 → 回滚改动 → 标记 BLOCKED → 跳过
- 同一文件累计修改 > 5 次 → 冻结 → 人工介入

---

## STEP 3: 审查

修复完成后立即审查：

- 修改了 `.java` 文件 → 调用 `code-reviewer-be <模块名>` 审查
- 修改了 `.vue` / `.js` 文件 → 调用 `code-reviewer-fe <页面名或模块名>` 审查
- 审查报告写入 `system/docs/对话记录/`
- 审查发现的 issue 自动标注 `// R-XX-issue-N` 注释

**判断**：
- 无高/中严重度 issue → 审查通过 → 进入 STEP 4
- 有高/中 issue → 回到 STEP 2 修复 → 再审查
- 同一 issue 在 3 轮内反复出现 → BLOCKED → 输出升级报告到 `docs/对话记录/Q-CR-escalation-<日期>.md` → 跳过该问题进入 STEP 4

---

## STEP 4: 规范提交

自动生成并执行 commit，格式固定：

```
<type>(<scope>): <subject>

Author: qxw
Author-ID: 2501060122

Validation:
- compile: <PASS>
- tests: <N/0/0>
- review: <R-XX passed>
- api: <health status>
- db: <table count> tables

Changes:
- <文件路径>: <改动说明>
```

### type/scope 自动推断

| 改动路径 | type | scope |
|---|---|---|
| 新增功能代码 | `feat` | `p4-<模块>` 或 `p5-<页面>` |
| 修复 bug | `fix` | 同上 |
| 重构 | `refactor` | 同上 |
| 测试文件 | `test` | `p6` |
| docs/ | `docs` | `docs` |
| sql/ | `chore` | `db` |
| .claude/ | `chore` | `rules` |

### 校验规则

- ❌ 禁止: `temp`, `wip`, `fix again`, `try fix`, `final`, `test`, `update code`
- ❌ subject 超过 50 字符 → 自动截断
- ✅ 必须含 Author + Author-ID + Validation + Changes 四段

执行 `git add <改动的文件> && git commit -m "<完整message>"`。输出 commit SHA。

---

## STEP 5: 收敛检测

满足以下全部条件 → 停机：

- [ ] 综合健康 🟢
- [ ] 待修复清单为空
- [ ] 连续 3 轮无新增 issue
- [ ] 无未提交改动
- [ ] 编译 + 测试全绿

**停机输出**：
```
╔══════════════════════════════════════════════╗
║  Q-CRΩ∞Ω 收敛 — 项目已达稳态                  ║
║  健康: 🟢  |  测试: 37/0  |  审查: 全通过      ║
║  Commits: N  |  Author: qxw/2501060122       ║
╚══════════════════════════════════════════════╝
```

不满足 → 回到 STEP 1，继续下一轮。

---

## 熔断规则

| 场景 | 动作 |
|---|---|
| 同一文件修改 > 5 次 | 冻结该文件，输出 BLOCKED |
| 同一 issue 3 轮未通过 | 输出升级报告，跳过 |
| 编译/测试失败 | 立即回滚，不计入重试 |
| 连续 10 轮无进展 | 强制停机，输出最终报告 |

---

## 项目配置

| 项 | 值 |
|---|---|
| 项目路径 | `system/` |
| 后端 | `system/backend/` (SpringBoot 3.5.14 + Maven) |
| 前端 | `system/frontend/` (Vue 3.5 + pnpm) |
| 数据库 | finance_db (MySQL root/root@localhost:3306) |
| API | `http://localhost:8080/api` |
| 测试账号 | zhangsan / 123456 |
| 审查报告 | `system/docs/对话记录/` |
| 状态文件 | `system/.claude/project-status.md` |
| Commit 规范 | 详见 CLAUDE.md §四 |

---

## 调用

```
/Q-CR     # 一键启动全流程，无需任何参数
```

每次调用自动跑完：健康观测 → 修复 → 审查 → 再审查直到通过 → 提交 → 复测 → 收敛停机。
