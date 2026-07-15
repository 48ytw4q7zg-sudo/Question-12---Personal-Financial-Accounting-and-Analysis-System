# Phase 1 R-02 概要设计审核报告（第四轮）· 2026-05-16

## 审核元数据
- 审核日期: 2026-05-16（第四轮，前三轮累计 7 条 issue 全部已修复）
- 使用模型: 当前对话模型
- 输入摘要: TECH_DESIGN.md §1-§5（约 270 行）+ PRD.md（R-01 已审已修 · 18 个功能 · 6 节结构）+ CLAUDE.md §一·一 技术栈基线
- 前三轮已审复核: 是（读过 R-02 round1-3 全部 issue 注释 · 7 条全部标注「已修复」· 本轮不重复审已修复 issue）

## 前三轮 issue 回归检查

| Issue | 严重度 | 问题 | 状态 |
|---|---|---|---|
| issue-1 | 中 | §5 文件上传依据自引用 | 已修复 ✓ |
| issue-2 | 低 | §4.4 N+1 查询 | 已修复 ✓ |
| issue-3 | 低 | §4.2 note 参数说明缺失 | 已修复 ✓ |
| issue-4 | 低 | /transaction/add 单数路径 | 已修复 ✓ |
| issue-5 | 高 | §4.2 余额检查在事务外 | 已修复 ✓ |
| issue-6 | 中 | §4.3 UPDATE+INSERT 无事务包裹 | 已修复 ✓ |
| issue-7 | 低 | /transaction 列表页单数路径 | 已修复 ✓ |

无回退，全部修复保留。

## 审核报告

### 维度 1: 架构合理性
- 无新 issue

> §1 Mermaid graph LR（非 ASCII）✓ · 端口 :5173/:8080/:3306 ✓ · §2 后端 8 包齐全（表头 4 列）✓ · 职责描述清晰（controller 参数校验+转发 / service 抛 BusinessException / mapper 用 BaseMapper）✓ · 关键类覆盖 User/Account/Transaction/Budget/RecurringBill/Category ✓ · 无跨层调用 ✓

### 维度 2: 流程完整性
- **issue-1** [严重度: 中]: §4.1 登录流程缺少 `userStore.setUser()` 步骤，与 tech-designer §一 规约「前端 3 步：① token 写入 localStorage + ② userStore.setUser(user) Pinia 持久化方案 A + ③ 跳转」不一致
  - **位置**: TECH_DESIGN §4.1 sequenceDiagram · 第 129 行（C->>C: 存 localStorage('token') → 跳转 redirect 或 '/'）
  - **问题**: 流程图中前端收到 Result.success(LoginResponse) 后仅执行"存 localStorage token → 跳转"两步，缺失 tech-designer 规约要求的 `userStore.setUser(user)` 步骤。下游 login-coder 若仅按此流程图实现，会漏写 Pinia userStore 调用，导致后续页面 `useUserStore().username` 为空
  - **修复建议**: C->>C 步骤改为三步：① `localStorage.setItem('token', token)` ② `userStore.setUser({userId, username})`（Pinia 持久化方案 A）③ `router.push(redirect || '/')`

- **issue-2** [严重度: 中]: §4.1 LoginResponse 仅返回 `{token, userId}`，缺少 username，导致前端 userStore 无法获取用户名
  - **位置**: TECH_DESIGN §4.1 sequenceDiagram · 第 127 行（`S-->>Ctrl: LoginResponse {token, userId}`）
  - **问题**: PRD P1-7 已移除 `GET /api/user/info` 接口（用户名从 JWT 解码获取），§3 路由守卫和 §6 UserSettingsPage 均需要用户名显示。但 LoginResponse 只含 token 和 userId，JWT payload 也只含 userId（§4.1 第 126 行 `JwtUtils.generateToken(userId)`）。前端无法从现有数据流获取用户名：JWT 解码只有 userId、无单独接口、LoginResponse 无 username。需二选一修复：(A) LoginResponse 增加 username 字段 `LoginResponse {token, userId, username}`；或 (B) JWT payload 增加 username `JwtUtils.generateToken(userId, username)` 前端从 token 解码
  - **修复建议**: 方案 A（推荐，不改 JWT）：LoginResponse 增加 username，同时 §4.1 C->>C 步骤同步更新 userStore 存储 `{userId, username}`

> 登录流程（4.1）整体完整（BCrypt + JWT + 注册/登录双分支 + 异常分支）✓ · 转账流程（4.2）余额检查在事务内 + 条件 UPDATE 防并发 ✓ · 周期性账单生成（4.3）条件 UPDATE + INSERT 事务包裹 ✓ · 汇总余额（4.4）纯统计 + N+1 注释说明 ✓ · 简单 CRUD 未画流程图 ✓

### 维度 3: 路由设计完整性
- 无新 issue

> 路由表 6 列齐全（路径/组件/守卫/角色限制/实现优先级/备注）✓ · `/login`（公开）+ `/`（DashboardPage · 需登录）必含 ✓ · PRD §5 全部 18 个功能对应页面已覆盖 ✓ · CategorySelector 不注册路由（可复用组件）✓ · 守卫规则含登录态校验 + 已登录跳公开页逻辑 ✓ · 单一用户角色无需角色不匹配分支 ✓

### 维度 4: 技术选型合理性
- 无新 issue

> §5 表格 12 项齐全（含错误码规范 + 日期与精度类型映射 + 文件上传）✓ · 每项有「依据」字段（均指向 CLAUDE.md/PRD/init-skeleton）✓ · 版本号精确到 patch（jjwt 0.13.0 / spring-security-crypto 6.3.4 / Pinia 3.0.4 / Axios 1.15.2）✓ · 无 @PreAuthorize ✓ · MP PaginationInnerInterceptor ✓ · Pinia（非 Vuex）✓ · 本地 uploads/（非 OSS）✓ · @RestControllerAdvice ✓ · Result<T> ✓

### 维度 5: 跨文档一致性
- 无新 issue（但维度 2 issue-2 同时涉及跨文档一致性：LoginResponse 字段与 PRD P1-7 用户名获取方式的衔接）

> §2 后端模块与 PRD §2 单一用户角色对应 ✓ · §3 路由与 PRD §5 映射表一一对应（P0-1~P2-5 + P0-6 + P1-7 全覆盖）✓ · §3 实现优先级列与 PRD §5 优先级列一致 ✓ · §4 流程覆盖 P0 复杂业务（登录/转账/账单生成/余额汇总）✓ · §5 技术选型与 CLAUDE.md §一·一 无版本冲突 ✓ · 角色「普通用户」与 CLAUDE.md 起手段「单一用户角色」一致 ✓

### 维度 6: 跨文档对账（强制 4 类对账）

#### 6.1 角色全集对账
- **参考集**（PRD §2 角色）: [普通用户]
- **被检集**（§3 路由表「角色限制」并集）: [全角色]
- **差集**: 空。PRD 仅定义单一用户角色，§3 路由全部标注"全角色"，一致。
- **结论**: **对账通过**。

#### 6.2 字段引用对账
- **参考集**（PRD 全文业务字段）: username, password, name, type, initial_balance, category_id, account_id, amount, note, time, transfer_id, keyword, startTime, endTime, pageNum, pageSize, year, month, status, next_due_date, currency
- **被检集**（TECH_DESIGN §1-§5 引用字段）: username, password, initialBalance, status, amount, type, category_id, account_id, transfer_id, note, fromAccountId, toAccountId, balance, next_due_date
- **差集**: TECH_DESIGN 未引用 PRD 中的 `currency`（P2-4 多币种）和 `keyword`（P1-1 多条件筛选）。但 `currency` 出现在 §5.5 教学简化声明中，`keyword` 属于 API query param 而非流程图字段，可接受。
- **结论**: **对账通过**。

#### 6.3 API ↔ UI 按钮对账
- **正向**（UI 按钮 → 必有对应 API）:
  - 新增/编辑/删除账户 → `POST/PUT/DELETE /api/account` ✓
  - 记一笔/编辑/筛选 → `POST/PUT/GET /api/transaction` ✓
  - 预算保存 → `POST /api/budget` ✓
  - 创建/编辑/停用/生成周期账单 → `POST/PUT/DELETE /api/recurring-bill` + `/generate` ✓
  - 转账 → `POST /api/transaction/transfer` ✓
  - CSV 导入 → `POST /api/transaction/import` ✓
  - 修改密码 → `POST /api/user/change-password` ✓
- **反向**（已声明 API → 必有 UI 入口）:
  - `POST /api/user/register` → LoginPage ✓
  - `POST /api/user/login` → LoginPage ✓
  - `GET /api/category` → CategorySelector + CategoryPage ✓
  - `GET /api/statistics/*` → DashboardPage/AnalyticsPage ✓
  - `GET /api/budget/progress` + `/alert` → BudgetPage/DashboardPage ✓
  - `GET /api/account/balance` → AccountPage ✓
  - `GET /api/exchange-rate` → AccountPage ✓
- **结论**: **对账通过**。

#### 6.4 登录后跳转对账
- **普通用户**: 登录成功 → 跳转 `/`（DashboardPage）· 该页守卫 = ✅ 需登录 · 角色限制 = 全角色（包含普通用户）✓
- **结论**: **对账通过**。

### 维度 7: 流程推演（强制 4 个反例）

#### 7.1 路由守卫推演
- **普通用户**:
  - login → 存 token → 跳 `/`（DashboardPage）
  - 守卫: `to.meta.requiresAuth = true`, `token` 存在 → 放行 ✓
  - 已登录访问 `/login` → `!to.meta.requiresAuth && token && to.path === '/login'` → 跳 `/` → 不成环 ✓
  - 未登录访问 `/budget` → `to.meta.requiresAuth && !token` → 跳 `/login?redirect=/budget` → 登录后跳 `redirect` 或 `/` ✓
- **推演通过**，无死循环。

#### 7.2 并发推演
- **§4.2 转账**: 余额检查 SELECT + 两条 INSERT 均在 `=== 事务开始 ===` 范围内。REPEATABLE READ 下第二个并发事务会看到第一个未提交行而等待/失败。**推演通过**。
- **§4.3 账单生成**: 条件 UPDATE（`WHERE next_due_date = 当前值`）+ affectedRows 判断 + INSERT 在同一 `=== 事务开始 ===` 内。UPDATE affectedRows=0 时提前返回；INSERT 失败则 UPDATE 一并回滚。**推演通过**。
- **§4.4 汇总余额**: 纯 SELECT，无状态变更。**推演通过**。

#### 7.3 删除依赖推演
- **账户删除（P0-2）**: PRD 异常流程②定义删除前检查 transaction + recurring_bill 引用，有则拒绝 + 提示。TECH_DESIGN §2 关键类包含 Account 和 Transaction/RecurringBill，逻辑链完整。**推演通过**。
- **周期性账单停用（P1-4）**: 软删除（status=0），无外键级联。**推演通过**。
- **收支记录**: 不支持删除（PRD P0-4 声明）。**推演通过**。
- **预算**: 不支持删除（PRD P1-3）。**推演通过**。

#### 7.4 NULL / 边界推演
- **transfer_id = NULL**: PRD P1-5 业务规则③已定义（NULL = 普通收支记录，非 NULL = 转账关联）。**推演通过**。
- **无收支记录账户**: PRD P0-5 业务规则③已定义（余额 = 初始余额，SUM 返回 NULL 按 0 处理）。**推演通过**。
- **某分类本月无消费**: PRD P0-6 业务规则③已定义（金额显示为 0）。**推演通过**。
- **预算未设置**: PRD P1-3 业务规则③已定义（不参与超支判断）。**推演通过**。

---

## 修复行动建议

本轮发现 **2 条新 issue**（均为中严重度），涉及 §4.1 登录流程的数据流完整性。

按严重度排序:

1. **issue-1（中）**: §4.1 登录流程缺少 `userStore.setUser()` 步骤 → 下游 login-coder 可能漏写 Pinia userStore 调用
2. **issue-2（中）**: LoginResponse 缺少 username 字段 → 前端无法获取用户名显示

**建议**: 调用 `/tech-designer 应用修复` 处理 2 条 issue（中 2 条）。修复后 §1-§5 可安全进入 Phase 1 Step 7 `/page-prototyper`。
