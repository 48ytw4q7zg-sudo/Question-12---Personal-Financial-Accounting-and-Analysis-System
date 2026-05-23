-- ============================================================
-- 预算预警表结构修复脚本（幂等）
-- 用途：补齐 budget_alert.update_time 列（与 sql/01-init.sql 设计对齐）
-- 触发：BudgetAlertServiceImpl.getAlerts() 因实体映射 update_time 但 DB 缺列报 500
-- 触发版本：2026-05-23
-- ============================================================

USE finance_db;

-- 通过 information_schema 判断列是否存在，存在即跳过 ALTER（幂等）
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'finance_db'
    AND TABLE_NAME   = 'budget_alert'
    AND COLUMN_NAME  = 'update_time'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE budget_alert ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
  'SELECT ''[OK] budget_alert.update_time 已存在，跳过'' AS info'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '[DONE] budget_alert 修复完成' AS result;
