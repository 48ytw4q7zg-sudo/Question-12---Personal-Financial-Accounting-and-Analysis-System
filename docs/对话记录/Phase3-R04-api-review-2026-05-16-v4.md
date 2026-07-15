# Phase 3 R-04 API 设计审核报告（第四轮）· 2026-05-16

## 审核元数据

- 审核日期: 2026-05-16
- 使用模型: mimo-v2-pro（V4 Pro · 同源自审）
- 输入摘要: docs/API_DESIGN.md · 28 个接口 · 8 个业务模块 · §1-§4 结构（R-04 第三轮修复后版本）
- 审核背景: 第一轮修复 11 条 issue（5 高 / 3 中 / 3 低）；第二轮修复 4 条 issue（1 高 / 1 中 / 2 低）；第三轮修复 2 条 issue（2 低）；本轮为第三轮修复后复审

## 审核报告

### 维度 1: RESTful 规范性

无 issue。

- 统一前缀 `/api` ✓
- 统一 `Result<T>` 响应格式 ✓
- HTTP 方法正确（GET 查询 / POST 创建 / PUT 更新 / DELETE 删除）✓
- 资源用单数名词，与 §1 约定一致 ✓
- 路径参数用 `{id}` ✓
- change-password 含动词 URL，已有教学简化说明 ✓

---

### 维度 2: 完整性

无 issue。

- **5 项必填**: 28 个接口逐一核对，每接口均含：① 功能 ② 是否需登录 ③ 请求参数表 ④ 成功响应 JSON（Result<T> 形态）⑤ 异常响应表 ✓
- **分页接口**: GET /api/transaction 含 pageNum + pageSize 参数，1-100 上限 ✓
- **复杂请求**: POST/PUT 接口均给出 JSON body 字段表 ✓
- **§4 全局异常码**: 400/401/403/404/500 五项齐全 ✓
- **§4 业务异常码**: 按模块编号 1xxx-5xxx，每模块预留 100 个 code ✓
- **§2 总计行**: P0: 11 个 / P1: 13 个 / P2: 4 个 = 28 ✓（与 §2 表格标签 + PRD 三者一致）

---

### 维度 3: 一致性

无 issue。

- 统一前缀 `/api` ✓
- 统一 `Result<T>` 响应格式 ✓
- 参数 camelCase 命名（accountId, categoryId, initialBalance 等）✓
- 时间字段格式一致：自定义 `yyyy-MM-dd HH:mm:ss`（§1 已标注非标准 ISO 8601 + Jackson 配置说明）✓
- §3 所有错误 code 均在 §4.3 表中有出处（1001/1002/2002/2003/3001-3006/4001/4002/5001-5005）✓
- **遗留说明（非 issue）**: code 3002 同时用于"账户不存在或已禁用"和"分类不存在"两种场景。教学简化可接受（前端按 message 区分），生产环境应拆为独立 code。
- **跨文档参考（非 API_DESIGN 内部问题）**: TECH_DESIGN §5 日期映射行写「DATETIME → `LocalDateTime`（Jackson ISO 8601 序列化）」，与 API_DESIGN §1 自定义格式不一致。Phase 4 配置 Jackson 时需同步 TECH_DESIGN。

---

### 维度 4: 安全性

无 issue。

- JWT 鉴权正确：仅 register/login 公开（2 接口），其余 26 个全部需登录 ✓
- 行级权限：写操作接口（POST/PUT/DELETE）均声明 `user_id = currentUserId` 隔离 ✓
- GET 接口隐含 userId 过滤 ✓
- 密码安全：LoginResponse 不含 password 字段（Entity 用 `@JsonIgnore`）✓
- 敏感操作：DELETE 账户有幂等性方案、transfer 有 @Transactional 事务保护 ✓
- 单一用户角色，无角色间越权场景 ✓

---

### 维度 5: 业务覆盖

无 issue。

**PRD §3 全量功能 → API 对应关系**:

| PRD 功能 | 对应接口 | 优先级一致 |
|---|---|:---:|
| P0-1 登录/JWT | POST register + POST login | P0 → P0 ✓ |
| P0-2 账户 CRUD | GET/POST/PUT/DELETE /api/account | P0 → P0 ✓ |
| P0-3 分类 GET 列表 | GET /api/category | P0 → P0 ✓ |
| P0-4 收支记录 | GET/POST/PUT /api/transaction | P0 → P0 ✓ |
| P0-5 按账户汇总余额 | GET /api/account/balance | P0 → P0 ✓ |
| P0-6 分类浏览 | GET /api/category + GET /api/statistics/category-summary | P0 → P0+P1 ✓ |
| P1-1 多条件筛选 | GET /api/transaction（筛选参数复用） | P1 → P0 ✓ |
| P1-2 月/年汇总 | GET /api/statistics/monthly + yearly | P1 → P1 ✓ |
| P1-3 预算管理 | GET/POST /api/budget + GET /api/budget/progress | P1 → P1 ✓ |
| P1-4 周期性账单 | GET/POST/PUT/DELETE + generate /api/recurring-bill | P1 → P1 ✓ |
| P1-5 转账 | POST /api/transaction/transfer | P1 → P1 ✓ |
| P1-6 分类汇总 | GET /api/statistics/category-summary（复用） | P1 → P1 ✓ |
| P1-7 改密码 | POST /api/user/change-password | P1 → P1 ✓ |
| P2-1 月度趋势 | GET /api/statistics/trend + 复用 | P2 → P2 ✓ |
| P2-2 预算预警 | GET /api/budget/alert | P2 → P2 ✓ |
| P2-3 CSV 导入 | POST /api/transaction/import | P2 → P2 ✓ |
| P2-4 汇率查询 | GET /api/exchange-rate | P2 → P2 ✓ |

- 全量 17 个 PRD 功能全部覆盖 ✓
- 无跨档依赖（P0 接口不依赖 P1/P2 接口）✓
- 每个数据库表至少有"列表 + 创建"接口 ✓

---

### 维度 6: 跨文档对账

#### 6.1 接口 ↔ PRD「API 形态」对账

- **参考集**（PRD §3 各功能 API 形态 · 27 个去重唯一 URL）:
  POST /api/user/register, POST /api/user/login, POST /api/user/change-password,
  GET /api/account, POST /api/account, PUT /api/account/{id}, DELETE /api/account/{id}, GET /api/account/balance,
  GET /api/category,
  GET /api/transaction, POST /api/transaction, PUT /api/transaction/{id}, POST /api/transaction/transfer, POST /api/transaction/import,
  GET /api/budget, POST /api/budget, GET /api/budget/progress, GET /api/budget/alert,
  GET /api/statistics/monthly, GET /api/statistics/yearly, GET /api/statistics/category-summary, GET /api/statistics/trend,
  GET /api/recurring-bill, POST /api/recurring-bill, PUT /api/recurring-bill/{id}, DELETE /api/recurring-bill/{id}, POST /api/recurring-bill/{id}/generate,
  GET /api/exchange-rate

- **被检集**（API_DESIGN §2 接口清单）: 28 个接口

- **差集**:
  - PRD 有 / API_DESIGN 缺: **无**
  - API_DESIGN 有 / PRD 缺: **无**
  - **结论**: 对账通过，无 issue。

#### 6.2 接口 ↔ DATABASE 字段对账

逐表核对 API 请求参数 + 响应 DTO 字段与 DATABASE 列的对应关系：

| 表 | API 涉及字段 | 数据库列 | 类型一致 |
|---|---|---|:---:|
| user | username(VARCHAR) → String ✓, password(VARCHAR) → String ✓ | 对应 | ✓ |
| account | id/user_id/name/type/initial_balance/currency/status → 全部一致 | 对应 | ✓ |
| category | id/name/type → 全部一致 | 对应 | ✓ |
| transaction | id/user_id/account_id/category_id/type/amount/note/time/transfer_id → 全部一致 | 对应 | ✓ |
| budget | id/user_id/category_id/month/amount → 全部一致 | 对应 | ✓ |
| recurring_bill | id/user_id/account_id/category_id/name/amount/type/period/next_due_date/status → 全部一致 | 对应 | ✓ |

- **差集**: **无**。所有 API 字段均在 DATABASE 找到对应列，类型一致。
- **结论**: 对账通过，无 issue。

#### 6.3 接口 ↔ TECH §3 UI 按钮对账

- **参考集**（11 个页面的 UI 交互元素）:
  - LoginPage: 登录/注册 → POST login + POST register
  - AccountPage: 新增/编辑/删除/余额列 → POST/PUT/DELETE /api/account + GET /api/account/balance
  - CategoryPage: 分类列表/卡片金额 → GET /api/category + GET /api/statistics/category-summary
  - TransactionListPage: 记一笔/编辑/搜索/分页 → POST/PUT/GET /api/transaction
  - TransferPage: 确认转账 → POST /api/transaction/transfer
  - BudgetPage: 保存/进度/预警 → POST /api/budget + GET /api/budget/progress + GET /api/budget/alert
  - RecurringBillPage: 新增/编辑/停用/生成 → POST/PUT/DELETE /api/recurring-bill + POST generate
  - DashboardPage: 月/年统计 + 趋势图 + 饼图 + 预警 → GET monthly/yearly/trend/category-summary + GET alert
  - UserSettingsPage: 改密码 → POST /api/user/change-password
  - ImportPage: 确认导入 → POST /api/transaction/import
  - AnalyticsPage: 趋势图 + 饼图 + 预算 vs 实际 → GET trend/category-summary + GET /api/budget/progress

- **被检集**: API_DESIGN §2 全部 28 个接口

- **差集**:
  - UI 按钮有 / API 缺: **无**
  - API 有 / UI 无: GET /api/exchange-rate 无直接 UI 按钮对应（内部工具接口，AccountPage 币种字段内部调用）→ 合理
- **结论**: 对账通过，无 issue。

#### 6.4 错误码段 ↔ TECH §5 错误码规范对账

- **参考集**（TECH §5 错误码规范）: 1xxx 用户 / 2xxx 账户 / 3xxx 交易 / 4xxx 预算 / 5xxx 周期账单
- **被检集**（API_DESIGN §3 实际使用 code）: 1001/1002 ✓ / 2002/2003 ✓ / 3001/3002/3003/3004/3005/3006 ✓ / 4001/4002 ✓ / 5001/5002/5003/5004/5005 ✓
- **差集**:
  - 所有 code 均落在 TECH 定义的模块段内，无越界 code ✓
  - 3002 多义性为已知遗留（见维度 3 说明）
- **结论**: 错误码段边界对账通过，无 issue。

---

### 维度 7: 反例推演

#### 7.1 资源不存在推演

对每个含 `{id}` 路径参数的接口，推演"客户端传入不存在的 id"场景：

| 接口 | 异常响应表是否明示"不存在" | 推演结论 |
|---|---|---|
| PUT /api/account/{id} | ✅ 有：2003 账户不存在 | ✓ |
| DELETE /api/account/{id} | ✅ 有：2003 账户不存在 | ✓ |
| PUT /api/transaction/{id} | ✅ 有：3006 收支记录不存在 | ✓ |
| PUT /api/recurring-bill/{id} | ✅ 有：5005 周期性账单不存在 | ✓ |
| DELETE /api/recurring-bill/{id} | ✅ 有：5005 周期性账单不存在 | ✓ |
| POST /api/recurring-bill/{id}/generate | ✅ 有：5005 周期性账单不存在 | ✓ |

**结论**: 无 issue。所有含 `{id}` 接口均已声明"资源不存在"异常响应。

#### 7.2 越权推演（行级权限）

- 所有写操作接口均声明 `user_id = currentUserId` → 用户 A 无法修改用户 B 的数据 ✓
- GET 接口均隐含 userId 过滤 → 用户 A 只看自己的数据 ✓
- 单一用户角色（普通用户），无角色间越权场景 ✓
- §20 明示"所有数据按 user_id 隔离，不存在跨用户共享场景" ✓

**结论**: 无 issue。行级权限覆盖完整。

#### 7.3 并发幂等推演

| 接口 | 幂等性方案 | 推演结论 |
|---|---|---|
| DELETE /api/account/{id} | 条件 UPDATE + affectedRows → 2003 | ✓ |
| POST /api/budget | 唯一约束 + ON DUPLICATE KEY UPDATE | ✓ |
| DELETE /api/recurring-bill/{id} | 条件 UPDATE + affectedRows → 5004 | ✓ |
| POST /api/recurring-bill/{id}/generate | 条件 UPDATE + affectedRows → 5003 | ✓ |
| POST /api/transaction | 无（教学简化） | ✓ |
| POST /api/transaction/transfer | 无（教学简化 + 前端 loading） | ✓ |

**结论**: 无 issue。状态变更类接口均有幂等性方案，非幂等接口 PRD 已声明教学简化。

#### 7.4 分页/边界/空集合推演

| 接口 | pageSize 上限 | 空集合返回 | 排序白名单 | 结论 |
|---|---|---|---|---|
| GET /api/transaction | ✅ 1-100（@Min @Max） | ✅ `data.records: []` | ✅ id/time/create_time | ✓ |
| GET /api/account | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/recurring-bill | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/budget | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/budget/progress | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/budget/alert | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/statistics/*（4 个） | 非分页 | ✅ 有说明 | N/A | ✓ |
| GET /api/account/balance | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/category | 非分页 | ✅ 有说明 | N/A | ✓ |
| GET /api/exchange-rate | 非分页 | ✅ `data: []` | N/A | ✓ |

**结论**: 无 issue。分页约束完整，空集合返回 `[]` 均已声明。

---

## 审核结论

**本轮无新 issue**。API_DESIGN.md 经三轮审核 + 修复（累计 17 条 issue 全部已修复），7 维度均通过。

| 维度 | 状态 | 备注 |
|---|---|---|
| 1. RESTful 规范性 | ✓ 通过 | 单数命名 + §1 约定一致 |
| 2. 完整性 | ✓ 通过 | 28 接口 5 项必填齐全 + P0/P1/P2 计数正确 |
| 3. 一致性 | ✓ 通过 | 3002 多义性为已知遗留（教学简化可接受） |
| 4. 安全性 | ✓ 通过 | JWT + 行级权限 + 密码过滤 |
| 5. 业务覆盖 | ✓ 通过 | PRD §3 全量 17 功能覆盖 + 优先级一致 |
| 6. 跨文档对账 | ✓ 通过 | 4 类对账全部通过 |
| 7. 反例推演 | ✓ 通过 | 4 项推演全部通过 |

**三轮审核累计**: 17 条 issue（6 高 / 4 中 / 7 低），全部已修复。API 设计质量已收敛，可进入 Phase 4。
