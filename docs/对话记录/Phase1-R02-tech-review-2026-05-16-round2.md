# Phase 1 R-02 概要设计审核报告（第二轮） · 2026-05-16

## 审核元数据
- 审核日期: 2026-05-16（第二轮，首轮 R-02 + 应用修复已完成）
- 使用模型: V4 Pro（同源自审）
- 输入摘要: TECH_DESIGN.md §1-§5（已修复 4 条首轮 issue）+ PRD.md（R-01 已审已修）+ CLAUDE.md §一·一 技术栈基线

## 首轮 issue 回归检查

首轮 4 条 R-02 issue 已全部标记"已修复"，逐条验证：
- **issue-1（中）**: §5 文件上传依据 → 已改为 `CLAUDE.md §二·五` ✓
- **issue-2（低）**: §4.4 N+1 查询 → 注释已补充优化方向 ✓
- **issue-3（低）**: §4.2 note 参数 → 已补充"可选参数，允许为空" ✓
- **issue-4（低）**: 路径 `/transaction/add` → 已改为 `/transactions/add` ✓

无回退，首轮修复全部保留。

## 审核报告

### 维度 1: 架构合理性
- 无新 issue

> §1 Mermaid graph LR ✓ · 端口标注 :5173/:8080/:3306 ✓ · §2 后端 8 包齐全（表头 4 列）✓ · 职责描述清晰（controller 参数校验+转发 / service 抛 BusinessException / mapper 用 BaseMapper）✓ · 关键类覆盖 User/Account/Transaction/Budget/RecurringBill/Category ✓ · 无跨层调用问题 ✓

### 维度 2: 流程完整性
- **issue-5** [严重度: 高]: §4.2 转账流程余额检查（SELECT balance）在数据库事务 `=== 事务开始 ===` 之外执行。并发场景下两个请求都能通过余额检查（都看到余额充足），然后各自在事务内 INSERT，导致余额透支。§4.2 注释声称"余额检查在事务内执行"与流程图实际绘制不符，会误导下游 coder
  - **位置**: TECH_DESIGN §4.2 sequenceDiagram · 第 151-157 行（SELECT balance + alt）位于第 159 行 `=== 事务开始 ===` 之前
  - **修复建议**: 将余额检查的 SELECT + alt 移入事务注释范围之内。利用 InnoDB REPEATABLE READ 隔离级别：事务内 SELECT 和 INSERT 同快照，第二个事务会看到第一个事务未提交的锁定行而等待/失败。或者改用原子条件 UPDATE（`UPDATE account SET ... WHERE id=? AND balance >= amount`）+ affectedRows 判断
- **issue-6** [严重度: 中]: §4.3 周期性账单一键生成流程中，条件 UPDATE next_due_date 和 INSERT transaction 之间没有事务包裹注释。若 UPDATE 成功后系统崩溃或 INSERT 失败，到期日已被推进但对应收支记录未生成，该到期日永久丢失
  - **位置**: TECH_DESIGN §4.3 sequenceDiagram · 第 200-208 行（条件 UPDATE → affectedRows 判断 → INSERT transaction）
  - **修复建议**: 在条件 UPDATE 前加 `=== 数据库事务开始 ===` 注释，INSERT transaction 后加 `=== 数据库事务提交 ===` 注释（对齐 §4.2 的事务标注风格），确保下游 coder 用 @Transactional 包裹这两个操作

> 登录流程（4.1）完整 ✓ · 汇总余额（4.4）纯统计查询，无并发问题 ✓ · 简单 CRUD（账户/分类/筛选）未画流程图（合理）✓ · 首轮 issue-3（note 参数说明）已修复保留 ✓

### 维度 3: 路由设计完整性
- **issue-7** [严重度: 低]: §3 路由表 `/transaction`（流水列表页）路径使用单数 `transaction`。首轮 issue-4 仅修复了 `/transaction/add` → `/transactions/add`，但列表页 `/transaction` 未同步改为复数 `/transactions`，导致父子路径不一致
  - **位置**: TECH_DESIGN §3 路由表 · `/transaction` 行（第 51 行）
  - **修复建议**: 将 `/transaction` 改为 `/transactions`，与 `/transactions/add` 保持一致的复数父路径

> 路由表 6 列齐全 ✓ · `/login`（公开）+ `/`（AccountPage · 需登录）必含 ✓ · PRD §5 全部 12 个功能页面已覆盖（含 P2）✓ · 实现优先级列与 PRD §5 一致 ✓ · CategorySelector 不注册路由 ✓ · 守卫规则含登录态校验 + 已登录跳公开页逻辑 ✓

### 维度 4: 技术选型合理性
- 无新 issue

> §5 表格 12 项齐全（含错误码规范 + 日期与精度类型映射）✓ · 每项有「依据」字段（均指向 CLAUDE.md 或 PRD 或 init-skeleton）✓ · 首轮 issue-1 文件上传依据已修复 ✓ · 版本号精确到 patch ✓ · 无 @PreAuthorize ✓ · 分页用 MP PaginationInnerInterceptor ✓ · 无 Vuex ✓ · 无 OSS ✓ · 全局异常用 @RestControllerAdvice ✓

### 维度 5: 跨文档一致性
- 无新 issue

> §2 后端模块与 PRD §2 单一用户角色对应 ✓ · §3 路由与 PRD §5 映射表一一对应（P0-1~P2-5 全覆盖）✓ · §3 实现优先级列与 PRD §5 优先级列一致 ✓ · §4 流程图覆盖 P0 复杂业务 ✓ · §5 技术选型与 CLAUDE.md §一·一 无版本冲突 ✓ · 角色「普通用户」与 CLAUDE.md 起手段「单一用户角色」一致 ✓

### 维度 6: 跨文档对账（强制 4 类对账）

#### 6.1 角色全集对账
- **参考集**（PRD §2 角色）: [普通用户]
- **被检集**（§3 路由表「角色限制」并集）: [全角色]
- **差集 / 结论**: 差集为空。**对账通过**。

#### 6.2 字段引用对账
- **参考集**（PRD 全文业务字段）: username, password, name, initial_balance, balance, type, category_id, account_id, amount, note, time, transfer_id, keyword, startTime, endTime, pageNum, pageSize, year, month, status, next_due_date, currency
- **被检集**（TECH_DESIGN 引用字段）: username, password, initialBalance, status, amount, type, category_id, account_id, transfer_id, note, fromAccountId, toAccountId, balance, next_due_date
- **差集 / 结论**: 所有被检集字段均可在参考集找到出处。`fromAccountId`/`toAccountId` 是 P1-5 转账入参具体化。**对账通过**。

#### 6.3 API ↔ UI 按钮对账
- **正向**（UI 按钮 → API）:
  - 新增账户 → `POST /api/account` ✓ · 编辑账户 → `PUT /api/account/{id}` ✓ · 删除账户 → `DELETE /api/account/{id}` ✓
  - 记一笔 → `POST /api/transaction` ✓ · 编辑记录 → `PUT /api/transaction/{id}` ✓ · 筛选 → `GET /api/transaction` ✓
  - 预算保存 → `POST /api/budget` ✓
  - 创建/编辑周期性账单 → `POST/PUT /api/recurring-bill` ✓ · 停用 → `DELETE /api/recurring-bill/{id}` ✓ · 生成 → `POST /api/recurring-bill/{id}/generate` ✓
  - 转账 → `POST /api/transaction/transfer` ✓ · CSV 导入 → `POST /api/transaction/import` ✓
- **反向**（API → UI 入口）:
  - `POST /api/user/register` → LoginPage ✓ · `POST /api/user/login` → LoginPage ✓
  - `GET /api/category` → CategorySelector ✓
  - `GET /api/statistics/monthly`/`yearly` → DashboardPage ✓ · `GET /api/statistics/category-summary` → DashboardPage ✓ · `GET /api/statistics/trend` → AnalyticsPage ✓
  - `GET /api/budget/progress` → BudgetPage ✓ · `GET /api/budget/alert` → DashboardPage/BudgetPage ✓
  - `GET /api/account/balance` → AccountPage ✓
- **结论**: 无 UI 缺 API 或 API 缺 UI 入口。**对账通过**。

#### 6.4 登录后跳转对账
- **普通用户**: 默认跳转 `/`（AccountPage）· 角色限制 =「全角色」· 包含普通用户 ✓
- **结论**: **对账通过**。

### 维度 7: 流程推演（强制 4 个反例）

#### 7.1 路由守卫推演
- **普通用户**: `POST /api/user/login` 成功 → 存 token → 跳 `/`（AccountPage）→ 守卫 `requiresAuth=true` + token 存在 → 放行。手动访问 `/login` → token 存在 + 公开页 → 跳 `/` → 不成环。**推演通过**。

#### 7.2 并发推演
- **转账余额检查（§4.2）—— 发现问题**:
  - **推演过程**: 假设用户余额 100 元，同时发起两笔各 80 元的转账。请求 A 和 B 的 SELECT balance 均在事务开始之前执行 → A 看到余额 100 ≥ 80 ✓ → B 看到余额 100 ≥ 80 ✓ → A 进入事务 INSERT 两条记录 → A 事务提交 → B 进入事务 INSERT 两条记录 → B 事务提交 → 最终余额 = 100 - 80 - 80 = -60（透支）
  - **根因**: 余额检查在 `=== 事务开始 ===` 注释之前，与事务 INSERT 不在同一事务快照内
  - **后果**: 余额透支，业务规则被违反
  - **修复方向**: 将余额检查 SELECT 移入事务内（REPEATABLE READ 下第二个事务会看到第一个未提交的锁定行）或改用原子条件 UPDATE
  - **结论**: **推演不通过 → 标记为 issue-5（高严重度）**
- **周期性账单生成（§4.3）—— 发现问题**:
  - **推演过程**: 假设 UPDATE next_due_date 成功（到期日从 5/15 推进到 6/15），紧接着 INSERT transaction 时数据库连接断开或异常 → 事务若未包裹则 UPDATE 已持久化 → 到期日 5/15 的记录永远丢失（下次检查直接跳到 6/15）
  - **修复方向**: 用 @Transactional 包裹 UPDATE + INSERT 两个操作
  - **结论**: **推演不通过 → 标记为 issue-6（中严重度）**
- **余额汇总（§4.4）**: 纯 SELECT 聚合，无状态变更。**推演通过**。

#### 7.3 删除依赖推演
- **账户删除（P0-2）**: PRD P0-2 异常流程②已定义删除前检查 transaction + recurring_bill 引用，有则拒绝。TECH_DESIGN §4 未画删除流程图（简单 CRUD），但 PRD 约束明确。**推演通过**。
- **周期性账单停用（P1-4）**: 软删除（status=0），无外键级联问题。**推演通过**。

#### 7.4 NULL / 边界推演
- **transaction.account_id**: PRD 明确必填（未选账户 → 400），不可空。**无 NULL 风险**。
- **transaction.transfer_id**: PRD P1-5 业务规则③已定义 NULL 语义（NULL = 普通记录，非 NULL = 转账关联记录）。**推演通过**。
- **account 下无收支记录**: PRD P0-5 已定义"余额 = 初始余额"，TECH_DESIGN §4.4 注释也说明"SUM 返回 NULL 时按 0 处理"。**推演通过**。

## 修复行动建议

本轮审核共发现 **3 条新 issue**（1 高 / 1 中 / 1 低），叠加首轮 4 条已修复 issue，累计 7 条（1 高 / 2 中 / 4 低）。

**按优先级排序:**
1. **issue-5（高）**: §4.2 转账余额检查在事务外 → **必须立即修复**（并发超支风险 + 流程图注释与实际不符会误导下游 coder）
2. **issue-6（中）**: §4.3 周期性账单 UPDATE + INSERT 无事务包裹 → 建议立即修复（到期日丢失风险）
3. **issue-7（低）**: §3 `/transaction` 未改复数 → 建议修复（路由命名一致性）

**维度 6 全部通过，维度 7 推演出 2 个并发问题**（issue-5 + issue-6），均属高/中严重度。
