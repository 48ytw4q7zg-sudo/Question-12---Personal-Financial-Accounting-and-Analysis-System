
# 个人财务记账与分析系统

## 一、项目简介
本项目是一个面向个人用户的财务记账与分析系统，支持账户管理、收支记录、预算控制、周期性账单和统计图表展示。系统基于 **Spring Boot + Vue 3** 前后端分离架构，涵盖从需求分析到前后端实现的完整流程，是一个全栈实战学习项目。

选题来源：西安科大高新实训课程第12题（qxw·计升2501）。

## 二、技术栈
### 后端
- **框架**：Spring Boot 3.5.14
- **持久层**：MyBatis-Plus 3.5.15
- **数据库**：MySQL 8.4 LTS
- **认证**：JWT（JJWT 0.13.0）
- **密码加密**：Spring Security Crypto 6.3.4
- **构建工具**：Maven

### 前端
- **框架**：Vue 3.5.34
- **构建工具**：Vite 8.0.0
- **UI 组件库**：Element Plus 2.13.7
- **状态管理**：Pinia 3.0.4
- **HTTP 客户端**：Axios 1.15.2
- **图表库**：ECharts

## 三、项目结构
```
├── backend/                         # 后端项目
│   └── src/main/java/com/example/finance/
│       ├── Application.java         # 启动类
│       ├── common/                  # 通用组件（异常、响应封装）
│       ├── config/                  # 配置（CORS、JWT、MyBatisPlus、WebMvc）
│       ├── controller/              # 控制器
│       ├── entity/                  # 实体类
│       │   └── dto/                 # 数据传输对象
│       ├── interceptor/             # 拦截器（登录验证）
│       ├── mapper/                  # MyBatis Mapper
│       ├── service/                 # 业务接口与实现
│       └── util/                    # 工具类（JWT）
├── frontend/                        # 前端项目
│   └── src/
│       ├── api/                     # API 请求模块
│       ├── components/              # 公共组件
│       ├── layout/                  # 布局组件
│       ├── router/                  # 路由配置
│       ├── stores/                  # Pinia 状态管理
│       └── views/                   # 页面组件
├── docs/                            # 项目文档
│   ├── PRD.md
│   ├── TECH_DESIGN.md
│   ├── DATABASE_DESIGN.md
│   ├── API_DESIGN.md
│   └── DEPLOY.md
├── sql/
│   └── 01-init.sql                  # 数据库初始化脚本
└── README.md
```

## 四、功能清单
### P0 核心功能
- 用户注册、登录、JWT 认证、修改密码
- 账户增删改查、余额汇总
- 收支分类查询
- 交易记录创建/修改、分页列表
- 按分类浏览交易

### P1 重要功能
- 多条件筛选（账户、分类、时间、关键词）
- 月度/年度收支汇总统计
- 预算管理（设置、进度跟踪、预警）
- 周期性账单自动提醒与一键生成记录
- 账户间转账
- ECharts 基础图表

### P2 加分功能
- 图表多图联动与钻取
- 多维度预算预警
- 银行 CSV 批量导入
- 多币种支持
- Service 层单元测试

## 五、数据库设计
| 表名            | 说明         | 优先级 |
|-----------------|--------------|--------|
| user            | 用户表       | P0     |
| account         | 账户表       | P0     |
| category        | 分类表（种子数据） | P0 |
| transaction     | 收支记录表   | P0     |
| budget          | 预算表       | P1     |
| recurring_bill  | 周期性账单表 | P1     |

- **账户类型**：1-现金，2-银行卡，3-支付宝，4-微信钱包  
- **交易类型**：1-收入，2-支出

## 六、API 接口概览
### 用户认证 `/api/user`
- `POST /register` – 注册
- `POST /login` – 登录
- `POST /change-password` – 修改密码

### 账户 `/api/account`
- `GET /` – 账户列表
- `POST /` – 创建账户
- `PUT /{id}` – 更新
- `DELETE /{id}` – 删除
- `GET /balance` – 余额汇总

### 交易记录 `/api/transaction`
- `GET /` – 分页列表
- `POST /` – 创建
- `PUT /{id}` – 更新
- `POST /transfer` – 转账
- `POST /import` – CSV 导入

### 统计 `/api/statistics`
- `GET /monthly` – 月度汇总
- `GET /yearly` – 年度汇总
- `GET /category-summary` – 分类汇总
- `GET /trend` – 趋势分析

### 预算 `/api/budget`
- `GET /` – 预算列表
- `POST /` – 保存预算
- `GET /progress` – 进度查询
- `GET /alert` – 超支预警

### 周期性账单 `/api/recurring-bill`
- `GET /` – 列表
- `POST /` – 创建
- `PUT /{id}` – 更新
- `DELETE /{id}` – 停用
- `POST /{id}/generate` – 生成交易记录

## 七、快速开始
### 环境要求
- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.4+

提交信息 
update README.md.
扩展信息
此处可填写为什么修改，做了什么样的修改，以及开发的思路等更加详细的提交信息。（相当于 Git Commit message 的 Body）
Signed-off-by: 初冬Q <254203510@qq.com> 更换邮箱
目标分支
master
 取消
Gitee - 基于 Git 的代码托管和研发协作平台
北京奥思研工智能科技有限公司版权所有
Git 大全
Git 命令学习
CopyCat 代码克隆检测
APP与插件下载
 
Gitee 封面人物
GVP 项目
Gitee 博客
Gitee 公益计划
Gitee 持续集成
 
OpenAPI
MCP Server
帮助文档
在线自助服务
更新日志
 
关于我们
加入我们
使用条款
意见建议
合作伙伴
 
技术交流QQ群
技术交流QQ群

 
微信服务号
微信服务号

 client@oschina.cn
 企业版在线使用：400-606-0201
专业版私有部署：
赖经理 13058176526
开放原子开源基金会
开放原子开源基金会
合作代码托管平台
违法和不良信息举报中心
违法和不良信息举报中心
京ICP备2025119063号
京公网安备11011502039387号
京公网安备11011502039387号
 简 体 / 繁 體 / English

### 后端
```bash
cd backend
# 修改 application.yml 中的数据库配置
mvn clean package -DskipTests
mvn spring-boot:run
# 或 java -jar target/finance-0.0.1-SNAPSHOT.jar
```
默认端口：`8080`

### 前端
```bash
cd frontend
npm install
npm run dev
```
默认端口：`5173`

访问 http://localhost:5173 进入系统。

## 八、默认测试账号
| 用户名   | 密码    |
|----------|---------|
| testuser | test123 |
| demo     | demo123 |

> ⚠️ 生产环境请务必更换为强密码。

## 九、验收清单
### P0 基础功能
- [x] 用户注册/登录/JWT 认证
- [x] 账户 CRUD
- [x] 分类列表查询
- [x] 收支记录（创建、修改、分页）
- [x] 账户余额汇总
- [x] 分类浏览页

### P1 进阶功能
- [x] 多条件筛选
- [x] 统计图表
- [x] 预算管理
- [x] 周期性账单
- [x] 转账功能

### P2 加分功能
- [x] CSV 导入
- [x] 单元测试

## 十、文档索引
- [需求规格说明书](./docs/PRD.md)
- [概要设计](./docs/TECH_DESIGN.md)
- [数据库设计](./docs/DATABASE_DESIGN.md)
- [API 接口设计](./docs/API_DESIGN.md)
- [部署文档](./docs/DEPLOY.md)

## 十一、联系方式
- 项目维护：[js2501](https://gitee.com/js2501)
- 指导教师：计升2501 · 西安科大高新