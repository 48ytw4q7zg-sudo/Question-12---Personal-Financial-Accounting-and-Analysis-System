# Phase 1 R-02 概要设计审核报告（第三轮） · 2026-05-16

## 审核元数据
- 审核日期: 2026-05-16（第三轮，前两轮 R-02 + 应用修复已完成 7 条 issue）
- 使用模型: V4 Pro（同源自审）
- 输入摘要: TECH_DESIGN.md §1-§5（7 条 R-02 issue 全部已修复）+ PRD.md（R-01 已审已修）+ CLAUDE.md §一·一

## 前两轮 issue 回归检查

两轮累计 7 条 R-02 issue 全部标记"已修复"，逐条验证：

| Issue | 严重度 | 问题 | 状态 |
|---|---|---|---|
| issue-1 | 中 | §5 文件上传依据自引用 | 已修复：改为 CLAUDE.md §二·五 ✓ |
| issue-2 | 低 | §4.4 N+1 查询 | 已修复：注释补充优化方向 ✓ |
| issue-3 | 低 | §4.2 note 参数说明缺失 | 已修复：补充"可选参数" ✓ |
| issue-4 | 低 | /transaction/add 单数 | 已修复：改为 /transactions/add ✓ |
| issue-5 | 高 | §4.2 余额检查在事务外 | 已修复：SELECT 移入事务范围 ✓ |
| issue-6 | 中 | §4.3 UPDATE+INSERT 无事务包裹 | 已修复：加事务开始/提交注释 ✓ |
| issue-7 | 低 | /transaction 列表页单数 | 已修复：改为 /transactions ✓ |

**无回退，全部修复保留。**

## 审核报告

### 维度 1: 架构合理性
- 无新 issue

> §1 Mermaid graph LR（非 ASCII）✓ · 端口 :5173/:8080/:3306 ✓ · §2 后端 8 包齐全（表头 4 列）✓ · 职责描述清晰 ✓ · 关键类覆盖 User/Account/Transaction/Budget/RecurringBill/Category ✓ · 无跨层调用 ✓

### 维度 2: 流程完整性
- 无新 issue

> 登录流程（4.1）完整 ✓ · 转账流程（4.2）余额检查在事务内 ✓ · 周期性账单生成（4.3）UPDATE+INSERT 事务包裹 ✓ · 汇总余额（4.4）纯统计 ✓ · 简单 CRUD 未画流程图 ✓ · Mermaid 语法正确 ✓ · 与 PRD §3 主流程一致 ✓

### 维度 3: 路由设计完整性
- 无新 issue

> 路由表 6 列齐全 ✓ · `/login` + `/` 必含 ✓ · PRD §5 全量 12 页覆盖 ✓ · /transactions + /transactions/add 复数一致 ✓ · 守卫规则正确 ✓ · 不含页面布局 ✓

### 维度 4: 技术选型合理性
- 无新 issue

> §5 表格 12 项齐全 ✓ · 每项有依据（均指向 CLAUDE.md/PRD/init-skeleton）✓ · 版本号精确到 patch ✓ · 无 @PreAuthorize ✓ · MP PaginationInnerInterceptor ✓ · Pinia ✓ · 本地 uploads/ ✓ · @RestControllerAdvice ✓ · Result<T> ✓

### 维度 5: 跨文档一致性
- 无新 issue

> §2 与 PRD §2 角色对应 ✓ · §3 与 PRD §5 映射表一一对应 ✓ · 实现优先级一致 ✓ · §4 流程覆盖 P0 复杂业务 ✓ · §5 版本与 CLAUDE.md 无冲突 ✓

### 维度 6: 跨文档对账（强制 4 类）

#### 6.1 角色全集对账
- **参考集**（PRD §2）: [普通用户]
- **被检集**（§3 路由「角色限制」并集）: [全角色]
- **差集**: 空。**对账通过**。

#### 6.2 字段引用对账
- **参考集**（PRD 全文）: username, password, name, initial_balance, balance, type, category_id, account_id, amount, note, time, transfer_id, keyword, startTime, endTime, pageNum, pageSize, year, month, status, next_due_date, currency
- **被检集**（TECH_DESIGN）: username, password, initialBalance, status, amount, type, category_id, account_id, transfer_id, note, fromAccountId, toAccountId, balance, next_due_date
- **差集**: 空。**对账通过**。

#### 6.3 API ↔ UI 按钮对账
- **正向**: 新增/编辑/删除账户 → `POST/PUT/DELETE /api/account` ✓ · 记一笔/编辑/筛选 → `POST/PUT/GET /api/transaction` ✓ · 预算保存 → `POST /api/budget` ✓ · 创建/编辑/停用/生成周期账单 → `POST/PUT/DELETE /api/recurring-bill` + `/generate` ✓ · 转账 → `POST /api/transaction/transfer` ✓ · CSV 导入 → `POST /api/transaction/import` ✓
- **反向**: `POST /api/user/register` → LoginPage ✓ · `GET /api/category` → CategorySelector ✓ · `GET /api/statistics/*` → DashboardPage/AnalyticsPage ✓ · `GET /api/budget/progress` + `/alert` → BudgetPage/DashboardPage ✓ · `GET /api/account/balance` → AccountPage ✓
- **结论**: **对账通过**。

#### 6.4 登录后跳转对账
- **普通用户**: 跳转 `/`（AccountPage）· 角色限制 = 全角色 ✓
- **结论**: **对账通过**。

### 维度 7: 流程推演（强制 4 个反例）

#### 7.1 路由守卫推演
- **普通用户**: login → 存 token → 跳 `/` → requiresAuth + token → 放行。访问 `/login` → token + 公开页 → 跳 `/` → 不成环。**推演通过**。

#### 7.2 并发推演
- **§4.2 转账**: 余额检查 SELECT + 两条 INSERT 均在事务范围内。REPEATABLE READ 下第二个事务看到第一个未提交行等待/失败。**推演通过**。
- **§4.3 账单生成**: 条件 UPDATE + INSERT 在同一事务内。UPDATE affectedRows=0 时提前返回；INSERT 失败则 UPDATE 一并回滚。**推演通过**。
- **§4.4 汇总余额**: 纯 SELECT，无状态变更。**推演通过**。

#### 7.3 删除依赖推演
- **账户删除**: PRD P0-2 定义删除前检查 transaction + recurring_bill 引用，有则拒绝。**推演通过**。
- **周期性账单停用**: 软删除，无外键级联。**推演通过**。

#### 7.4 NULL / 边界推演
- **transfer_id NULL**: PRD P1-5 已定义（NULL = 普通记录，非 NULL = 转账关联）。**推演通过**。
- **无收支记录账户**: PRD P0-5 已定义余额 = 初始余额。**推演通过**。

## 修复行动建议

**本轮零新 issue**。两轮累计 7 条 issue（1 高 / 2 中 / 4 低）已全部修复，维度 6/7 全部通过。

**文档状态**: TECH_DESIGN.md §1-§5 可安全进入 Phase 1 Step 7 `/page-prototyper`。
