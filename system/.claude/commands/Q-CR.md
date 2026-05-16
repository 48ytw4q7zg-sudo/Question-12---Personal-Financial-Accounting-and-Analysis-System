---
description: "Q-CRΩ∞Ω v7 — 强制审查闭环·全局测试·全局审查·∞级收敛·严于全部参考文件 (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 企业答辩级自治闭环 v7

> **一个命令。∞级收敛。每次修改必审查直到通过。最终全局测试+全局审查。** 策略全内嵌。

你是 Q-CRΩ∞Ω 答辩级调度器。元数据（所有 commit 强制嵌入）：Author: qxw / Author-ID: 2501060122。

---

## ⚠️ 铁律（不可违）

| # | 铁律 |
|:---:|---|
| 1 | **≥5 轮** · 全绿也不停 · ×1.2 收紧 |
| 2 | **每轮 ≥1 改动** · 无 P0/P1 → 自选 P2/P3 |
| 3 | **每次改动 → 立即审查 → 不通过 → 修复 → 再审查** · 直到零高/中 issue（最多 3 轮修复-审查） |
| 4 | **每轮 ≥6 Skill 真实调用** · `Skill` 工具强制 |
| 5 | **最终轮必须执行全局测试 + 全局审查** · 全部通过才可收敛 |
| 6 | 收敛 = ≥5轮 + 连续3轮零新增 + 全局测试PASS + 全局审查PASS + 评分≥95 |
| 7 | **标准高于全部参考文件** · 本文件 > 评分细节.doc > 08b > CLAUDE.md template > 项目 docs |

---

## 循环计数器

```
╔══════════════════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω v7  第 <N> 轮 · L<X> · 评分 <S>/100 · 收紧×1.2    ║
║  最低 5 轮 · 当前 <N>/5 · 零新增 <C>/3 · 全局测试 <待/通过>  ║
╚══════════════════════════════════════════════════════════════╝
```

**∞级递进**：

| 轮 | 等级 | 额外约束 | 必调 Skill |
|:---:|:---:|---|:---:|
| 1 | L1·基线 | 五维全绿 · 零 ERROR | ≥6 |
| 2 | L2·强化 | +R-XX 全闭环 · 零 TODO · 精确版本 | ≥8 |
| 3 | L3·深度 | +N+1 归零 · 代码简化 · UI 审计 | ≥10 |
| 4 | L4·安全 | +OWASP 全项 · 依赖 CVE | ≥12 |
| 5 | L5·交付 | +139 打分 · 全局测试 · 全局审查 · ≥95 | ≥14 |
| 6+ | L6·∞ | 每轮必发现 ≥1 微优化 · 自演进 | ≥16 |

**收紧系数**：上轮零新增 → `下轮阈值 = min(100, 上轮×1.2)`。L5=95 → L6=100。

---

## 主执行流

```
/Q-CR 调用
    ↓
┌─────────────────────────────────────────────┐
│ PHASE A · 调度器: 8 维健康观测 [并行]         │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│ PHASE B · Worker: 嵌套 Skill 矩阵 [≥6次调用]  │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│ PHASE C · 修复器: [P0→P3 排序 · ≥1 改动]     │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│ PHASE D · Verifier: 改后必审 [循环至通过]     │
│  .java→code-reviewer-be  .vue/.js→reviewer-fe│
│  通过? → PHASE E   不通过? → PHASE C(≤3轮)   │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│ PHASE E · 提交器: 规范 commit + 收敛检测      │
└─────────────────────────────────────────────┘
    ↓
  轮<5 → 收紧 ×1.2 → 下一轮
  ≥5 + 收敛条件全满足 → 全局测试 → 全局审查 → 停机
  ≥15 → 强制停机
```

---

## PHASE A: 调度器 — 8 维健康观测

全部并行执行。每项输出 PASS/FAIL + 具体数值。

**A1 编译**（L3+ 要求零 WARNING）：
```powershell
cd system/backend; mvn clean compile 2>&1 | Select-String "BUILD|ERROR|WARNING"
```
```bash
cd system/frontend && pnpm build 2>&1 | tail -3
```

**A2 测试**（全绿 + L5+ 覆盖率）：
```powershell
cd system/backend; mvn test 2>&1 | Select-String "Tests run:|BUILD"
```
通过: `Failures: 0, Errors: 0, Skipped: 0` · ≥37 用例。

**A3 API**（28 端点全量 · L4+ p95）：后端未运行则 `nohup mvn spring-boot:run &` 等 25s。逐模块 curl 28 端点，记录响应码。

**A4 数据库**（结构+数据+类型）：
```powershell
mysql -u root -proot -e "SELECT TABLE_NAME,TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db'; SELECT COLUMN_NAME,DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='finance_db' AND COLUMN_NAME IN('amount','initial_balance') AND DATA_TYPE!='decimal';" 2>&1
```
通过: 6 表 · 金额全部 decimal(12,2) · 零 float/double。

**A5 Git**（洁净度+milestone）：
```bash
git log --oneline -10 && git status --short
```
通过: 仅白名单可未提交。≥7 类 milestone。

**A6 依赖精确**（L2+）：检查 pom.xml/package.json 零 `^~LATEST*SNAPSHOT`。

**A7 文件完整**（L2+）：backend 10 目录 + frontend 6 目录 + docs 5 文档 + sql 1 脚本。

**A8 R-XX 审计**（L2+）：
```bash
grep -rn "R-0[2-8]-issue" system/backend/src system/frontend/src | grep -v "已修复"
```
通过: 零输出。

---

## PHASE B: Worker — Skill 强制调用矩阵

**铁律: 每个 skill 必须通过 `Skill` 工具真实调用。调用后输出摘要。发现 issue → 追加待修复清单。**

### L1（≥6）
| # | Skill | 调用 |
|:---:|---|---|
| 1 | `code-reviewer-be` | `Skill "code-reviewer-be" args "TransactionServiceImpl"` |
| 2 | `code-reviewer-fe` | `Skill "code-reviewer-fe" args "DashboardPage"` |
| 3 | `simplify` | `Skill "simplify"` |
| 4 | `git-commit` | `Skill "git-commit"` |
| 5 | `conventional-commit` | `Skill "conventional-commit"` |
| 6 | `using-skills` | `Skill "using-skills"` |

### L2（+2=≥8）
7. `karpathy-guidelines` · 8. `systematic-debugging`

### L3（+4=≥10）
9. `code-simplifier` · 10. `frontend-design` · 11. `element-plus-vue3` · 12. `vue-testing-best-practices`

### L4（+4=≥12）
13. `security-reviewer` · 14. `find-skills` · 15. `mysql` · 16. `mysql-best-practices`

### L5（+4=≥14）
17. `requesting-code-review` · 18. `brainstorming` · 19. `rest-api-design` · 20. `springboot-patterns`

### L6+（+6=≥20）
21. `unittest-coder` · 22. `perf-optimizer` · 23. `refactor-helper` · 24. `test-driven-development` · 25. `spring-boot-testing` · 26. `java-springboot`

---

## PHASE C: 修复器

| 优先级 | 类型 | 例子 |
|:---:|---|---|
| 🔴 P0 | 编译/测试失败、API 5xx、安全漏洞 | 立即 |
| 🟡 P1 | R-XX 未闭环、N+1、事务缺失 | 本轮 |
| 🟢 P2 | 代码风格、命名、注释、格式 | 本轮 |
| 🔵 P3 | 微优化、依赖升级、文档 | L3+ |

**约束**: 改前 Read → 改后编译+测试 → 失败回滚 → 同文件 >5 次冻结。L5+ 零 P0/P1 → 必须选 ≥1 项 P2/P3。

---

## PHASE D: Verifier — 强制审查闭环

> ⚠️ **这是 v7 最关键的增强：每次代码修改后必须通过审查，不通过就循环修复直到通过。**

### 审查流程

```
PHASE C 产生改动
    ↓
判断改动类型:
  .java → Skill "code-reviewer-be" args "<模块名>"
  .vue/.js → Skill "code-reviewer-fe" args "<页面或模块名>"
    ↓
审查报告:
  零高/中 issue → ✅ 审查通过 → 进入 PHASE E
  有高/中 issue → ❌ 不通过 → 回到 PHASE C 修复 → 再次审查
    ↓
修复-审查循环:
  第 1 次不通过 → 修复 → 第 2 次审查
  第 2 次不通过 → 修复 → 第 3 次审查
  第 3 次仍不通过 → 🚫 BLOCKED → 升级报告 → 跳过此问题
```

**审查报告写入**: `system/docs/对话记录/Q-CR-v7-loop<N>-review-<日期>.md`

**BLOCKED 升级报告**: `system/docs/对话记录/Q-CR-escalation-<日期>.md`

**关键**: 同一 issue 3 轮审查未通过 → 禁止继续修 → 标记 BLOCKED → 生成升级报告 → 本轮跳过。不得无限循环修复同一问题。

---

## PHASE E: 提交器 + 收敛检测

### E1: 规范 commit（5 段固定格式）

```
<type>(<scope>): <中文subject>

Author: qxw
Author-ID: 2501060122
Q-CR-v7 Loop: <N>/5+  Level: L<X>  Score: <S>/100  139check: <P>/139
Skills: <list> (<K>/<required>)

Validation:
- compile: <PASS/FAIL> (warnings:N)
- tests: <N/0/0>
- api: <X>/28 endpoints
- db: <N> tables · 0 float/double
- review: <R-XX status> · <N> iterations to pass

Changes:
- <文件>: <说明>
```

**校验**: 禁止 `temp/wip/fix again/try fix/final/update code`。缺任一段 → 拒绝。

### E2: 全局测试（L5 最终轮强制执行）

> ⚠️ **v7 新增：收敛前必须执行全局测试。**

```
1. mvn clean compile → BUILD SUCCESS (零 ERROR · L3+ 零 WARN)
2. mvn test → 全部 PASS (≥37 用例 · 0 fail)
3. pnpm build → built (零 error)
4. 后端启动 + 28 端点全量 curl 验证 (全部 200/400)
5. mysql 6 表 + 数据完整性
6. git status 洁净
```
全部 6 项 PASS → 全局测试通过 ✅。

### E3: 全局审查（L5 最终轮强制执行）

> ⚠️ **v7 新增：收敛前必须执行全局审查。**

对**全部后端模块**和**全部前端页面**执行最终审查：

```
后端 7 模块逐一审查:
  Skill "code-reviewer-be" args "UserServiceImpl"
  Skill "code-reviewer-be" args "AccountServiceImpl"
  Skill "code-reviewer-be" args "TransactionServiceImpl"
  Skill "code-reviewer-be" args "BudgetServiceImpl"
  Skill "code-reviewer-be" args "RecurringBillServiceImpl"
  Skill "code-reviewer-be" args "StatisticsServiceImpl"
  Skill "code-reviewer-be" args "CategoryServiceImpl"

前端 4 核心页面审查:
  Skill "code-reviewer-fe" args "DashboardPage"
  Skill "code-reviewer-fe" args "TransactionListPage"
  Skill "code-reviewer-fe" args "AccountPage"
  Skill "code-reviewer-fe" args "LoginPage"
```

全部模块/页面零高/中 issue → 全局审查通过 ✅。

### E4: 收敛检测（10 项全满足 → 停机）

| # | 条件 | 状态 |
|:---:|---|---|
| 1 | 轮次 ≥ 5 | `<N>/5` |
| 2 | 连续 ≥ 3 轮零新增 issue | `<C>/3` |
| 3 | 139 项全部 ≥ 3 分 | `<P>/139` |
| 4 | 总评分 ≥ 95/100 | `<S>/100` |
| 5 | 编译零 ERROR + 测试全绿 | — |
| 6 | Git 洁净（仅白名单） | — |
| 7 | 本轮全部 Skill 通过 | — |
| 8 | 近 5 轮每轮 ≥1 commit | — |
| 9 | **全局测试 6/6 通过** ⚠️ v7 新增 | — |
| 10 | **全局审查零高/中 issue** ⚠️ v7 新增 | — |

**全部 10 项满足 → 收敛停机证明**：
```
╔══════════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω v7 终极收敛 · 答辩就绪                       ║
║  轮次: N · 评分: S/100 · 139项: 全≥3                 ║
║  编译: PASS(零ERR) · 测试: 37/0 · API: 28/28        ║
║  全局测试: 6/6 PASS · 全局审查: 零高/中 issue         ║
║  DB: 6表·0float · Commits: N · Skills: K次           ║
║  Author: qxw · ID: 2501060122                        ║
║  结论: ✅ 可演示 ✅ 可答辩 ✅ 可交付                    ║
╚══════════════════════════════════════════════════════╝
```

**任一项不满足 → 收紧 ×1.2 → 下一轮。≥15 轮 → 强制停机。**

---

## 内嵌策略

**熔断**: 同 issue 3 轮 → BLOCKED · 同文件 >5 次 → 冻结 · 编译/测试失败 → 回滚

**收敛**: min_loops=5 · consecutive_clean=3 · min_score=95 · global_test=6/6 · global_review=0高/中 · max_loops=15

**失败分类**: compile×3 → unit_test×3 → integration×2 escalate → semantic×2 rollback → architecture BLOCKED

---

## 139 项验收清单（L5 逐条 1-4 分打分 · /556 折百）

一、Phase流程(1-5)[20] · 二、PRD功能(6-10)[20] · 三、TECH架构(11-20)[40] · 四、DB(21-35)[60] · 五、API(36-48)[52] · 六、JWT(49-60)[48] · 七、账户(61-68)[32] · 八、分类(69-73)[20] · 九、流水(74-85)[48] · 十、Dashboard(86-95)[40] · 十一、前端(96-105)[40] · 十二、后端(106-114)[36] · 十三、安全(115-120)[24] · 十四、构建(121-130)[40] · 十五、CC流程(131-139)[36]

---

## 参考权威源

本文件 > 评分细节.doc > 08b > CLAUDE.md template > 项目 CLAUDE.md > 项目 docs/
题12: 单一用户角色 · 6表 · 28接口 · 11页面 · P2满分

## 项目硬配置

后端 `system/backend/` (SB 3.5.14 · Maven · Java21) · 前端 `system/frontend/` (Vue 3.5.34 · pnpm · Vite8) · DB `finance_db`@`localhost:3306` (root/root · MySQL8.4) · API `http://localhost:8080/api` · 测试账号 zhangsan/123456 · BCrypt `$2a$10$dGB2.RDec.rSTe7KZ/EkT.Mi9pWrxkJMiKL4fJmQ8OVXuBq/KHlG6`

## 调用

```
/Q-CR
```

无参数。自动 ≥5 轮。每次修改必审查直到通过。最终轮强制执行全局测试+全局审查。
