

# 个人财务记账与分析系统

## 一、项目简介

个人财务记账与分析系统是一款面向个人用户的财务管理应用，帮助用户全面记录日常收支、进行分类管理、设置预算提醒、追踪财务趋势，从而实现清晰的个人财务规划。

### 核心功能

- **账户管理**：支持多账户管理（银行卡、支付宝、微信、现金等），实时查看各账户余额及汇总
- **收支记录**：快速记录每一笔收入/支出，支持转账功能强大的多条件筛选和分页查询
- **分类管理**：灵活的收入/支出分类体系
- **预算管理**：月度预算设置，超支预警提醒
- **周期性账单**：自动生成重复账单（如房租、水电费）的提醒
- **统计分析**：月度/年度收支汇总、分类统计、趋势图表展示

---

## 二、技术栈

### 后端

| 技术 | 版本 |
|------|------|
| Spring Boot | 3.5.14 |
| MyBatis-Plus | 3.5.15 |
| MySQL | 8.4 LTS |
| JJWT | 0.13.0 |

### 前端

| 技术 | 版本 |
|------|------|
| Vue | 3.5.34 |
| Vite | 8.0.0 |
| Element Plus | 2.13.7 |
| Pinia | 3.0.4 |
| Axios | 1.15.2 |

---

## 三、项目结构

```
├── backend/                    # 后端项目
│   └── src/main/java/com/example/finance/
│       ├── common/            # 通用类（Result、异常处理）
│       ├── config/           # 配置类（CORS、JWT、MyBatis、拦截器）
│       ├── controller/        # 控制器
│       ├── entity/           # 实体类 + DTO
│       ├── interceptor/      # 登录拦截器
│       ├── mapper/          # MyBatis Mapper
│       ├── service/        # Service 接口 + 实现
│       └── util/           # 工具类
│
├── frontend/                  # 前端项目
│   └── src/
│       ├── api/             # Axios API 模块
│       ├── components/       # 公共组件
│       ├── layout/          # 布局组件
│       ├── router/          # 路由配置
│       ├── stores/          # Pinia 状态管理
│       └── views/           # 页面组件
│
├── docs/                    # 设计文档
│   ├── PRD.md               # 需求规格说明书
│   ├── DATABASE_DESIGN.md   # 数据库设计
│   ├── API_DESIGN.md       # API 接口设计
│   └── TECH_DESIGN.md      # 概要设计
│
└── sql/
    └── 01-init.sql         # 数据库初始化脚本
```

---

## 四、数据库设计

### 核心表结构

| 表名 | 说明 | 优先级 |
|------|------|--------|
| user | 用户表 | P0 |
| account | 账户表 | P0 |
| category | 分类表（种子数据） | P0 |
| transaction | 收支记录表 | P0 |
| budget | 预算表 | P1 |
| recurring_bill | 周期性账单表 | P1 |

### ER 图关系

```
user (1)