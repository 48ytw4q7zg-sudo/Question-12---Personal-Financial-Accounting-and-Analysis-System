

# 个人财务记账与分析系统

## 一、项目简介

本项目是一个面向个人用户的财务记账与分析系统，旨在帮助用户管理个人账户、记录收支流水、设置预算、进行财务统计分析。

### 核心功能

- **账户管理**：支持多账户创建、修改、删除，按账户汇总余额
- **收支记录**：记录收入/支出，支持多条件筛选、分页查询
- **分类管理**：支出/收入分类，13种预设分类
- **预算管理**：月度预算设置，实时进度跟踪，超支预警
- **周期性账单**：自动提醒周期性支出/收入，支持一键生成记录
- **转账功能**：账户间转账，余额自动扣减
- **统计分析**：月度/年度汇总，分类统计，趋势分析，ECharts可视化图表
- **CSV导入**：支持银行流水CSV文件导入

## 二、技术栈

### 后端
- Spring Boot 3.5.14
- MyBatis-Plus 3.5.15
- MySQL 8.4 LTS
- JJWT 0.13.0 (JWT认证)
- Spring Security Crypto 6.3.4

### 前端
- Vue 3.5.34
- Vite 8.0.0
- Element Plus 2.13.7
- Pinia 3.0.4 (状态管理)
- Axios 1.15.2
- ECharts 5.x (图表可视化)

## 三、项目结构

```
├── backend/                    # 后端项目
│   ├── src/main/java/
│   │   └── com/example/finance/
│   │       ├── config/        # 配置类
│   │       ├── controller/    # 控制器
│   │       ├── entity/        # 实体类
│   │       │   └── dto/       # 数据传输对象
│   │       ├── interceptor/  # 拦截器
│   │       ├── mapper/        # MyBatis Mapper
│   │       ├── service/      # 服务接口
│   │       │   └── impl/     # 服务实现
│   │       └── util/         # 工具类
│   └── src/main/resources/
│       ├── application.yml   # 应用配置
│       └── mapper/          # Mapper XML
├── frontend/                  # 前端项目
│   ├── src/
│   │   ├── api/            # API请求模块
│   │   ├── components/     # 公共组件
│   │   ├── layout/         # 布局组件
│   │   ├── router/        # 路由配置
│   │   ├── stores/       # Pinia状态管理
│   │   └── views/        # 页面组件
│   └── vite.config.js
├── docs/                     # 文档
│   ├── PRD.md              # 需求规格说明书
│   ├── TECH_DESIGN.md     # 概要设计
│   ├── DATABASE_DESIGN.md  # 数据库设计
│   ├── API_DESIGN.md      # API接口设计
│   └── DEPLOY.md         # 部署文档
└── sql/
    └── 01-init.sql        # 数据库初始化脚本
```

## 四、数据库设计

### 核心表结构

| 表名 | 说明 | 优先级 |
|------|------|--------|
| user | 用户表 | P0 |
| account | 账户表 | P0 |
| category | 分类表 | P0 |
| transaction | 收支记录表 | P0 |
| budget | 预算表 | P1 |
| recurring_bill | 周期性账单表 | P1 |

### 账户类型
- 1: 现金账户
- 2: 银行账户
- 3: 支付宝
- 4: 微信钱包

### 收支类型
- 1: 收入
- 2: 支出

## 五、API 接口

### 用户认证模块 `/api/user`
- `POST /api/user/register` - 用户注册
- `POST /api/user/login` - 用户登录
- `POST /api/user/change-password` - 修改密码

### 账户模块 `/api/account`
- `GET /api/account` - 获取账户列表
- `POST /api/account` - 创建账户
- `PUT /api/account/{id}` - 更新账户
- `DELETE /api/account/{id}` - 删除账户
- `GET /api/account/balance` - 按账户汇总余额

### 分类模块 `/api/category`
- `GET /api/category` - 获取分类列表

### 交易记录模块 `/api/transaction`
- `GET /api/transaction` - 交易记录列表(分页)
- `POST /api/transaction` - 创建记录
- `PUT /api/transaction/{id}` - 更新记录
- `POST /api/transaction/transfer` - 转账
- `POST /api/transaction/import` - CSV导入

### 预算模块 `/api/budget`
- `GET /api/budget` - 预算列表
- `POST /api/budget` - 保存预算
- `GET /api/budget/progress` - 预算进度
- `GET /api/budget/alert` - 超支预警

### 统计分析模块 `/api/statistics`
- `GET /api/statistics/monthly` - 月度汇总
- `GET /api/statistics/yearly` - 年度汇总
- `GET /api/statistics/category-summary` - 分类汇总
- `GET /api/statistics/trend` - 趋势分析

### 周期性账单模块 `/api/recurring-bill`
- `GET /api/recurring-bill` - 账单列表
- `POST /api/recurring-bill` - 创建账单
- `PUT /api/recurring-bill/{id}` - 更新账单
- `DELETE /api/recurring-bill/{id}` - 停用账单
- `POST /api/recurring-bill/{id}/generate` - 一键生成记录

## 六、快速开始

### 后端启动

```bash
# 1. 进入后端目录
cd backend

# 2. 构建项目
mvn clean package -DskipTests

# 3. 启动应用
mvn spring-boot:run
# 或运行jar包
java -jar target/finance-0.0.1-SNAPSHOT.jar
```

默认端口：`8080`

### 前端启动

```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖
npm install
# 或使用pnrm
pnpm install

# 3. 启动开发服务器
npm run dev
# 或使用pnrm
pnpm run dev
```

默认端口：`5173`

### 访问地址

- 前端：http://localhost:5173
- 后端API：http://localhost:8080/api

## 七、文档索引

| 文档 | 说明 |
|------|------|
| PRD.md | 需求规格说明书 - 功能需求列表、优先级定义 |
| TECH_DESIGN.md | 概要设计 - 系统架构、路由设计、页面原型 |
| DATABASE_DESIGN.md | 数据库设计 - ER图、表结构、测试数据 |
| API_DESIGN.md | API接口设计 - 接口清单、详细说明 |
| DEPLOY.md | 部署文档 - 环境要求、部署步骤 |

## 八、验收清单

### P0 基础功能（必做）

- [ ] 用户注册/登录/JWT认证
- [ ] 账户CRUD
- [ ] 分类列表查询
- [ ] 收支记录（创建、修改、列表分页）
- [ ] 按账户汇总余额
- [ ] 分类浏览页

### P1 进阶功能

- [ ] 多条件筛选
- [ ] 月度/年度汇总统计
- [ ] 预算管理
- [ ] 周期性账单提醒
- [ ] 转账功能
- [ ] ECharts基础图表
- [ ] 用户设置（修改密码）

### P2 加分项

- [ ] ECharts多图联动+drill-down
- [ ] 多维度预算预警
- [ ] 导入银行CSV
- [ ] 多币种支持
- [ ] 单元测试

## 九、联系方式

- 项目维护：[js2501](https://gitee.com/js2501)
- 指导教师：计升2501 · 西安科大高新