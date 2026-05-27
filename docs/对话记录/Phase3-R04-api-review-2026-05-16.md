# Phase 3 R-04 API 设计审核报告 · 2026-05-16

## 审核元数据

- 审核日期: 2026-05-16
- 使用模型: mimo-v2-pro（V4 Pro · 同源自审）
- 输入摘要: docs/API_DESIGN.md · 26 个接口 · 7 个业务模块 · §1-§4 结构

## 审核报告

### 维度 1: RESTful 规范性

- **issue-1** [严重度: 低]: URL 资源命名与 §1 约定自相矛盾
  - **位置**: API_DESIGN.md §1 接口约定 + §2 全部接口
  - **问题**: §1 约定写「资源用复数名词（`/api/accounts`）」，但实际所有接口用单数：`/api/account`、`/api/category`、`/api/transaction` 等。约定与实现自相矛盾。
  - **修复建议**: 修改 §1 约定改为「资源用单数名词」，与实际接口一致（单数命名在教学项目中可接受，保持内部一致即可）。

- **issue-2** [严重度: 中]: change-password 接口 URL 含动词，非 RESTful 资源化
  - **位置**: API_DESIGN.md §3 POST /api/user/change-password
  - **问题**: URL 含动词 `change-password`，不符合 RESTful「资源 + HTTP 动词」原则。RESTful 做法应为 `PUT /api/user/password`。
  - **修复建议**: 保留现有 URL（教学简化，前端/后端同步改动少），但在 §3 该接口补充说明「教学简化：使用动词 URL，生产环境应改为 PUT /api/user/password」。

---

### 维度 2: 完整性

- **issue-3** [严重度: 低]: §4.4 标题与 §4.3 内容不对应
  - **位置**: API_DESIGN.md §4.3 / §4.4
  - **问题**: skill 模板要求 §4.3 = 全局异常码 / §4.4 = 业务异常码 + DTO，但实际文档中 §4.3 = 业务异常码编号约定 / §4.4 = DTO 数据模型，缺少 §4.3 标题的全局异常码独立小节（全局异常码实际在 §4.2）。标题层级与模板不完全对齐。
  - **修复建议**: 重排 §4 子节编号：§4.1 Result\<T\> / §4.2 全局异常码 / §4.3 业务异常码 / §4.4 DTO 数据模型，去掉「编号约定」多余后缀。

每接口 5 项必填字段齐全（功能 / 鉴权 / 请求参数表 / 成功响应 JSON / 异常响应表），分页接口含 pageNum + pageSize 参数，复杂请求给了 JSON body 例子。§4 全局异常码 5 项齐全（400/401/403/404/500）。**无其他 issue**。

---

### 维度 3: 一致性

- **issue-4** [严重度: 低]: §1 时间格式声明为「ISO 8601」但实际格式不一致
  - **位置**: API_DESIGN.md §1 接口约定
  - **问题**: §1 时间格式行写「`yyyy-MM-dd HH:mm:ss`」并标注引用「Jackson 序列化 + LocalDateTime」。但 CLAUDE.md §一·三 接口约定描述为「ISO 8601（如 `2026-05-10T08:30:00`）」——含 T 分隔符。实际 API 设计用空格分隔不是标准 ISO 8601。需确认 Jackson 配置的序列化格式。
  - **修复建议**: §1 明确写「自定义格式 `yyyy-MM-dd HH:mm:ss`（需配置 Jackson `WRITE_DATES_AS_TIMESTAMPS=false` + 自定义 date-format），非标准 ISO 8601」，或统一改为 ISO 8601 格式（含 T）。

其余一致性检查通过：统一前缀 `/api`、统一 `Result<T>` 响应格式、参数 camelCase 命名、错误 code 与 §4 异常码表一致。

---

### 维度 4: 安全性

无 issue。

- JWT 鉴权正确：仅 register/login 公开，其余全部 ✅ 需登录。
- 行级权限：写操作接口（POST/PUT/DELETE）均声明 `user_id = currentUserId` 隔离。
- 密码安全：LoginResponse 不含 password 字段（Entity 用 `@JsonIgnore`）。
- 敏感操作：DELETE 账户有二次确认（前端 el-message-box）、transfer 有前端 loading 防连点。

---

### 维度 5: 业务覆盖

- **issue-5** [严重度: 中]: P2-3 CSV 导入接口在 §2 接口清单缺失
  - **位置**: API_DESIGN.md §2 接口清单
  - **问题**: PRD.md P2-3 定义 `POST /api/transaction/import` → `Result<ImportResultDTO>`，但 API_DESIGN.md §2 接口清单和 §3 接口详情均无此接口。TECH_DESIGN §6.11 ImportPage「确认导入」按钮也对应此接口。虽为 P2 可选，但 6.1 对账零容忍要求 PRD 声明的接口必须在 API_DESIGN 出现。
  - **修复建议**: 在 §2.4 交易记录模块追加 #14 `POST /api/transaction/import`（P2），在 §3 补充接口详情（5 项字段）。

- **issue-6** [严重度: 中]: P2-4 汇率接口在 §2 接口清单缺失
  - **位置**: API_DESIGN.md §2 接口清单
  - **问题**: PRD.md P2-4 定义 `GET /api/exchange-rate` → `Result<List<ExchangeRateDTO>>`，但 API_DESIGN.md 无此接口。TECH_DESIGN §6.3 AccountPage 币种字段依赖此接口。同为 P2 可选但 6.1 对账零容忍。
  - **修复建议**: 在 §2 新增模块 2.8 汇率模块（`/api/exchange-rate`），或在 §2.2 账户模块追加此接口（P2），在 §3 补充接口详情。

其余 PRD §3 P0+P1 功能全部覆盖：P0-1~P0-6 / P1-1~P1-7 均有对应接口，实现优先级标签与 PRD 一致。无跨档依赖（P0 接口不依赖 P1/P2 接口）。

---

### 维度 6: 跨文档对账

#### 6.1 接口 ↔ PRD「API 形态」对账

- **参考集**（PRD §3 各功能 API 形态）:
  - P0-1: POST /api/user/register, POST /api/user/login
  - P0-2: GET /api/account, POST /api/account, PUT /api/account/{id}, DELETE /api/account/{id}
  - P0-3: GET /api/category
  - P0-4: POST /api/transaction, PUT /api/transaction/{id}, GET /api/transaction
  - P0-5: GET /api/account/balance
  - P0-6: GET /api/category, GET /api/statistics/category-summary
  - P1-1: GET /api/transaction（复用，加筛选参数）
  - P1-2: GET /api/statistics/monthly, GET /api/statistics/yearly
  - P1-3: GET /api/budget, POST /api/budget, GET /api/budget/progress
  - P1-4: GET /api/recurring-bill, POST /api/recurring-bill, PUT /api/recurring-bill/{id}, DELETE /api/recurring-bill/{id}, POST /api/recurring-bill/{id}/generate
  - P1-5: POST /api/transaction/transfer
  - P1-6: GET /api/statistics/category-summary（复用）
  - P1-7: POST /api/user/change-password
  - P2-1: GET /api/statistics/trend, GET /api/statistics/category-summary（复用）, GET /api/budget/progress（复用）
  - P2-2: GET /api/budget/alert
  - **P2-3: POST /api/transaction/import** ← PRD 声明
  - **P2-4: GET /api/exchange-rate** ← PRD 声明

- **被检集**（API_DESIGN §2 接口清单）: 26 个接口（见 §2 全文）

- **差集**:
  - PRD 有 / API_DESIGN 缺: `POST /api/transaction/import`（P2-3）, `GET /api/exchange-rate`（P2-4）→ **issue-5 + issue-6**（与维度 5 合并，不重复编号）
  - API_DESIGN 有 / PRD 缺: **无**（26 个接口均在 PRD 找到出处）
  - **结论**: 2 条差集 issue（均为 P2 可选功能，但 6.1 零容忍要求标记）

#### 6.2 接口 ↔ DATABASE 字段对账

- **account 表字段**: id(BIGINT), user_id(BIGINT), name(VARCHAR), type(TINYINT), initial_balance(DECIMAL), currency(VARCHAR), status(TINYINT), create_time(DATETIME), update_time(DATETIME)
  - API 使用: id→Long ✓, name→String ✓, type→Integer ✓, initialBalance→BigDecimal ✓, currency→String ✓, status→Integer ✓, createTime/updateTime→String ✓
  - balanceDTO 中 totalIncome/totalExpense/currentBalance 为计算值，不在表中 → 合理

- **category 表字段**: id(BIGINT), name(VARCHAR), type(TINYINT), create_time(DATETIME), update_time(DATETIME)
  - API 使用: id→Long ✓, name→String ✓, type→Integer ✓
  - **注意**: API CategoryDTO 无 createTime/updateTime 字段（分类为种子数据，PRD P0-3 不展示时间）→ 合理

- **transaction 表字段**: id(BIGINT), user_id(BIGINT), account_id(BIGINT), category_id(BIGINT), type(TINYINT), amount(DECIMAL), note(VARCHAR), time(DATETIME), transfer_id(VARCHAR), create_time(DATETIME), update_time(DATETIME)
  - API 使用: 全部字段类型一致 ✓
  - accountName/categoryName 为关联查询填充 → 合理

- **budget 表字段**: id(BIGINT), user_id(BIGINT), category_id(BIGINT), month(VARCHAR), amount(DECIMAL), create_time(DATETIME), update_time(DATETIME)
  - API 使用: 全部一致 ✓

- **recurring_bill 表字段**: id(BIGINT), user_id(BIGINT), account_id(BIGINT), category_id(BIGINT), name(VARCHAR), amount(DECIMAL), type(TINYINT), period(VARCHAR), next_due_date(DATE), status(TINYINT), create_time(DATETIME), update_time(DATETIME)
  - API 使用: nextDueDate→String（DB 为 DATE，Java 侧应映射 LocalDate）→ 类型兼容 ✓
  - 其余全部一致 ✓

- **差集**: **无**。所有 API 字段均在 DATABASE 找到对应列，类型一致。
- **结论**: 对账通过，无 issue。

#### 6.3 接口 ↔ TECH §3 UI 按钮对账

- **参考集**（TECH §6 页面原型 UI 交互元素）:
  - LoginPage: 登录→POST /api/user/login, 注册→POST /api/user/register
  - AccountPage: 新增→POST /api/account, 编辑→PUT /api/account/{id}, 删除→DELETE /api/account/{id}, 余额列→GET /api/account/balance
  - CategoryPage: 分类列表→GET /api/category, 分类卡片金额→GET /api/statistics/category-summary
  - TransactionListPage: 记一笔→POST /api/transaction, 编辑→PUT /api/transaction/{id}, 搜索→GET /api/transaction, 分页→GET /api/transaction
  - TransferPage: 确认转账→POST /api/transaction/transfer
  - BudgetPage: 保存预算→POST /api/budget, 进度→GET /api/budget/progress, 预警→GET /api/budget/alert
  - RecurringBillPage: 新增→POST /api/recurring-bill, 编辑→PUT /api/recurring-bill/{id}, 停用→DELETE /api/recurring-bill/{id}, 生成→POST /api/recurring-bill/{id}/generate
  - DashboardPage: 月统计→GET /api/statistics/monthly, 年统计→GET /api/statistics/yearly, 趋势图→GET /api/statistics/trend, 饼图→GET /api/statistics/category-summary, 预警→GET /api/budget/alert
  - UserSettingsPage: 保存修改→POST /api/user/change-password
  - **ImportPage: 确认导入→POST /api/transaction/import** ← UI 声明
  - AnalyticsPage: 趋势图→GET /api/statistics/trend, 饼图→GET /api/statistics/category-summary, 预算 vs 实际→GET /api/budget/progress

- **被检集**: API_DESIGN §2 全部 26 个接口

- **差集**:
  - UI 按钮有 / API 缺: ImportPage「确认导入」对应 `POST /api/transaction/import` → API_DESIGN 无此接口 → **issue-5**（与维度 5/6.1 合并）
  - API 有 / UI 无: **无**
- **结论**: 1 条差集 issue（P2 CSV 导入）。

#### 6.4 错误码段 ↔ TECH §5 错误码规范对账

- **参考集**（PRD 附：统一错误码规范 · TECH §5 对齐）:
  - 1001-1099 用户 / 2001-2099 账户 / 3001-3099 交易 / 4001-4099 预算 / 5001-5099 周期账单

- **被检集**（API_DESIGN §4.3 业务异常码）:
  - 1001-1099 用户 ✓ / 2001-2099 账户 ✓ / 3001-3099 交易 ✓ / 4001-4099 预算 ✓ / 5001-5099 周期账单 ✓

- **差集**:
  - 所有 code 均落在 PRD/TECH 定义的段内，无自创 code（如 9999/8888）→ 无越界 issue
  - **但是**: code 5002 在 §3 不同接口中用于三种不同场景（见 issue-8）→ 编号一致性问题

- **结论**: 错误码段边界对账通过。编号一致性问题在维度 2 issue-8 处理。

---

### 维度 7: 反例推演

#### 7.1 资源不存在推演

对每个含 `{id}` 路径参数的接口，推演"客户端传入不存在的 id"场景：

| 接口 | API_DESIGN 异常响应是否明示"不存在" | 推演结论 |
|---|---|---|
| PUT /api/account/{id} | ❌ 无。仅有 2003（账户已禁用）| 假设 id=99999 不存在 → Service 查询返回 null → Controller 无定义 → 不同 coder 可能返回 200+null 或 500 → **issue-7** |
| DELETE /api/account/{id} | ❌ 无。仅有 2002（关联数据）和 2003（已禁用）| 假设 id=99999 不存在 → Service 无数据可查 → 未定义行为 → **issue-9** |
| PUT /api/transaction/{id} | ❌ 无。3002 语义是"账户或分类不存在"，不是"记录不存在" | 假设 id=99999 不存在 → 3002 message 误导（实际是记录不存在，不是账户/分类不存在） → **issue-11** |
| DELETE /api/recurring-bill/{id} | ❌ 无。仅有 5002（已停用）| 假设 id=99999 不存在 → 未定义行为 → **issue-10** |
| POST /api/recurring-bill/{id}/generate | ✅ 有：5002 "周期性账单不存在" | 推演通过 |

**结论**: 4 个含 `{id}` 接口缺少"资源不存在"异常响应定义（issue-7/9/10/11 · 高严重度）。

#### 7.2 越权推演（行级权限）

本系统为单一用户角色（普通用户），无角色间越权场景。

行级权限推演：
- 所有写操作接口（POST/PUT/DELETE）均声明 `user_id = currentUserId` → 用户 A 无法修改用户 B 的数据 ✓
- GET 接口（列表/余额/统计）均隐含 userId 过滤（通过 JWT 获取）→ 用户 A 只看自己的数据 ✓

**结论**: 无 issue。行级权限覆盖完整。

#### 7.3 并发幂等推演

| 接口 | 幂等性方案 | 推演结论 |
|---|---|---|
| DELETE /api/account/{id} | 条件 UPDATE（`WHERE id=? AND status=1`）+ affectedRows | 连点 2 次：第 1 次成功 → 第 2 次 affectedRows=0 → 返回 2003 ✓ |
| POST /api/budget | 唯一约束 + INSERT ON DUPLICATE KEY UPDATE | 连点 2 次：同一条记录覆盖更新 ✓ |
| DELETE /api/recurring-bill/{id} | 条件 UPDATE（`WHERE id=? AND status=1`）+ affectedRows | 同 account 逻辑 ✓ |
| POST /api/recurring-bill/{id}/generate | 条件 UPDATE（`WHERE next_due_date = 当前值`）+ affectedRows + @Transactional | 连点 2 次：第 1 次推进到期日 → 第 2 次 affectedRows=0 → 返回 5003 ✓ |
| POST /api/transaction | 无（教学简化，PRD 明确允许重复） | 连点 2 次 → 2 条记录 → PRD 已声明此行为 ✓ |
| POST /api/transaction/transfer | 无（教学简化，前端 loading 防连点） | 连点 2 次 → 2 组转账 → PRD 已声明此后果 ✓ |

**结论**: 无 issue。状态变更类接口均有幂等性方案，非幂等接口 PRD 已明确声明教学简化。

#### 7.4 分页/边界/空集合推演

| 接口 | pageSize 上限 | 空集合返回 | 排序白名单 | 结论 |
|---|---|---|---|---|
| GET /api/transaction | ✅ 1-100 | ✅ `data.records: []` | ✅ id/time/create_time | ✓ |
| GET /api/account | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/recurring-bill | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/budget | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/budget/progress | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/budget/alert | 非分页 | ✅ `data: []` | N/A | ✓ |
| GET /api/statistics/* (4个) | 非分页 | ✅ 有说明或隐含 | N/A | ✓ |

**结论**: 无 issue。分页约束完整，空集合返回 `[]` 均已声明。

---

## 修复行动建议

按严重度排序：

1. **issue-7** [高] PUT /api/account/{id} 补充"资源不存在"异常响应 → 追加 code 2003 行
2. **issue-9** [高] DELETE /api/account/{id} 补充"资源不存在"异常响应 → 追加 code 2003 行
3. **issue-10** [高] DELETE /api/recurring-bill/{id} 补充"资源不存在"异常响应 → 追加 code 5002 行
4. **issue-11** [高] PUT /api/transaction/{id} 修正"资源不存在"响应 → 3002 语义歧义需新增独立 code 或明示
5. **issue-8** [高] 错误码 5002 多义性 → 拆分为不同 code 或在 message 中区分
6. **issue-5** [中] 补充 POST /api/transaction/import 接口（P2-3 CSV 导入）
7. **issue-6** [中] 补充 GET /api/exchange-rate 接口（P2-4 多币种）
8. **issue-2** [中] change-password 接口补充教学简化说明
9. **issue-1** [低] §1 资源命名约定改为单数（与实际一致）
10. **issue-3** [低] §4 子节编号重排
11. **issue-4** [低] §1 时间格式标注明确（自定义格式 vs ISO 8601）
