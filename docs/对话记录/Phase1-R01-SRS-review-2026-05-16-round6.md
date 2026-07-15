# Phase 1 R-01 SRS 审核报告（第 6 轮）· 2026-05-16

## 审核元数据
- 审核日期: 2026-05-16
- 使用模型: mimo-v2-pro（与前 5 轮相同对话模型 · 同源自审）
- 输入摘要: `docs/PRD.md`（610 行 · 6 节结构 · §3 含 P0×6 + P1×7 + P2×5 共 18 个功能 · §5 含 18 行映射表）
- 前轮已审复核: 是（读过 R-01 第 1-5 轮全部 issue 注释 · 全部标注「已修复」· 本轮不重复审已修复 issue）
- 审核背景: 第 5 轮修复闭环后，全维度最终扫描确认 PRD 成熟度

## 审核报告

### 维度 1: 完整性

- **§2 角色清单**: 单一用户角色 · 与 CLAUDE.md 起手段一致 ✓
- **§3 功能数量**: P0-1 到 P0-6 (6) + P1-1 到 P1-7 (7) + P2-1 到 P2-5 (5) = 18 个 ✓
- **标定卡对标**: 标定卡 §三 P0 5 项 → PRD P0-1~P0-5 + P0-6(分类浏览页)；标定卡 §四 P1 5 项 → PRD P1-1~P1-6 + P1-7(用户设置)；标定卡 §五 P2 3 项 → PRD P2-1~P2-5 ✓
- **8 字段完整性**: 全部 18 个功能均有完整 8 字段块（实现优先级 / 描述 / 前置条件 / 主流程 / 异常流程 / 业务规则 / API 形态 / 关联页面）✓
- **实现优先级取值**: 全部 ∈ {P0 必做, P1 应做, P2 可选} ✓

未发现 issue。

### 维度 2: 一致性

- **§5 ↔ §3 编号**: 18 行映射对 18 个功能，编号一一对应 ✓
- **§5 ↔ §3 优先级**: P0 6 行 / P1 7 行 / P2 5 行，全部一致 ✓
- **功能间无矛盾**: 单一用户角色，无跨角色矛盾 ✓
- **API 复用无冲突**: P0-6 与 P1-6 共用 `GET /api/statistics/category-summary`，参数一致 ✓

未发现 issue。

### 维度 3: 可行性

- **P0 数量**: 6 个功能 + 约 12 个接口，在标定卡 P0 锚点（4 表 / ~10 接口 / 5 页面）范围内 ✓
- **主流程复杂度**: 全部 ≤ 4 步 ✓
- **外部依赖**: 无；CSV 为本地上传 ✓

未发现 issue。

### 维度 4: 明确性

- **模糊表述扫描**: 全文无「等功能」「相关信息」「根据需要」✓
- **数值范围**: 用户名 3-20 / 密码 6-20 / 账户名 1-20 / 备注 ≤ 200 / 金额 DECIMAL(12,2) / CSV ≤ 5MB / 导入 ≤ 1000 条 ✓
- **权限**: 每个功能均有「用户只能操作自己的 XXX · 通过 JWT userId 过滤」✓

未发现 issue。

### 维度 5: 业务规则

#### 5.1 边界 / 异常 / 权限

- 金额 ≤ 0 → 400（P0-4, P1-3, P1-4, P1-5）✓
- 时间范围最大跨度 1 年（P1-1）✓
- 权限均为 JWT userId 行级过滤 ✓

未发现 issue。

#### 5.2 CRUD 完整性

| 实体 | 创建 | 读取 | 更新 | 删除 | 删除依赖处理 |
|---|---|---|---|---|---|
| user | P0-1 注册 | P1-7 用户名(JWT) | P1-7 改密码 | 不删除 | N/A |
| account | P0-2 新增 | P0-2 列表 | P0-2 编辑 | P0-2 软删 | ✅ 检查 transaction + recurring_bill |
| category | 种子数据 | P0-3 列表 + P0-6 浏览 | 不支持 | 不支持 | N/A |
| transaction | P0-4 记一笔 | P0-4 列表 | P0-4 编辑 | 不支持 | N/A |
| budget | P1-3 创建 | P1-3 列表 | P1-3 覆盖写 | 不支持 | N/A |
| recurring_bill | P1-4 创建 | P1-4 列表 | P1-4 编辑 | P1-4 停用 | ✅ 软删 |

全部通过。

#### 5.3 API ↔ UI 入口对照

**孤儿 API 检查**:

| API | 来源 | UI 入口 | 结论 |
|---|---|---|---|
| POST /api/user/register | P0-1 | LoginPage 注册按钮 | ✓ |
| POST /api/user/login | P0-1 | LoginPage 登录按钮 | ✓ |
| GET/POST/PUT/DELETE /api/account | P0-2 | AccountPage 列表/新增/编辑/禁用 | ✓ |
| GET /api/category | P0-3, P0-6 | CategorySelector + CategoryPage | ✓ |
| POST/PUT/GET /api/transaction | P0-4 | TransactionListPage 记一笔/编辑/列表 | ✓ |
| GET /api/account/balance | P0-5 | AccountPage 余额列 | ✓ |
| GET /api/statistics/category-summary | P0-6, P1-6 | CategoryPage + DashboardPage 图表 | ✓ |
| GET /api/transaction (筛选) | P1-1 | TransactionListPage 筛选器 | ✓ |
| GET /api/statistics/monthly | P1-2 | DashboardPage 汇总卡片 | ✓ |
| GET /api/statistics/yearly | P1-2 | DashboardPage 汇总卡片 | ✓ |
| GET/POST /api/budget | P1-3 | BudgetPage 列表/保存 | ✓ |
| GET /api/budget/progress | P1-3, P2-1 | BudgetPage 进度条 + AnalyticsPage | ✓ |
| GET/POST/PUT/DELETE /api/recurring-bill | P1-4 | RecurringBillPage 全部操作 | ✓ |
| POST /api/recurring-bill/{id}/generate | P1-4 | RecurringBillPage 生成按钮 | ✓ |
| POST /api/transaction/transfer | P1-5 | TransferPage 转账按钮 | ✓ |
| GET /api/statistics/trend | P2-1 | AnalyticsPage 趋势图 | ✓ |
| GET /api/budget/alert | P2-2 | DashboardPage 预警 + BudgetPage | ✓ |
| POST /api/transaction/import | P2-3 | ImportPage 上传 | ✓ |
| GET /api/exchange-rate | P2-4 | AccountPage 币种设置 | ✓ |
| POST /api/user/change-password | P1-7 | UserSettingsPage 保存修改 | ✓ |

全部通过，无孤儿 API。

未发现 issue。

### 维度 6: 阶段演进

#### 6.1 状态机 / 字段 / 角色 迁移说明

- account: status {1, 0} — P0 定义，P1/P2 未扩展 ✓
- recurring_bill: status {1, 0} — P1 定义，P2 未扩展 ✓
- P2-4 新增 account.currency（可空，默认 CNY）— 无需迁移 ✓
- 单一用户角色，无权限扩展 ✓

未发现 issue。

#### 6.2 教学简化的边界声明

全部 18 个功能的教学简化均已有显式声明（P0-1 硬编码 JWT / P0-2 软删不可恢复 / P0-3 种子数据 / P0-4 无撤销 / P0-5 实时计算 / P1-3 月粒度 / P1-4 手动触发 / P1-5 无撤销 / P1-6 单图表 / P1-7 无密码找回 / P2-2 硬编码阈值 / P2-3 单一 CSV 格式 / P2-4 硬编码汇率 / P2-5 Mockito mock）✓

#### 6.3 错误码 / 通用规约缺失

- §3 附录已定义 5 模块业务码段 + HTTP 状态码映射 ✓

未发现 issue。

### 维度 7: 反例推演

#### 7.1 删除依赖推演

| 实体 | 推演场景 | 结论 |
|---|---|---|
| account | 有 10 条 transaction → 点击禁用 | ✅ 拒绝 + 提示 |
| account | 有 3 个活跃 recurring_bill → 点击禁用 | ✅ 拒绝 + 提示 |
| account | 有 0 条 transaction + 3 个已停用 recurring_bill → 点击禁用 | ✅ 允许 |
| recurring_bill | 停用后 → 一键生成按钮消失 | ✅ 仅 status=1 可生成 |

#### 7.2 NULL / 空集合推演

| 字段 | 推演 | 结论 |
|---|---|---|
| transaction.note = NULL | 关键词搜索不匹配 | ✅ P1-1 已定义 |
| transaction.transfer_id = NULL | 普通收支正常展示 | ✅ P1-5 已定义 |
| budget 未设置 | 不参与超支判断 | ✅ P1-3 已定义 |
| 账户无 transaction | 余额 = 初始余额 | ✅ P0-5 已定义 |
| 全月无收支 | 收入=0/支出=0/结余=0 | ✅ P1-2 已定义 |
| 某分类本月无消费 | 金额显示为 0 | ✅ P0-6 已定义 |

#### 7.3 并发 / 重复操作推演

| 操作 | 推演 | 结论 |
|---|---|---|
| 转账连点 | 两次成功，金额翻倍 | ✅ P1-5 声明「不做幂等，前端 loading 防连点」 |
| 注册同用户名 | DB UNIQUE 兜底 | ✅ P0-1 已定义 |
| 预算重复保存 | 最后覆盖 | ✅ P1-3 已定义 |
| 收支重复提交 | 允许重复 | ✅ P0-4 已声明 |

#### 7.4 跨角色访问推演

- 不适用。单一用户角色，无跨角色场景。所有数据按 userId 隔离。
- 登录死循环: 已登录访问 /login → 守卫跳 `/`（DashboardPage）✓

---

## 修复行动建议

**本轮结论**: PRD.md 经过前 5 轮累计 17 条调整，本次第 6 轮全维度扫描 **未发现任何 issue**。7 维度全部通过，PRD 已完全成熟。

**建议**: 无需修复，PRD 可直接进入 Phase 1 Step 4 `/tech-designer`。
