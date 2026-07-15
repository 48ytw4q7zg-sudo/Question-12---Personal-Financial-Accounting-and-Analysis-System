# Phase 1 R-02 概要设计审核报告 · 2026-05-16

## 审核元数据
- 审核日期: 2026-05-16
- 使用模型: 当前对话模型（V4 Pro）
- 输入摘要: TECH_DESIGN.md §1-§5（约 270 行）+ PRD.md（R-01 已审已修 · 9 条 issue 全部修复）+ CLAUDE.md §一·一 技术栈基线

## 审核报告

### 维度 1: 架构合理性
- **issue-1** [严重度: 低]: §5「文件上传」选型的「依据」字段指向 `TECH_DESIGN.md §5`（自引用），未指向权威源
  - **位置**: TECH_DESIGN §5 技术方案选型表 · 文件上传行
  - **修复建议**: 将依据改为 `CLAUDE.md §二·五`（文件上传统一为本地 `uploads/` 目录）或 `CLAUDE.md §一·一`

> §1 Mermaid graph LR ✓ · 端口标注 :5173/:8080/:3306 ✓ · §2 后端 8 包齐全 ✓ · 职责描述清晰 ✓ · 关键类覆盖 User/Account/Transaction/Budget/RecurringBill/Category ✓ · 无跨层调用问题 ✓

### 维度 2: 流程完整性
- **issue-2** [严重度: 低]: §4.4 按账户汇总余额流程存在 N+1 查询问题（loop 中每个账户 2 次 SELECT SUM），账户数多时性能劣化
  - **位置**: TECH_DESIGN §4.4 汇总余额 sequenceDiagram · loop 每个账户段
  - **修复建议**: 可用一条 SQL 完成（`SELECT account_id, type, SUM(amount) FROM transaction WHERE account_id IN (...) GROUP BY account_id, type`），再在 Java 内存中聚合。若坚持教学简化不改实现，建议在流程图注释中标注「教学简化：N+1 查询，生产环境应优化为批量查询」
- **issue-3** [严重度: 低]: §4.2 转账流程主流程描述缺少 `note`（备注）参数说明
  - **位置**: TECH_DESIGN §4.2 sequenceDiagram · 入参 `{fromAccountId, toAccountId, amount, note}`
  - **修复建议**: 在 §4.2 流程图前或 PRD P1-5 主流程中明确 `note` 为可选参数（PRD 主流程写的是「金额和备注」，但 TECH_DESIGN 流程图直接用了 `note` 未说明必填/选填）

> 登录流程（4.1）完整（含 BCrypt + JWT + 注册/登录双分支 + 异常分支）✓ · 转账流程（4.2）含 @Transactional 事务 + 余额不足异常 ✓ · 周期性账单一键生成（4.3）含条件 UPDATE 防并发 + 账户状态校验 ✓ · 汇总余额（4.4）纯统计查询 ✓ · 简单 CRUD（账户/分类/多条件筛选）未画流程图（合理）✓

### 维度 3: 路由设计完整性
- **issue-4** [严重度: 低]: §3 路由表 `/transaction/add` 路径使用单数 `transaction`，不符合 §3 path 命名规约「资源页用复数」的要求
  - **位置**: TECH_DESIGN §3 路由表 · `/transaction/add` 行
  - **修复建议**: 改为 `/transactions/add`（同时注意 PRD §5 中相关路径引用也需同步更新）

> 路由表 6 列齐全（路径/组件/守卫/角色限制/实现优先级/备注）✓ · `/login`（公开）+ `/`（AccountPage · 需登录）必含 ✓ · PRD §5 全部 12 个功能页面已覆盖（含 P2）✓ · CategorySelector 不注册路由（合理 · 是可复用组件）✓ · P2-2 预算预警映射到已有 DashboardPage/BudgetPage，无遗漏 ✓

### 维度 4: 技术选型合理性
- **issue-1**（同维度 1 issue-1）: §5「文件上传」依据自引用 TECH_DESIGN.md §5，应指向 CLAUDE.md 权威源
  - **位置**: TECH_DESIGN §5 · 文件上传行
  - **修复建议**: 依据改为 `CLAUDE.md §二·五`

> §5 表格 11 项齐全（含错误码规范 + 日期与精度类型映射）✓ · 每项有「依据」字段 ✓ · 版本号精确到 patch（jjwt 0.13.0 / spring-security-crypto 6.3.4 / Pinia 3.0.4 / Axios 1.15.2）✓ · 无 @PreAuthorize（因未引 spring-boot-starter-security）✓ · 分页用 MP PaginationInnerInterceptor ✓ · 无 Vuex ✓ · 无 OSS ✓ · 全局异常用 @RestControllerAdvice ✓

### 维度 5: 跨文档一致性
- 无 issue

> §2 后端模块与 PRD §2 单一用户角色对应 ✓ · §3 路由与 PRD §5 映射表一一对应（P0-1~P2-5 全覆盖）✓ · §3 实现优先级列与 PRD §5 优先级列一致 ✓ · §4 流程图覆盖 P0 复杂业务（登录/转账/账单生成/余额汇总）✓ · §5 技术选型与 CLAUDE.md §一·一 无版本冲突 ✓ · 角色「普通用户」与 CLAUDE.md 起手段「单一用户角色」一致 ✓

### 维度 6: 跨文档对账（强制 4 类对账）

#### 6.1 角色全集对账
- **参考集**（PRD §2 角色）: [普通用户]
- **被检集**（§3 路由表「角色限制」并集）: [全角色]
- **差集 / 结论**: 差集为空。PRD 定义单一「普通用户」角色，§3 路由表所有需登录页面的「角色限制」均为「全角色」，覆盖该角色。**对账通过**。

#### 6.2 字段引用对账
- **参考集**（PRD 全文出现的业务字段）: username, password, name, initial_balance, balance, type, category_id, account_id, amount, note, time, transfer_id, keyword, startTime, endTime, pageNum, pageSize, year, month, status, next_due_date, currency
- **被检集**（TECH_DESIGN 引用的字段）: username, password, initialBalance, status, amount, type, category_id, account_id, transfer_id, note, fromAccountId, toAccountId, balance, next_due_date
- **差集 / 结论**: TECH_DESIGN 引用的字段均可在 PRD 全文中找到出处。`fromAccountId`/`toAccountId` 是 PRD P1-5 转账入参的具体化。PRD 中 `keyword`/`startTime`/`endTime`/`pageNum`/`pageSize` 等筛选字段在 TECH_DESIGN §3 路由表备注中提及（P1-1 筛选器区域）。**对账通过**。

#### 6.3 API ↔ UI 按钮对账
- **正向**（UI 按钮 → 必有对应 API）:
  - 新增账户 → `POST /api/account` ✓
  - 编辑账户 → `PUT /api/account/{id}` ✓
  - 删除账户 → `DELETE /api/account/{id}` ✓
  - 记一笔 → `POST /api/transaction` ✓
  - 编辑记录 → `PUT /api/transaction/{id}` ✓
  - 筛选 → `GET /api/transaction`（复用 + query params）✓
  - 预算保存 → `POST /api/budget` ✓
  - 创建/编辑周期性账单 → `POST/PUT /api/recurring-bill` ✓
  - 停用周期性账单 → `DELETE /api/recurring-bill/{id}` ✓
  - 生成收支记录 → `POST /api/recurring-bill/{id}/generate` ✓
  - 转账 → `POST /api/transaction/transfer` ✓
  - CSV 导入 → `POST /api/transaction/import` ✓
- **反向**（已声明 API → 必有 UI 入口）:
  - `POST /api/user/register` → LoginPage ✓
  - `POST /api/user/login` → LoginPage ✓
  - `GET /api/category` → CategorySelector 组件（复用）✓
  - `GET /api/statistics/monthly` / `yearly` → DashboardPage ✓
  - `GET /api/statistics/category-summary` → DashboardPage ✓
  - `GET /api/statistics/trend` → AnalyticsPage ✓
  - `GET /api/budget/progress` → BudgetPage ✓
  - `GET /api/budget/alert` → DashboardPage / BudgetPage ✓
  - `GET /api/account/balance` → AccountPage（余额列）✓
- **结论**: 无 UI 缺 API 或 API 缺 UI 入口的情况。**对账通过**。

#### 6.4 登录后跳转对账
- **普通用户**: 登录成功后默认跳转 `/`（AccountPage）· 该 URL 的角色限制 =「全角色」· 包含普通用户 ✓
- **结论**: 守卫不会成环（单一角色 + 单一跳转目标 + 角色限制匹配）。**对账通过**。

### 维度 7: 流程推演（强制 4 个反例）

#### 7.1 路由守卫推演
- **普通用户**: 调用 `POST /api/user/login` 成功 → 前端存 token → 跳转 `/`（AccountPage）→ 守卫检查 `to.meta.requiresAuth` = true + token 存在 → 放行 → 正常渲染。若用户手动访问 `/login` → 守卫检测到 token + 公开页 → 跳转 `/` → 不成环。**推演通过，无死循环风险**。

#### 7.2 并发推演
- **转账流程（§4.2）**: 余额检查（SELECT balance）+ 两条 INSERT 在同一 `@Transactional` 事务内，InnoDB REPEATABLE READ 隔离级别保证并发安全。注释明确说明「利用 InnoDB REPEATABLE READ 隔离级别防止并发转账超支」。**推演通过**。
- **周期性账单一键生成（§4.3）**: `next_due_date` 使用条件 UPDATE（`WHERE next_due_date = 当前值`）+ `affectedRows` 判断。假设两个请求同时进入：第一个 UPDATE affectedRows=1 成功 → 第二个 UPDATE affectedRows=0 → 抛出 BusinessException(5003)。**推演通过，已实现原子条件更新**。
- **余额汇总（§4.4）**: 纯 SELECT 聚合查询，无状态变更，并发安全。**推演通过**。

#### 7.3 删除依赖推演
- **账户删除（P0-2）**: PRD P0-2 异常流程②已定义：删除前检查 transaction 表引用 + recurring_bill 活跃引用，有依赖则拒绝删除。TECH_DESIGN §4 未画账户删除流程图（简单 CRUD），但 PRD 约束明确。**推演通过**。
- **周期性账单停用（P1-4）**: 软删除（改 status=0），无外键级联问题。PRD 已定义「停用后不可恢复」。**推演通过**。

#### 7.4 NULL / 边界推演
- **transaction.account_id**: PRD 明确 account_id 必填（未选择账户 → 400），不可空。**无 NULL 风险**。
- **transaction.transfer_id**: PRD P1-5 业务规则③已定义 NULL 语义：NULL = 普通收支记录；非 NULL = 转账关联记录（标记转出/转入 + 展示关联账户名称）。**推演通过**。
- **account 下无收支记录**: PRD P0-5 业务规则③已定义「账户下无收支记录时，余额 = 初始余额」。TECH_DESIGN §4.4 注释也说明「SUM 返回 NULL 时按 0 处理」。**推演通过**。

## 修复行动建议

本次审核共发现 **4 个 issue**（0 高 / 1 中 / 3 低），整体质量良好。

**按优先级排序:**
1. **issue-1（中）**: §5 文件上传依据自引用 → 立即修复（影响下游 Phase 4 coder 判断依据链）
2. **issue-4（低）**: `/transaction/add` 路径单数 → 建议修复（路由命名一致性）
3. **issue-3（低）**: 转账流程 note 参数说明缺失 → 建议修复（下游接口文档完整性）
4. **issue-2（低）**: 汇总余额 N+1 查询 → 可选优化（教学场景可接受，但建议标注优化方向）

**维度 6/7 全部通过**，无跨文档对账失败或流程推演问题，无需紧急修复。
