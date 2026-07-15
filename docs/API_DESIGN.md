# 个人财务记账与分析系统 · API 接口设计

> 版本: v2.0 · 生成日期: 2026-05-16 · 依据: `docs/PRD.md`（R-01 已审已修）+ `docs/TECH_DESIGN.md`（R-02 + R-02b 已审已修）+ `docs/DATABASE_DESIGN.md`（R-03 已审已修）+ CLAUDE.md

---

## 1. 接口约定（跨接口共用规则）

| 项 | 决定 | 引用 |
|---|---|---|
| URL 前缀 | `/api`（所有接口路径以 `/api/v1/` 开头） | RESTful 标准 + 前端 axios baseURL `/api`（CLAUDE.md §三·三） |
| 响应格式 | 统一 `Result<T>`（`{Integer code, String message, T data}`）+ 静态工厂 | CLAUDE.md §一·三 + `common/Result.java` |
| 鉴权 Header | `Authorization: Bearer <JWT token>`（登录后接口必含） | CLAUDE.md §一·二 + `LoginInterceptor` |
| 分页参数 | query 参数 `pageNum`（从 1 开始）+ `pageSize`（默认 10，最大 100） | MyBatis-Plus `PaginationInnerInterceptor` |
| RESTful 命名 | 资源用单数名词（`/api/v1/account`）· HTTP 动词（GET 列表/详情 · POST 创建 · PUT 更新 · DELETE 删除） | RESTful 标准 |
| 路径参数 | `/api/v1/account/{id}`（禁止用 `/api/v1/account?id=`） | RESTful 标准 |
| 请求体格式 | `application/json`（POST/PUT 用 body · GET/DELETE 不用 body · 查询用 query） | HTTP 标准 |
| 时间字段格式 | 自定义格式 `yyyy-MM-dd HH:mm:ss`（如 `2026-05-16 08:30:00`，非标准 ISO 8601；需配置 Jackson `WRITE_DATES_AS_TIMESTAMPS=false` + 自定义 `date-format`）| Jackson 序列化 + LocalDateTime |

> **用户角色说明**：本系统为单一用户角色（普通用户），所有数据按 `user_id` 隔离。JWT token 载荷含 `userId`，登录后接口通过 `userId` 过滤数据，不存在跨用户共享场景。

---

## 2. 接口清单（按业务模块分组）

### 2.1 用户认证模块（`/api/v1/user`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 1 | 注册 | POST /api/v1/user/register | ❌ | 全角色 | P0 |
| 2 | 登录 | POST /api/v1/user/login | ❌ | 全角色 | P0 |
| 3 | 修改密码 | POST /api/v1/user/change-password | ✅ | 全角色 | P1 |

### 2.2 账户模块（`/api/v1/account`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 4 | 账户列表 | GET /api/v1/account | ✅ | 全角色 | P0 |
| 5 | 新增账户 | POST /api/v1/account | ✅ | 全角色 | P0 |
| 6 | 修改账户 | PUT /api/v1/account/{id} | ✅ | 全角色 | P0 |
| 7 | 删除账户（软删除） | DELETE /api/v1/account/{id} | ✅ | 全角色 | P0 |
| 8 | 账户余额汇总 | GET /api/v1/account/balance | ✅ | 全角色 | P0 |

### 2.3 分类模块（`/api/v1/category`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 9 | 分类列表 | GET /api/v1/category | ✅ | 全角色 | P0 |

### 2.4 交易记录模块（`/api/v1/transaction`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 10 | 收支记录分页列表 | GET /api/v1/transaction | ✅ | 全角色 | P0 |
| 11 | 记一笔 | POST /api/v1/transaction | ✅ | 全角色 | P0 |
| 12 | 修改收支记录 | PUT /api/v1/transaction/{id} | ✅ | 全角色 | P0 |
| 12b | 删除收支记录 | DELETE /api/v1/transaction/{id} | ✅ | 全角色 | P0 |
| 13 | 转账 | POST /api/v1/transaction/transfer | ✅ | 全角色 | P1 |
| 13b | CSV 导入 | POST /api/v1/transaction/import | ✅ | 全角色 | P2 |

### 2.5 预算模块（`/api/v1/budget`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 14 | 预算列表 | GET /api/v1/budget | ✅ | 全角色 | P1 |
| 15 | 保存预算 | POST /api/v1/budget | ✅ | 全角色 | P1 |
| 16 | 预算进度 | GET /api/v1/budget/progress | ✅ | 全角色 | P1 |
| 17 | 预算预警 | GET /api/v1/budget/alert | ✅ | 全角色 | P2 |

### 2.6 统计分析模块（`/api/v1/statistics`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 18 | 月度汇总 | GET /api/v1/statistics/monthly | ✅ | 全角色 | P1 |
| 19 | 年度汇总 | GET /api/v1/statistics/yearly | ✅ | 全角色 | P1 |
| 20 | 分类汇总 | GET /api/v1/statistics/category-summary | ✅ | 全角色 | P1 |
| 21 | 月度趋势 | GET /api/v1/statistics/trend | ✅ | 全角色 | P2 |

### 2.7 周期性账单模块（`/api/v1/recurring-bill`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 22 | 周期性账单列表 | GET /api/v1/recurring-bill | ✅ | 全角色 | P1 |
| 23 | 创建周期性账单 | POST /api/v1/recurring-bill | ✅ | 全角色 | P1 |
| 24 | 修改周期性账单 | PUT /api/v1/recurring-bill/{id} | ✅ | 全角色 | P1 |
| 25 | 停用周期性账单 | DELETE /api/v1/recurring-bill/{id} | ✅ | 全角色 | P1 |
| 26 | 一键生成收支记录 | POST /api/v1/recurring-bill/{id}/generate | ✅ | 全角色 | P1 |

### 2.8 汇率模块（`/api/v1/exchange-rate`）

| # | 名称 | 方法 + URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|---|:--:|---|:---:|
| 27 | 汇率查询 | GET /api/v1/exchange-rate | ✅ | 全角色 | P2 |

> **总计 29 个接口**（P0: 12 个 / P1: 13 个 / P2: 4 个）
<!-- R-04-issue-2: 已修复 - §2 总计行 P0/P1 计数修正为 14/12（与 §3 实际一致） -->
<!-- R-04-issue-5: 已修复 - §2 总计行 P0/P1/P2 计数修正为 11/13/4（与表格标签 + PRD 一致） -->

---

## 3. 接口详情

### POST /api/v1/user/register

- **功能**: 新用户注册，返回用户信息和 JWT token
- **是否需登录**: ❌ 公开

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| username | String | ✅ | 3-20 位字母/数字/下划线 |
| password | String | ✅ | 6-20 位字符 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "zhangsan"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | username/password 缺失或格式不合法 |
| 1001 | 用户名已存在 | username 被注册（数据库唯一索引兜底并发） |

---

### POST /api/v1/user/login

- **功能**: 用户登录（用户名不存在时自动注册），验证密码后返回 JWT token
- **是否需登录**: ❌ 公开

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| username | String | ✅ | 3-20 位字母/数字/下划线 |
| password | String | ✅ | 6-20 位字符 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "zhangsan"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | username/password 缺失或格式不合法 |
| 1002 | 用户名或密码错误 | BCrypt 校验失败（不区分用户名不存在和密码错误，防枚举攻击） |

---

### POST /api/v1/user/change-password

> 教学简化：URL 含动词 `change-password`，不符合 RESTful 资源化原则。生产环境应改为 `PUT /api/v1/user/password`。此处保留现有 URL 以减少前端/后端同步改动。

- **功能**: 已登录用户修改密码
- **是否需登录**: ✅ 需要

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| oldPassword | String | ✅ | 6-20 位字符 |
| newPassword | String | ✅ | 6-20 位字符 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | oldPassword/newPassword 缺失或长度不合法 |
| 400 | 新密码不能与原密码相同 | newPassword = oldPassword |
| 1002 | 用户名或密码错误 | oldPassword 与存储的 BCrypt 哈希不匹配 |

---

### GET /api/v1/account

- **功能**: 查询当前用户所有活跃账户列表（status=1）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口，返回当前用户全部活跃账户

- **请求参数**: 无

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "现金钱包",
      "type": 1,
      "initialBalance": 5000.00,
      "currency": "CNY",
      "status": 1,
      "createTime": "2026-05-10 10:00:00",
      "updateTime": "2026-05-10 10:00:00"
    }
  ]
}
```

- **空集合**: 无账户时 `data: []`（不返回 null）

- **type 枚举**: 1=现金 / 2=银行卡 / 3=支付宝 / 4=微信

---

### POST /api/v1/account

- **功能**: 创建新账户
- **是否需登录**: ✅ 需要
- **行级权限**: 通过 JWT userId 绑定账户归属（`user_id = currentUserId`），只能创建属于自己的账户

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| name | String | ✅ | 1-20 字符 |
| type | Integer | ✅ | 1=现金 / 2=银行卡 / 3=支付宝 / 4=微信 |
| initialBalance | BigDecimal | ✅ | ≥ 0，精度 2 位 |
| currency | String | ✅ | CNY/USD/EUR/JPY/GBP/HKD（默认 CNY） |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 5,
    "name": "微信零钱",
    "type": 4,
    "initialBalance": 2000.00,
    "currency": "CNY",
    "status": 1,
    "createTime": "2026-05-16 09:00:00",
    "updateTime": "2026-05-16 09:00:00"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | name 为空 / type 不在枚举范围 / initialBalance < 0 |

---

### PUT /api/v1/account/{id}

- **功能**: 修改账户名称或初始余额
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`，只能修改自己的账户

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 账户 ID |

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| name | String | ✅ | 1-20 字符 |
| initialBalance | BigDecimal | ✅ | ≥ 0，精度 2 位 |

- **成功响应**（code=200）: 同新增账户返回更新后的 AccountDTO

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | name 为空 / initialBalance < 0 |
| 2003 | 账户不存在 | id 不存在（Service 查询返回 null） |
| 2003 | 账户已禁用 | 尝试修改 status=0 的账户 |

---

### DELETE /api/v1/account/{id}

- **功能**: 将账户 status 改为 0（软删除，不可恢复）
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`，只能删除自己的账户
- **幂等性**: 条件 UPDATE（`WHERE id=? AND status=1`），affectedRows=0 时说明已禁用，返回 2003

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 账户 ID |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 2002 | 该账户下有 N 条收支记录，请先处理后再禁用 | 账户下存在 transaction 记录 |
| 2002 | 该账户下有 N 个活跃周期性账单，请先停用后再禁用 | 账户被活跃 recurring_bill 引用（status=1） |
| 2003 | 账户不存在 | id 不存在（Service 查询返回 null） |
| 2003 | 账户已禁用 | 账户已是 status=0（affectedRows=0） |

---

### GET /api/v1/account/balance

- **功能**: 查询各账户当前余额（实时计算 = 初始余额 + 累计收入 - 累计支出）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口，返回当前用户全部活跃账户余额

- **请求参数**: 无

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "accountId": 1,
      "accountName": "现金钱包",
      "accountType": 1,
      "initialBalance": 5000.00,
      "totalIncome": 200.00,
      "totalExpense": 50.00,
      "currentBalance": 5150.00
    }
  ]
}
```

- **空集合**: 无账户时 `data: []`

---

### GET /api/v1/category

- **功能**: 查询全量收支分类列表（种子数据，不做增改删）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口，返回全部 13 条分类

- **请求参数**: 无

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    { "id": 1, "name": "餐饮", "type": 1 },
    { "id": 2, "name": "交通", "type": 1 },
    { "id": 3, "name": "购物", "type": 1 },
    { "id": 4, "name": "住房", "type": 1 },
    { "id": 5, "name": "娱乐", "type": 1 },
    { "id": 6, "name": "医疗", "type": 1 },
    { "id": 7, "name": "教育", "type": 1 },
    { "id": 8, "name": "其他", "type": 1 },
    { "id": 9, "name": "工资", "type": 2 },
    { "id": 10, "name": "奖金", "type": 2 },
    { "id": 11, "name": "兼职", "type": 2 },
    { "id": 12, "name": "理财", "type": 2 },
    { "id": 13, "name": "其他", "type": 2 }
  ]
}
```

- **type 枚举**: 1=支出 / 2=收入

---

### GET /api/v1/transaction

- **功能**: 分页查询当前用户的收支记录，支持多条件筛选（时间/账户/分类/关键词）
- **是否需登录**: ✅ 需要
- **分页约束**: pageNum ≥ 1 / pageSize 1-100（后端 @Min @Max 校验 · 超限返回 400）；排序参数 `sortBy` 白名单（`id` / `time` / `create_time`），默认 `time DESC`；无数据时 `data.records: []` 不返回 null

- **Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 校验规则 |
|---|---|:--:|---|---|
| pageNum | Integer | 否 | 1 | ≥ 1 |
| pageSize | Integer | 否 | 10 | 1-100 |
| startTime | String | 否 | - | yyyy-MM-dd HH:mm:ss |
| endTime | String | 否 | - | yyyy-MM-dd HH:mm:ss（必须 ≥ startTime） |
| accountId | Long | 否 | - | 空 = 不过滤 |
| categoryId | Long | 否 | - | 空 = 不过滤 |
| keyword | String | 否 | - | ≤ 50 字符，模糊匹配备注 |
| sortBy | String | 否 | time | 白名单: id / time / create_time |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "accountId": 1,
        "accountName": "现金钱包",
        "categoryId": 1,
        "categoryName": "餐饮",
        "type": 2,
        "amount": 50.00,
        "note": "午餐外卖",
        "time": "2026-05-16 12:30:00",
        "transferId": null,
        "createTime": "2026-05-16 12:30:00",
        "updateTime": "2026-05-16 12:30:00"
      }
    ],
    "total": 56,
    "size": 10,
    "current": 1,
    "pages": 6
  }
}
```

- **type 枚举**: 1=收入 / 2=支出；transferId 非空时为转账关联记录
- **transferId**: null=普通收支记录；非 null=转账关联记录（流水列表标记转出/转入）

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | startTime > endTime / keyword 超长 / pageNum/pageSize 超限 |

---

### POST /api/v1/transaction

- **功能**: 创建一条收支记录（记一笔）
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `user_id = currentUserId`，只能给自己记账

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| accountId | Long | ✅ | 账户必须存在且 status=1 |
| categoryId | Long | ✅ | 分类必须存在 |
| type | Integer | ✅ | 1=收入 / 2=支出 |
| amount | BigDecimal | ✅ | > 0，精度 2 位 |
| note | String | 否 | ≤ 200 字符 |
| time | String | ✅ | yyyy-MM-dd HH:mm:ss |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "id": 10,
    "accountId": 1,
    "accountName": "现金钱包",
    "categoryId": 1,
    "categoryName": "餐饮",
    "type": 2,
    "amount": 50.00,
    "note": "午饭",
    "time": "2026-05-16 12:30:00",
    "transferId": null,
    "createTime": "2026-05-16 12:30:00",
    "updateTime": "2026-05-16 12:30:00"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | 必填字段缺失 / amount ≤ 0 |
| 3001 | 金额不合法 | amount ≤ 0 或超出 DECIMAL(12,2) 范围 |
| 3002 | 账户不存在或已禁用 | accountId 无效或 status=0 |
| 3002 | 分类不存在 | categoryId 无效 |

---

### PUT /api/v1/transaction/{id}

- **功能**: 修改已有收支记录；转账关联记录（transferId 非空）仅允许修改备注，禁止修改金额
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`，只能修改自己的记录

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 记录 ID |

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| accountId | Long | ✅ | 账户必须存在且 status=1 |
| categoryId | Long | ✅ | 分类必须存在 |
| type | Integer | ✅ | 1=收入 / 2=支出 |
| amount | BigDecimal | ✅ | > 0，精度 2 位 |
| note | String | 否 | ≤ 200 字符 |
| time | String | ✅ | yyyy-MM-dd HH:mm:ss |

- **成功响应**（code=200）: 同记一笔返回更新后的 TransactionDTO

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | 必填字段缺失 / amount ≤ 0 |
| 3003 | 转账记录金额不可修改 | transferId 非空时修改了 amount |
| 3006 | 收支记录不存在 | id 不存在（Service 查询返回 null） |
| 3002 | 账户不存在或已禁用 | accountId 无效或 status=0 |
| 3002 | 分类不存在 | categoryId 无效 |

---

### DELETE /api/v1/transaction/{id}

- **功能**: 删除一条收支记录；转账关联记录（transferId 非空）禁止删除
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`，只能删除自己的记录

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 记录 ID |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "记录已删除",
  "data": null
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 3006 | 收支记录不存在 | id 不存在 |
| 3003 | 转账记录禁止删除 | transferId 非空时拒绝删除，避免破坏转账配对完整性 |

---

### POST /api/v1/transaction/transfer

- **功能**: 在两个账户之间转账，生成一出一进两条关联收支记录
- **是否需登录**: ✅ 需要
- **行级权限**: 强制校验两个账户 `user_id = currentUserId`，只能操作自己的账户

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| fromAccountId | Long | ✅ | 转出账户，必须存在且 status=1 |
| toAccountId | Long | ✅ | 转入账户，必须存在且 status=1 |
| amount | BigDecimal | ✅ | > 0，精度 2 位 |
| note | String | 否 | ≤ 200 字符 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "转账成功",
  "data": {
    "transferId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "outRecord": {
      "id": 20,
      "accountId": 2,
      "accountName": "招商银行卡",
      "type": 2,
      "amount": 200.00,
      "note": "银行卡→现金",
      "time": "2026-05-16 14:00:00",
      "transferId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    },
    "inRecord": {
      "id": 21,
      "accountId": 1,
      "accountName": "现金钱包",
      "type": 1,
      "amount": 200.00,
      "note": "银行卡→现金",
      "time": "2026-05-16 14:00:00",
      "transferId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    }
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | fromAccountId/toAccountId 缺失 / amount ≤ 0 |
| 3004 | 转出账户和转入账户不可相同 | fromAccountId = toAccountId |
| 3002 | 账户不存在或已禁用 | 账户无效或 status=0 |
| 3005 | 余额不足 | 转出账户当前余额 < amount（@Transactional 事务内余额校验） |

> **并发安全**：转账使用 `@Transactional` 事务包裹余额检查 + 两条 INSERT，利用 InnoDB REPEATABLE READ 隔离级别防并发透支。教学简化不做后端幂等，前端通过按钮 loading 状态防连点。

---

### POST /api/v1/transaction/import

- **功能**: 通过 CSV 文件批量导入收支记录
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `user_id = currentUserId`，导入的记录归属当前用户

- **请求参数**（body · multipart/form-data）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| file | File | ✅ | CSV 格式，≤ 5MB，UTF-8 编码 |
| accountId | Long | ✅ | 导入目标账户，必须存在且 status=1 |

- **CSV 格式要求**: 首行为表头 `categoryName,type,amount,note,time`；type 为 1(收入)或 2(支出)；time 格式 `yyyy-MM-dd HH:mm:ss`

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "totalCount": 50,
    "successCount": 48,
    "failCount": 2,
    "failRows": [
      { "row": 3, "reason": "分类不存在" },
      { "row": 15, "reason": "金额格式错误" }
    ]
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | file 为空 / accountId 缺失 |
| 400 | 文件格式错误 | 非 CSV 文件或编码错误 |
| 400 | 文件大小超限 | 文件 > 5MB |
| 2003 | 账户不存在或已禁用 | accountId 无效或 status=0 |

---

### GET /api/v1/budget

- **功能**: 查询指定月份所有分类的预算设置
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | ✅ | 如 2026 |
| month | Integer | ✅ | 1-12 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "categoryId": 1,
      "categoryName": "餐饮",
      "month": "2026-05",
      "amount": 2000.00,
      "createTime": "2026-05-01 00:00:00",
      "updateTime": "2026-05-01 00:00:00"
    }
  ]
}
```

- **空集合**: 无预算设置时 `data: []`

---

### POST /api/v1/budget

- **功能**: 创建或更新预算（同一用户+同一分类+同一月份，覆盖写入）
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `user_id = currentUserId`
- **幂等性**: 数据库唯一约束 `uk_budget_user_category_month`（user_id, category_id, month），使用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现有则更新、无则插入

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| categoryId | Long | ✅ | 必须是支出分类（type=1） |
| month | String | ✅ | 格式 yyyy-MM（如 2026-05） |
| amount | BigDecimal | ✅ | > 0，精度 2 位 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "id": 1,
    "categoryId": 1,
    "categoryName": "餐饮",
    "month": "2026-05",
    "amount": 2000.00,
    "createTime": "2026-05-01 00:00:00",
    "updateTime": "2026-05-16 09:00:00"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | categoryId 缺失 / month 格式错误 / amount ≤ 0 |
| 4001 | 预算金额不合法 | amount ≤ 0 或超出精度范围 |
| 4002 | 分类不存在或为收入分类 | categoryId 无效或 type=2 |

---

### GET /api/v1/budget/progress

- **功能**: 查询指定月份各分类的预算消耗进度和超支标记
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | ✅ | 如 2026 |
| month | Integer | ✅ | 1-12 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "categoryId": 1,
      "categoryName": "餐饮",
      "budgetAmount": 2000.00,
      "spentAmount": 1800.00,
      "percentage": 90.0,
      "overspent": false
    },
    {
      "categoryId": 2,
      "categoryName": "交通",
      "budgetAmount": 500.00,
      "spentAmount": 600.00,
      "percentage": 120.0,
      "overspent": true
    }
  ]
}
```

- **空集合**: 无预算设置时 `data: []`

---

### GET /api/v1/budget/alert

- **功能**: 查询当前用户本月的预算预警记录（由 BudgetScheduler 每日凌晨 2:00 持久化到 budget_alert 表）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | 否 | 如 2026，空时默认当前年 |
| month | Integer | 否 | 1-12，空时默认当前月 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "categoryId": 1,
      "categoryName": "餐饮",
      "month": "2026-05",
      "alertLevel": "MONTHLY_WARN",
      "budgetAmount": 2000.00,
      "spentAmount": 1800.00,
      "percentage": 90.00
    },
    {
      "id": 2,
      "categoryId": 2,
      "categoryName": "交通",
      "month": "2026-05",
      "alertLevel": "OVERSPENT",
      "budgetAmount": 500.00,
      "spentAmount": 600.00,
      "percentage": 120.00
    }
  ]
}
```

- **alertLevel 枚举**: `NORMAL`=正常 / `DAILY_WARN`=日预警（日均超 150%）/ `MONTHLY_WARN`=月预警（月耗超 80%）/ `OVERSPENT`=已超支
- **数据来源**: BudgetScheduler 每日凌晨 2:00 写入 budget_alert 表，查询时批量加载分类名称
- **空集合**: 无预警记录时 `data: []`

---

### GET /api/v1/statistics/monthly

- **功能**: 按月统计收入总额、支出总额、结余
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | ✅ | 如 2026 |
| month | Integer | ✅ | 1-12 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "year": 2026,
    "month": 5,
    "totalIncome": 12000.00,
    "totalExpense": 8500.00,
    "balance": 3500.00
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | year/month 缺失或 month 不在 1-12 |

> 某月无收支记录时返回 `totalIncome: 0, totalExpense: 0, balance: 0`。

---

### GET /api/v1/statistics/yearly

- **功能**: 按年统计收入总额、支出总额、结余
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | ✅ | 如 2026 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "year": 2026,
    "totalIncome": 144000.00,
    "totalExpense": 96000.00,
    "balance": 48000.00
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | year 缺失 |

---

### GET /api/v1/statistics/category-summary

- **功能**: 按分类统计指定月份的收入/支出分布（饼图数据源 + CategoryPage 本月消费金额）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | ✅ | 如 2026 |
| month | Integer | ✅ | 1-12 |
| type | Integer | 否 | 1=支出 / 2=收入（为空时返回全部） |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "categoryId": 1,
      "categoryName": "餐饮",
      "type": 1,
      "totalAmount": 3200.00,
      "transactionCount": 28
    },
    {
      "categoryId": 9,
      "categoryName": "工资",
      "type": 2,
      "totalAmount": 8000.00,
      "transactionCount": 1
    }
  ]
}
```

- **空集合**: 某月无数据时 `data: []`

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | year/month 缺失或 month 不在 1-12 |

---

### GET /api/v1/statistics/trend

- **功能**: 查询 12 个月的收支趋势数据（折线图数据源）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口，返回固定 12 个月数据

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| year | Integer | ✅ | 如 2026 |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "month": "2026-01",
      "totalIncome": 12000.00,
      "totalExpense": 8000.00
    },
    {
      "month": "2026-02",
      "totalIncome": 11500.00,
      "totalExpense": 9200.00
    }
  ]
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | year 缺失 |

---

### GET /api/v1/recurring-bill

- **功能**: 查询当前用户所有活跃周期性账单（status=1）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口

- **请求参数**: 无

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "月房租",
      "accountId": 2,
      "accountName": "招商银行卡",
      "categoryId": 4,
      "categoryName": "住房",
      "amount": 2500.00,
      "type": 2,
      "period": "monthly",
      "nextDueDate": "2026-06-01",
      "status": 1,
      "createTime": "2026-05-01 00:00:00",
      "updateTime": "2026-05-01 00:00:00"
    }
  ]
}
```

- **空集合**: 无活跃账单时 `data: []`

---

### POST /api/v1/recurring-bill

- **功能**: 创建周期性账单模板
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `user_id = currentUserId`

- **请求参数**（body · application/json）:

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| name | String | ✅ | 1-30 字符 |
| accountId | Long | ✅ | 账户必须存在且 status=1 |
| categoryId | Long | ✅ | 分类必须存在 |
| amount | BigDecimal | ✅ | > 0，精度 2 位 |
| type | Integer | ✅ | 1=收入 / 2=支出 |
| period | String | ✅ | monthly / weekly |
| nextDueDate | String | ✅ | yyyy-MM-dd |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 4,
    "name": "网费",
    "accountId": 3,
    "accountName": "支付宝",
    "categoryId": 5,
    "categoryName": "娱乐",
    "amount": 120.00,
    "type": 2,
    "period": "monthly",
    "nextDueDate": "2026-06-15",
    "status": 1,
    "createTime": "2026-05-16 10:00:00",
    "updateTime": "2026-05-16 10:00:00"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | 必填字段缺失 / amount ≤ 0 / period 非法 / nextDueDate 为空 |
| 5001 | 周期账单名称为空 | name 为空 |
| 5002 | 关联账户已禁用 | accountId 对应账户 status=0 |

---

### PUT /api/v1/recurring-bill/{id}

- **功能**: 修改周期性账单模板
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 账单 ID |

- **请求参数**（body · application/json）: 同创建周期性账单

- **成功响应**（code=200）: 同创建返回更新后的 RecurringBillDTO

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | 必填字段缺失 |
| 5004 | 周期性账单已停用 | bill.status=0 时拒绝修改 |
| 5002 | 关联账户已禁用 | accountId 对应账户 status=0 |
| 5005 | 周期性账单不存在 | id 不存在（Service 查询返回 null） |
<!-- R-04-issue-3: 已修复 - PUT /api/v1/recurring-bill/{id} 追加 5005 资源不存在响应 -->

---

### DELETE /api/v1/recurring-bill/{id}

- **功能**: 将周期性账单 status 改为 0（停用，不可恢复）
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`
- **幂等性**: 条件 UPDATE（`WHERE id=? AND status=1`），affectedRows=0 时说明已停用，返回 5004
<!-- R-04-issue-4: 已修复 - 幂等性描述返回码从 5002 改为 5004（与异常响应表一致） -->

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 账单 ID |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "停用成功",
  "data": null
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 5005 | 周期性账单不存在 | id 不存在（Service 查询返回 null） |
| 5004 | 周期性账单已停用 | 账单已是 status=0（affectedRows=0） |

---

### POST /api/v1/recurring-bill/{id}/generate

- **功能**: 根据周期性账单模板生成一条收支记录，并推进下次到期日
- **是否需登录**: ✅ 需要
- **行级权限**: 强制 `WHERE user_id = currentUserId`
- **幂等性**: 条件 UPDATE（`WHERE next_due_date = 当前值`），affectedRows=0 时返回 5003 到期日已被更新

- **Path 参数**:

| 参数 | 类型 | 说明 |
|---|---|---|
| id | Long | 账单 ID |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "已生成收支记录",
  "data": {
    "id": 50,
    "accountId": 2,
    "accountName": "招商银行卡",
    "categoryId": 4,
    "categoryName": "住房",
    "type": 2,
    "amount": 2500.00,
    "note": "月房租（自动生成）",
    "time": "2026-06-01 00:00:00",
    "transferId": null,
    "createTime": "2026-05-16 10:00:00",
    "updateTime": "2026-05-16 10:00:00"
  }
}
```

- **异常响应**:

| code | message | 触发场景 |
|---|---|---|
| 5005 | 周期性账单不存在 | id 不存在 |
| 5004 | 周期性账单已停用 | bill.status=0 |
| 5002 | 关联账户已禁用，请先启用或更换账户 | 账户 status=0 |
| 5003 | 到期日已被更新，请刷新后重试 | 并发生成时乐观锁冲突（affectedRows=0） |

> **并发安全**：`next_due_date` 使用条件 UPDATE + affectedRows 判断 + INSERT 在同一 `@Transactional` 事务内，防止同一到期日被重复生成。

---

### GET /api/v1/exchange-rate

- **功能**: 查询支持的币种汇率（用于多币种账户余额折算展示）
- **是否需登录**: ✅ 需要
- **分页约束**: 非分页接口，返回全部支持币种的汇率

- **Query 参数**:

| 参数 | 类型 | 必填 | 校验规则 |
|---|---|:--:|---|
| baseCurrency | String | 否 | 基准币种，默认 CNY |

- **成功响应**（code=200）:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    { "currency": "USD", "rate": 7.25, "updateTime": "2026-05-16 08:00:00" },
    { "currency": "EUR", "rate": 7.85, "updateTime": "2026-05-16 08:00:00" },
    { "currency": "JPY", "rate": 0.048, "updateTime": "2026-05-16 08:00:00" },
    { "currency": "GBP", "rate": 9.15, "updateTime": "2026-05-16 08:00:00" },
    { "currency": "HKD", "rate": 0.93, "updateTime": "2026-05-16 08:00:00" },
    { "currency": "CNY", "rate": 1.00, "updateTime": "2026-05-16 08:00:00" }
  ]
}
```

- **空集合**: 无汇率数据时 `data: []`

---

## 4. 通用响应格式 + 异常码表

### 4.1 Result\<T\> 响应格式（全栈通用 · 详见 CLAUDE.md §一·三）

成功响应:

```json
{ "code": 200, "message": "操作成功", "data": <T> }
```

失败响应（由全局/业务异常处理）:

```json
{ "code": <错误码>, "message": "<错误说明>", "data": null }
```

### 4.2 全局异常码（由 `@RestControllerAdvice` 处理 · 见 CLAUDE.md §二·三）

| code | message 模板 | 触发场景 | 触发位置 |
|---|---|---|---|
| 400 | 参数校验失败:\<字段名\> | @Valid 校验失败（MethodArgumentNotValidException） | 任何 Controller 入参带 @Valid |
| 401 | 未登录或 token 过期 | LoginInterceptor JWT 校验失败 | LoginInterceptor.preHandle |
| 403 | 越权访问 | 访问非本人数据 | Controller / Service 层 |
| 404 | 资源不存在 | 路径不匹配任何 @RequestMapping | Spring Boot 默认 |
| 500 | 服务器内部错误 | Exception 兜底 | GlobalExceptionHandler |

### 4.3 业务异常码（由 Service 抛 `BusinessException` · 编号约定 1xxx-5xxx）

| code 范围 | 模块 | 示例 |
|---|---|---|
| 1001-1099 | 用户/认证模块 | 1001=用户名已存在 · 1002=用户名或密码错误 |
| 2001-2099 | 账户模块 | 2001=账户名称为空 · 2002=账户下有收支记录不可删除 · 2003=账户已禁用 |
| 3001-3099 | 交易模块 | 3001=金额不合法 · 3002=账户或分类不存在 · 3003=转账记录金额不可修改 · 3004=转出转入账户不可相同 · 3005=余额不足 · 3006=收支记录不存在 |
| 4001-4099 | 预算模块 | 4001=预算金额不合法 · 4002=分类不存在或为收入分类 |
| 5001-5099 | 周期账单模块 | 5001=周期账单名称为空 · 5002=关联账户已禁用 · 5003=到期日已被更新 · 5004=账单已停用 · 5005=账单不存在 · 5006=账户不存在 · 5007=分类不存在 |

<!-- R-04-issue-1: 已修复 - §4.3 交易模块示例追加 3006=收支记录不存在 -->

### 4.4 DTO 数据模型

#### LoginResponse

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "username": "zhangsan"
}
```

#### AccountDTO

```json
{
  "id": 1,
  "name": "现金钱包",
  "type": 1,
  "initialBalance": 5000.00,
  "currency": "CNY",
  "status": 1,
  "createTime": "2026-05-10 10:00:00",
  "updateTime": "2026-05-10 10:00:00"
}
```

#### AccountBalanceDTO

```json
{
  "accountId": 1,
  "accountName": "现金钱包",
  "accountType": 1,
  "initialBalance": 5000.00,
  "totalIncome": 200.00,
  "totalExpense": 50.00,
  "currentBalance": 5150.00
}
```

#### CategoryDTO

```json
{
  "id": 1,
  "name": "餐饮",
  "type": 1
}
```

#### TransactionDTO

```json
{
  "id": 1,
  "accountId": 1,
  "accountName": "现金钱包",
  "categoryId": 1,
  "categoryName": "餐饮",
  "type": 2,
  "amount": 50.00,
  "note": "午餐外卖",
  "time": "2026-05-16 12:30:00",
  "transferId": null,
  "createTime": "2026-05-16 12:30:00",
  "updateTime": "2026-05-16 12:30:00"
}
```

#### TransferDTO

```json
{
  "transferId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "outRecord": {},
  "inRecord": {}
}
```

#### BudgetDTO

```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "餐饮",
  "month": "2026-05",
  "amount": 2000.00,
  "createTime": "2026-05-01 00:00:00",
  "updateTime": "2026-05-01 00:00:00"
}
```

#### BudgetProgressDTO

```json
{
  "categoryId": 1,
  "categoryName": "餐饮",
  "budgetAmount": 2000.00,
  "spentAmount": 1800.00,
  "percentage": 90.0,
  "overspent": false
}
```

#### MonthlySummaryDTO

```json
{
  "year": 2026,
  "month": 5,
  "totalIncome": 12000.00,
  "totalExpense": 8500.00,
  "balance": 3500.00
}
```

#### CategorySummaryDTO

```json
{
  "categoryId": 1,
  "categoryName": "餐饮",
  "type": 1,
  "totalAmount": 3200.00,
  "transactionCount": 28
}
```

#### MonthlyTrendDTO

```json
{
  "month": "2026-01",
  "totalIncome": 12000.00,
  "totalExpense": 8000.00
}
```

#### RecurringBillDTO

```json
{
  "id": 1,
  "name": "月房租",
  "accountId": 2,
  "accountName": "招商银行卡",
  "categoryId": 4,
  "categoryName": "住房",
  "amount": 2500.00,
  "type": 2,
  "period": "monthly",
  "nextDueDate": "2026-06-01",
  "status": 1,
  "createTime": "2026-05-01 00:00:00",
  "updateTime": "2026-05-01 00:00:00"
}
```

#### BudgetAlertDTO（P2-2 预算预警）

```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "餐饮",
  "month": "2026-05",
  "alertLevel": "OVERSPENT",
  "budgetAmount": 2000.00,
  "spentAmount": 2500.00,
  "percentage": 125.00
}
```

> alertLevel 枚举: `NORMAL` / `DAILY_WARN` / `MONTHLY_WARN` / `OVERSPENT`

#### ImportResultDTO（P2-3 CSV 导入结果）

```json
{
  "successCount": 48,
  "failCount": 2,
  "failRows": [
    { "row": 3, "reason": "分类不存在" },
    { "row": 15, "reason": "金额格式错误" }
  ]
}
```

---

> **总计 28 个接口** · P0: 11 个（登录注册 + 账户 CRUD + 余额 + 分类 + 收支记录增改查）· P1: 13 个（改密码 + 转账 + 预算 3 + 统计 3 + 周期账单 5）· P2: 4 个（预算预警 + 月度趋势 + CSV 导入 + 汇率查询）
<!-- R-04-issue-6: 已修复 - §4 末尾总计行 P0/P1/P2 计数修正为 11/13/4（与表格标签 + PRD 一致） -->
