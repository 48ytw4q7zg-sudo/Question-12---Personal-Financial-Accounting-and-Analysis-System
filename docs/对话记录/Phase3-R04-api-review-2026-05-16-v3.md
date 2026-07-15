# Phase 3 R-04 API 设计审核报告（第三轮）· 2026-05-16

## 审核元数据

- 审核日期: 2026-05-16
- 使用模型: mimo-v2-pro（V4 Pro · 同源自审）
- 输入摘要: docs/API_DESIGN.md · 28 个接口 · 8 个业务模块 · §1-§4 结构（R-04 第二轮修复后版本）
- 审核背景: 第一轮 R-04 修复 11 条 issue（5 高 / 3 中 / 3 低）；第二轮修复 4 条 issue（1 高 / 1 中 / 2 低）；本轮为第二轮修复后复审

## 审核报告

### 维度 1: RESTful 规范性

无新 issue。

- 统一前缀 `/api` ✓
- 统一 `Result<T>` 响应格式 ✓
- HTTP 方法正确（GET 查询 / POST 创建 / PUT 更新 / DELETE 删除）✓
- 资源用单数名词，与 §1 约定一致 ✓

---

### 维度 2: 完整性

- **issue-5** [严重度: 低]: §2 总计行（header）P0/P1/P2 计数与 §2 表格实际标签不一致
  - **位置**: API_DESIGN.md §2 末尾总计行（line 94）
  - **问题**: 总计行写「P0: 14 个 / P1: 12 个 / P2: 2 个」，但逐一核对 §2 表格中每个接口的「实现优先级」标签：
    - P0（11 个）: #1 注册 / #2 登录 / #4 账户列表 / #5 新增账户 / #6 修改账户 / #7 删除账户 / #8 账户余额 / #9 分类列表 / #10 收支列表 / #11 记一笔 / #12 修改收支
    - P1（13 个）: #3 改密码 / #13 转账 / #14 预算列表 / #15 保存预算 / #16 预算进度 / #18 月度汇总 / #19 年度汇总 / #20 分类汇总 / #22 周期账单列表 / #23 创建周期账单 / #24 修改周期账单 / #25 停用周期账单 / #26 生成收支
    - P2（4 个）: #13b CSV 导入 / #17 预算预警 / #21 月度趋势 / #27 汇率查询
    - 合计 28，与总数一致 ✓
  - **根因**: 第二轮 issue-2 修复将 header 从「P0: 13 / P1: 11」改为「P0: 14 / P1: 12」，但修正值本身与表格标签不匹配（表格 P0 实际 11 个，非 14 个）。PRD §3 全量 P0=11 / P1=13 / P2=4，表格标签与 PRD 一致，header 计数错误。
  - **修复建议**: 修正 §2 总计行为「P0: 11 个 / P1: 13 个 / P2: 4 个」

- **issue-6** [严重度: 低]: §4 末尾总计行 P0/P1/P2 计数与 §2 表格实际不一致
  - **位置**: API_DESIGN.md line 1426
  - **问题**: 文档末尾写「P0: 13 个 / P1: 11 个 / P2: 4 个」，既不匹配 §2 header（P0:14/P1:12/P2:2），也不匹配 §2 表格实际标签（P0:11/P1:13/P2:4）。此行为第一轮遗留的旧计数，两轮修复均未更新。
  - **修复建议**: 修正为「P0: 11 个 / P1: 13 个 / P2: 4 个」（与 §2 header + 表格标签 + PRD 三者统一）

其余完整性检查通过：每接口 5 项必填齐全、§4 全局异常码 5 项齐全（400/401/403/404/500）、业务异常码按模块编号。

---

### 维度 3: 一致性

无新 issue。

- 统一前缀 `/api` ✓
- 统一 `Result<T>` 响应格式 ✓
- 参数 camelCase 命名 ✓
- §3 所有错误 code 均在 §4.3 表中有出处 ✓
- **跨文档参考（非 API_DESIGN 内部问题）**: TECH_DESIGN §5 日期映射行写「DATETIME → `LocalDateTime`（Jackson ISO 8601 序列化）」，但 API_DESIGN §1 已改为「自定义格式 `yyyy-MM-dd HH:mm:ss`（非标准 ISO 8601）」。Phase 4 配置 Jackson 时需同步确认 TECH_DESIGN 该行描述是否更新（不在本审核修改范围内）。

---

### 维度 4: 安全性

无新 issue。

- JWT 鉴权正确：仅 register/login 公开，其余全部需登录 ✓
- 行级权限：写操作接口均声明 `user_id = currentUserId` 隔离 ✓
- 密码安全：LoginResponse 不含 password 字段（Entity 用 `@JsonIgnore`）✓
- 敏感操作：DELETE 账户有幂等性方案、transfer 有 @Transactional 事务保护 ✓

---

### 维度 5: 业务覆盖

无新 issue。

- PRD §3 全量功能（P0+P1+P2）全部覆盖 ✓
- 接口实现优先级标签与 PRD 一致（表格标签正确，仅 header/footer 计数笔误，见 issue-5/6）✓
- 无跨档依赖（P0 接口不依赖 P1/P2 接口）✓

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
  - **结论**: 对账通过，无 issue。

#### 6.2 接口 ↔ DATABASE 字段对账

- **account 表**: id(BIGINT)→Long ✓, user_id(BIGINT)→Long ✓, name(VARCHAR)→String ✓, type(TINYINT)→Integer ✓, initial_balance(DECIMAL)→BigDecimal ✓, currency(VARCHAR)→String ✓, status(TINYINT)→Integer ✓
- **category 表**: id/name/type 一致 ✓
- **transaction 表**: 全部字段一致 ✓（含 transfer_id VARCHAR→String）
- **budget 表**: 全部字段一致 ✓
- **recurring_bill 表**: 全部字段一致 ✓（next_due_date DATE→String 兼容）
- **新增接口**: POST /api/transaction/import 用 multipart/form-data（file + accountId），accountId 对应 account.id ✓；GET /api/exchange-rate 返回硬编码汇率（无数据库表）✓
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
  - API 有 / UI 无: GET /api/exchange-rate 无直接 UI 按钮对应（内部工具接口，AccountPage 币种字段内部调用）→ 合理
- **结论**: 对账通过，无 issue。

#### 6.4 错误码段 ↔ TECH §5 错误码规范对账

- **参考集**（TECH §5 错误码规范）:
  - 业务码段: 1xxx 用户 / 2xxx 账户 / 3xxx 交易 / 4xxx 预算 / 5xxx 周期账单

- **被检集**（API_DESIGN §3 实际使用 code + §4.3 示例）:
  - 1001/1002 ✓ / 2002/2003 ✓ / 3001/3002/3003/3004/3005/3006 ✓ / 4001/4002 ✓ / 5001/5002/5003/5004/5005 ✓

- **差集**:
  - 所有 code 均落在 TECH 定义的模块段内，无越界 code ✓
  - **遗留**: code 3002 在 POST/PUT /api/transaction 中同时用于"账户不存在或已禁用"和"分类不存在"两种场景（上一轮已声明，教学简化可接受）
- **结论**: 错误码段边界对账通过。3002 多义性为已知遗留。

---

### 维度 7: 反例推演

#### 7.1 资源不存在推演

对每个含 `{id}` 路径参数的接口：

| 接口 | 异常响应是否明示"不存在" | 推演结论 |
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
- 单一用户角色，无角色间越权场景 ✓

**结论**: 无 issue。

#### 7.3 并发幂等推演

| 接口 | 幂等性方案 | 推演结论 |
|---|---|---|
| DELETE /api/account/{id} | 条件 UPDATE + affectedRows → 2003 ✓ |
| POST /api/budget | 唯一约束 + ON DUPLICATE KEY UPDATE ✓ |
| DELETE /api/recurring-bill/{id} | 条件 UPDATE + affectedRows → 5004 ✓ |
| POST /api/recurring-bill/{id}/generate | 条件 UPDATE + affectedRows → 5003 ✓ |
| POST /api/transaction | 无（教学简化）✓ |
| POST /api/transaction/transfer | 无（教学简化 + 前端 loading）✓ |

**结论**: 无 issue。幂等性方案完整，描述与异常码表一致。

#### 7.4 分页/边界/空集合推演

| 接口 | pageSize 上限 | 空集合返回 | 排序白名单 | 结论 |
|---|---|---|---|---|
| GET /api/transaction | ✅ 1-100 | ✅ `data.records: []` | ✅ id/time/create_time | ✓ |
| 其余列表接口 | 非分页 | ✅ `data: []` | N/A | ✓ |

**结论**: 无 issue。

---

## 修复行动建议

按严重度排序（本轮新发现 2 条，均为低严重度）:

1. **issue-5** [低] §2 总计行修正 P0/P1/P2 计数为「P0: 11 个 / P1: 13 个 / P2: 4 个」（与 §2 表格标签 + PRD 一致）
2. **issue-6** [低] §4 末尾总计行修正为「P0: 11 个 / P1: 13 个 / P2: 4 个」（与 §2 统一）

### 上一轮遗留

- **3002 多义性**: code 3002 在 POST/PUT /api/transaction 中同时用于"账户不存在或已禁用"和"分类不存在"两种场景。第一轮新增 3006 解决了"记录不存在"问题，但 3002 的账户/分类歧义仍在。教学简化可接受（前端按 message 区分），但生产环境应拆为独立 code。

### 三轮审核总结

| 轮次 | 新发现 issue 数 | 高/中/低 |
|---|---|---|
| 第一轮 | 11 | 5 高 / 3 中 / 3 低 |
| 第二轮 | 4 | 1 高 / 1 中 / 2 低 |
| 第三轮 | 2 | 0 高 / 0 中 / 2 低 |
| **合计** | **17** | **6 高 / 4 中 / 7 低** |

三轮审核累计发现 17 条 issue，全部已修复或待修复（本轮 2 条）。API 设计质量逐步收敛，核心业务逻辑、安全性、跨文档对账均通过。剩余 2 条均为计数笔误，不影响实现。
