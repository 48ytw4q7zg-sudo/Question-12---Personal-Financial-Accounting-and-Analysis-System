# 个人财务记账与分析系统 · 部署文档

> 版本: v1.0 · 日期: 2026-05-16

---

## 1. 环境要求

| 组件 | 版本 | 用途 |
|---|---|---|
| JDK | 21+ | 后端运行环境 |
| Maven | 3.9+ | 后端构建工具 |
| Node.js | 24 LTS | 前端构建环境 |
| pnpm | 10+ | 前端包管理器 |
| MySQL | 8.0+ | 数据库 |

---

## 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本（自动创建库 + 表 + 测试数据）
source /path/to/sql/01-init.sql
```

或通过命令行直接执行：

```bash
mysql -u root -p < sql/01-init.sql
```

脚本会自动：
- 创建 `finance_db` 数据库
- 创建 7 张表（user / account / category / transaction / budget / recurring_bill / budget_alert）
- 插入 13 条分类种子数据
- 插入测试数据（用户 zhangsan/lisi，密码均为 123456）

---

## 3. 后端启动

```bash
cd system/backend

# 编译
mvn clean compile

# 启动（默认端口 8080）
mvn spring-boot:run
```

### 配置说明

数据库连接配置在 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/finance_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root  # 生产环境请修改
```

如需修改数据库密码，创建 `application-local.yml`（已在 .gitignore 中）：

```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

启动后访问 `http://localhost:8080/api/health` 验证后端是否正常。

### 定时任务说明

后端启动后会自动启用预算预警定时任务（`BudgetScheduler`）：
- **执行时间**: 每日凌晨 2:00（cron: `0 0 2 * * ?`）
- **功能**: 检查所有用户的各分类预算消耗，生成 4 级预警（NORMAL / DAILY_WARN / MONTHLY_WARN / OVERSPENT）并持久化到 `budget_alert` 表
- **幂等设计**: 每次执行先删除该用户该月的旧预警记录，再写入新预警，同一天多次执行不重复
- **教学简化**: 仅记录预警状态到数据库，不发送短信/邮件通知

---

## 4. 前端启动

```bash
cd system/frontend

# 安装依赖
pnpm install

# 开发模式启动（默认端口 5173）
pnpm dev

# 生产构建
pnpm build
```

前端通过 Vite proxy 将 `/api` 请求代理到后端 `http://localhost:8080`，开发时无需额外配置。

启动后访问 `http://localhost:5173` 即可使用系统。

---

## 5. 生产部署

### 5.1 后端打包

```bash
cd system/backend
mvn clean package -DskipTests
java -jar target/finance-backend-1.0.0.jar
```

### 5.2 前端构建 + Nginx 部署

```bash
cd system/frontend
pnpm build
# dist/ 目录即为静态文件，部署到 Nginx
```

Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 6. 测试账号

| 用户名 | 密码 | 说明 |
|---|---|---|
| zhangsan | 123456 | 测试用户，含 4 个账户 + 示例数据 |
| lisi | 123456 | 测试用户，空数据 |
| admin | 123456 | 管理员账号（可管理用户：查看列表/删除/角色切换） |

新用户访问系统时会自动注册（首次登录即创建账号）。注册默认为普通用户(role=0)，管理员(role=1)需由已有管理员在「用户管理」页面手动提升。

---

## 7. 常见问题

### 后端启动报错 "Communications link failure"
- 检查 MySQL 是否启动：`mysql -u root -p`
- 检查数据库名是否正确：`finance_db`

### 前端启动后页面空白
- 检查后端是否已启动（端口 8080）
- 检查浏览器控制台是否有 CORS 错误

### 登录后立即跳回登录页
- 检查 localStorage 中是否有 token
- 检查后端 JWT 密钥是否一致

### 编译报 Lombok 警告
- 这是 Lombok 与 JDK 26 的兼容性警告，不影响功能
- 生产环境建议使用 JDK 21
