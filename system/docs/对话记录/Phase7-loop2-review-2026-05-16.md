# Phase 7 · Loop 2 验收审查记录

日期: 2026-05-16
类型: 150 项验收检查 (第 2 轮)

## Loop 2 新增修复

### 1. BCrypt 测试数据哈希修复 (检查项 66, 75)
- **问题**: `01-init.sql` 中 zhangsan/lisi 的 BCrypt 哈希不对应 "123456"，导致登录返回 1002
- **修复**: 用 `BCryptPasswordEncoder` 重新生成正确哈希，更新 SQL 脚本和数据库
- **验证**: `POST /api/user/login` → code=200, 返回 token ✅

### 2. 统计 SQL 排除 transfer 虚增 (检查项 89, 99)
- **问题**: `selectMonthlySummary`/`selectYearlySummary`/`selectTrend`/`selectCategorySummary` 4 个查询没有排除 transfer 记录，导致月度收支数据虚增
- **修复**: 4 个查询均添加 `AND t.transfer_id IS NULL`
- **验证**: 修复前 totalIncome=8200/totalExpense=2870 → 修复后 totalIncome=8000/totalExpense=2670 ✅

### 3. 补充单元测试 (检查项 137)
- 新增 `TransactionServiceImplTest` (8 用例): 创建/更新/转账核心逻辑覆盖
- 新增 `BudgetServiceImplTest` (6 用例): 保存/列表/预警逻辑覆盖
- 单元测试总数: 14 → 28
- `mvn test` → 28 用例 0 失败 ✅

## 运行时 API 验证

| API | 方法 | 状态 | 备注 |
|-----|------|:---:|------|
| /api/health | GET | ✅ | status=UP |
| /api/user/register | POST | ✅ | 返回 token |
| /api/user/login | POST | ✅ | zhangsan/123456 成功 |
| /api/account | GET | ✅ | 4 个账户 |
| /api/category | GET | ✅ | 13 个分类 |
| /api/transaction | GET | ✅ | 6 条记录分页 |
| /api/statistics/monthly | GET | ✅ | 收入8000/支出2670/结余5330 |

## 当前状态

- **150 项检查通过**: 149/150 (仅缺 1 项需 MySQL 环境验证)
- **mvn clean compile**: ✅ BUILD SUCCESS
- **pnpm build**: ✅ built in 697ms
- **mvn test**: ✅ 28 用例 0 失败
- **运行后端**: ✅ 所有核心 API 正常

## 结论

代码层面全部通过，统计 transfer 排除修复确保数据准确性。后端 API 运行时验证通过。系统达到可演示标准。
