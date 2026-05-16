# AGENTS.md — 开发协作规则

> 本文件定义项目开发阶段的协作约束,AI 助手必须遵守。技术栈细节见 `CLAUDE.md`。

---

## 1. 文档完成状态

| 文档 | 状态 | 说明 |
|---|---|---|
| `docs/PRD.md` | ✅ 已完成并通过审核 | 产品需求规格说明(P0/P1/P2 全量功能) |
| `docs/TECH_DESIGN.md` §1-§5 | ✅ 已完成并通过审核 | 技术概要设计(架构/模块/路由/方案选型) |
| `docs/TECH_DESIGN.md` §6 | ✅ 已完成并通过审核 | 页面原型描述(11 页面 + AppLayout + 路由表 + 页面流程图) |
| `docs/DATABASE_DESIGN.md` | ✅ 已完成 | 数据库设计(6 表 + ER + 约束) |
| `docs/API_DESIGN.md` | ✅ 已完成 | RESTful API 设计(26 接口 + 错误码 + DTO) |
| `docs/DEPLOY.md` | ⬜ 未开始 | 部署文档(Phase 8) |

---

## 2. 当前阶段

**当前**:Phase 2 — 数据库设计
**下一个**:Phase 4 — 后端开发(Phase 3 API 设计已完成)

---

## 3. 开发模式:Vertical Slice

本项目采用 **Vertical Slice**(纵向切片) 开发模式:

- **每个功能切片** = Controller → Service → Mapper → Entity → (前端) API → Store → Page,端到端完整交付
- **每次只做一个切片**,不允许一次生成整个系统
- **切片顺序**:按 PRD 优先级 P0 → P1 → P2
- **每个切片完成后必须 review**,确认与已审文档对齐后才进入下一个

### P0 切片清单(按顺序)

1. **Auth 切片**:注册 + 登录 + JWT → `UserController` + `LoginPage.vue`
2. **Account 切片**:账户 CRUD + 余额汇总 → `AccountController` + `AccountPage.vue`
3. **Category 切片**:分类列表(种子数据只读) → `CategoryController` + `CategoryPage.vue`
4. **Transaction 切片**:记一笔 + 列表分页 + 筛选 → `TransactionController` + `TransactionListPage.vue`
5. **Dashboard 切片**:月统计 + 收支趋势 + 分类占比 → `DashboardController` + `DashboardPage.vue`

### P1 切片清单

6. **Budget 切片**:月预算 CRUD + 超支标记 → `BudgetController` + `BudgetPage.vue`
7. **RecurringBill 切片**:周期账单 + 一键生成 → `RecurringBillController` + 对应页面
8. **Analytics 切片**:ECharts 图表 + 多维分析 → `AnalyticsPage.vue`

---

## 4. 硬约束

### 4.1 文档对齐

- 所有功能实现**必须**基于已审核通过的文档:
  - 需求: `docs/PRD.md`
  - 设计: `docs/TECH_DESIGN.md`(§1-§6)
  - 数据库: `docs/DATABASE_DESIGN.md` + `sql/01-init.sql`
  - API: `docs/API_DESIGN.md`
- 实现与文档不一致时,**以文档为准**,先改文档再改代码

### 4.2 页面路由规范

对齐 `TECH_DESIGN.md §3 路由表` + `TECH_DESIGN.md §6.12 页面路由表`:

| 页面 | 路径 | 布局 | 优先级 |
|---|---|---|:---:|
| LoginPage | `/login` | 无 AppLayout(独立全屏) | P0 |
| DashboardPage | `/` | AppLayout | P0 |
| AccountPage | `/account` | AppLayout | P0 |
| CategoryPage | `/category` | AppLayout | P0 |
| TransactionListPage | `/transaction` | AppLayout | P0 |
| BudgetPage | `/budget` | AppLayout | P1 |
| RecurringBillPage | `/recurring-bill` | AppLayout | P1 |
| TransferPage | `/transfer` | AppLayout | P1 |
| AnalyticsPage | `/analytics` | AppLayout | P2 |
| ImportPage | `/import` | AppLayout | P2 |
| UserSettingsPage | `/settings` | AppLayout | P1 |

- **命名**:大驼峰 + `Page` 后缀(如 `LoginPage.vue`)
- **路由守卫**:未登录跳 `/login`,已登录访问 `/login` 跳 `/`
- **文件位置**:`frontend/src/views/<PageName>.vue`

### 4.3 AppLayout 规范

对齐 `TECH_DESIGN.md §6.0 AppLayout 全局布局`:

```
┌────────────────────────────────────────────┐
│  el-header (60px)                          │
│  Logo + 系统名 | 导航菜单 | 用户名+退出     │
├────────┬───────────────────────────────────┤
│ aside  │  el-main (内容区)                 │
│ 200px  │  <router-view />                  │
│ 导航   │  面包屑 + 页面内容                │
├────────┴───────────────────────────────────┤
│  el-footer (可选)                          │
└────────────────────────────────────────────┘
```

- **组件**:`el-container` > `el-header` + `el-container` > `el-aside` + `el-main`
- **响应式**:
  - `≥992px`:侧栏固定 200px
  - `768-991px`:侧栏折叠至 64px(仅图标)
  - `<768px`:侧栏变抽屉(`el-drawer`)
- **路由嵌套**:AppLayout 作为父路由,业务页面作为 children

### 4.4 API Result 统一规范

对齐 `CLAUDE.md §一·三` + `API_DESIGN.md §2`:

**后端**:
```java
// 成功
return Result.success(data);
return Result.success(data, "操作成功");

// 失败
throw new BusinessException(1001, "用户名已存在");
// → GlobalExceptionHandler 捕获 → Result.error(1001, "用户名已存在")
```

**前端 axios 拦截器**:
```js
// code === 200  → 返回 response.data.data 业务数据
// code === 401  → 清 token + 跳 /login + ElMessage.error
// 其他 code     → ElMessage.error(message) + reject
```

**禁止**:
- ❌ Controller 直接返回 entity / List / Map
- ❌ 前端组件里写 `if (res.code !== 200)` 判断(拦截器职责)
- ❌ 自定义返回结构(必须用 `Result<T>`)

### 4.5 其他约束

- **Vertical Slice**:每次只实现一个功能切片,不允许一次生成整个系统
- **Feature Review**:每完成一个 feature 必须 review
- **数据库表**:必须包含 `create_time` + `update_time`
- **代码可读性优先**:宁可多写一行清晰代码,不写一行晦涩代码
- **密钥管理**:禁止硬编码密钥/密码,使用环境变量或 `application-local.yml`

---

## 5. 已有基础设施速查

### 后端骨架(init-skeleton 已生成)

| 文件 | 路径 |
|---|---|
| Application.java | `backend/src/main/java/com/example/finance/Application.java` |
| Result.java | `common/Result.java` |
| BusinessException.java | `common/BusinessException.java` |
| GlobalExceptionHandler.java | `common/GlobalExceptionHandler.java` |
| JwtUtils.java | `util/JwtUtils.java` |
| LoginInterceptor.java | `interceptor/LoginInterceptor.java` |
| CorsConfig / MybatisPlusPlus / WebMvc | `config/` |

### 前端骨架(init-skeleton 已生成)

| 文件 | 路径 |
|---|---|
| main.js | `frontend/src/main.js` |
| App.vue | `frontend/src/App.vue` |
| router/index.js | `frontend/src/router/index.js` |
| api/request.js | `frontend/src/api/request.js` |
| LoginPage.vue | `frontend/src/views/LoginPage.vue` |
| HomePage.vue | `frontend/src/views/HomePage.vue` |

### 数据库(sql/01-init.sql 已就绪)

6 张表: `user` / `account` / `category` / `transaction` / `budget` / `recurring_bill`
种子数据: category 表 13 条(8 支出 + 5 收入)
