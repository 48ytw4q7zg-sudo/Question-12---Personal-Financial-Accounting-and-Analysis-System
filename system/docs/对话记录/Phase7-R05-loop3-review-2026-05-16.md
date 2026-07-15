# Phase 7 · Loop 3 R-05 后端代码审核报告 · 2026-05-16

## 审核元数据
- 审核日期: 2026-05-16
- 审核切片: TransactionMapper + TransactionServiceImpl + BudgetServiceImpl
- 输入摘要: TransactionMapper.xml(154行) + TransactionMapper.java(91行) + TransactionServiceImpl.java(316行) + TransactionController.java(91行) + TransactionRequest.java(39行) + BudgetServiceImpl.java(197行) + BudgetController.java(70行) + BudgetRequest.java(25行)

## 审核报告

### 维度 1: 安全漏洞
- **无新问题**: SQL 全部 `#{}` 参数化，transfer 排除使用 `t.transfer_id IS NULL`，无 SQL 注入风险。userId 归属校验完整（每个写操作都通过 `validateAccount` / `getTransactionById` 校验归属）。密码字段在 Transaction/Budget 模块不涉及。

### 维度 2: 逻辑正确性
- **issue-1** [严重度: 高]: Transfer 转账记录的 outNote 方向写反
  - **位置**: `service/impl/TransactionServiceImpl.java:162`
  - **问题**: `outNote = toAccount.getName() + "→" + fromAccount.getName() + "(转出)"` — 转出记录的备注中箭头方向反了。从 fromAccount 转出到 toAccount，应该是 `fromAccount→toAccount(转出)`
  - **修复**: `String outNote = fromAccount.getName() + "→" + toAccount.getName() + "(转出)"`
  - **对比**: inNote (163行) 方向正确: `fromAccount.getName() + "→" + toAccount.getName() + "(转入)"`

### 维度 3: 异常处理
- **无新问题**: BusinessException 正确使用模块编号 3xxx。Controller 未 try-catch。

### 维度 4: 代码规范
- **无新问题**: Controller 纯转发，无业务逻辑。`@RequiredArgsConstructor` 构造器注入。

### 维度 5: MyBatis-Plus 用法
- **无新问题**: LambdaQueryWrapper 用于简单查询，XML `#{}` 用于复杂统计查询，符合 §二·四 例外路径。

### 维度 6: 性能
- **issue-2** [严重度: 中]: BudgetServiceImpl.getProgress() 中 N+1 查询
  - **位置**: `service/impl/BudgetServiceImpl.java:162`
  - **问题**: `selectCategorySummary(..., 1)` 在 for 循环内被调用 N 次，每次参数完全相同
  - **修复**: 将 `selectCategorySummary` 调用提到 for 循环外，只查一次后用 Map 索引

### 维度 7: 幂等性
- **无新问题**: Budget 保存有 UNIQUE 索引 + DuplicateKeyException 兜底。Transfer 有 @Transactional 保证原子性。

### 维度 8: 依规范核对(三段式)

#### Entity 核对
- **参考集**: @TableName / @TableId(IdType.AUTO) / @TableField / DECIMAL→BigDecimal / DATETIME→LocalDateTime / @Data
- **被检集**: Transaction.java: @TableName("transaction") ✅ / @TableId(IdType.AUTO) ✅ / @TableField ✅ / BigDecimal ✅ / LocalDateTime ✅ / @Data ✅
- **结论**: 全部对齐，6 项全符合

#### Mapper 核对
- **参考集**: extends BaseMapper<Entity> / @Mapper
- **被检集**: TransactionMapper.java: extends BaseMapper<Transaction> ✅ / @Mapper ✅. BudgetMapper.java: extends BaseMapper<Budget> ✅ / @Mapper ✅
- **结论**: 全部对齐

#### Service 核对
- **参考集**: 接口方法签名不使用 Result<T> / 入参用 DTO / 分页返回 IPage
- **被检集**: 接口方法返 IPage<TransactionDTO>/List<BudgetDTO>/TransferDTO ✅
- **结论**: 全部对齐

#### ServiceImpl 核对
- **参考集**: @Service + @Slf4j + @RequiredArgsConstructor / @Transactional 写操作 / BusinessException
- **被检集**: 两个 ServiceImpl 均有 @Service @Slf4j @RequiredArgsConstructor ✅ / transfer 有 @Transactional ✅ / importCsv 有 @Transactional ✅ / BusinessException 正确使用 ✅
- **结论**: 全部对齐 · 7 项全符合

#### Controller 核对
- **参考集**: @RestController + @RequestMapping("/api/...") + @RequiredArgsConstructor / Result<T> 包装 / 无 try-catch
- **被检集**: TransactionController: 全部符合 ✅ / BudgetController: 全部符合 ✅
- **结论**: 全部对齐

#### DTO 核对
- **参考集**: @Data / @NotNull/@NotBlank/@Size 校验 / 禁止密码字段 / 放 entity/dto/
- **被检集**: TransactionRequest: @Data ✅ / @NotNull + @DecimalMin ✅ / 无密码字段 ✅. BudgetRequest: @Data ✅ / @NotNull + @NotBlank + @DecimalMin ✅
- **结论**: 全部对齐 · 无 issue

### 反例推演结果

#### 推演 A · 越权推演
- Transaction PUT /api/transaction/{id}: 假设用户A携token改用户B的记录 → Controller → Service.update(userId=B'sUserId, id=A'sRecordId) → getTransactionById 校验 `!transaction.getUserId().equals(userId)` → 抛 BusinessException(3006) ✅
- Budget POST /api/budget: 假设用户A创建预算关联用户B的分类 → Controller → Service.save(userId=A) → 写入的是 userId=A ✅

#### 推演 B · 空指针推演
- `accountMapper.selectById(accountId)` 返回 null → validateAccount 判 null 抛 BusinessException(3002) ✅
- `categoryMapper.selectById(categoryId)` 返回 null → validateCategory 判 null 抛 BusinessException(3002) ✅

#### 推演 C · 并发/幂等推演
- Budget 并发保存同一分类同月: 双请求同时进入 → 请求1 SELECT 无 existing → 请求2 SELECT 也无 existing → 请求1 INSERT 成功 → 请求2 INSERT 触发 DuplicateKeyException → catch 后重新查询+UPDATE ✅

#### 推演 D · 异常传播推演
- Service 抛 BusinessException(3006, "收支记录不存在") → Controller 不 try-catch → GlobalExceptionHandler.handleBusinessException → Result.error(3006, msg) → 前端 axios 拦截器 ElMessage.error ✅

## 修复建议

| 优先级 | issue | 修复方式 |
|:---:|---|:---|
| 高 | outNote 方向反 | 交换 outNote 拼接顺序 |
| 中 | getProgress N+1 | 提 selectCategorySummary 到循环外 |

## 结论

Transaction 和 Budget 模块整体质量好：SQL 全部参数化、userId 权限校验完整、异常处理规范。2 个 issue 修复后可通过 R-05。
