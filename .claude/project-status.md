# 项目当前进度状态

> 📌 **本文件由 `/rules-updater` 命令每 Phase 末自动重写**(动态状态 · 单独存放在 `.claude/` 目录 · 与根 `CLAUDE.md` 静态规则解耦)。

> 📋 **Phase 编号(0-8 共 9 阶段)**
>
> `0 项目初始化` · `1 SRS + 概要设计` · `2 数据库设计` · `3 API 设计` · `4 后端开发` · `5 前端开发` · `6 集成调试 + 单测` · `7 重构 + 代码审查` · `8 部署 + 文档`

> 📌 **维护分工**
>
> - **本文件结构(字段名 + 顺序)**:由教师项目模板维护(2026-05-12 基线 · 与 `/rules-updater` 输出 100% 对齐)。学生**不要**新增 / 删除 / 重命名字段——改字段会让 `/rules-updater` 输出对不上,后续命令读到错误状态。
> - **字段值更新**:
>   - **Phase 0 → 1 切换**:**手动**改第一行"当前 Phase"为 `Phase 1`(此时 docs/sql/views 还为空,无需扫描;详见 `08b-项目实施操作流程.md §7「必改 2」`)
>   - **Phase 1+ 各 Phase 末**:跑 `/rules-updater` **自动**扫描重写整个文件(详见 `08b-项目实施操作流程.md §8.11 规则 1`)

## 当前状态字段(9 个)

- **当前 Phase**:Phase 8 (Q-CR v16 终极审计 · JWT双重解析修复 · ErrorCode枚举 · 枚举替代魔法值 · ExchangeRate修复 · 测试修复)
- **上次更新**:2026-05-20 (Q-CR v16 — JWT双重解析消除 + ErrorCode集中管理 + 4枚举类 + ExchangeRate updateTime修复 + BudgetSchedulerTest修复 + 149 tests全绿)
- **已完成文档**:PRD.md, TECH_DESIGN.md, DATABASE_DESIGN.md, API_DESIGN.md, DEPLOY.md, PERFORMANCE-REPORT.md
- **数据库表**:user(含role: 0=普通用户/1=管理员), account, category, transaction, budget, recurring_bill
- **已有接口**:30 个接口 (含 /api/health + /api/exchange-rate + 3 个 /api/admin 管理员接口)
- **已完成的后端模块**:UserServiceImpl, AccountServiceImpl, CategoryServiceImpl, TransactionServiceImpl, BudgetServiceImpl, RecurringBillServiceImpl, StatisticsServiceImpl, BudgetScheduler, AdminController
- **已完成的前端页面**:LoginPage, DashboardPage, AccountPage, CategoryPage, TransactionListPage(含URL筛选持久化), BudgetPage, RecurringBillPage, TransferPage, AnalyticsPage, ImportPage, UserSettingsPage, AdminPage
- **Bug修复记录**:11 个 (generate账户校验/预算类型混淆/CSV文件校验/URL筛选持久化/@Transactional缺失/异常传播/AdminService分层/userId统一7处/admin路由守卫/401 redirect/索引重命名)
- **测试**:140 用例 (12 test files · 白盒49+黑盒55+集成19+系统10+CSV7)

## 每 Phase 末该做的事

1. **Phase 0 → 1 切换**:不调用 `/rules-updater`(扫描结果会全为空),只手动改"当前 Phase"为 `Phase 1`,其他字段仍为"无"/"0"
2. **Phase 1+ 各 Phase 末**:跑 `/rules-updater` 自动重写本文件(扫描 docs/ + sql/ + backend/ + frontend/ 重新填字段)
3. 跑 `/git-committer` 把本文件改动 commit + push(commit message 形如 `chore(rules): Phase X 末状态同步`)

> ⚠️ **不要把"技术栈/编码规范/AI 协作约束"等内容写到这里**——那些都在根目录 `CLAUDE.md`(对应单一权威源)。本文件只装"项目当前到哪儿了"的动态状态值。
>
> 📌 **本文件用于记录项目当前进度状态,由 `/rules-updater` 命令自动维护**。学生仅可在 Phase 0 切换至 Phase 1 时手动修改"当前 Phase"字段,其他字段(已完成文档、数据库表、已有接口等)严禁手动修改,否则会导致后续命令读取错误状态。
