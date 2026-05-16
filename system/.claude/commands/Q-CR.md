---
description: "Q-CRΩ∞Ω — /Q-CR 一键全流程: 健康→修复→审查→提交→收敛 (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 全自动工程闭环

> 调用即执行全流程，无参数、无模式选择。

你是全自动工程调度器。每次调用按以下 5 步顺序执行，**不能跳过任何步骤**。
元数据（所有 commit 强制嵌入）：Author: qxw / Author-ID: 2501060122

---

## ⚠️ 执行协议（硬约束）

1. **禁止只读不干** — 加载本指令后必须立即执行 STEP 1，不得只输出"已理解"
2. **禁止跳过步骤** — 必须按 STEP 1→2→3→4→5 顺序
3. **每步必须输出结果** — 编译结果、测试数、API 状态码、表数、commit SHA
4. **健康全绿 + 无待修复项 → 立即执行 STEP 5 收敛停机**
5. **有问题 → 必须修 → 必须审 → 必须提交**

---

## STEP 1: 五维健康观测

**并行执行以下全部检查**（用 PowerShell 运行后端命令，Bash 运行前端和 API 命令）：

### 1.1 编译
```powershell
cd system/backend; mvn clean compile 2>&1 | Select-String "BUILD|ERROR"
```
后端通过标准: 输出含 `BUILD SUCCESS` 且无 `ERROR`
```bash
cd system/frontend && pnpm build 2>&1 | tail -3
```
前端通过标准: 输出含 `built` 且无 `error`

### 1.2 测试
```powershell
cd system/backend; mvn test 2>&1 | Select-String "Tests run:"
```
通过标准: `Failures: 0, Errors: 0`

### 1.3 API（若后端未运行，先执行 `nohup mvn spring-boot:run &` 等待 20 秒）
```bash
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/health
```
通过标准: 返回 `200`

### 1.4 数据库
```powershell
mysql -u root -proot -e "SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db';" 2>&1
```
通过标准: 输出含 6 张表（user, account, category, transaction, budget, recurring_bill）

### 1.5 Git
```bash
git log --oneline -3 && echo "---" && git status --short
```
通过标准: 无未提交改动（`M ` 或 `?? ` 仅允许 `.claude/scheduled_tasks*` 和 `auto-push.ps1`）

### 输出健康报告

将以上结果汇总为表格，格式固定：
```
╔══════════════════════════════════════════╗
║   Q-CRΩ∞Ω 健康报告  <当前日期时间>       ║
╠══════════════════════════════════════════╣
║ 编译 │ be: PASS/FAIL │ fe: PASS/FAIL    ║
║ 测试 │ N run / X fail / Y err / Z skip  ║
║ API  │ /health=XXX                       ║
║ 数据库│ finance_db: N 表                   ║
║ Git  │ N commits │ 待提交: M 文件         ║
╠══════════════════════════════════════════╣
║ 综合 │ 🟢 HEALTHY / 🟡 WARN / 🔴 CRIT   ║
╚══════════════════════════════════════════╝
```

### 收集待修复清单

从以下 6 个来源汇总：
1. 编译/测试失败 → 提取错误信息
2. `git status --short` 中非白名单的未提交文件 → 需提交
3. `grep -r "R-0[5-8]-issue-[0-9]*:" system/backend/src system/frontend/src | grep -v "已修复"` → 未闭环 issue
4. API 返回非 200 → 需排查
5. 数据库表数 ≠ 6 → 需执行 `sql/01-init.sql`
6. 代码中 `TODO` 或 `FIXME` → 低优

**若健康 🟢 且待修复清单为空 → 跳至 STEP 5 收敛停机。**

---

## STEP 2: 自动修复

从待修复清单取**第一个最高严重度**的问题修复：

| 优先级 | 类型 | 动作 |
|---|---|---|
| 🔴 P0 | 编译失败 / 测试失败 / API 5xx | 立即修复 |
| 🟡 P1 | 未提交代码 / R-XX issue 未闭环 | 次优先 |
| 🟢 P2 | TODO / FIXME / 代码风格 | 最后 |

### 修复规则

- 修改代码前**必须先 Read 该文件**
- 修复后**立即**运行 `mvn compile` 或 `pnpm build` 验证
- 编译/测试失败 → **回滚此修改** → 标记 `BLOCKED` → 跳过此问题
- 同一文件累计修改超过 5 次 → **冻结** → 输出 `BLOCKED: <文件路径>` → 跳过
- 每轮只修 1 个问题

---

## STEP 3: 审查

修复完成后**必须调用审查 skill**（不可跳过）：

```
改动 .java 文件 → Skill "code-reviewer-be" args "<模块名>"
改动 .vue/.js 文件 → Skill "code-reviewer-fe" args "<页面名或模块名>"
```

### 判断逻辑

- 审查报告无 🔴高 / 🟡中 issue → **审查通过** → 进入 STEP 4
- 有 issue → **回到 STEP 2** 修复（最多循环 3 轮）
- 同一 issue 3 轮仍未通过 → 标记 `BLOCKED` → 输出升级报告到 `system/docs/对话记录/Q-CR-escalation-<日期>.md` → 跳过进入 STEP 4

---

## STEP 4: 规范提交

### 生成 commit message（固定 4 段格式）

```
<type>(<scope>): <中文subject>

Author: qxw
Author-ID: 2501060122

Validation:
- compile: <PASS/FAIL>
- tests: <N run / X fail>
- review: <R-XX passed / 未审查>
- api: <200/异常>
- db: <N tables>

Changes:
- <文件路径1>: <改动说明>
- <文件路径2>: <改动说明>
```

### type/scope 推断规则

| 改动路径含 | type | scope 示例 |
|---|---|---|
| `src/test/` | `test` | `p6` |
| `docs/` | `docs` | `docs` |
| `sql/` | `chore` | `db` |
| `.claude/` | `chore` | `rules` |
| `fix/repair/bug` | `fix` | `p4-<模块>` |
| 其他新增/修改 | `feat` | `p4-<模块>` 或 `p5-<页面>` |

### 校验与执行

- ❌ 禁止词: `temp`, `wip`, `fix again`, `try fix`, `final`, `test`, `update code`, `改了一些`
- ❌ 缺 Author/Author-ID/Validation/Changes 任一段 → 拒绝
- ❌ subject 超 25 中文/50 字符 → 截断

通过校验后执行：
```bash
git add <所有改动的文件> && git commit -m "<完整message>"
```
输出 `Committed: <SHA>`

---

## STEP 5: 收敛检测

满足以下**全部 5 项** → 输出收敛停机：

- [x] 综合健康 🟢
- [x] 待修复清单为空
- [x] 无未提交改动（白名单除外）
- [x] 编译 + 测试全绿
- [x] 本轮无新增 issue

**收敛输出**（之后不能再改代码）：
```
╔══════════════════════════════════════════════╗
║  Q-CRΩ∞Ω 收敛 — 项目已达稳态                  ║
║  编译: PASS │ 测试: 37/0 │ API: 200          ║
║  DB: 6表 │ Commits: N │ Author: qxw/2501060122
╚══════════════════════════════════════════════╝
```

**不满足任一项** → 回到 STEP 1（形成循环，直到收敛或 10 轮强制停机）。

---

## 熔断表

| 触发条件 | 动作 |
|---|---|
| 同文件改 > 5 次 | 冻结文件 + 输出 BLOCKED |
| 同 issue 3 轮不通过 | 输出升级报告 + 跳过 |
| 编译/测试失败 | 回滚改动（不计入重试） |
| 循环 ≥ 10 轮 | 强制停机 + 最终报告 |

---

## 项目硬编码配置（本文件为项目专属版）

- 工作目录: `system/`
- 后端: `system/backend/` (SpringBoot 3.5.14 + Maven + Java 21)
- 前端: `system/frontend/` (Vue 3.5 + pnpm + Vite 8)
- 数据库: `finance_db` @ `localhost:3306` (root/root)
- API 基址: `http://localhost:8080/api`
- 测试账号: zhangsan / 123456
- BCrypt 哈希: `$2a$10$dGB2.RDec.rSTe7KZ/EkT.Mi9pWrxkJMiKL4fJmQ8OVXuBq/KHlG6`
- 审查报告路径: `system/docs/对话记录/`
- 状态文件: `system/.claude/project-status.md`
- Commit 规范: CLAUDE.md §四
- 数据库初始化: `mysql -u root -proot < sql/01-init.sql`
