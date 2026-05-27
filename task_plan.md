# task_plan.md — 个人财务记账与分析系统

> 项目类型: SpringBoot 3 + Vue 3 全栈 Web 应用（个人财务记账与分析系统）
> 最终目标: 构建一个支持多账户管理、收支记录、预算控制、周期性账单、数据可视化分析的个人财务管理平台
> 开发阶段: Phase 0 ~ Phase 8

---

## 阶段 0: 项目初始化
- **子任务 0.1**: 使用 /init-skeleton 初始化项目骨架（backend/ + frontend/ + docs/ + CLAUDE.md + git init）
- **子任务 0.2**: 配置数据库（MySQL 8.4）+ 验证开发环境连接
- **子任务 0.3**: 运行 sql/01-init.sql 建库建表 + 插入种子数据
- **子任务 0.4**: 在 CLAUDE.md §一 起手段写入题目「个人财务记账与分析系统」和角色信息
- **产出物**: 可运行的空项目骨架 + 数据库就绪 + git 初始提交

---

## 阶段 1: 需求分析与设计文档
- **子任务 1.1**: /srs-writer 生成 PRD.md（P0 + P1 + P2 全量功能需求）
- **子任务 1.2**: /srs-reviewer 审核 PRD.md（R-01，修复 issue 直到通过）
- **子任务 1.3**: /tech-designer 生成 TECH_DESIGN.md §1-§5（架构/模块/路由/流程图/技术方案）
- **子任务 1.4**: /page-prototyper 生成 TECH_DESIGN.md §6（11 个页面的低保真原型）
- **子任务 1.5**: /tech-reviewer 审核 TECH_DESIGN.md §1-§5（R-02）
- **子任务 1.6**: /page-reviewer 审核 TECH_DESIGN.md §6（R-02b）
- **产出物**: PRD.md（R-01 通过）+ TECH_DESIGN.md（R-02 + R-02b 通过）

---

## 阶段 2: 数据库设计
- **子任务 2.1**: /db-designer 生成 DATABASE_DESIGN.md + sql/01-init.sql（6 张表 + 索引 + 测试数据）
- **子任务 2.2**: /db-reviewer 审核 DATABASE_DESIGN.md（R-03，修复 issue 直到通过）
- **子任务 2.3**: 执行 sql/01-init.sql 验证建表成功 + 数据插入正确
- **产出物**: DATABASE_DESIGN.md（R-03 通过）+ 可执行的 01-init.sql

---

## 阶段 3: API 设计
- **子任务 3.1**: /api-designer 生成 API_DESIGN.md（28 个 RESTful 接口 + 错误码 + DTO 定义）
- **子任务 3.2**: /api-reviewer 审核 API_DESIGN.md（R-04，修复 issue 直到通过）
- **产出物**: API_DESIGN.md（R-04 通过，28 接口 + 错误码表）

---

## 阶段 4: 后端开发（Vertical Slice 模式）
> 按 PRD 优先级 P0 → P1 → P2 纵向切片，每个切片 = Controller → Service → Mapper → Entity

### P0 切片
- **子任务 4.1**: 登录/JWT 模块 — UserController + UserService + JwtUtils + LoginInterceptor + 注册/登录接口
- **子任务 4.2**: 账户 CRUD — AccountController + AccountService + 账户列表/新增/修改/禁用接口
- **子任务 4.3**: 分类列表 — CategoryController + CategoryService + 分类查询接口（种子数据只读）
- **子任务 4.4**: 收支记录 — TransactionController + TransactionService + 记一笔/修改/列表分页接口
- **子任务 4.5**: 账户余额汇总 — StatisticsController + 按账户统计余额接口

### P1 切片
- **子任务 4.6**: 预算管理 — BudgetController + BudgetService + 预算 CRUD + 超支标记
- **子任务 4.7**: 周期性账单 — RecurringBillController + RecurringBillService + 账单 CRUD + 一键生成收支
- **子任务 4.8**: 多条件筛选 — TransactionService 增加时间/账户/分类/关键词筛选

### P2 切片
- **子任务 4.9**: 统计图表 — StatisticsController 增加收支趋势 + 分类饼图 + 预算对比数据接口
- **子任务 4.10**: CSV 导入 — ImportController + CSV 解析 + 数据校验 + 批量插入

### 审核与测试
- **子任务 4.11**: /code-reviewer-be 审核后端代码（R-05，按模块逐个审核修复）
- **子任务 4.12**: /unittest-coder 生成单元测试（目标 37 个测试用例全绿）
- **产出物**: 7 个 Controller + 7 个 ServiceImpl + 完整 Mapper/Entity + 37/37 单测全绿

---

## 阶段 5: 前端开发（Vertical Slice 模式）
> 每个页面切片 = API 模块 → Pinia Store → Vue 页面组件

### P0 页面
- **子任务 5.1**: LoginPage.vue — 登录表单 + axios 拦截器 + token 存储 + 路由守卫
- **子任务 5.2**: DashboardPage.vue — 首页概览（账户余额卡片 + 收支概览 + 快捷记账）
- **子任务 5.3**: AccountPage.vue — 账户列表 + 新增/编辑弹窗 + 禁用操作
- **子任务 5.4**: CategoryPage.vue — 分类列表展示（收入/支出分组）
- **子任务 5.5**: TransactionListPage.vue — 收支列表 + 分页 + 新增/编辑 + 多条件筛选

### P1 页面
- **子任务 5.6**: BudgetPage.vue — 预算列表 + 新增/编辑 + 超支警告标记
- **子任务 5.7**: RecurringBillPage.vue — 周期性账单列表 + CRUD + 一键生成
- **子任务 5.8**: TransferPage.vue — 转账操作（选源账户/目标账户/金额）
- **子任务 5.9**: UserSettingsPage.vue — 用户设置（修改密码等）

### P2 页面
- **子任务 5.10**: AnalyticsPage.vue — ECharts 图表（趋势图 + 饼图 + 预算对比）
- **子任务 5.11**: ImportPage.vue — CSV 文件上传 + 预览 + 导入

### 布局与公共
- **子任务 5.12**: AppLayout.vue — 响应式布局（侧栏导航 + 三断点适配）
- **子任务 5.13**: /code-reviewer-fe 审核前端代码（R-06，按页面逐个审核修复）
- **产出物**: 11 个页面 + AppLayout + 路由守卫 + 三断点响应式

---

## 阶段 6: 集成调试
- **子任务 6.1**: H4 smoke test — 28/28 API 端点冒烟测试全部通过
- **子任务 6.2**: H5 DB audit — 数据库数据完整性审计
- **子任务 6.3**: V4 四连通 — 前后端联调，4 个核心链路端到端通过
- **子任务 6.4**: /bug-tracer-be + /bug-tracer-fe 排查修复集成问题
- **产出物**: 28/28 API smoke + DB audit 通过 + 四连通验证

---

## 阶段 7: 安全加固与重构
- **子任务 7.1**: /code-reviewer-full 全栈综合 Code Review（R-07，6 维度）
- **子任务 7.2**: /security-reviewer OWASP 深度安全审核（R-08，8 维度）
- **子任务 7.3**: /refactor-helper 应用 R-07/R-08 修复建议
- **子任务 7.4**: /perf-optimizer 性能优化（如有性能问题）
- **产出物**: R-07 + R-08 审核报告 + 全部修复完成

---

## 阶段 8: 部署与文档
- **子任务 8.1**: 编写 DEPLOY.md 部署文档（Docker Compose 一键部署方案）
- **子任务 8.2**: 编写 README.md 项目说明
- **子任务 8.3**: /git-committer 完成最终提交 + push
- **子任务 8.4**: 准备答辩材料（docs/DEFENSE-FAQ.md 等）
- **产出物**: DEPLOY.md + README.md + 全部代码提交 + 答辩材料就绪
