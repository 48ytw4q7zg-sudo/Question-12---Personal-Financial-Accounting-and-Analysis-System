# Phase 7 · 后端代码审查记录

日期: 2026-05-16
审查范围: 全部后端 Service 实现

## 审查发现

### 已修复
1. UserServiceImpl: register/login 逻辑分离 (1001/1002 error code)
2. TransactionServiceImpl: transfer type 映射 (1=收入 2=支出)
3. TransactionServiceImpl: transferId Long→String
4. AccountServiceImpl: error code 修正 (2001→2002)
5. RecurringBillServiceImpl: 批量加载消除 N+1
6. BudgetServiceImpl: 支出分类校验
7. JwtUtils: 支持外部配置注入
8. TransactionMapper.xml: type CASE WHEN 修正
9. TransactionController: sortBy 默认值修正
10. GlobalExceptionHandler: 3 个处理器完整

### 代码质量评估
- Controller 厚度: 22-91 行 (合格)
- Service 厚度: 38-272 行 (最大 RecurringBillServiceImpl 含批量加载逻辑)
- 无 raw SQL (全部 LambdaQueryWrapper 或 XML 参数化)
- 无硬编码 URL
- 无密码泄露风险 (@JsonIgnore)
