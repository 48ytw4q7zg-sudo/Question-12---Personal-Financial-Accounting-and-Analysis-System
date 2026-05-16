---
description: "Q-CRΩ∞Ω v5 — ∞级收敛·≥5轮·嵌套30+skills·评分锚点·严于全部参考文件 (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 终极自治闭环 v5

> **无参数。调用即执行 ≥5 轮。每轮自动收紧。标准严于全部参考文件。**

你是企业答辩级全栈工程调度器。元数据（所有 commit 强制嵌入）：Author: qxw / Author-ID: 2501060122。

---

## ⚠️ 硬执行协议（不可违）

1. **最少 5 轮，上不封顶** — 即使第 1 轮 139 项全绿也必须继续。每轮通过 → 下轮标准自动乘 1.2 倍
2. **动态收敛公式** — `收敛 = (轮次≥5) ∧ (连续3轮零新增) ∧ (评分≥95) ∧ (所有skill零issue)`
3. **每轮必产出** — 健康报告 + ≥1 改动 + commit。无改动可做 → 自动触发微优化扫描（命名/注释/格式/依赖）
4. **嵌套 ≥8 个 skill/轮** — 按收紧等级矩阵强制调用（Skill 工具真实调用，不得只提建议）
5. **标准优先级** — 本文件 > 评分细节.doc > 08b 操作流程 > CLAUDE.md template > 项目 docs/

---

## 循环计数器（每轮开始必须输出）

```
╔══════════════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω v5  第 <N> 轮 · 收紧等级 L<X> · 评分锚点 <S>/100║
║  强制最低: 5 轮 · 当前: <N>/5 · 连续零新增: <C>/3       ║
║  下轮收紧系数: ×1.2                                      ║
╚══════════════════════════════════════════════════════════╝
```

**∞级收紧递进**（L6+ 自动生成）：

| 轮次 | 收紧等级 | 附加约束 | 必调 Skill 数 |
|:---:|:---:|---|---|
| 1 | L1·基线 | 五维全绿 + 编译零 warn | ≥6 |
| 2 | L2·强化 | L1 + R-XX 全闭环 + 零 TODO + 依赖精确版本 | ≥8 |
| 3 | L3·深度 | L2 + N+1 归零 + 代码简化 + UI 设计审计 | ≥10 |
| 4 | L4·安全 | L3 + OWASP 全项 + 依赖 CVE 扫描 | ≥12 |
| 5 | L5·交付 | L4 + 139 项全量逐条打分 + 演示流程 | ≥14 |
| 6 | L6·极致 | L5 + 每个 Service 方法必须有测试 + 前端 E2E 场景 | ≥16 |
| 7 | L7·变态 | L6 + 性能基准(baseline ±3%) + 注释覆盖率 ≥60% | ≥18 |
| 8+ | L8·∞ | L7 + 每轮自动发现新优化维度 + 自演进 | ≥20 |

**收紧系数**：若上一轮零新增 issue → 下轮通过阈值 = 上一轮阈值 × 1.2。例如 L5 评分 95 通过 → L6 目标 = 95 × 1.2 → min(100, 114) = 100。

---

## 每轮固定执行流

```
╔══════════════════════════════════════════════════════╗
║ PHASE A: 8 维健康观测 [并行执行]                      ║
║ 1.编译 2.测试 3.API(全28端点) 4.数据库 5.Git        ║
║ 6.依赖版本精确性 7.文件完整性 8.R-XX审计              ║
╚══════════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════════╗
║ PHASE B: 嵌套 Skill 矩阵 [按等级强制调用]             ║
║ L1: code-reviewer-be/fe + git-commit + simplify      ║
║ L2: + karpathy-guidelines + systematic-debugging      ║
║ L3: + code-simplifier + frontend-design               ║
║ L4: + security-reviewer + find-skills + mysql         ║
║ L5: + requesting-code-review + brainstorming          ║
║ L6+: + unittest-coder + perf-optimizer + refactor     ║
╚══════════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════════╗
║ PHASE C: 修复与优化 [P0→P1→P2 排序·≥1条]            ║
║ 改前读→改后编译+测试→失败回滚→同文件>5冻结           ║
╚══════════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════════╗
║ PHASE D: 二次审查 [改什么审什么·最多3轮修复-审查]     ║
║ .java→code-reviewer-be · .vue/.js→code-reviewer-fe   ║
╚══════════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════════╗
║ PHASE E: 规范提交 [Author/ID/Loop/Score/Changes]     ║
╚══════════════════════════════════════════════════════╝
    ↓
  轮次<5 → 收紧 ×1.2 → 下一轮
  ≥5 + 3轮零新增 + 评分≥95 → 收敛
  ≥15 → 强制停机 + 答辩就绪证明
```

---

## PHASE A: 8 维健康观测（全并行）

### A1 编译（零 warn 标准）
```powershell
cd system/backend; mvn clean compile 2>&1 | Select-String "BUILD|ERROR|WARNING"
```
```bash
cd system/frontend && pnpm build 2>&1 | tail -5
```
通过: `BUILD SUCCESS` + 零 `ERROR`。L3+ 要求零 `WARNING`。

### A2 测试（全绿标准）
```powershell
cd system/backend; mvn test 2>&1 | Select-String "Tests run:|BUILD"
```
通过: `Failures: 0, Errors: 0, Skipped: 0`。L5+ 要求覆盖率报告。

### A3 API（28 端点全量验证）
后端未运行则先启动（`nohup mvn spring-boot:run &` 等 25s），然后逐模块验证：
```bash
# 认证模块 (3)
curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/api/user/login -H "Content-Type: application/json" -d '{"username":"zhangsan","password":"123456"}'
curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/api/user/register -H "Content-Type: application/json" -d '{}'
# 账户模块 (5)
curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/account
# 分类 (1) · 交易 (5) · 预算 (4) · 统计 (4) · 周期账单 (5) · 汇率 (1)
```
通过: 全部 28 端点返回码 ∈ {200,400}（400=参数校验通过，500=BUG）。L4+ 需记录 p95 延迟。

### A4 数据库（结构+数据完整性）
```powershell
mysql -u root -proot -e "
SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db';
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA='finance_db' AND COLUMN_NAME IN ('amount','initial_balance')
AND DATA_TYPE != 'decimal';" 2>&1
```
通过: 6 表(user/account/category/transaction/budget/recurring_bill)。金额列全部 decimal(12,2)。零 float/double。

### A5 Git（洁净度+milestone）
```bash
git log --oneline -10 && echo "---MILESTONES---" && git log --oneline | grep -E "feat\(p2\)|docs\(p3\)|chore\(rules\)|feat\(p4\)|feat\(p5\)|test\(p6\)|docs\(p8\)" && echo "---STATUS---" && git status --short
```
通过: ≥6 类 milestone 存在。未提交仅白名单（`.claude/scheduled_tasks*` `auto-push.ps1`）。

### A6 依赖版本精确性（L2+）
检查 `pom.xml` 和 `package.json`：禁止 `LATEST`/`^`/`~`/`-SNAPSHOT`/`*`。所有版本号必须锁定到精确版本。对照 CLAUDE.md §一·一 技术栈版本表逐项核对。

### A7 文件完整性（L2+）
```
backend: controller/ service/ impl/ mapper/ entity/ dto/ config/ common/ interceptor/ util/ → 10/10
frontend: api/ router/ stores/ views/ components/ layout/ → 6/6
docs: PRD/TECH_DESIGN/DATABASE_DESIGN/API_DESIGN/DEPLOY/00-选题标定 → 5/5
sql: 01-init.sql → 1/1
```
缺任一项 → P0 修复。

### A8 R-XX 审计（L2+）
```bash
grep -rn "R-0[2-8]-issue" system/ --include="*.java" --include="*.vue" --include="*.js" --include="*.md" | grep -v "已修复"
```
通过: 零输出。L5+ 需输出"已修复"总数统计。

---

## PHASE B: 嵌套 Skill 强制调用矩阵

**协议：每个 Skill 必须通过 `Skill` 工具真实调用。调用后输出摘要。发现 issue → 追加待修复清单。L5+ 需统计 skill 调用覆盖率。**

### L1 基线层（≥6 skill）
| # | Skill | 目标 | 调用 |
|:---:|---|---|---|
| 1 | `code-reviewer-be` | 审查核心 ServiceImpl | `Skill "code-reviewer-be" args "TransactionServiceImpl"` |
| 2 | `code-reviewer-fe` | 审查核心页面 | `Skill "code-reviewer-fe" args "DashboardPage"` |
| 3 | `simplify` | 代码精简 | `Skill "simplify"` |
| 4 | `git-commit` | 规范化提交 | `Skill "git-commit"` |
| 5 | `conventional-commit` | commit 格式校验 | `Skill "conventional-commit"` |
| 6 | `using-skills` | skill 使用规范 | `Skill "using-skills"` |

### L2 强化层（+2 = ≥8 skill）
| # | Skill | 目标 |
|:---:|---|---|
| 7 | `karpathy-guidelines` | AI 编码最佳实践 |
| 8 | `systematic-debugging` | 系统性排错 |

### L3 深度层（+4 = ≥10 skill）
| # | Skill | 目标 |
|:---:|---|---|
| 9 | `code-simplifier` | 代码深度简化 |
| 10 | `frontend-design` | UI 设计审计 |
| 11 | `element-plus-vue3` | Element Plus 最佳实践 |
| 12 | `vue-testing-best-practices` | Vue 测试规范 |

### L4 安全层（+4 = ≥12 skill）
| # | Skill | 目标 |
|:---:|---|---|
| 13 | `security-reviewer` | OWASP 深度安全 |
| 14 | `find-skills` | 发现缺失能力 |
| 15 | `mysql` | MySQL 审查 |
| 16 | `mysql-best-practices` | 数据库最佳实践 |

### L5 交付层（+4 = ≥14 skill）
| # | Skill | 目标 |
|:---:|---|---|
| 17 | `requesting-code-review` | 最终全量审查 |
| 18 | `brainstorming` | 发现改进方向 |
| 19 | `rest-api-design` | API 设计审查 |
| 20 | `springboot-patterns` | SpringBoot 模式审查 |

### L6+ 极致层（+6 = ≥20 skill）
| # | Skill | 目标 |
|:---:|---|---|
| 21 | `unittest-coder` | 补充缺失测试 |
| 22 | `perf-optimizer` | 性能优化 |
| 23 | `refactor-helper` | 结构化重构 |
| 24 | `test-driven-development` | TDD 方法 |
| 25 | `spring-boot-testing` | SpringBoot 测试 |
| 26 | `java-springboot` | Java SpringBoot 模式 |

---

## PHASE C: 修复与优化

| 优先级 | 类型 | 例子 | 时限 |
|:---:|---|---|---|
| 🔴 P0 | 编译/测试失败、API 5xx、安全漏洞、数据丢失 | 立即 | 本轮 |
| 🟡 P1 | R-XX 未闭环、N+1、事务缺失、校验缺失、幽灵路由 | 本轮 | 本轮 |
| 🟢 P2 | 代码风格、命名、注释覆盖、格式、import 排序 | 本轮 | 本轮 |
| 🔵 P3 | 微优化、依赖升级补丁、文档措辞、测试补充 | L3+ | 可累积 |

**约束**: 改前 Read → 改后编译+测试 → 失败回滚 → 同文件 >5 次冻结 → 同一 issue 3 轮未过 BLOCKED。

**L5+ 特殊规则**: 若本轮零 P0/P1 问题 → 必须从 P2/P3 中自选 ≥1 项优化（不可跳过）。

---

## PHASE D: 二次审查

- `.java` → `Skill "code-reviewer-be" args "<模块>"`
- `.vue`/`.js` → `Skill "code-reviewer-fe" args "<页面或模块>"`
- 审查报告写入 `system/docs/对话记录/Q-CR-v5-loop<N>-review-<日期>.md`
- 不通过 → PHASE C（最多 3 轮）→ 超限 BLOCKED → 升级报告

---

## PHASE E: 规范提交

```
<type>(<scope>): <中文subject>

Author: qxw
Author-ID: 2501060122
Q-CR-v5 Loop: <N>/5+  Level: L<X>  Score: <S>/100  139check: <通过>/139
Skills called: <skill1>, <skill2>, ... (<K>/<required>)

Validation:
- compile: <PASS> (warnings: <N>)
- tests: <N run / X fail / Y err / Z skip>
- api: 28 endpoints, <pass>/28 pass, p95=<X>ms
- db: <N> tables, <M> rows, 0 float/double
- review: <R-XX status>, <N> issues found
- git: <N> commits, <M> files uncommitted

Changes:
- <文件路径1>: <改动说明>
- <文件路径2>: <改动说明>
```

校验: 禁止 `temp/wip/fix again/try fix/final/test/update code/改了一些`。缺任一段 → 拒绝。subject >25 中文/50 字符 → 自动截断。

---

## 139 项企业答辩级验收清单（L5 逐条打分）

> 每项评分: 1=未实现 2=部分 3=通过 4=优秀。总分 / 556。折算百分制 = 总分/556×100。

### 一、Phase 流程验收 (1-5) [20分]
1. `.claude/project-status.md` — Phase/文档/commit 三同步
2. R-02/R-02b/R-03/R-04 — 全部"已修复"无例外
3. `grep -r "R-0[2-8]-issue" | grep -v "已修复"` → 零输出
4. `docs/对话记录/` — ≥15 份，覆盖 Phase1-8
5. Git milestone — feat(p2)/docs(p3)/chore(rules)/feat(p4)/feat(p5)/test(p6)/docs(p8) ≥7 类

### 二、PRD 功能验收 (6-10) [20分]
6. P0 全实现：登录+JWT+账户CRUD+分类列表+记账+按账户汇总
7. P1 全实现：预算+月度趋势+分类统计+转账+最近流水+周期账单
8. P2 全实现：CSV导入+多币种汇率+ECharts多图+预算预警
9. 页面=11（PRD=TECH_DESIGN=实际views/）· 标定卡规定 P0 5 → P1 10-15 → 已达 P2 级别
10. 命名三角一致：PRD 功能名 ↔ TECH_DESIGN 路由 ↔ views/*Page.vue

### 三、TECH_DESIGN 架构验收 (11-20) [40分]
11. AppLayout.vue — `el-container > el-header + el-container > el-aside + el-main`
12. 响应式: ≥992px 侧栏200px / 768-991px 折叠64px / <768px 抽屉
13. LoginPage 不套 AppLayout（R-02b 修复·独立路由 `meta.requiresAuth: false`）
14. 10 个业务页面全嵌套在 AppLayout children
15. TECH_DESIGN §6 原型落地：结构/按钮/表格/搜索栏/分页/Dialog 全部存在
16. 侧边栏完整 10 项：首页/账户/分类/收支/预算/周期账单/转账/统计/导入/设置
17. 零"页面存在但菜单不可达"
18. 零"菜单存在但页面不存在"（幽灵路由）
19. 所有数据页面含 `v-loading` 或 `:loading`
20. 所有删除含 `ElMessageBox.confirm` + `.catch(() => {})`

### 四、DATABASE_DESIGN 验收 (21-35) [60分]
21. `sql/01-init.sql` 真实执行零报错（`mysql -u root -proot < sql/01-init.sql`）
22. 数据库=`finance_db`（与 `application.yml` `spring.datasource.url` 一致）
23. `SHOW TABLES` = user/account/category/transaction/budget/recurring_bill(6)
24. transaction 含 `transfer_id VARCHAR(36)` 标记转账
25. **🔴 最高优先级**: transfer 不进入 Dashboard 统计 — SQL `WHERE transfer_id IS NULL`
26. 全部金额字段 `decimal(12,2)` — account.initial_balance/budget.amount/recurring_bill.amount/transaction.amount
27. `grep -i "float\|double" sql/01-init.sql` → 零出现（注释除外）
28. recurring_bill 含 `next_due_date DATE`
29. recurring_bill.period 支持 monthly/weekly（VARCHAR(10)）
30. 索引覆盖: user_id(全部关联表) + account_id + category_id + time + transfer_id + uk_user_category_month
31. category.type TINYINT: 1=支出 2=收入
32. transaction.time DATETIME
33. 全部 6 表 `create_time DATETIME DEFAULT CURRENT_TIMESTAMP` + `update_time ON UPDATE`
34. `DROP TABLE IF EXISTS` → 可重复执行
35. 测试数据: ≥2 users + ≥4 accounts(4种type) + ≥6 transactions(含transfer组) + ≥3 budgets + ≥3 recurring_bills

### 五、API_DESIGN 验收 (36-48) [52分]
36. API 总数=28（P0 11 + P1 13 + P2 4）
37. URL 全匹配: `/api/user/login|register|change-password` `/api/account[/{id}|/balance]` `/api/category` `/api/transaction[/{id}|/transfer|/import]` `/api/budget[/progress|/alert]` `/api/statistics/monthly|yearly|category-summary|trend` `/api/recurring-bill[/{id}|/generate]` `/api/exchange-rate`
38. HTTP Method: GET(查询列表/统计) POST(创建/登录/注册/转账/导入) PUT(更新) DELETE(软删除)
39. 前端 API 函数 ↔ 后端 Controller 一一对应（7 个前端 api/*.js → 8 个后端 Controller）
40. API_DESIGN.md v2.0 版本，与代码同步
41. 分页: `{records, total, size, current, pages}` · 参数 pageNum(从1)/pageSize(默认10,最大100)
42. Result: `{code: Integer, message: String, data: T}` · code=200 成功
43. 无 data/result 混用 — Controller 返回全部 `Result.success()` 包装
44. ErrorCode: BusinessException(code, msg) · 模块编号 1xxx(用户)/2xxx(账户)/3xxx(交易)/4xxx(预算)/5xxx(周期账单)
45. 时间: `yyyy-MM-dd HH:mm:ss` · Jackson `date-format` + `@JsonFormat` 双保险
46. DTO ↔ DB: camelCase ↔ snake_case MyBatis-Plus 自动映射
47. 零 DTO 漂移 — 文档字段 = 代码字段 = 前端接收字段
48. curl 验证 28/28 端点可访问 — L4+ 需输出通过矩阵

### 六、JWT 登录系统 (49-60) [48分]
49. POST /api/user/login → 200 + `{token, userId, username}`
50. JWT sub=userId · iat/exp 完整
51. Axios request.js 拦截器 → `Authorization: Bearer <token>`
52. router.beforeEach → token 检查 → redirect
53. 未登录访问 `/` → 401 API 拦截 → redirect `/login?redirect=原路径`
54. token 存储 `localStorage.getItem('token')`
55. 刷新后仍登录（localStorage 持久）
56. token 无效/过期 → 401 → `localStorage.removeItem('token')` + `router.push('/login')`
57. logout → `userStore.clearUser()` + `localStorage.removeItem('token')` + 跳转
58. BCrypt: `new BCryptPasswordEncoder().encode(raw)` 注册 · `matches(raw, hash)` 登录
59. LoginInterceptor: `/api/**` 全拦截 · 白名单外 → 401 JSON
60. 白名单: `/api/user/login` `/api/user/register` `/api/health`（WebMvcConfig 3 项）

### 七、账户模块 (61-68) [32分]
61. POST /api/account → 200 · 入参 @Valid
62. PUT /api/account/{id} → 200 · userId 归属校验
63. DELETE /api/account/{id} → 200 · 软删除 status=0 · 有 transaction/recurring_bill 时拒绝
64. 删除按钮 `el-button type="danger"`
65. 删除确认 `ElMessageBox.confirm('确定删除该账户吗？', '提示', { type: 'warning' })`
66. 余额 = initial_balance + SUM(income) - SUM(expense) （含 transfer 真实进出）
67. accountTypeMap: {1:'现金', 2:'银行卡', 3:'支付宝', 4:'微信'}
68. 表单含 type 下拉 + currency 字段（P2 多币种）

### 八、分类模块 (69-73) [20分]
69. GET /api/category → 200 · 13 条种子数据（支出8+收入5）
70. 种子数据不做增改删（PRD P0-3 规定）
71. CategoryPage 按 type 分组展示
72. 接口完整
73. TransactionListPage 分类下拉按 type 过滤（收入/支出）

### 九、流水模块 (74-85) [48分]
74. POST /api/transaction → 200 · @Valid + @JsonFormat
75. PUT /api/transaction/{id} → 200 · userId 归属校验 · 转账记录仅可改备注
76. 软删除通过 status
77. 分页: pageNum/pageSize + 筛选
78. 日期筛选: startTime/endTime
79. 账户筛选: accountId
80. 分类筛选: categoryId + keyword 搜索
81. POST /api/transaction/transfer → @Transactional · 一收一支 + UUID transfer_id · 余额不足拒绝
82. 🔴 transfer 不进入 Dashboard 统计
83. ORDER BY t.time DESC
84. 金额: 收入绿色/支出红色
85. 空数据: EmptyState 组件或 v-if 占位

### 十、Dashboard (86-95) [40分]
86. 月收入=SUM(type=1 AND transfer_id IS NULL)
87. 月支出=SUM(type=2 AND transfer_id IS NULL)
88. 月结余=收入-支出
89. 🔴 transfer 必须不进入（3 重验证: SQL + API + 手动计算）
90. ECharts 饼图: category-summary 数据
91. ECharts 折线/柱状图: trend 12 个月
92. 最近流水前 5 条
93. window resize → ECharts resize
94. null → BigDecimal.ZERO → 显示 ¥0.00
95. 索引覆盖 · 无 N+1（StatisticsServiceImpl 纯 Mapper 调用）

### 十一、前端工程 (96-105) [40分]
96. 零 Vue warning（grep console）
97. 零 console.error
98. 零白屏（全部路由 component 懒加载成功）
99. 全部 11 路由可进入
100. 全部 10 菜单可点击跳转
101. v-loading 或 :loading 全覆盖
102. el-form rules 全覆盖（必填/长度/格式）
103. 提交按钮 :loading 防重复
104. ref(基本类型/单值) / reactive(对象/数组) 正确
105. async/await 无遗漏（no .then() chain）

### 十二、后端工程 (106-114) [36分]
106. Controller 只做 @Valid + 转发 Service
107. ServiceImpl max ≤350 行
108. 零 N+1（批量 selectBatchIds + Map 索引）
109. @Transactional on 写操作（transfer/importCsv/generate）
110. Mapper XML 全部 `#{}`（零 `${}`）
111. GlobalExceptionHandler: 400/401/403/404/500 + BusinessException + Exception
112. 零未捕获 500（全部 catch 或 throws 到 Handler）
113. 零硬编码 URL/密码/密钥
114. 零重复代码块（相似 >10 行 → 提取）

### 十三、安全 (115-120) [24分]
115. 零 SQL 注入（LambdaQueryWrapper + #{}）
116. 零 XSS（Vue {{ }} + @Size）
117. 零未鉴权 API（LoginInterceptor 全拦截）
118. User.password @JsonIgnore
119. JWT secret: `${JWT_SECRET:finance-system-jwt-secret-key-2026}` 可覆盖
120. CorsConfig: allowedOriginPattern + methods + headers + credentials

### 十四、构建部署 (121-130) [40分]
121. `mvn clean compile` → BUILD SUCCESS
122. `mvn test` → 全部通过（≥37 用例）
123. `pnpm install` → 零报错
124. `pnpm dev` → 启动成功
125. `pnpm build` → 构建成功
126. 前后端同时启动 8080+5173
127. MySQL finance_db 可达
128. README: 启动步骤+SQL初始化+测试账号+技术栈+测试覆盖表
129. DEPLOY.md: 7 节完整（环境/DB/后端/前端/生产部署/测试账号/FAQ）
130. 完整演示: 登录→加账户→记账→Dashboard→预算→周期账单（全部 curl 可验证）

### 十五、Claude Code 流程 (131-139) [36分]
131. 每个 feature 后 reviewer（R-05/R-06/R-07/R-08）
132. reviewer 报告写入 `docs/对话记录/`（≥15 份）
133. 文档=代码（无 AI 擅自改文档不写代码）
134. 数据库结构无 AI 擅自修改
135. 零 API 漂移（文档 URL/Method = 代码 = 前端调用）
136. 零 DTO 漂移（字段名/类型 前后端一致）
137. 零 Context Drift
138. 阶段性 commit（≥30 次，覆盖全部 Phase）
139. 前后端真实联调通过

---

## ∞级收敛条件（全部满足方可停机）

- [ ] 轮次 ≥ **5**（上不封顶）
- [ ] 连续 ≥ **3** 轮 PHASE B 零新增 issue
- [ ] **139 项全部 ≥ 3 分**（无 1/2 分项）
- [ ] 总评分 ≥ **95/100**（折算）
- [ ] 五维全绿 + 编译零 WARNING
- [ ] Git 洁净（仅白名单未提交）
- [ ] 本轮调用的全部 skill 返回通过
- [ ] 近 5 轮每轮 ≥1 commit
- [ ] **L5+ 附加**: 至少 1 个 ServiceImpl 达到 100% 方法覆盖

**停机输出**：
```
╔══════════════════════════════════════════════════════╗
║     Q-CRΩ∞Ω v5 终极收敛 — 答辩就绪                     ║
║                                                      ║
║  总轮次: N · 评分: S/100 · 139项: 全≥3分              ║
║  编译: PASS(零warn) · 测试: 37/0 · API: 28/28        ║
║  DB: 6表·0float · Commits: N · Skill调用: K次         ║
║  审查报告: M 份 · 审查闭环: R-02~R-08                 ║
║                                                      ║
║  Author: qxw · ID: 2501060122                        ║
║  结论: ✅ 可演示 ✅ 可答辩 ✅ 可交付                    ║
╚══════════════════════════════════════════════════════╝
```

---

## 评分锚点（对标评分细节.doc）

| 评分维度 | 满分 | 本文件标准 | 对标 |
|---|---|---|---|
| Gitee 交付清单 | 25 | 10 项全量 | ≥ 评分细节 5.1 |
| 答辩演示 | 25 | P0 全跑通(20) + P1×3(+3) + P2×2(+2) = 25 | = 满分 |
| 项目架构讲解 | 10 | 数据流+模块边界清晰 | ≥ 优秀线 9 |
| 核心代码讲解 | 30 | 逐行 Why+How | ≥ 优秀线 27 |
| 提问 | 10 | @Transactional/JWT/BCrypt/N+1 | ≥ 优秀线 9 |
| **总分** | **100** | **目标 ≥95** | |

---

## 参考权威源（本文件标准高于全部）

- 本文件 139 条 + ∞级收敛 > `软件框架技术及应用评分细节.doc` > `08b-项目实施操作流程.md` > `course-project-template/CLAUDE.md` > 项目 `CLAUDE.md` > `docs/` 全部文档
- **题12标定卡**: 单一用户角色 · 4→6表 · 10→28接口 · 5→11页面 · 11h→38h工时 → 已达 P2 满分级别
- **角色列表汇总**: 题12 = 单角色 ✓

---

## 调用

```
/Q-CR
```

无参数。自动执行 ≥5 轮 ∞级收敛。每轮嵌套 ≥6-20 个 skill。输出 139 项评分矩阵 + 答辩就绪证明。
