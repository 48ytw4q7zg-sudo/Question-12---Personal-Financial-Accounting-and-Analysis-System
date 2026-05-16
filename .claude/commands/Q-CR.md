---
description: "Q-CRΩ∞Ω v4 — 139项企业级验收·≥5轮强制循环·渐进收紧·嵌套skills (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 企业交付级自治闭环 v4

> **无参数、无模式。调用即完整执行 ≥5 轮循环。每轮自动收紧标准。**

你是企业交付级全栈工程调度器。元数据：Author: qxw / Author-ID: 2501060122（所有 commit 强制嵌入）。

---

## ⚠️ 硬执行协议（不可违）

1. **最少 5 轮** — 即使第 1 轮全绿也必须继续，每轮自动提高标准
2. **每轮必须产出** — 健康报告 + 至少 1 条修复/优化 + commit
3. **嵌套 skill** — 每轮根据问题类型自动调用相关 skill（code-reviewer-be/fe、code-simplifier、frontend-design、find-skills 等）
4. **参照体系** — 以本文件嵌入的 139 条检查点为验收锚点，以 `docs/` 下 6 份文档 + 参考文件夹全部文档为权威源
5. **收敛条件** — 连续 3 轮零新增 + 轮次 ≥5 + 139 项全部通过 → 方可停机

---

## 循环计数器（每轮开始必须声明）

```
╔══════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω 第 <N> 轮 · 收紧等级: L<1-5>           ║
║  最低要求: 5 轮 · 当前: <N>/5                    ║
╚══════════════════════════════════════════════════╝
```

**收紧等级递进**：

| 轮次 | 收紧等级 | 额外要求 |
|:---:|:---:|---|
| 1 | L1 基线 | 编译+测试+API+DB+Git 五维全绿 |
| 2 | L2 强化 | L1 + 所有 R-XX issue 闭环 + 无 TODO/FIXME |
| 3 | L3 深度 | L2 + N+1 扫描 + 代码简化(code-simplifier) + 前端设计审查(frontend-design) |
| 4 | L4 安全 | L3 + OWASP 安全审查(security-reviewer) + 依赖版本锁定验证 |
| 5 | L5 交付 | L4 + 139 项全量验收 + 完整演示流程验证 |
| 6+ | L5+ | L5 保持 + 每次必须发现 ≥1 项微优化点 |

---

## 每轮固定执行流

```
声明轮次+收紧等级
    │
    ▼
┌──────────────────────────────────────────────┐
│ PHASE A: 多维健康观测                         │
│  1.编译 2.测试 3.API 4.数据库 5.Git           │
│  6.依赖版本 7.文件完整性 8.R-XX闭环           │
└──────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────┐
│ PHASE B: 嵌套 Skill 诊断                      │
│  根据收紧等级自动调用:                         │
│  L1-2: code-reviewer-be + code-reviewer-fe   │
│  L3: + code-simplifier + frontend-design      │
│  L4: + security-reviewer + find-skills        │
│  L5: + 全量139项逐条核对                      │
└──────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────┐
│ PHASE C: 修复与优化                            │
│  每轮至少 1 条改动，按 P0>P1>P2 排序           │
│  改动后立即编译+测试验证                       │
└──────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────┐
│ PHASE D: 二次审查                              │
│  修复后重新调用对应 reviewer                   │
│  最多 3 轮修复-审查循环，超限 BLOCKED          │
└──────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────┐
│ PHASE E: 规范提交                              │
│  含 Author/Author-ID/Validation/139项进度     │
└──────────────────────────────────────────────┘
    │
    ├── 轮次<5 → 收紧标准 → 下一轮
    ├── 轮次≥5 + 连续3轮零新增 + 139全过 → 收敛停机
    └── 轮次≥10 → 强制停机 + 最终报告
```

---

## PHASE A: 多维健康观测

### A1-A5: 基础五维（每轮必查）

**A1 编译**：
```powershell
cd system/backend; mvn clean compile 2>&1 | Select-String "BUILD|ERROR"
```
```bash
cd system/frontend && pnpm build 2>&1 | tail -3
```
通过: 含 BUILD SUCCESS + 含 built

**A2 测试**：
```powershell
cd system/backend; mvn test 2>&1 | Select-String "Tests run:"
```
通过: `Failures: 0, Errors: 0`

**A3 API**（后端未运行则先启动 `nohup mvn spring-boot:run &` 等 25s）：
```bash
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/health
```
通过: 200

**A4 数据库**：
```powershell
mysql -u root -proot -e "SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db';" 2>&1
```
通过: 6 表 (user/account/category/transaction/budget/recurring_bill)

**A5 Git**：
```bash
git log --oneline -5 && echo "---" && git status --short
```
通过: 仅白名单文件可未提交

### A6-A8: 增强检查（L2+ 执行）

**A6 依赖版本**：检查 `pom.xml` 和 `package.json` 中无 `LATEST`/`^`/`~` 前缀

**A7 文件完整性**：
```
backend: controller/ service/ service/impl/ mapper/ entity/ entity/dto/ config/ common/ interceptor/ util/ — 10/10
frontend: api/ router/ stores/ views/ components/ layout/ — 6/6
docs: PRD/TECH_DESIGN/DATABASE_DESIGN/API_DESIGN/DEPLOY — 5/5
```

**A8 R-XX 闭环**：
```bash
grep -r "R-0[2-8]-issue" system/ --include="*.java" --include="*.vue" --include="*.js" --include="*.md" | grep -v "已修复"
```
通过: 零输出

---

## PHASE B: 嵌套 Skill 诊断矩阵

根据收紧等级自动调用。**每个 skill 必须真实调用（`Skill` 工具），不得只提建议**。

| 收紧等级 | 调用的 Skill | 调用方式 |
|:---:|---|---|
| L1 | `code-reviewer-be` 全模块 | `Skill "code-reviewer-be" args "TransactionServiceImpl"` 等 |
| L2 | `code-reviewer-fe` 全页面 | `Skill "code-reviewer-fe" args "DashboardPage"` 等 |
| L3 | `code-simplifier` | `Skill "code-simplifier"` |
| L3 | `frontend-design` | `Skill "frontend-design"` |
| L4 | `security-reviewer` | `Skill "security-reviewer"` |
| L4 | `find-skills` | `Skill "find-skills"` 搜索可优化点 |
| L5 | `requesting-code-review` | `Skill "requesting-code-review"` 最终审查 |
| L5 | `skill-creator` | `Skill "skill-creator"` 评估是否需要新 skill |

**调用规则**：
- 每个 skill 调用后，将输出摘要写入本轮 report
- 若 skill 发现 issue，追加到待修复清单
- 不可跳过 — 即使用户未要求，本协议强制调用

---

## PHASE C: 修复与优化

按优先级处理：

| 优先级 | 类型 | 例子 |
|:---:|---|---|
| P0 | 编译/测试失败、API 5xx、安全漏洞 | 立即修复 |
| P1 | R-XX 未闭环、N+1、事务缺失、校验缺失 | 本轮内修复 |
| P2 | 代码风格、命名、注释、性能微优化 | 累积到 L3+ 处理 |

**约束**：
- 改前先 `Read` 文件
- 改后立即 `mvn compile` + `mvn test`（或 `pnpm build`）
- 失败 → 回滚 → 标记 BLOCKED
- 同文件 >5 次 → 冻结

---

## PHASE D: 二次审查

改动了什么就审查什么：
- `.java` → `Skill "code-reviewer-be" args "<模块>"`
- `.vue/.js` → `Skill "code-reviewer-fe" args "<页面或模块>"`
- 审查不通过 → 回到 PHASE C（最多 3 轮）
- 3 轮未过 → BLOCKED → 升级报告 → 跳过

---

## PHASE E: 规范提交

```
<type>(<scope>): <中文subject>

Author: qxw
Author-ID: 2501060122
Loop: <N>/5+  L<等级>  139check: <通过数>/139

Validation:
- compile: <PASS>
- tests: <N run / X fail>
- review: <R-XX status>
- api: <HTTP code>
- db: <N tables>

Changes:
- <文件路径>: <说明>
```

提交后输出 commit SHA，轮次计数器 +1。

---

## 139 项企业交付级验收清单（L5 逐条核对）

### 一、Phase 流程验收 (1-5)
1. `.claude/project-status.md` Phase/文档数/commit数同步
2. R-02/R-02b/R-03/R-04 全部标记"已修复"
3. 搜索 `<!-- R-` 无"待修复"
4. `docs/对话记录/` 含 Phase1-Phase8 完整记录
5. Git milestone: feat(p2)/docs(p3)/chore(rules) 均存在

### 二、PRD 功能验收 (6-10)
6. P0: 登录+JWT+账户+分类+记账+Dashboard 全实现
7. P1: 预算+趋势+分类统计+最近流水 全实现
8. P2: recurring_bill+高级筛选+数据分析 全实现
9. PRD 页面数 = TECH_DESIGN = 实际 views/ 数 = 11
10. 页面命名: PRD ↔ router ↔ views/ 三者一致

### 三、TECH_DESIGN 架构验收 (11-20)
11. AppLayout.vue 存在
12. AppLayout 含 el-header + el-aside + el-main + router-view
13. LoginPage 不套 AppLayout（R-02b 修复项）
14. 除 LoginPage 外全部嵌套在 AppLayout children
15. 页面 ASCII 原型 → 真实落地: 结构/按钮/表格/搜索栏 存在
16. 侧边栏菜单: Dashboard/账户/分类/流水/预算/周期账单/统计/转账/导入/设置
17. 无页面存在但菜单不可达
18. 无菜单存在但页面不存在（幽灵路由）
19. 所有页面有 v-loading 或 loading 状态
20. 所有删除操作有 ElMessageBox.confirm 二次确认

### 四、DATABASE_DESIGN 验收 (21-35)
21. `sql/01-init.sql` 真实执行无报错
22. 数据库名 = `finance_db`
23. SHOW TABLES 返回 6 表
24. transaction.type 支持 income(1)/expense(2)/transfer(transfer_id)
25. **transfer 不进入 Dashboard 支出统计**（SQL 已加 `transfer_id IS NULL`）
26. 所有金额字段: `decimal(12,2)`
27. 查 `float|double` 类型 → 零出现
28. recurring_bill 有 `next_due_date` 字段
29. recurring_bill.period 支持 monthly/weekly
30. 索引: idx_user_id / idx_account_id / idx_category_id / idx_transaction_time / idx_transfer_id
31. category.type 区分 1=支出 2=收入
32. transaction.time 类型 DATETIME
33. create_time/update_time 全部 6 表自动填充
34. SQL 含 `DROP TABLE IF EXISTS` 可重复执行
35. 测试数据真实存在: 2+ users / 4+ accounts / 6+ transactions / 3+ budgets / 3+ recurring_bills

### 五、API_DESIGN 验收 (36-48)
36. API 总数 = 28（与 API_DESIGN.md §2 一致）
37. 所有 URL 与文档一致: `/api/user/login` `/api/account` `/api/transaction` 等
38. HTTP Method: GET(查询) POST(创建) PUT(更新) DELETE(删除) 与文档一致
39. 前端调用的 API 函数名与后端 Controller @RequestMapping 一一对应
40. API_DESIGN.md 版本号与代码同步
41. 分页结构: `{records, total, size, current, pages}` 统一
42. Result 结构: `{code:200, message:"...", data:{}}` 统一
43. 无 data/result 混用
44. ErrorCode 统一: BusinessException(code, msg)
45. 时间格式: `yyyy-MM-dd HH:mm:ss` 全局一致
46. DTO 字段与数据库字段 camelCase 映射一致
47. 无 DTO 漂移（文档与代码字段不同）
48. curl 验证全部 28 接口可访问（P0 11 + P1 13 + P2 4）

### 六、JWT 登录系统验收 (49-60)
49. `POST /api/user/login` 返回 200 + token
50. JWT 含 userId sub claim
51. Axios request.js 拦截器自动注入 `Authorization: Bearer <token>`
52. router.beforeEach 守卫存在且校验 token
53. 未登录访问 `/` → 302/redirect → `/login`
54. token 存 localStorage 键名 `token`
55. 刷新页面后仍保持登录状态
56. token 过期/无效 → 401 → 清 localStorage → 跳 /login
57. logout: 清 localStorage + userStore.clearUser()
58. 密码 BCrypt 加密: `new BCryptPasswordEncoder().encode()`
59. LoginInterceptor 拦截 `/api/**` 除白名单外
60. 白名单: `/api/user/login` `/api/user/register` `/api/health`

### 七、账户模块验收 (61-68)
61. 新增账户: POST /api/account → 200
62. 编辑账户: PUT /api/account/{id} → 200
63. 删除账户: DELETE /api/account/{id} → 200 (软删除 status=0)
64. 删除按钮 `el-button type="danger"`
65. 删除前 ElMessageBox.confirm
66. 账户余额 = initial_balance + 收入 - 支出（含 transfer）
67. account.type 显示: 1=现金 2=银行卡 3=支付宝 4=微信
68. AccountPage 含 4 种类型下拉选择

### 八、分类模块验收 (69-73)
69. 分类列表: GET /api/category → 200 含 13 条种子数据
70. 分类为种子数据（PRD 规定不做增改删）
71. 显示分离: 支出 8 条 + 收入 5 条
72. P0-3 分类 GET 列表接口完整
73. 记账时分类下拉按 type 过滤

### 九、流水模块验收 (74-85)
74. 新增流水: POST /api/transaction → 200
75. 编辑流水: PUT /api/transaction/{id} → 200
76. 删除流水: 通过 status 或直接物理删除
77. 分页查询: GET /api/transaction?pageNum=1&pageSize=10
78. 日期筛选: startTime/endTime 参数
79. 账户筛选: accountId 参数
80. 分类筛选: categoryId 参数
81. transfer: POST /api/transaction/transfer → 一收一支 + transfer_id UUID
82. **transfer 不统计为支出**: SQL 加 `transfer_id IS NULL`
83. 列表按 time DESC 排序
84. 金额符号: 收入正/支出负 或颜色区分
85. 空数据: empty 状态组件或提示文字

### 十、Dashboard 验收 (86-95)
86. 月收入 = SUM(type=1 AND transfer_id IS NULL)
87. 月支出 = SUM(type=2 AND transfer_id IS NULL)
88. 月结余 = 收入 - 支出
89. transfer 必须不进入收支统计（最高优先级检查）
90. 分类占比: ECharts 饼图数据正确
91. 趋势图: 12 个月折线/柱状图
92. 最近流水: 前 5-10 条记录
93. 图表 resize: window resize 时 ECharts resize
94. 空数据: null → BigDecimal.ZERO 兜底
95. 查询性能: 索引覆盖 + 无 N+1

### 十一、前端工程验收 (96-105)
96. 浏览器 console 无 Vue warning
97. 浏览器 console 无 error
98. 无白屏页面
99. 所有路由可正常进入
100. 所有菜单项可点击跳转
101. 所有数据加载有 v-loading 或 loading 状态
102. 所有 el-form 有 rules 校验
103. 按钮有 :loading 防重复提交
104. ref/reactive 使用正确
105. async/await 无遗漏

### 十二、后端工程验收 (106-114)
106. Controller 只做参数校验 + 转发 Service
107. ServiceImpl 最大不超过 350 行
108. 无 N+1 查询（批量加载 + Map 索引）
109. 写操作 @Transactional（跨表操作必加）
110. Mapper XML 全部 `#{}` 参数化
111. GlobalExceptionHandler 含 400/401/403/404/500 + BusinessException
112. 无未捕获 500
113. 无硬编码 URL/密码/密钥
114. 无重复代码块

### 十三、安全验收 (115-120)
115. 无 SQL 注入（全 LambdaQueryWrapper 或 `#{}`）
116. 无 XSS（Vue `{{ }}` 转义 + `@Size` 限制）
117. 无未鉴权 API（LoginInterceptor 全拦截）
118. 密码不返回前端（`@JsonIgnore` on User.password）
119. JWT secret 外部化（`${JWT_SECRET:...}` 环境变量）
120. CORS 配置正确（CorsConfig）

### 十四、构建部署验收 (121-130)
121. `mvn clean compile` → BUILD SUCCESS
122. `mvn test` → 全部通过
123. `pnpm install` → 无报错
124. `pnpm dev` → 启动成功
125. `pnpm build` → 构建成功
126. 前后端可同时启动（8080 + 5173）
127. MySQL 连接成功（finance_db 可达）
128. README: 启动步骤 + SQL 初始化 + 默认账号 + 技术栈
129. DEPLOY.md: 环境要求 + DB 初始化 + 后端启动 + 前端启动 + Nginx 部署 + 测试账号 + FAQ
130. 支持完整演示: 登录→加账户→记账→Dashboard→预算→周期账单

### 十五、Claude Code 流程验收 (131-139)
131. 每个 feature 后 reviewer 已执行
132. reviewer 报告已写入 `docs/对话记录/`
133. 文档与代码一致（无 AI 擅自改文档不写代码）
134. 数据库结构无 AI 擅自修改
135. API 无漂移（文档=代码=前端调用）
136. DTO 无漂移（字段名/类型前后端一致）
137. 无 Context Drift（每轮对话上下文自洽）
138. 阶段性 git commit 记录完整
139. 前后端真实联调通过

---

## 收敛条件（全部满足方可停机）

- [ ] 轮次 ≥ **5**
- [ ] 连续 **3** 轮 PHASE B 零新增 issue
- [ ] 139 项验收 **全部** ✅
- [ ] 编译 + 测试 + API + DB + Git **五维全绿**
- [ ] 无未提交改动
- [ ] 所有 skill 调用均返回通过
- [ ] 最近 5 轮每轮均有至少 1 次有意义的 commit

**停机输出**：
```
╔══════════════════════════════════════════════════╗
║   Q-CRΩ∞Ω v4 企业交付级收敛                       ║
║   总轮次: N · 139项: 全部通过                      ║
║   编译: PASS · 测试: 37/0 · API: 200 · DB: 6表    ║
║   Commits: N · Author: qxw/2501060122             ║
║   嵌套Skill调用: M 次 · 审查报告: K 份              ║
║   结论: 可演示 · 可答辩 · 可交付                    ║
╚══════════════════════════════════════════════════╝
```

---

## 参考权威源（严格遵循）

本 skill 标准**高于**以下所有参考文件：

**项目文档**：`docs/PRD.md` `docs/TECH_DESIGN.md` `docs/DATABASE_DESIGN.md` `docs/API_DESIGN.md` `docs/DEPLOY.md` `docs/00-选题标定.md`

**参考文件夹**：`选题标定-第12题-个人财务记账与分析系统/` 下全部文件（08b 操作流程、08c 命令字典、08e Cookbook、08f FAQ、评分细节.doc、标定卡、角色列表、README）

**AI 规则文件**：`CLAUDE.md` `AGENTS.md` `.claude/project-status.md`

**检查标准优先级**：本文件 139 条 > 评分细节.doc > 参考文件夹文档 > CLAUDE.md > 项目 docs/

---

## 调用

```
/Q-CR
```

无参数。自动执行 ≥5 轮，嵌套全部必要 skill，输出 139 项验收矩阵。
