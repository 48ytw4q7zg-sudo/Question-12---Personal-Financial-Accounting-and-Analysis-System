# 个人财务记账与分析系统

SpringBoot 3 + Vue 3 全栈个人财务管理应用，支持多账户管理、收支记录、预算控制、周期性账单、统计分析等功能。

## 技术栈

**后端**: SpringBoot 3.5.14 + MyBatis-Plus 3.5.15 + MySQL 8.0 + JWT + BCrypt

**前端**: Vue 3.5 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios + ECharts + Vite 8

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < sql/01-init.sql
```

### 2. 启动后端

```bash
cd system/backend
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd system/frontend
pnpm install
pnpm dev
```

访问 http://localhost:5173，使用测试账号 zhangsan / 123456 登录。

## 功能清单

| 优先级 | 功能 | 说明 |
|:---:|---|---|
| P0 | 用户登录/注册 | JWT 鉴权，自动注册 |
| P0 | 账户管理 | 现金/银行卡/支付宝/微信，软删除 |
| P0 | 分类浏览 | 13 个收支分类（种子数据） |
| P0 | 收支记录 | 记一笔 + 列表分页 + 多条件筛选 |
| P1 | 预算管理 | 月预算按分类设置 + 超支标记 |
| P1 | 周期性账单 | 模板管理 + 一键生成收支记录 |
| P1 | 转账 | 账户间转账，自动生成关联记录 |
| P1 | 统计分析 | ECharts 收支趋势 + 分类饼图 |
| P2 | 数据导入 | CSV 批量导入收支记录 |

## 项目结构

```
system/
├── backend/          # SpringBoot 后端
│   ├── src/main/java/com/example/finance/
│   │   ├── controller/    # 控制器层
│   │   ├── service/       # 业务逻辑层
│   │   ├── mapper/        # 数据访问层
│   │   ├── entity/        # 实体类 + DTO
│   │   ├── config/        # 配置类
│   │   ├── common/        # 通用类 (Result/BusinessException)
│   │   ├── interceptor/   # JWT 拦截器
│   │   └── util/          # 工具类
│   └── src/main/resources/
│       ├── mapper/        # MyBatis XML
│       └── application.yml
├── frontend/         # Vue 3 前端
│   └── src/
│       ├── api/           # API 模块
│       ├── router/        # 路由配置
│       ├── stores/        # Pinia 状态管理
│       ├── views/         # 页面组件
│       ├── layout/        # 布局组件
│       └── components/    # 公共组件
├── docs/             # 项目文档
│   ├── PRD.md             # 需求规格说明
│   ├── TECH_DESIGN.md     # 技术设计
│   ├── DATABASE_DESIGN.md # 数据库设计
│   ├── API_DESIGN.md      # API 接口设计
│   └── DEPLOY.md          # 部署文档
└── sql/
    └── 01-init.sql        # 数据库初始化脚本
```

## API 接口

共 28 个 RESTful 接口，统一返回 `Result<T>` 结构：

```json
{ "code": 200, "message": "操作成功", "data": { ... } }
```

详见 [API 设计文档](docs/API_DESIGN.md)。

## 数据库设计

共 6 张表，全部使用 `DECIMAL(12,2)` 存储金额，`create_time` + `update_time` 自动维护：

| 表名 | 说明 | 核心字段 |
|---|---|---|
| user | 用户表 | username, password(BCrypt) |
| account | 账户表 | name, type(1-4), initial_balance, currency |
| category | 分类表(种子数据) | name, type(1=支出/2=收入) |
| transaction | 收支记录表 | account_id, category_id, type, amount, time, transfer_id |
| budget | 预算表 | category_id, month(YYYY-MM), amount |
| recurring_bill | 周期账单表 | account_id, category_id, period, next_due_date, status |

详见 [数据库设计文档](docs/DATABASE_DESIGN.md)。

## 测试账号

| 用户名 | 密码 | 说明 |
|---|---|---|
| zhangsan | 123456 | 已有 4 个账户 + 6 条收支记录 + 3 条预算 + 3 条周期账单 |
| lisi | 123456 | 空账号，用于测试全新用户流程 |

## 常见问题

**Q: 后端启动报数据库连接错误？**
A: 确认 MySQL 已启动且 `finance_db` 数据库已创建（执行 `sql/01-init.sql`）。

**Q: 前端页面空白？**
A: 确认后端已在 8080 端口运行，检查 `vite.config.js` 的 proxy 配置。

**Q: 如何重新初始化数据库？**
A: `sql/01-init.sql` 使用 `DROP TABLE IF EXISTS`，可重复执行。

## 开发文档

- [需求规格说明](docs/PRD.md) — 功能列表、业务规则、页面映射
- [技术设计](docs/TECH_DESIGN.md) — 架构、模块、路由、流程图
- [数据库设计](docs/DATABASE_DESIGN.md) — ER 图、建表脚本、字段约定
- [API 接口设计](docs/API_DESIGN.md) — 28 个接口详情、错误码、DTO
- [部署文档](docs/DEPLOY.md) — 环境要求、启动步骤、生产部署

## 验证状态

| 检查项 | 结果 |
|---|---|
| `mvn clean compile` | ✅ BUILD SUCCESS |
| `pnpm build` | ✅ built |
| `mvn test` | ✅ 37 用例 / 0 失败 |
| 后端 API 运行时 | ✅ 全 28 接口响应正常 |

### 测试覆盖

| ServiceImpl | 用例数 |
|---|---|
| UserServiceImpl | 9 |
| TransactionServiceImpl | 8 |
| BudgetServiceImpl | 6 |
| RecurringBillServiceImpl | 6 |
| AccountServiceImpl | 3 |
| StatisticsServiceImpl | 3 |
| CategoryServiceImpl | 2 |
| **合计** | **37** |
