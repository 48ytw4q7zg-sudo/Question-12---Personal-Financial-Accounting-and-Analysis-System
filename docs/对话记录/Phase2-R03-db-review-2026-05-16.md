# Phase 2 R-03 数据库设计审核报告 · 2026-05-16

## 审核元数据

- 审核日期: 2026-05-16
- 使用模型: V4 Pro（同源自审 · 教学可接受）
- 输入摘要: `docs/DATABASE_DESIGN.md` v2.0 · 6 表 · 业务字段约 35 个

---

## 审核报告

### 维度 1: 完整性

- **结论**: ✅ 通过，无 issue
- 6 表覆盖 PRD §3 全量功能（P0-1~P0-6 + P1-1~P1-7 + P2-1~P2-5）:
  - user → P0-1 登录/JWT
  - account → P0-2 账户 CRUD + P0-5 余额汇总
  - category → P0-3 分类列表 + P0-6 分类浏览
  - transaction → P0-4 收支记录 + P1-1 多条件筛选 + P1-5 转账
  - budget → P1-3 预算管理
  - recurring_bill → P1-4 周期性账单
- §2 表清单「实现优先级」列与 PRD §3 各功能优先级一致
- 无跨档依赖（P0 表不依赖 P1/P2 表）
- 核心实体表名与 `docs/00-选题标定.md §一` 一致：user / account / category / transaction（原文 4 个核心实体全部对齐）
- N:M 关系：无（本系统无 N:M 实体关系，所有关系均为 1:N）

---

### 维度 2: 范式

- **结论**: ✅ 通过，无 issue
- 无冗余字段：余额实时计算（初始余额 + SUM 收入 - SUM 支出），不在 transaction 表存储余额
- 无一对多反向建表：所有 1:N 关系正确通过子表外键字段实现
- 软删除通过 status 字段实现（account.status / recurring_bill.status），未使用 is_deleted 字段，但 §4 约定 #4 已明确说明此策略且与 PRD §3 P0-2/P1-4 的禁用/停用流程一致

---

### 维度 3: 字段类型

- **结论**: ✅ 通过，无 issue
- VARCHAR 长度合理：username(20) / name(20) / name-category(10) / name-bill(30) / note(200) / transfer_id(36)
- 金额字段全部使用 `DECIMAL(12,2)`，未使用 FLOAT/DOUBLE
- 时间字段全部使用 `DATETIME`，未使用 TIMESTAMP
- 所有 6 张表均声明 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`
- 所有 6 张表均显式声明 `ENGINE=InnoDB`

---

### 维度 4: 约束

- **issue-1** [严重度: 中]:
  - **问题**: §3 所有 CREATE TABLE 不含 `FOREIGN KEY` 约束；§4 约定 #8 声明「不建物理外键约束（教学简化，应用层校验关联完整性）」但未说明选择理由
  - **位置**: DATABASE_DESIGN §4 约定 #8 + §3 所有 CREATE TABLE
  - **风险**: 若 Service 层遗漏关联校验（如删除账户时未检查 transaction 引用），数据库层面无兜底约束，可能产生孤立记录（account_id 指向不存在的 account）
  - **修复建议**: 在 §4 约定 #8 补充理由注释：「教学简化：物理外键会增加建表顺序复杂度 + 阻止级联删操作灵活性，应用层通过 PRD §3 异常流程②的检查逻辑保证完整性」；同时在 §3 每个外键字段 COMMENT 中注明对应的 PRD 异常流程编号

- NOT NULL: 合理（业务必填字段均 NOT NULL，可空字段 note/transfer_id 合理 NULL）
- DEFAULT: 合理（create_time DEFAULT CURRENT_TIMESTAMP / update_time ON UPDATE CURRENT_TIMESTAMP / status DEFAULT 1 / initial_balance DEFAULT 0.00 / currency DEFAULT 'CNY'）
- UNIQUE: user.username 唯一索引正确（防并发注册重复）
- 外键完整性: 所有关联字段均有索引（见维度 5），完整性由应用层保证

---

### 维度 5: 索引

- **issue-2** [严重度: 低]:
  - **问题**: recurring_bill 表仅对 user_id 建索引，缺少 account_id 和 category_id 的索引
  - **位置**: DATABASE_DESIGN §3.6 recurring_bill CREATE TABLE
  - **影响**: PRD P1-4 异常流程②要求「一键生成时校验关联账户 status=1」，需按 account_id 查询；虽当前 API 设计中 account_id 查询不是主路径，但加索引后可覆盖未来按账户/分类筛选场景
  - **修复建议**: 在 recurring_bill CREATE TABLE 中追加 `KEY idx_recurring_bill_account_id (account_id)` 和 `KEY idx_recurring_bill_category_id (category_id)`

- 其余索引检查通过:
  - account: `idx_account_user_id` (user_id) ✓
  - transaction: `idx_transaction_user_id` (user_id) / `idx_transaction_account_id` (account_id) / `idx_transaction_time` (user_id, time) 复合索引 / `idx_transaction_transfer_id` (transfer_id) ✓
  - budget: `uk_budget_user_category_month` 唯一索引 / `idx_budget_user_month` (user_id, month) ✓
  - user: `uk_user_username` 唯一索引 ✓
- 无冗余索引（无联合索引 (a,b) + 单独 (a) 的冗余情况）
- 索引命名规范: `idx_` / `uk_` 前缀一致

---

### 维度 6: 跨文档对账（4 类强制对账）

#### 6.1 字段 ↔ PRD 字段引用对账

- **参考集**（PRD §3 全量功能提到的业务字段）:
  - user: username, password, create_time
  - account: name, type{1:现金,2:银行卡,3:支付宝,4:微信}, initial_balance, status{1:正常,0:禁用}
  - category: name, type{1:支出,2:收入}
  - transaction: type{1:收入,2:支出}, amount, note, time, transfer_id
  - budget: category_id, month, amount
  - recurring_bill: name, amount, type{1:收入,2:支出}, account_id, category_id, period{monthly,weekly}, next_due_date, status{1:活跃,0:停用}
- **被检集**（DATABASE_DESIGN §3 各表业务字段）:
  - user: username ✓, password ✓
  - account: name ✓, type ✓, initial_balance ✓, currency ✓, status ✓
  - category: name ✓, type ✓
  - transaction: type ✓, amount ✓, note ✓, time ✓, transfer_id ✓
  - budget: category_id ✓, month ✓, amount ✓
  - recurring_bill: name ✓, amount ✓, type ✓, account_id ✓, category_id ✓, period ✓, next_due_date ✓, status ✓
- **差集**: 无差集
- **结论**: ✅ 对账通过，无 issue

#### 6.2 表 ↔ TECH_DESIGN §2 模块对账

- **参考集**（TECH §2 关键类示例）: User / Account / Transaction / Budget / RecurringBill / Category
- **被检集**（DATABASE §2 表清单）: user / account / category / transaction / budget / recurring_bill
- **差集**: 无差集（6 表 ↔ 6 实体类一一对应）
- **结论**: ✅ 对账通过，无 issue

#### 6.3 外键 ↔ PRD 业务关系对账

- **DATABASE 外键字段**（COMMENT 中标注的关联）:
  - account.user_id → user（账户属于用户）
  - transaction.user_id → user（记录属于用户）
  - transaction.account_id → account（记录关联账户）
  - transaction.category_id → category（记录关联分类）
  - budget.user_id → user（预算属于用户）
  - budget.category_id → category（预算关联分类）
  - recurring_bill.user_id → user（账单属于用户）
  - recurring_bill.account_id → account（账单关联账户）
  - recurring_bill.category_id → category（账单关联分类）
- **PRD 业务关系出处**:
  - account → user: P0-2「用户管理多账户」✓
  - transaction → user: P0-4「用户记一笔收支」✓
  - transaction → account: P0-4「选择账户」✓
  - transaction → category: P0-4「选择分类」✓
  - budget → user: P1-3「用户按分类设置月预算」✓
  - budget → category: P1-3「为每个支出分类设置预算」✓
  - recurring_bill → user: P1-4「用户设置周期性收支」✓
  - recurring_bill → account: P1-4「关联账户」+ P0-2 异常流程②「删除账户时检查 recurring_bill 引用」✓
  - recurring_bill → category: P1-4「关联分类」✓
- **差集**: 无差集
- **结论**: ✅ 对账通过，无 issue

#### 6.4 优先级一致对账

- **DATABASE §2 表优先级 vs PRD §3 功能优先级**:
  - user (P0) → PRD P0-1 最低 = P0 ✓
  - account (P0) → PRD P0-2 最低 = P0 ✓
  - category (P0) → PRD P0-3 最低 = P0 ✓
  - transaction (P0) → PRD P0-4 最低 = P0 ✓
  - budget (P1) → PRD P1-3 最低 = P1 ✓
  - recurring_bill (P1) → PRD P1-4 最低 = P1 ✓
- **差集**: 无差集
- **跨档依赖检查**: 无 P0 表依赖 P1/P2 表
- **结论**: ✅ 对账通过，无 issue

---

### 维度 7: 反例推演

#### 7.1 删除推演（逐外键 ON DELETE 行为）

本设计 §4 约定 #8 声明「不建物理外键约束」，因此所有外键字段均无 `ON DELETE` 行为定义。以下逐外键推演:

- **account.user_id → user**: 无用户删除流程（PRD 无「删除用户」功能）→ 无删除冲突风险
- **transaction.user_id → user**: 同上
- **transaction.account_id → account**:
  - 推演: 假设用户删除一个有 5 条 transaction 的 account → 无物理 FK → 数据库不报错 → 但 transaction.account_id 指向已禁用的 account → 余额统计会将该账户交易纳入（因为统计查 status=1 的 account 再 JOIN transaction）→ **业务层通过 PRD P0-2 异常流程② 的检查逻辑在 Service 层阻止删除** → 安全
  - 风险: 若 Service 层遗漏检查 → 孤立记录 → 余额统计可能异常
  - **issue-3** [严重度: 中]:
    - **问题**: §3 CREATE TABLE 不含 FOREIGN KEY 约束，§4 约定 #8 声明不建物理外键但缺少业务理由注释
    - **位置**: DATABASE_DESIGN §4 约定 #8
    - **推演结论**: 无物理 FK 时，删除保护完全依赖 Service 层 PRD P0-2 异常流程② 的检查逻辑。教学场景可接受，但需在 §4 约定 #8 补充「选择理由 + 应用层对应 PRD 条目」
    - **修复建议**: §4 约定 #8 改为：「不建物理外键约束（教学简化: ① 物理 FK 增加建表顺序依赖 ② 删除保护由 Service 层实现,对齐 PRD §3 P0-2 异常流程② + P1-4 异常流程② 的检查逻辑 ③ 无物理级联删需求）」

- **transaction.category_id → category**: category 为种子数据，无删除流程 → 无风险
- **budget.user_id → user**: 无用户删除流程 → 无风险
- **budget.category_id → category**: category 无删除流程 → 无风险
- **recurring_bill.user_id → user**: 无用户删除流程 → 无风险
- **recurring_bill.account_id → account**:
  - 推演: 假设用户禁用一个被 2 个 recurring_bill 引用的 account → 无物理 FK → 数据库不报错 → Service 层应按 PRD P0-2 异常流程② 检查后拒绝禁用 → 安全
  - **同 issue-3 覆盖**
- **recurring_bill.category_id → category**: category 无删除流程 → 无风险

#### 7.2 NULL 推演（逐可空字段）

- **transaction.note DEFAULT NULL**:
  - NULL 语义: 用户未填写备注 → 流水列表展示为空 → 编辑弹窗备注字段为空 → PRD P0-4 异常流程无备注约束 ✓
  - 已在 §3.4 COMMENT 中明确「NULL=无备注」 ✓
- **transaction.transfer_id DEFAULT NULL**:
  - NULL 语义: 普通收支记录 → 流水列表正常展示编辑按钮 → PRD P1-5 业务规则③明确「NULL 表示普通收支记录」 ✓
  - 非 NULL 语义: 转账关联记录 → 流水列表标记转出/转入 + 隐藏编辑按钮 → PRD P0-4 异常流程②「transfer_id 非空时禁止修改金额」 ✓
  - 已在 §3.4 COMMENT 中明确 NULL/非 NULL 双向语义 ✓
- **结论**: ✅ 推演通过，无 issue

#### 7.3 并发推演（状态 / 余额类字段）

- **account.status**:
  - 并发场景: 用户快速双击「禁用」按钮 → 两个请求同时到达
  - 保护方案: 条件 UPDATE (`WHERE id=? AND status=1`) → 第一个请求 affectedRows=1 成功 → 第二个请求 affectedRows=0 → Service 层判断后返回「已禁用」→ 安全 ✓
- **recurring_bill.status**:
  - 并发场景: 用户快速双击「停用」按钮
  - 保护方案: 同 account.status 的条件 UPDATE → 安全 ✓
- **recurring_bill.next_due_date**:
  - 并发场景: 用户快速双击「生成」按钮 → 两个请求同时尝试生成同一到期日的收支记录
  - 保护方案: 条件 UPDATE (`WHERE next_due_date = 当前值`) → affectedRows=0 时抛 BusinessException(5003) → INSERT 在同一 @Transactional 内 → 安全 ✓
  - 已在 §3.6 说明此并发保护方案 ✓
- **budget 并发插入**:
  - 并发场景: 用户快速双击「保存预算」→ 两个 INSERT 同 user_id + category_id + month
  - 保护方案: UNIQUE KEY `uk_budget_user_category_month` → 第二个 INSERT 报 DuplicateKeyException → Service 层转为 UPDATE → 安全 ✓
- **余额并发**:
  - 并发场景: 两个转账同时操作同一账户
  - 保护方案: @Transactional + REPEATABLE READ 隔离级别 → 事务内余额检查与 INSERT 共享快照 → 安全 ✓
  - 已在 §3.4 说明此方案 ✓
- **结论**: ✅ 推演通过，无 issue

#### 7.4 精度 / 类型推演

- **金额字段**: 全部使用 `DECIMAL(12,2)` → 推演: 存 0.10 + 0.20 = 0.30 精确无误差 → 安全 ✓
- **时间字段**: 全部使用 `DATETIME` → 推演: 范围 1000-9999 年，不受 2038 问题影响 → 安全 ✓
- **跨时区**: 教学场景统一 Asia/Shanghai（见 application.yml serverTimezone），无需 UTC 转换 → 安全 ✓
- **结论**: ✅ 推演通过，无 issue

---

## 修复行动建议

按严重度排序:

| 优先级 | issue | 严重度 | 修复项 |
|:---:|---|:---:|---|
| 1 | issue-1 | 中 | §4 约定 #8 补充「不建物理外键」的理由注释 + 应用层对应 PRD 条目 |
| 2 | issue-2 | 低 | §3.6 recurring_bill CREATE TABLE 追加 account_id + category_id 索引 |
| 3 | issue-3 | 中 | 同 issue-1 修复内容（删除推演角度补充 §4 约定 #8 说明） |

**修复后**: 调用 `/db-designer 应用修复` 扫描 R-03 注释逐条修复 + 自动同步 `sql/01-init.sql`。
