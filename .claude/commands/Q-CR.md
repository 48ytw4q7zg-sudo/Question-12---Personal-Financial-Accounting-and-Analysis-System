---
description: "Q-CRΩ∞Ω v8 — 强制审查闭环·全局测试·全局审查·联通测试·∞级收敛·严于全部参考文件 (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 答辩交付级自治闭环 v8

> **一个命令。∞级收敛。每次修改必审直到通过。最终三阀门：全局测试→全局审查→联通测试。**

你是 Q-CRΩ∞Ω 答辩交付级调度器。元数据（所有 commit 强制嵌入）：Author: qxw / Author-ID: 2501060122。

---

## ⚠️ 铁律（不可违）

| # | 铁律 |
|:---:|---|
| 1 | **≥5 轮** · 全绿也不停 · 每轮通过 ×1.2 收紧 |
| 2 | **每轮 ≥1 改动 + 1 commit** · 无 P0/P1 → 自选 P2/P3 |
| 3 | **每次改动 → 立即审查 → 不通过 → 修复 → 再审查** · 直到零高/中 issue（≤3 轮） |
| 4 | **每轮 ≥6 Skill 真实调用** · `Skill` 工具强制 |
| 5 | **最终轮三阀门**: 全局测试(6项) → 全局审查(11模块) → 联通测试(4链路) · 全部 PASS 才收敛 |
| 6 | 收敛 = ≥5轮 + 连续3轮零新增 + 三阀门全PASS + 评分≥95 |
| 7 | **本文件 > 全部参考文件** |

---

## 循环计数器

```
╔══════════════════════════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω v8  第 <N> 轮 · L<X> · 评分 <S>/100 · 收紧×1.2            ║
║  最低 5 轮 · 当前 <N>/5 · 零新增 <C>/3 · 三阀门 <待/通过>           ║
╚══════════════════════════════════════════════════════════════════════╝
```

**∞级递进**：

| 轮 | 等级 | 额外约束 | 必调 Skill |
|:---:|:---:|---|:---:|
| 1 | L1·基线 | 五维全绿 · 零 ERROR | ≥6 |
| 2 | L2·强化 | +R-XX 闭环 · 零 TODO · 精确版本 | ≥8 |
| 3 | L3·深度 | +N+1 归零 · 代码简化 · UI 审计 | ≥10 |
| 4 | L4·安全 | +OWASP 全项 · 依赖 CVE | ≥12 |
| 5 | L5·交付 | +139 打分 · 三阀门 · ≥95 | ≥14 |
| 6+ | L6·∞ | 每轮必发现 ≥1 微优化 · 自演进 | ≥16 |

**收紧系数**: 上轮零新增 → `下轮阈值 = min(100, 上轮×1.2)`。L5=95 → L6=100。

---

## 主执行流（6 PHASE）

```
/Q-CR 调用
    ↓
┌──────────────────────────────────────────────┐
│ PHASE A · 调度器: 8 维健康观测 [并行]          │
└──────────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────────┐
│ PHASE B · Worker: Skill 矩阵 [≥6 真实调用]    │
└──────────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────────┐
│ PHASE C · 修复器: [P0→P3 · ≥1 改动]          │
└──────────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────────┐
│ PHASE D · Verifier: 改后必审 [循环至通过]     │
│  通过? → PHASE E   不通过? → PHASE C(≤3轮)   │
└──────────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────────┐
│ PHASE E · 提交器: 规范 commit + 初步检测       │
└──────────────────────────────────────────────┘
    ↓
  轮<5 → 收紧×1.2 → 下一轮
  ≥5 + 初步条件满足 → 进入 PHASE F
    ↓
┌──────────────────────────────────────────────┐
│ PHASE F · 三阀门: 全局测试→全局审查→联通测试   │
│  全PASS → 收敛停机   任一FAIL → 修复→下一轮    │
└──────────────────────────────────────────────┘
  ≥15 → 强制停机
```

---

## PHASE A: 调度器 — 8 维健康观测（并行）

**A1 编译**（L3+ 零 WARNING）：`mvn clean compile` + `pnpm build`

**A2 测试**：`mvn test` · ≥37 用例 · `Failures: 0, Errors: 0`

**A3 API**（28 端点 · L4+ p95）：后端未运行→`nohup mvn spring-boot:run &` 等 25s → curl 全部端点

**A4 数据库**：6 表 · 金额 decimal(12,2) · 零 float/double

**A5 Git**：≥7 类 milestone · 仅白名单未提交

**A6 依赖精确**（L2+）：零 `^~LATEST*SNAPSHOT`

**A7 文件完整**（L2+）：backend 10 + frontend 6 + docs 5 + sql 1 = 22/22

**A8 R-XX 审计**（L2+）：`grep -rn "R-0[2-8]-issue" system/backend/src system/frontend/src | grep -v "已修复"` → 零输出

---

## PHASE B: Worker — Skill 强制调用矩阵

**每个 skill 必须通过 `Skill` 工具真实调用。调用后输出摘要。发现 issue → 追加待修复清单。**

### L1（≥6）
1. `code-reviewer-be` · 2. `code-reviewer-fe` · 3. `simplify` · 4. `git-commit` · 5. `conventional-commit` · 6. `using-skills`

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

| 优先级 | 类型 | 时限 |
|:---:|---|---|
| 🔴 P0 | 编译/测试失败、API 5xx、安全漏洞 | 立即 |
| 🟡 P1 | R-XX 未闭环、N+1、事务缺失 | 本轮 |
| 🟢 P2 | 代码风格、命名、注释、格式 | 本轮 |
| 🔵 P3 | 微优化、依赖升级、文档 | L3+ |

**约束**: 改前 Read → 改后编译+测试 → 失败回滚 → 同文件 >5 次冻结。L5+ 零 P0/P1 → 必选 ≥1 P2/P3。

---

## PHASE D: Verifier — 强制审查闭环

```
PHASE C 产生改动
    ↓
.java → Skill "code-reviewer-be" args "<模块>"
.vue/.js → Skill "code-reviewer-fe" args "<页面或模块>"
    ↓
零高/中 issue → ✅ 通过 → PHASE E
有高/中 issue → ❌ → PHASE C 修复 → 再审查 (≤3轮)
第3轮仍不通过 → 🚫 BLOCKED → 升级报告 → 跳过
```

审查报告: `system/docs/对话记录/Q-CR-v8-loop<N>-review-<日期>.md`

---

## PHASE E: 提交器 + 初步收敛

### 规范 commit

```
<type>(<scope>): <中文subject>

Author: qxw
Author-ID: 2501060122
Q-CR-v8 Loop: <N>/5+  L<X>  Score: <S>/100  139: <P>/139  Skills: <K>/<required>

Validation:
- compile: <PASS> (warn:N) · tests: <N/0/0> · api: <X>/28 · db: <N>表·0float
- review: <R-XX> · iterations: <N> · connectivity: <待测/通过>

Changes:
- <文件>: <说明>
```

禁止词: `temp/wip/fix again/try fix/final/update code`。缺段→拒绝。

### 初步收敛（10 项）

| # | 条件 |
|:---:|---|
| 1-8 | 同 v7（轮次≥5、连续3轮零新增、139全≥3、评分≥95、编译零ERR、Git洁净、Skill全过、近5轮≥1 commit） |
| 9 | 全局测试 6/6 PASS |
| 10 | 全局审查 零高/中 issue |

10 项全满足 → 进入 PHASE F（联通测试）。不满足 → 收紧 → 下一轮。

---

## PHASE F: 三阀门（v8 最终收敛必须全通过）

> ⚠️ **v8 核心增强：三道阀门依次执行。任一 FAIL → 修复 → 下一轮。**

### 阀门 1: 全局测试（6 项）

```
1. mvn clean compile → BUILD SUCCESS (零 ERROR · L3+ 零 WARN)
2. mvn test → 全部 PASS (≥37 · 0 fail)
3. pnpm build → built (零 error)
4. 后端启动 + 28 端点全量 curl (全部 200/400)
5. mysql 6 表 + 数据完整性 + 零 float/double
6. git status 洁净
```
全部 → ✅ 阀门1 PASS。

### 阀门 2: 全局审查（11 模块）

```
后端 7 模块:
  code-reviewer-be: UserServiceImpl, AccountServiceImpl, TransactionServiceImpl,
    BudgetServiceImpl, RecurringBillServiceImpl, StatisticsServiceImpl, CategoryServiceImpl
前端 4 核心页面:
  code-reviewer-fe: DashboardPage, TransactionListPage, AccountPage, LoginPage
```
全部零高/中 issue → ✅ 阀门2 PASS。

### 阀门 3: 系统联通测试（4 链路）🆕 v8 新增

> **验证前端↔后端↔数据库全链路数据流通。每条链路真实执行 curl + 数据库验证。**

#### 链路 1: 注册→登录→鉴权 [认证链路]
```bash
# 1.1 注册新用户
curl -X POST /api/user/register -d '{"username":"qcr-test","password":"test123"}'
# 1.2 登录获取 token
curl -X POST /api/user/login -d '{"username":"qcr-test","password":"test123"}'
# 1.3 携带 token 访问受保护接口
curl -H "Authorization: Bearer <token>" /api/account
# 验证: 注册→200, 登录→200+token, 受保护→200
```

#### 链路 2: 记账→数据库写入→查询回读 [数据写入链路]
```bash
# 2.1 创建交易记录
curl -X POST /api/transaction -H "Authorization: Bearer <token>" \
  -d '{"accountId":1,"categoryId":1,"type":2,"amount":88.88,"note":"联通测试","time":"2026-05-16 18:00:00"}'
# 2.2 数据库验证写入
mysql -e "SELECT * FROM finance_db.transaction WHERE note='联通测试'"
# 2.3 API 查询回读
curl -H "Authorization: Bearer <token>" "/api/transaction?keyword=联通测试"
# 验证: 创建→200, DB有一行, API返回该记录
```

#### 链路 3: Dashboard 统计→数据一致性校验 [统计链路]
```bash
# 3.1 调用月度统计
curl -H "Authorization: Bearer <token>" "/api/statistics/monthly?year=2026&month=5"
# 3.2 手动 SQL 验证
mysql -e "SELECT SUM(CASE WHEN type=1 AND transfer_id IS NULL THEN amount ELSE 0 END) FROM finance_db.transaction WHERE user_id=1 AND YEAR(time)=2026 AND MONTH(time)=5"
# 验证: API 返回的 totalIncome = SQL 手动计算值 (误差<0.01)
```

#### 链路 4: 转账→双账户余额→原子性校验 [事务链路]
```bash
# 4.1 记录转账前余额
curl -H "Authorization: Bearer <token>" /api/account/balance  # 记录 A余额+B余额 = S_before
# 4.2 执行转账
curl -X POST /api/transaction/transfer -H "Authorization: Bearer <token>" \
  -d '{"fromAccountId":2,"toAccountId":1,"amount":50.00,"note":"联通测试-转账"}'
# 4.3 验证转账后余额
curl -H "Authorization: Bearer <token>" /api/account/balance  # A'余额+B'余额 = S_after
# 验证: S_before == S_after (总余额不变), transfer记录含transfer_id UUID, 统计不含此记录
```

**4 链路全部 PASS → ✅ 阀门3 PASS → 全系统联通确认。**

---

## 最终收敛（12 项全满足 → 停机）

| # | 条件 |
|:---:|---|
| 1-8 | 初步收敛 8 项 |
| 9 | 阀门1: 全局测试 6/6 ✅ |
| 10 | 阀门2: 全局审查 11模块 零高/中 ✅ |
| 11 | 阀门3: 联通测试 4链路 数据一致 ✅ ⚠️ v8 新增 |
| 12 | 阀门3: 联通测试 S_before==S_after ✅ ⚠️ v8 新增 |

**全部 12 项满足 → 收敛停机证明**：

```
╔══════════════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω v8 终极收敛 · 全系统联通 · 答辩就绪              ║
║  轮次: N · 评分: S/100 · 139项: 全≥3                     ║
║  编译: PASS(零ERR) · 测试: 37/0 · API: 28/28            ║
║  阀门1·全局测试: 6/6 ✅                                    ║
║  阀门2·全局审查: 11模块 零高/中 ✅                         ║
║  阀门3·联通测试: 4链路 PASS · 数据一致 ✅                  ║
║  DB: 6表·0float · Commits: N · Skills: K次               ║
║  Author: qxw · ID: 2501060122                            ║
║  结论: ✅ 可演示 ✅ 可答辩 ✅ 可交付 ✅ 全系统联通验证       ║
╚══════════════════════════════════════════════════════════╝
```

**任一项不满足 → 收紧 ×1.2 → 下一轮。≥15 轮 → 强制停机 + 最终报告。**

---

## 内嵌策略

**熔断**: 同issue 3轮→BLOCKED · 同文件>5次→冻结 · 编译/测试失败→回滚

**失败分类**: compile×3→unit_test×3→integration×2 escalate→semantic×2 rollback→architecture BLOCKED

**收敛**: min_loops=5 · clean=3 · score≥95 · 阀门1=6/6 · 阀门2=0高/中 · 阀门3=4/4+S_before==S_after · max_loops=15

---

## 139 项验收清单（L5 逐条 1-4 分 · /556 折百）

一、Phase流程(1-5)[20] · 二、PRD功能(6-10)[20] · 三、TECH架构(11-20)[40] · 四、DB(21-35)[60] · 五、API(36-48)[52] · 六、JWT(49-60)[48] · 七、账户(61-68)[32] · 八、分类(69-73)[20] · 九、流水(74-85)[48] · 十、Dashboard(86-95)[40] · 十一、前端(96-105)[40] · 十二、后端(106-114)[36] · 十三、安全(115-120)[24] · 十四、构建(121-130)[40] · 十五、CC流程(131-139)[36]

---

## 参考权威源（优先级递减）

本文件 > 评分细节.doc > 08b操作流程 > CLAUDE.md template > 项目CLAUDE.md > 项目docs/

题12标定卡: 单一用户角色 · 6表 · 28接口 · 11页面 · P2满分

---

## 项目硬配置

后端 `system/backend/` (SB3.5.14·Maven·Java21) · 前端 `system/frontend/` (Vue3.5.34·pnpm·Vite8)
DB `finance_db`@`localhost:3306` (root/root·MySQL8.4) · API `http://localhost:8080/api`
测试账号 zhangsan/123456 · BCrypt `$2a$10$dGB2.RDec.rSTe7KZ/EkT.Mi9pWrxkJMiKL4fJmQ8OVXuBq/KHlG6`

---

## 调用

```
/Q-CR
```

无参数。≥5轮。每次修改必审直到通过。最终轮强制执行三阀门：全局测试→全局审查→联通测试。
