# Phase 3 R-04 API 设计审核报告（第二轮）· 2026-05-16

## 审核元数据

- 审核日期: 2026-05-16
- 使用模型: mimo-v2-pro（V4 Pro · 同源自审）
- 输入摘要: docs/API_DESIGN.md · 28 个接口 · 8 个业务模块 · §1-§4 结构（R-04 第一轮修复后版本）
- 审核背景: 第一轮 R-04 已修复 11 条 issue（5 高 / 3 中 / 3 低），本轮为修复后复审

## 审核报告

### 维度 1: RESTful 规范性

无新 issue。

- 所有接口 URL 使用单数名词（`/api/account`、`/api/transaction`），与 §1 约定一致 ✓
- HTTP 方法正确：GET 查询 / POST 创建 / PUT 更新 / DELETE 删除 ✓
- 路径参数用 `{id}` ✓
- change-password 含动词 URL，已补充教学简化说明（第一轮 issue-2 已修）✓

---

### 维度 2: 完整性

- **issue-1** [严重度: 低]: §4.3 交易模块示例未列出 code 3006
  - **位置**: API_DESIGN.md §4.3 业务异常码表
  - **问题**: §3 PUT /api/transaction/{id} 已使用 code 3006（收支记录不存在），但 §4.3 交易模块示例行仍为「3001=金额不合法 · 3002=账户或分类不存在 · 3003=转账记录金额不可修改 · 3004=转出转入账户不可相同 · 3005=余额不足」，缺少 3006。
  - **修复建议**: §4.3 交易模块示例追加「· 3006=收支记录不存在」

其余完整性检查通过：每接口 5 项必填齐全、§4 全局异常码 5 项齐全（400/401/403/404/500）、业务异常码按模块编号。

---

### 维度 3: 一致性

无新 issue。

- 统一前缀 `/api` ✓
- 统一 `Result<T>` 响应格式 ✓
- 参数 camelCase 命名 ✓
- §3 所有错误 code 均在 §4.3 表中有出处（含新增 5004/5005）✓
- **跨文档不一致（参考 · 非 API_DESIGN 内部问题）**: TECH_DESIGN §5 日期映射行写「DATETIME → `LocalDateTime`（Jackson ISO 8601 序列化）」，但 API_DESIGN §1 已改为「自定义格式 `yyyy-MM-dd HH:mm:ss`（非标准 ISO 8601）」。Phase 4 配置 Jackson 时需同步确认 TECH_DESIGN 该行描述是否更新（不在本审核修改范围内）。

---

### 维度 4: 安全性

无新 issue。

- JWT 鉴权正确：仅 register/login 公开，其余全部需登录 ✓
- 行级权限：写操作接口均声明 `user_id = currentUserId` 隔离 ✓
- 密码安全：LoginResponse 不含 password 字段（Entity 用 `@JsonIgnore`）✓
- 敏感操作：DELETE 账户有幂等性方案、transfer 有 @Transactional 事务保护 ✓

---

### 维度 5: 业务覆盖

- **issue-2** [严重度: 低]: §2 接口清单 P0/P1 计数与实际不符
  - **位置**: API_DESIGN.md §2 末尾总计行
  - **问题**: §2 总计写「P0: 13 个 / P1: 11 个」，但逐一核对 §3 接口详情：P0 实际 14 个（register/login/4 个 account/balance/category/4 个 transaction/budget 两个），P1 实际 12 个（change-password/transfer/budget×3/statistics×3/recurring-bill×4）。差异原因：§2 表 #15 POST /api/budget 标为 P1，但 PRD P1-3「保存预算」对应此接口，标 P1 正确；实际是 §2 表列出的 P0 条目数为 12 而非 13（漏数 1 个）。
  - **修复建议**: 修正 §2 总计行为「P0: 14 个 / P1: 12 个」（接口总数 28 不变）

其余 PRD §3 P0+P1+P2 功能全部覆盖，优先级标签一致，无跨档依赖。

---

### 维度 6: 跨文档对账

#### 6.1 接口 ↔ PRD「API 形态」对账

- **参考集**（PRD §3 各功能 API 形态 · 含 P2-3/P2-4 已在第一轮补全）:
  - P0-1: POST /api/user/register, POST /api/user/login
  - P0-2: GET /api/account, POST /api/account, PUT /api/account/{id}, DELETE /api/account/{id}
  - P0-3: GET /api/category
  - P0-4: POST /api/transaction, PUT /api/transaction/{id}, GET /api/transaction
  - P0-5: GET /api/account/balance
  - P0-6: GET /api/category, GET /api/statistics/category-summary
  - P1-1: GET /api/transaction（复用）
  - P1-2: GET /api/statistics/monthly, GET /api/statistics/yearly
  - P1-3: GET /api/budget, POST /api/budget, GET /api/budget/progress
  - P1-4: GET /api/recurring-bill, POST /api/recurring-bill, PUT /api/recurring-bill/{id}, DELETE /api/recurring-bill/{id}, POST /api/recurring-bill/{id}/generate
  - P1-5: POST /api/transaction/transfer
  - P1-6: GET /api/statistics/category-summary（复用）
  - P1-7: POST /api/user/change-password
  - P2-1: GET /api/statistics/trend, GET /api/statistics/category-summary（复用）, GET /api/budget/progress（复用）
  - P2-2: GET /api/budget/alert
  - P2-3: POST /api/transaction/import
  - P2-4: GET /api/exchange-rate

- **被检集**（API_DESIGN §2 接口清单）: 28 个接口（§2 全文）

- **差集**:
  - PRD 有 / API_DESIGN 缺: **无**
  - API_DESIGN 有 / PRD 缺: **无**
  - **结论**: 对账通过，无 issue。第一轮修复已补齐 P2-3（import）和 P2-4（exchange-rate）。

#### 6.2 接口 ↔ DATABASE 字段对账

- **account 表**: id(BIGINT)→Long ✓, user_id(BIGINT)→Long ✓, name(VARCHAR)→String ✓, type(TINYINT)→Integer ✓, initial_balance(DECIMAL)→BigDecimal ✓, currency(VARCHAR)→String ✓, status(TINYINT)→Integer ✓
- **category 表**: id/name/type 一致 ✓
- **transaction 表**: 全部字段一致 ✓（含 transfer_id VARCHAR→String）
- **budget 表**: 全部字段一致 ✓
- **recurring_bill 表**: 全部字段一致 ✓（next_due_date DATE→String 兼容）
- **新增接口**: POST /api/transaction/import 用 multipart/form-data（file + accountId），accountId 对应 account.id ✓；GET /api/exchange-rate 返回硬编码汇率（无数据库表，PRD P2-4 教学简化声明汇率用固定值）✓
- **差集**: **无**。所有 API 字段均在 DATABASE 找到对应列，类型一致。
- **结论**: 对账通过，无 issue。

#### 6.3 接口 ↔ TECH §3 UI 按钮对账

- **参考集**（TECH §3 路由表 + §6 页面原型 UI 交互元素）:
  - LoginPage: 登录→POST /api/user/login, 注册→POST /api/user/register
  - AccountPage: 新增→POST /api/account, 编辑→PUT /api/account/{id}, 删除→DELETE /api/account/{id}, 余额列→GET /api/account/balance
  - CategoryPage: 分类列表→GET /api/category, 分类卡片金额→GET /api/statistics/category-summary
  - TransactionListPage: 记一笔→POST /api/transaction, 编辑→PUT /api/transaction/{id}, 搜索→GET /api/transaction, 分页→GET /api/transaction
  - TransferPage: 确认转账→POST /api/transaction/transfer
  - BudgetPage: 保存预算→POST /api/budget, 进度→GET /api/budget/progress, 预警→GET /api/budget/alert
  - RecurringBillPage: 新增→POST /api/recurring-bill, 编辑→PUT /api/recurring-bill/{id}, 停用→DELETE /api/recurring-bill/{id}, 生成→POST /api/recurring-bill/{id}/generate
  - DashboardPage: 月统计→GET /api/statistics/monthly, 年统计→GET /api/statistics/yearly, 趋势图→GET /api/statistics/trend, 饼图→GET /api/statistics/category-summary, 预警→GET /api/budget/alert
  - UserSettingsPage: 保存修改→POST /api/user/change-password
  - ImportPage: 确认导入→POST /api/transaction/import
  - AnalyticsPage: 趋势图→GET /api/statistics/trend, 饼图→GET /api/statistics/category-summary, 预算 vs 实际→GET /api/budget/progress

- **被检集**: API_DESIGN §2 全部 28 个接口

- **差集**:
  - UI 按钮有 / API 缺: **无**
  - API 有 / UI 无: GET /api/exchange-rate 无直接 UI 按钮对应（内部工具接口，AccountPage 币种字段内部调用，非用户显式触发）→ 合理
- **结论**: 对账通过，无 issue。

#### 6.4 错误码段 ↔ TECH §5 错误码规范对账

- **参考集**（TECH §5 错误码规范）:
  - 业务码段: 1xxx 用户 / 2xxx 账户 / 3xxx 交易 / 4xxx 预算 / 5xxx 周期账单

- **被检集**（API_DESIGN §3 实际使用 code + §4.3 示例）:
  - 1001/1002 ✓ / 2002/2003 ✓ / 3001/3002/3003/3004/3005/3006 ✓ / 4001/4002 ✓ / 5001/5002/5003/5004/5005 ✓

- **差集**:
  - 所有 code 均落在 TECH 定义的模块段内，无越界 code（如 9999/8888）→ 无越界 issue
  - 新增 code 3006/5004/5005 均在对应模块段内 → 合理
  - **但是**: code 3002 在 §3 同一接口中用于两种不同场景（见维度 3 上一轮 issue-8 遗留）→ 编号一致性问题
- **结论**: 错误码段边界对账通过。3002 多义性为上一轮遗留问题（见下方遗留说明）。

---

### 维度 7: 反例推演

#### 7.1 资源不存在推演

对每个含 `{id}` 路径参数的接口，推演"客户端传入不存在的 id"场景：

| 接口 | 异常响应是否明示"不存在" | 推演结论 |
|---|---|---|
| PUT /api/account/{id} | ✅ 有：2003 账户不存在 | 第一轮已修 ✓ |
| DELETE /api/account/{id} | ✅ 有：2003 账户不存在 | 第一轮已修 ✓ |
| PUT /api/transaction/{id} | ✅ 有：3006 收支记录不存在 | 第一轮已修 ✓ |
| DELETE /api/recurring-bill/{id} | ✅ 有：5005 周期性账单不存在 | 第一轮已修 ✓ |
| POST /api/recurring-bill/{id}/generate | ✅ 有：5005 周期性账单不存在 | 原本就有 ✓ |
| PUT /api/recurring-bill/{id} | ❌ 无。仅有 5004（已停用）和 5002（关联账户已禁用）| **issue-3** |
| POST /api/transaction/import | N/A（无 {id} 路径参数，用 accountId body 参数）| 账户不存在用 2003 ✓ |

**结论**: PUT /api/recurring-bill/{id} 缺少"资源不存在"异常响应（issue-3 · 高严重度）。

#### 7.2 越权推演（行级权限）

- 所有写操作接口均声明 `user_id = currentUserId` → 用户 A 无法修改用户 B 的数据 ✓
- GET 接口均隐含 userId 过滤 → 用户 A 只看自己的数据 ✓
- 单一用户角色，无角色间越权场景 ✓

**结论**: 无 issue。行级权限覆盖完整。

#### 7.3 并发幂等推演

| 接口 | 幂等性方案 | 推演结论 |
|---|---|---|
| DELETE /api/account/{id} | 条件 UPDATE + affectedRows → 2003 ✓ |
| POST /api/budget | 唯一约束 + ON DUPLICATE KEY UPDATE ✓ |
| DELETE /api/recurring-bill/{id} | 条件 UPDATE + affectedRows | **幂等性描述与错误表不一致** → **issue-4** |
| POST /api/recurring-bill/{id}/generate | 条件 UPDATE + affectedRows → 5003 ✓ |
| POST /api/transaction | 无（教学简化）✓ |
| POST /api/transaction/transfer | 无（教学简化 + 前端 loading）✓ |

**推演过程**:
- DELETE /api/recurring-bill/{id}: 幂等性描述写「affectedRows=0 时说明已停用，返回 5002」，但异常响应表中 code 5004 = 周期性账单已停用（第一轮 R-04 issue-8 修复将"已停用"从 5002 拆为 5004）。连点 2 次：第 1 次成功 → 第 2 次 affectedRows=0 → 实际应返回 5004，但幂等性描述写 5002 → **描述与实现不一致**。

**结论**: 1 条 issue（issue-4 · 中严重度）。

#### 7.4 分页/边界/空集合推演

| 接口 | pageSize 上限 | 空集合返回 | 排序白名单 | 结论 |
|---|---|---|---|---|
| GET /api/transaction | ✅ 1-100 | ✅ `data.records: []` | ✅ id/time/create_time | ✓ |
| 其余列表接口 | 非分页 | ✅ `data: []` | N/A | ✓ |

**结论**: 无 issue。分页约束完整。

---

## 修复行动建议

按严重度排序（本轮新发现 5 条）：

1. **issue-3** [高] PUT /api/recurring-bill/{id} 补充"资源不存在"异常响应 → 追加 code 5005 行
2. **issue-4** [中] DELETE /api/recurring-bill/{id} 幂等性描述中 `返回 5002` 改为 `返回 5004`（与异常响应表一致）
3. **issue-1** [低] §4.3 交易模块示例追加「· 3006=收支记录不存在」
4. **issue-2** [低] §2 总计行修正 P0/P1 计数（14/12 而非 13/11）
5. **跨文档参考** [低] TECH_DESIGN §5 日期映射行在 Phase 4 时确认是否需从「ISO 8601」改为「自定义格式」

### 上一轮遗留

- **3002 多义性**: code 3002 在 POST/PUT /api/transaction 中同时用于"账户不存在或已禁用"和"分类不存在"两种场景。第一轮新增 3006 解决了"记录不存在"问题，但 3002 的账户/分类歧义仍在。教学简化可接受（前端按 message 区分），但生产环境应拆为独立 code。
