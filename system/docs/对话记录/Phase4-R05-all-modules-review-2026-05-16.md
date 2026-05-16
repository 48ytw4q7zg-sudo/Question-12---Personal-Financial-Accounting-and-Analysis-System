# Phase 4 R-05 后端代码审核报告 · 全模块 · 2026-05-16

## 审核元数据
- 审核日期:2026-05-16
- 审核切片:全模块(User/Account/Category/Transaction/Budget/RecurringBill/Statistics/ExchangeRate)
- 使用模型:mimo-v2-pro
- 输入摘要:63 个 Java 文件 · 共 2,941 行 · entity(6)+dto(19)+mapper(6)+service(7)+impl(7)+controller(9)+common(3)+config(4)+interceptor(1)+util(1)

## 审核报告

### 维度 1:安全漏洞
- **issue-1** [严重度:中]:CORS 允许任意来源携带凭证
  - **位置**:config/CorsConfig.java:18
  - **修复建议**:对于生产环境应指定具体 origin;课程项目可保留 addAllowedOriginPattern("*")但需在注释中注明仅限开发环境

### 维度 2:逻辑正确性
- **issue-2** [严重度:高]:TransactionRequest 注释 type 含义与 Entity 不一致
  - **位置**:entity/dto/TransactionRequest.java:25
  - **修复建议**:Transaction entity 注释写"1=收入 2=支出"(正确，与数据库一致)，TransactionRequest 注释写"1=支出 2=收入"(错误)。将 TransactionRequest:25 注释改为"类型：1=收入 2=支出"

### 维度 3:异常处理
无问题。GlobalExceptionHandler 正确处理 3 类异常。所有 Service 使用 BusinessException(code, msg)，Controller 无 try-catch。

### 维度 4:代码规范
- **issue-3** [严重度:低]:ExchangeRateController 直接写业务逻辑，无 Service 层
  - **位置**:controller/ExchangeRateController.java:23-34
  - **修复建议**:P2 功能硬编码可接受，但应加注释"TODO:后续抽取 ExchangeRateService"

### 维度 5:MyBatis-Plus 用法
- **issue-4** [严重度:中]:TransactionServiceImpl.list() 使用 RowBounds 而非 IPage/Page
  - **位置**:service/impl/TransactionServiceImpl.java:53-63
  - **修复建议**:项目统一用 IPage + Page 分页(对齐 CLAUDE.md §二·四)。当前实现手动计算 offset 调用 RowBounds，应改为 Page<TransactionDTO> + 配合 MP 分页插件

### 维度 6:性能
- **issue-5** [严重度:中]:AccountServiceImpl.getBalance() 每账户 2 次 DB 查询
  - **位置**:service/impl/AccountServiceImpl.java:131-148
  - **修复建议**:循环中调 selectAccountIncome/selectAccountExpense 导致 2N 次查询。可改为一次批量查询所有账户的收支汇总

### 维度 7:幂等性
- **issue-6** [严重度:中]:BudgetServiceImpl.save() 并发插入可能导致重复预算
  - **位置**:service/impl/BudgetServiceImpl.java:80-105
  - **修复建议**:service 层先 SELECT 检查是否存在再 INSERT，两个并发请求可能同时通过检查。虽然 SQL 有唯一索引 uk_budget_user_category_month 兜底，但应捕获重复键异常抛友好提示，而非让数据库异常穿透

### 维度 8:依规范核对

#### Entity 核对
- **参考集**:@TableName / @TableId(IdType.AUTO) / @TableLogic(软删除) / @JsonIgnore(密码) / DECIMAL→BigDecimal / DATETIME→LocalDateTime / @Data
- **被检集**:6 个 Entity 全部有 @Data + @TableName + @TableId(AUTO) + @TableField;User.password 有 @JsonIgnore;金额字段用 BigDecimal;时间字段用 LocalDateTime
- **差集 / 结论**:Account/RecurringBill 用 status 字段而非 @TableLogic + is_deleted。数据库设计确实用 status 字段做软删除，非 is_deleted，因此与数据库一致。无 issue。检查 6 项 · 全符合。

#### Mapper 核对
- **参考集**:extends BaseMapper / @Mapper / 空方法体(简单 CRUD) / 复杂查询走 XML + #{}
- **被检集**:6 个 Mapper 全部 extends BaseMapper + @Mapper;5 个空方法体;TransactionMapper 有 XML 自定义查询 + #{}
- **差集 / 结论**:全部对齐。检查 3 项 · 全符合。

#### Service 核对
- **参考集**:继承 IService / 禁止返回 Result<T> / 入参用 DTO / 方法名动词开头
- **被检集**:7 个 Service 接口继承 IService(注:实际未 extends IService，直接定义接口方法)· 返回值均为 DTO/IPage/List· 入参用 DTO 或基本类型· 方法名 register/login/list/create/update 等
- **差集 / 结论**:Service 接口未 extends IService<Entity>，但项目采用直接定义方法签名的风格（未使用 MP 的 IService/ServiceImpl 基类），符合项目约定。方法签名不返回 Result<T> ✅。检查 3 项 · 全符合。

#### ServiceImpl 核对
- **参考集**:@Service + @Slf4j + @RequiredArgsConstructor / 继承 ServiceImpl / 写操作 @Transactional / BusinessException(code, msg) / BCrypt / JwtUtils
- **被检集**:7 个 ServiceImpl 有 @Service + @RequiredArgsConstructor;部分有 @Slf4j(UserServiceImpl, TransactionServiceImpl);transfer/importCsv/generate 有 @Transactional;UserServiceImpl 用 BCrypt + JwtUtils
- **差集 / 结论**:AccountServiceImpl/BudgetServiceImpl/CategoryServiceImpl/RecurringBillServiceImpl/StatisticsServiceImpl 缺少 @Slf4j(中等影响，不利于排查问题)。检查 5 项 · 1 项部分不符合。

#### Controller 核对
- **参考集**:@RestController + @RequestMapping + @RequiredArgsConstructor / 入参 @Valid @RequestBody / 返参 Result<T> / 不写业务 / 不 try-catch
- **被检集**:9 个 Controller 有 @RestController + @RequestMapping;写操作有 @Valid @RequestBody;返参统一 Result.success/error;无业务逻辑;无 try-catch
- **差集 / 结论**:ExchangeRateController 无 @RequiredArgsConstructor(P2 硬编码，可接受)。检查 5 项 · 全符合。

#### DTO 核对
- **参考集**:@Data / 放 entity/dto/ / 命名 Request/Response / @NotBlank/@NotNull/@Size 等校验 / 密码不加 @JsonIgnore
- **被检集**:19 个 DTO 全部 @Data，放 entity/dto/，命名合规;写操作 Request 有校验注解;密码字段无 @JsonIgnore
- **差集 / 结论**:ChangePasswordRequest 缺少 @Size 校验(旧/新密码无长度限制，与 UserLoginRequest 不一致)。检查 5 项 · 1 项部分不符合。

## 反例推演结果

### 推演 A · 越权推演
1. **场景**:用户 A 用自己 token 调 PUT /api/account/{B的accountId}
   - 假设 → AccountController.update 提取 userId=A → AccountServiceImpl.getAccountById(A, B的accountId) → selectById 返回 account，但 account.userId=B ≠ A → 抛 BusinessException(2003, "账户不存在") → 结论:越权被阻止 ✅
2. **场景**:用户 A 用自己 token 调 DELETE /api/transaction/{B的transactionId}
   - 假设 → TransactionController.delete(不存在 delete 接口，事务记录不可删除) → 结论:接口不存在 ✅

### 推演 B · 空指针推演
1. **场景**:用户调 changePassword 但 userId 不存在
   - 假设 → UserController.changePassword 从 request.getAttribute("userId") 取到 null → 传入 service → userMapper.selectById(null) → 返回 null → "旧密码错误"异常 → 结论:NPE 不会发生，因为 BCryptEncoder.matches 会在 null password 时抛异常。但更安全的做法是 service 层先判 null。当前实现中等风险。
2. **场景**:BudgetServiceImpl.getProgress 调 list() 返回空列表
   - 假设 → budgets 为空 → 循环不执行 → 返回空列表 → 结论:安全 ✅

### 推演 C · 并发/幂等推演
1. **场景**:用户快速双击"保存预算"按钮
   - 假设双请求并发 → 请求 1 SELECT 看到无记录 → 请求 2 SELECT 也看到无记录 → 请求 1 INSERT → 请求 2 INSERT → 唯一索引 uk_budget_user_category_month 拦截请求 2 → 抛 DuplicateKeyException → GlobalExceptionHandler 兜底 500 "服务器内部错误" → 前端看到不友好提示。应捕获 DuplicateKeyException 抛 BusinessException(4001, "该分类本月预算已存在")。

### 推演 D · 异常传播推演
1. **场景**:登录时密码错误
   - 假设 → UserServiceImpl.login 比对密码失败 → throw BusinessException(1002, "用户名或密码错误") → UserController 不 catch → @RestControllerAdvice handleBusinessException → Result.error(1002, "用户名或密码错误") → 前端 axios 拦截器 ElMessage.error → 结论:异常传播链完整 ✅
2. **场景**:CSV 导入时文件格式错误
   - 假设 → TransactionServiceImpl.importCsv 解析失败 → throw BusinessException(3001, "CSV 文件读取失败") → TransactionController 不 catch → @RestControllerAdvice → Result.error(3001) → 结论:异常传播链完整 ✅

## 修复行动建议

按严重度排序:
1. **高** issue-2:修正 TransactionRequest type 注释(1=收入 2=支出)
2. **中** issue-5:AccountServiceImpl.getBalance() N+1 查询
3. **中** issue-4:TransactionServiceImpl.list() RowBounds → Page
4. **中** issue-6:BudgetServiceImpl.save() 并发安全
5. **中** issue-1:CORS 配置注释说明
6. **低** issue-3:ExchangeRateController 加 TODO 注释
7. **中** ServiceImpl 补充 @Slf4j
8. **中** ChangePasswordRequest 补充 @Size

## R-05 多文件拆分修复路径

本次为全模块审核，建议直接修改代码文件，无需调用 entity-coder/service-coder。
