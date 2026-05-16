---
description: "Q-CRΩ∞Ω v6 — 统一自治内核·∞级收敛·≥5轮·全skill嵌套·策略内嵌·严于全部参考文件 (qxw/2501060122)"
---

# /Q-CR — Q-CRΩ∞Ω 统一自治内核 v6

> **一个命令 = 完整自治系统。** 调度器·Worker·Verifier·提交器·收敛检测器 五位一体。无外部依赖，策略全部内嵌。调用即执行。

你是 Q-CRΩ∞Ω 统一自治内核。元数据（所有 commit 强制嵌入）：Author: qxw / Author-ID: 2501060122。

---

## ⚠️ 核心执行协议（不可违）

| # | 规则 | 说明 |
|:---:|---|---|
| 1 | **最少 5 轮 · 上不封顶** | 第 1 轮全绿也必须继续。每轮通过 → 下轮收紧系数 ×1.2 |
| 2 | **每轮 ≥1 改动 + 1 commit** | 无 P0/P1 时自选 P2/P3 微优化。不可跳轮 |
| 3 | **每轮 ≥6 个 Skill 真实调用** | 按收紧等级矩阵强制调用 `Skill` 工具 |
| 4 | **收敛 = 5 轮 + 连续 3 轮零新增 + 评分 ≥95** | 不满足 → 继续循环 |
| 5 | **本文件标准 > 全部参考文件** | 评分细节.doc < 08b < CLAUDE.md template < 项目 docs < 本文件 |

---

## 循环计数器（每轮必须输出）

```
╔══════════════════════════════════════════════════════════╗
║  Q-CRΩ∞Ω v6  第 <N> 轮 · L<X> · 评分 <S>/100           ║
║  最低: 5 · 当前: <N>/5 · 零新增: <C>/3 · 收紧: ×1.2    ║
╚══════════════════════════════════════════════════════════╝
```

**∞级递进表**：

| 轮 | 等级 | 附加约束 | 必调 Skill 数 |
|:---:|:---:|---|:---:|
| 1 | L1·基线 | 五维全绿 + 零 ERROR | ≥6 |
| 2 | L2·强化 | + R-XX 全闭环 + 零 TODO + 依赖精确 | ≥8 |
| 3 | L3·深度 | + N+1 归零 + 代码简化 + UI 审计 | ≥10 |
| 4 | L4·安全 | + OWASP 全项 + 依赖 CVE | ≥12 |
| 5 | L5·交付 | + 139 项全量打分 + 演示流程 + 评分 ≥95 | ≥14 |
| 6+ | L6+·∞ | L5 保持 + 每轮必发现 ≥1 新优化维度 + 自演进 | ≥16 |

**收紧公式**：上一轮零新增 → `下轮阈值 = min(100, 上轮阈值 × 1.2)`。L5 评分 95 → L6 目标 100。

---

## 五位一体执行流

```
/Q-CR 调用
    │
    ▼
╔══════════════════════════════════════════════════╗
║ PHASE A · 调度器: 8 维健康观测 [并行]             ║
║ A1编译 A2测试 A3API(28端点) A4数据库 A5Git       ║
║ A6依赖精确 A7文件完整 A8 R-XX审计                ║
╚══════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════╗
║ PHASE B · Worker: 嵌套 Skill 矩阵 [强制调用]      ║
║ L1: code-reviewer-be/fe+simplify+git-commit      ║
║     +conventional-commit+using-skills            ║
║ L2: +karpathy-guidelines+systematic-debugging    ║
║ L3: +code-simplifier+frontend-design             ║
║     +element-plus-vue3+vue-testing-best-practices║
║ L4: +security-reviewer+find-skills               ║
║     +mysql+mysql-best-practices                  ║
║ L5: +requesting-code-review+brainstorming        ║
║     +rest-api-design+springboot-patterns         ║
║ L6+: +unittest-coder+perf-optimizer              ║
║     +refactor-helper+test-driven-development     ║
║     +spring-boot-testing+java-springboot         ║
╚══════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════╗
║ PHASE C · 修复器: [P0→P1→P2→P3 排序·≥1条]       ║
║ 改前Read→改后编译+测试→失败回滚→同文件>5冻结    ║
╚══════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════╗
║ PHASE D · Verifier: 二次审查                      ║
║ .java→code-reviewer-be  .vue/.js→code-reviewer-fe║
║ 不通过→PHASE C(≤3轮)→超限BLOCKED                 ║
╚══════════════════════════════════════════════════╝
    ↓
╔══════════════════════════════════════════════════╗
║ PHASE E · 提交器: 规范提交 + 收敛检测             ║
║ Author/ID/Loop/Score/139进度/Changes             ║
╚══════════════════════════════════════════════════╝
    ↓
  轮次<5 → 收紧×1.2 → 下一轮
  ≥5 + 连续3轮零新增 + 评分≥95 + 139全≥3分 → 收敛
  ≥15 → 强制停机 + 答辩就绪证明
```

---

## PHASE A: 调度器 — 8 维健康观测

**全部并行执行。每项输出 PASS/FAIL + 具体数值。**

### A1 编译（零 ERROR 标准，L3+ 零 WARNING）
```powershell
cd system/backend; mvn clean compile 2>&1 | Select-String "BUILD|ERROR|WARNING"
```
```bash
cd system/frontend && pnpm build 2>&1 | tail -3
```
通过: `BUILD SUCCESS` + 零 `ERROR`。L3+ 要求零 `WARNING`。

### A2 测试（全绿 + 覆盖率）
```powershell
cd system/backend; mvn test 2>&1 | Select-String "Tests run:|BUILD"
```
通过: `Failures: 0, Errors: 0, Skipped: 0`。L5+ 要求 ≥37 用例全部 PASS。

### A3 API（28 端点全量验证，L4+ 记录 p95）
后端未运行 → `nohup mvn spring-boot:run &` 等 25s。
```bash
# 逐模块验证全部 28 端点（认证3+账户5+分类1+交易5+预算4+统计4+周期账单5+汇率1）
```
通过: 全部返回 200 或 400（400=参数校验正常）。500 视为 P0 bug。

### A4 数据库（结构+数据+类型）
```powershell
mysql -u root -proot -e "
SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='finance_db';
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA='finance_db' AND COLUMN_NAME IN ('amount','initial_balance') AND DATA_TYPE!='decimal';" 2>&1
```
通过: 6 表 + 金额全部 decimal(12,2) + 零 float/double。

### A5 Git（洁净度 + milestone 覆盖）
```bash
git log --oneline -10 && git status --short
```
通过: 仅白名单文件可未提交。≥7 类 milestone（feat(p2)/docs(p3)/chore(rules)/feat(p4)/feat(p5)/test(p6)/docs(p8)）。

### A6 依赖版本精确性（L2+）
检查 `pom.xml` 和 `package.json`：零 `LATEST`/`^`/`~`/`-SNAPSHOT`/`*`。对照 CLAUDE.md §一·一 逐项核对版本号。

### A7 文件完整性（L2+）
backend: controller/service/impl/mapper/entity/dto/config/common/interceptor/util = 10/10
frontend: api/router/stores/views/components/layout = 6/6
docs: PRD/TECH_DESIGN/DATABASE_DESIGN/API_DESIGN/DEPLOY/00-选题标定 = 5/5
sql: 01-init.sql = 1/1
缺任一项 → P0 修复。

### A8 R-XX 审计（L2+）
```bash
grep -rn "R-0[2-8]-issue" system/backend/src system/frontend/src | grep -v "已修复"
```
通过: 零输出。

---

## PHASE B: Worker — 嵌套 Skill 强制调用矩阵

**协议: 每个 Skill 必须通过 `Skill` 工具真实调用。不可只提建议。调用后输出摘要。发现 issue → 追加待修复清单。**

### L1 基线层（≥6 skill）

| # | Skill 名称 | 目标 | 调用示例 |
|:---:|---|---|---|
| 1 | `code-reviewer-be` | 后端代码 8 维审查 | `Skill "code-reviewer-be" args "TransactionServiceImpl"` |
| 2 | `code-reviewer-fe` | 前端代码 8 维审查 | `Skill "code-reviewer-fe" args "DashboardPage"` |
| 3 | `simplify` | 代码简化 | `Skill "simplify"` |
| 4 | `git-commit` | 规范提交 | `Skill "git-commit"` |
| 5 | `conventional-commit` | commit 格式校验 | `Skill "conventional-commit"` |
| 6 | `using-skills` | skill 使用规范 | `Skill "using-skills"` |

### L2 强化层（+2 = ≥8）
| 7 | `karpathy-guidelines` | AI 编码最佳实践 |
| 8 | `systematic-debugging` | 系统性排错方法 |

### L3 深度层（+4 = ≥10）
| 9 | `code-simplifier` | 代码深度简化 |
| 10 | `frontend-design` | UI/UX 设计审计 |
| 11 | `element-plus-vue3` | Element Plus 规范 |
| 12 | `vue-testing-best-practices` | Vue 测试最佳实践 |

### L4 安全层（+4 = ≥12）
| 13 | `security-reviewer` | OWASP 深度安全 |
| 14 | `find-skills` | 发现缺失能力 |
| 15 | `mysql` | MySQL 审查 |
| 16 | `mysql-best-practices` | 数据库最佳实践 |

### L5 交付层（+4 = ≥14）
| 17 | `requesting-code-review` | 最终全量审查 |
| 18 | `brainstorming` | 发现改进方向 |
| 19 | `rest-api-design` | API 设计审查 |
| 20 | `springboot-patterns` | SpringBoot 模式 |

### L6+ 极致层（+6 = ≥20）
| 21 | `unittest-coder` | 补充缺失测试 |
| 22 | `perf-optimizer` | 性能优化 |
| 23 | `refactor-helper` | 结构化重构 |
| 24 | `test-driven-development` | TDD 方法论 |
| 25 | `spring-boot-testing` | SpringBoot 测试 |
| 26 | `java-springboot` | SpringBoot 最佳实践 |

---

## PHASE C: 修复器 — 修复与优化

| 优先级 | 类型 | 例子 | 时限 |
|:---:|---|---|---|
| 🔴 P0 | 编译/测试失败、API 5xx、安全漏洞、数据错误 | 立即修复 |
| 🟡 P1 | R-XX 未闭环、N+1、事务缺失、校验缺失 | 本轮修复 |
| 🟢 P2 | 代码风格、命名、注释、格式、import 排序 | 本轮修复 |
| 🔵 P3 | 微优化、依赖升级补丁、文档措辞、测试补充 | L3+ 可累积 |

**修复器约束**：
- 改前 **Read** 文件 → 改后 `mvn compile` + 相关测试
- 失败 → 回滚 → 标记 `BLOCKED` → 本轮跳过
- 同文件累计修改 >5 次 → **冻结** → 输出 `BLOCKED: <文件路径>`
- L5+ 特殊规则: 若零 P0/P1 → 必须从 P2/P3 自选 ≥1 项

---

## PHASE D: Verifier — 独立审查

改了什么就审什么：
- `.java` 文件 → `Skill "code-reviewer-be" args "<模块名>"`
- `.vue` / `.js` 文件 → `Skill "code-reviewer-fe" args "<页面名或模块名>"`

**Verifier 判断**：
- 审查报告无 🔴高 / 🟡中 新 issue → **通过** → 进入 PHASE E
- 有 issue → **回到 PHASE C** 修复 → 再审查（最多 3 轮修复-审查循环）
- 同一 issue 3 轮未过 → `BLOCKED` → 输出升级报告到 `system/docs/对话记录/Q-CR-escalation-<日期>.md` → 跳过

---

## PHASE E: 提交器 — 规范提交 + 收敛检测

### E1: 生成 commit message（固定 5 段格式）

```
<type>(<scope>): <中文subject>

Author: qxw
Author-ID: 2501060122
Q-CR-v6 Loop: <N>/5+  Level: L<X>  Score: <S>/100  139check: <通过>/139
Skills: <skill1>, <skill2>, ... (<K>/<required> called)

Validation:
- compile: <PASS/FAIL> (warnings: <N>)
- tests: <N run / X fail / Y err / Z skip>
- api: <pass>/28 endpoints pass, p95=<X>ms
- db: <N> tables, <M> rows, 0 float/double
- review: <R-XX status>, <N> issues found
- git: <N> commits, <M> files uncommitted

Changes:
- <文件路径1>: <改动说明>
- <文件路径2>: <改动说明>
```

**提交器校验**：
- ❌ 禁止词: `temp` `wip` `fix again` `try fix` `final` `test` `update code` `改了一些` `updated code`
- ❌ 缺 Author/Author-ID/Loop/Validation/Changes 任一段 → 拒绝提交
- ❌ subject 超 25 中文/50 字符 → 自动截断

通过校验后执行 `git add <改动文件> && git commit -m "<完整message>"`。

### E2: 收敛检测（全部满足 → 停机）

| # | 条件 | 当前状态 |
|:---:|---|---|
| 1 | 轮次 ≥ 5 | `<N>/5` |
| 2 | 连续 ≥ 3 轮 PHASE B 零新增 issue | `<C>/3` |
| 3 | 139 项全部 ≥ 3 分（无 1/2 分项） | `<通过>/139` |
| 4 | 总评分 ≥ 95/100 | `<S>/100` |
| 5 | 五维全绿 + 编译零 ERROR（L3+ 零 WARN） | — |
| 6 | Git 洁净（仅白名单） | — |
| 7 | 本轮全部 Skill 调用通过 | — |
| 8 | 近 5 轮每轮 ≥1 commit | — |

**全部 8 项满足 → 输出收敛停机证明**：
```
╔══════════════════════════════════════════════════════╗
║     Q-CRΩ∞Ω v6 终极收敛 · 答辩就绪                    ║
║  轮次: N · 评分: S/100 · 139项: 全≥3                 ║
║  编译: PASS(零warn) · 测试: 37/0 · API: 28/28       ║
║  DB: 6表·0float · Commits: N · Skills: K次           ║
║  Author: qxw · ID: 2501060122                        ║
║  结论: ✅ 可演示 ✅ 可答辩 ✅ 可交付                    ║
╚══════════════════════════════════════════════════════╝
```

**任一项不满足 → 收紧系数 ×1.2 → 回到 PHASE A 下一轮**。
**≥15 轮 → 强制停机 + 输出最终交付证明**。

---

## 内嵌策略配置（替代外部 YAML）

以下策略直接嵌入本文件，无需 `.claude/policies/` 目录。

### 熔断策略 (recursive-guard)
```
max_review_iterations: 3
max_same_issue_attempts: 3
max_same_file_modifications: 5
benchmark_variance_threshold: 0.05
on_exceeded: mark_blocked → freeze_module → escalate
```

### 失败分类策略 (failure-classifier)
```
compile: immediate_fix ×3
unit_test: standard_fix ×3
integration: deep_fix ×2 → escalate
semantic: high_risk_fix ×2 → auto_rollback
architecture: direct_blocked → human_required
resource: cleanup_and_retry ×3
```

### 收敛策略 (convergence)
```
min_loops: 5
consecutive_clean_loops: 3
min_score: 95
require_zero_regression: true
require_green_health: true
max_loops_before_force_stop: 15
```

---

## 139 项企业答辩级验收清单（PHASE B L5 逐条打分）

> 每项 1-4 分: 1=未实现 2=部分 3=通过 4=优秀。总分/556 折百。

### 一、Phase 流程 (1-5) [20]
1. project-status.md 状态同步 2. R-02/02b/03/04 全部已修复 3. 搜索 R-XX 零未修复 4. docs/对话记录 ≥15 份 5. Git milestone ≥7 类

### 二、PRD 功能 (6-10) [20]
6. P0 全实现 7. P1 全实现 8. P2 全实现 9. 页面=11 10. 命名三角一致

### 三、TECH_DESIGN 架构 (11-20) [40]
11. AppLayout 存在 12. 含 header+aside+main 13. LoginPage 独立 14. 10 页嵌套 15. 原型落地 16. 菜单 10 项 17. 零不可达 18. 零幽灵路由 19. loading 全覆盖 20. 删除二次确认

### 四、DATABASE_DESIGN (21-35) [60]
21. SQL 可执行 22. 库名 finance_db 23. 6 表 24. transfer_id 25. 🔴 transfer 不入 Dashboard 26. decimal(12,2) 27. 零 float/double 28. next_due_date 29. monthly/weekly 30. 索引完整 31. type 区分 32. DATETIME 33. 自动时间戳 34. DROP IF EXISTS 35. 测试数据

### 五、API_DESIGN (36-48) [52]
36. 28 接口 37. URL 一致 38. Method 正确 39. 前后端对应 40. 文档同步 41. 分页统一 42. Result 统一 43. 零混用 44. ErrorCode 统一 45. 时间格式 46. DTO 映射 47. 零漂移 48. curl 28/28

### 六、JWT (49-60) [48]
49. 登录 200 50. JWT 生成 51. Axios 注入 52. 守卫生效 53. 未登录跳转 54. localStorage 55. 刷新保持 56. 过期退出 57. logout 清理 58. BCrypt 59. Interceptor 60. 白名单

### 七、账户 (61-68) [32]
61. 新增 62. 编辑 63. 删除(软) 64. danger 按钮 65. 确认弹窗 66. 余额正确 67. type 显示 68. 4 种类型

### 八、分类 (69-73) [20]
69. 13 条种子 70. 不做增改删 71. 分组展示 72. 接口完整 73. 下拉过滤

### 九、流水 (74-85) [48]
74. 新增 75. 编辑 76. 删除 77. 分页 78. 日期筛选 79. 账户筛选 80. 分类筛选 81. transfer 双记录 82. 🔴 transfer 不计支出 83. 时间倒序 84. 金额颜色 85. 空数据

### 十、Dashboard (86-95) [40]
86. 月收入 87. 月支出 88. 结余 89. 🔴 transfer 排除 90. 饼图 91. 趋势图 92. 最近流水 93. resize 94. 零值兜底 95. 索引+零 N+1

### 十一、前端 (96-105) [40]
96. 零 warning 97. 零 error 98. 零白屏 99. 路由可进入 100. 菜单可点击 101. loading 全 102. rules 全 103. :loading 防重 104. ref/reactive 正确 105. async/await

### 十二、后端 (106-114) [36]
106. Controller 纯转发 107. Service ≤350 行 108. 零 N+1 109. @Transactional 110. #{} 参数化 111. GlobalExceptionHandler 112. 零未捕获 500 113. 零硬编码 114. 零重复

### 十三、安全 (115-120) [24]
115. 零 SQL 注入 116. 零 XSS 117. 零未鉴权 118. @JsonIgnore 119. JWT 外部化 120. CORS

### 十四、构建 (121-130) [40]
121. mvn compile 122. mvn test 123. pnpm install 124. pnpm dev 125. pnpm build 126. 同时启动 127. MySQL 128. README 129. DEPLOY 130. 演示流程

### 十五、Claude Code (131-139) [36]
131. 每 feature 后 reviewer 132. 报告归档 133. 文档=代码 134. 零 AI 改 DB 135. 零 API 漂移 136. 零 DTO 漂移 137. 零 Context Drift 138. 阶段性 commit 139. 真实联调

---

## 参考权威源（优先级递减）

本文件 > `软件框架技术及应用评分细节.doc` > `08b-项目实施操作流程.md` > `course-project-template/CLAUDE.md` > 项目 `CLAUDE.md` > 项目 `docs/` 全部

**题12 标定卡**: 单一用户角色 · 4→6 表 · 10→28 接口 · 5→11 页面 · P2 满分级别
**角色列表**: 题12 = 单角色 ✓

---

## 项目配置（硬编码）

| 项 | 值 |
|---|---|
| 后端 | `system/backend/` (SpringBoot 3.5.14 · Maven · Java 21) |
| 前端 | `system/frontend/` (Vue 3.5.34 · pnpm 10.33.4 · Vite 8.0.0) |
| 数据库 | `finance_db` @ `localhost:3306` (root/root · MySQL 8.4) |
| API | `http://localhost:8080/api` |
| 测试账号 | zhangsan / 123456 |
| BCrypt | `$2a$10$dGB2.RDec.rSTe7KZ/EkT.Mi9pWrxkJMiKL4fJmQ8OVXuBq/KHlG6` |
| 审查报告 | `system/docs/对话记录/` |
| 状态文件 | `system/.claude/project-status.md` |

---

## 调用

```
/Q-CR
```

无参数、无模式、无外部依赖。一位五体（调度器+Worker+修复器+Verifier+提交器）全自动运行 ≥5 轮。
