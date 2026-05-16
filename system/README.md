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
