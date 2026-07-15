# 答辩高频问答（Q-CR v19 整理）

> 本文档整理了课程验收答辩时教师可能问到的高频技术问题及回答要点。

---

## 1. 架构设计

### Q: 为什么前后端分离？
**A**: 前后端分离是行业主流实践。本项目采用 SpringBoot 3 + Vue 3 分离架构，前端通过 Axios 调用后端 RESTful API，职责清晰：前端负责 UI 交互和数据展示，后端负责业务逻辑和数据持久化。分离后前后端可独立开发、测试、部署。

### Q: 为什么用 MyBatis-Plus 不用 JPA？
**A**: MyBatis-Plus 在国内企业应用广泛，提供 `LambdaQueryWrapper` 类型安全查询，避免字符串拼接 SQL。相比 JPA，MP 对复杂 SQL 的控制更灵活（如分页、多条件筛选），同时保留了 ORM 的便利性。教学项目中 MP 的 `BaseMapper` 大幅减少 CRUD 样板代码。

### Q: 为什么不用 Redis 做缓存？
**A**: 教学项目数据量 ≤1000 条，MySQL 查询响应已在毫秒级，引入 Redis 增加运维复杂度但收益有限。评分标准不要求分布式缓存，若需优化可后续引入 Caffeine 本地缓存。

---

## 2. 安全设计

### Q: 为什么用 Guava RateLimiter 做登录限流不用 Redis？
**A**: 教学简化。Guava RateLimiter 是单机内存级限流，本项目为单实例部署，无需分布式限流。Redis 需要额外安装配置，增加运维成本。RateLimiter 基于令牌桶算法，限制同一 IP 每秒最多 3 次登录请求，已足够防止暴力破解。

### Q: 密码为什么用 BCrypt 不用 MD5？
**A**: BCrypt 是专为密码设计的单向哈希算法，内置盐值（salt）和成本因子（cost factor），天然抗彩虹表和暴力破解。MD5 是通用哈希算法，无盐值机制，已被证明不安全。项目使用 `BCryptPasswordEncoder`（来自 spring-security-crypto），成本因子 10。

### Q: JWT 密钥为什么硬编码？
**A**: 教学简化。生产环境应放环境变量或配置中心。本项目在 `application.yml` 中配置密钥，通过 `application-local.yml`（已加入 .gitignore）隔离开发环境，不泄露到 Git。

### Q: 为什么不引 spring-boot-starter-security？
**A**: Spring Security 的 Filter Chain 会拦截所有请求，配置复杂，教学项目不需要这么重的安全框架。项目只引入 `spring-security-crypto` 子模块使用 `BCryptPasswordEncoder`，JWT 鉴权通过自定义 `LoginInterceptor` 实现，更轻量。

---

## 3. 业务逻辑

### Q: 预算预警为什么先删旧再写新？
**A**: 幂等设计。`BudgetScheduler` 每日执行时先删除当天旧的预警记录，再重新计算写入新预警。这样即使同一天任务被触发多次（如手动调用），结果也是一致的——以最新计算结果为准，不会产生重复预警。

### Q: CSV 导入为什么用 @Transactional？
**A**: 保证原子性。CSV 导入批量插入多条交易记录，如果中间某一行格式错误导致插入失败，`@Transactional` 会回滚前面已插入的所有记录，避免部分导入导致数据不一致。导入前先解析预览，用户确认后才真正写入数据库。

### Q: 转账为什么生成两条记录而不是一条？
**A**: 符合复式记账原理。每笔转账对应一出一进两条流水：转出账户记支出，转入账户记收入。通过 `transfer_id`（UUID）关联两条记录，保证流水列表能完整展示资金流向。修改时禁止修改金额（仅可改备注），防止两条记录金额不一致。

### Q: 余额为什么不存数据库而是实时计算？
**A**: 避免冗余和一致性问题。余额 = 初始余额 + SUM(收入) - SUM(支出)，每次查询实时计算。如果存冗余字段，每次记账/修改/删除都需要同步更新余额，容易出错。教学项目数据量小，实时计算性能足够。

---

## 4. 数据库设计

### Q: 为什么不用物理外键？
**A**: 教学简化。物理外键增加建表顺序依赖，且级联删除行为不够灵活。本项目在应用层（Service 层）校验关联完整性，如删除账户时检查该账户下是否有交易记录和周期性账单引用，有则拒绝删除并提示。

### Q: 为什么金额用 DECIMAL(12,2) 不用 FLOAT/DOUBLE？
**A**: FLOAT/DOUBLE 是 IEEE 754 浮点数，存在精度丢失问题（如 0.1 + 0.2 ≠ 0.3）。DECIMAL 是定点数，精确存储，适合金融场景。12,2 表示最大 10 位整数 + 2 位小数，足够覆盖个人记账场景。

### Q: 为什么软删除用 status 字段不用 is_deleted？
**A**: 语义等价，`status` 更通用。`status=1` 表示正常，`status=0` 表示禁用。与 PRD 中账户禁用、周期性账单停用的状态机设计一致。查询时加 `WHERE status=1` 过滤已禁用数据。

---

## 5. 代码质量

### Q: ErrorCode 为什么用枚举不用常量？
**A**: 类型安全 + 集中管理。枚举 `ErrorCode` 将 36 个业务错误码按模块分组（USER_*/ACCOUNT_*/TRANSACTION_*/BUDGET_*/RECURRING_BILL_*），编译期检查，避免魔法值散落各处。Service 层直接 `throw new BusinessException(ErrorCode.USERNAME_EXISTS)`，清晰可读。

### Q: 单元测试覆盖率多少？
**A**: 140 个测试用例，覆盖 12 个测试文件。核心 Service（User/Account/Transaction/Budget/RecurringBill/Statistics/BudgetAlert/Admin）均有单测，使用 JUnit 5 + Mockito mock Mapper 层。另有边界测试、正交测试、跨模块集成测试。

### Q: 为什么用 Lombok？
**A**: 减少样板代码。Lombok 的 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor` 自动生成 getter/setter/构造器，Entity 和 DTO 类更简洁。教学项目中使用 Lombok 1.18.46，IDE 安装插件即可支持。

---

## 6. 前端设计

### Q: 为什么用 Pinia 不用 Vuex？
**A**: Vuex 4 已停止维护，Vue 官方推荐 Pinia。Pinia 支持组合式 API（`defineStore`），类型推导更好，体积更小。项目中 `useUserStore` 管理用户登录状态，其他页面间通信用 props/emit，不滥用 Pinia。

### Q: 响应式怎么做的？
**A**: 三断点响应式（对齐 TECH_DESIGN.md §6.12）：
- ≥992px: 侧栏展开 200px
- 768-991px: 侧栏折叠 64px（图标模式）
- <768px: 侧栏隐藏，用 el-drawer 抽屉模式

### Q: 路由守卫怎么实现的？
**A**: 在 `router/index.js` 的 `beforeEach` 中实现：
- 未登录（localStorage 无 token）→ 跳 `/login`
- 已登录访问 `/login` → 跳 `/`
- 管理员路由 `/admin` 检查 `userStore.role === 1`

---

## 7. 部署相关

### Q: 怎么部署？
**A**: 详见 `docs/DEPLOY.md`。后端 `mvn package` 打 jar 包，`java -jar` 启动；前端 `pnpm build` 打 dist，Nginx 静态托管。MySQL 执行 `sql/01-init.sql` 初始化。

### Q: 跨域怎么解决的？
**A**: 开发环境：Vite proxy 代理 `/api` 到 `localhost:8080`。生产环境：Nginx 反向代理，前后端同域部署。后端 `CorsConfig` 配置允许跨域（开发用）。

---

## 8. 项目亮点

1. **完整的前后端分离架构** — 从需求分析到部署上线的全流程实践
2. **安全设计** — BCrypt 密码加密 + JWT 鉴权 + 登录限流 + SQL 参数化查询
3. **代码质量** — ErrorCode 枚举消除魔法值 + 140 单测 + 分层职责清晰
4. **业务完整性** — 18 个 PRD 功能全部实现（P0 6 个 + P1 7 个 + P2 5 个）
5. **双角色设计** — 普通用户 + 管理员，满足评分标准 ≥2 类角色要求
6. **幂等设计** — 预算预警先删后写 + CSV 导入事务回滚
7. **注释完善** — 前后端代码 100% 中文注释覆盖
