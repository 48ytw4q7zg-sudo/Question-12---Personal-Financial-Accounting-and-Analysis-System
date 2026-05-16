-- ============================================================
-- 个人财务记账与分析系统 · 数据库初始化脚本
-- 版本: v2.0 · 生成日期: 2026-05-16
-- 数据库名: finance_db（与 application.yml 的 spring.datasource.url 一致）
-- 依据: docs/DATABASE_DESIGN.md §3 + §4
-- 注: 本文件由 /db-designer 自动生成,与 docs/DATABASE_DESIGN.md §3+§4 保持同步
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `finance_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `finance_db`;

-- ============================================================
-- 表 1: user（用户表 · P0）
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '用户主键',
  `username`    VARCHAR(20)   NOT NULL                 COMMENT '用户名（3-20位字母/数字/下划线）',
  `password`    VARCHAR(100)  NOT NULL                 COMMENT 'BCrypt加密后的密码哈希值（@JsonIgnore防响应泄漏）',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 表 2: account（账户表 · P0）
-- ============================================================
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '账户主键',
  `user_id`         BIGINT        NOT NULL                 COMMENT '所属用户ID（N:1 → user）',
  `name`            VARCHAR(20)   NOT NULL                 COMMENT '账户名称（1-20字符，如：现金、支付宝）',
  `type`            TINYINT       NOT NULL                 COMMENT '账户类型：1=现金, 2=银行卡, 3=支付宝, 4=微信',
  `initial_balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '初始余额（精度2位，禁用FLOAT/DOUBLE）',
  `currency`        VARCHAR(3)    NOT NULL DEFAULT 'CNY'   COMMENT '币种代码：CNY/USD/EUR/JPY/GBP/HKD（默认CNY）',
  `status`          TINYINT       NOT NULL DEFAULT 1       COMMENT '状态：1=正常, 0=禁用（软删除，禁用后不可恢复）',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- ============================================================
-- 表 3: category（分类表 · 种子数据 · P0）
-- ============================================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '分类主键',
  `name`        VARCHAR(10)  NOT NULL                 COMMENT '分类名称（1-10字符）',
  `type`        TINYINT      NOT NULL                 COMMENT '分类类型：1=支出, 2=收入',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支分类表（种子数据）';

-- 种子数据：支出分类（type=1）共 8 条
INSERT INTO `category` (`name`, `type`) VALUES
  ('餐饮', 1),
  ('交通', 1),
  ('购物', 1),
  ('住房', 1),
  ('娱乐', 1),
  ('医疗', 1),
  ('教育', 1),
  ('其他', 1);

-- 种子数据：收入分类（type=2）共 5 条
INSERT INTO `category` (`name`, `type`) VALUES
  ('工资', 2),
  ('奖金', 2),
  ('兼职', 2),
  ('理财', 2),
  ('其他', 2);

-- ============================================================
-- 表 4: transaction（收支记录表 · P0）
-- ============================================================
DROP TABLE IF EXISTS `transaction`;
CREATE TABLE `transaction` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '记录主键',
  `user_id`     BIGINT        NOT NULL                 COMMENT '所属用户ID（N:1 → user）',
  `account_id`  BIGINT        NOT NULL                 COMMENT '关联账户ID（N:1 → account）',
  `category_id` BIGINT        NOT NULL                 COMMENT '关联分类ID（N:1 → category）',
  `type`        TINYINT       NOT NULL                 COMMENT '交易类型：1=收入, 2=支出（转账时生成一收一支两条记录）',
  `amount`      DECIMAL(12,2) NOT NULL                 COMMENT '金额（必须>0，精度2位，禁用FLOAT/DOUBLE）',
  `note`        VARCHAR(200)  DEFAULT NULL             COMMENT '备注（≤200字符，可为空；NULL=无备注）',
  `time`        DATETIME      NOT NULL                 COMMENT '交易时间（ISO 8601格式）',
  `transfer_id` VARCHAR(36)   DEFAULT NULL             COMMENT '转账关联ID（UUID；NULL=普通收支，非NULL=转账关联记录）',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_transaction_user_id`      (`user_id`),
  KEY `idx_transaction_account_id`   (`account_id`),
  KEY `idx_transaction_time`         (`user_id`, `time`),
  KEY `idx_transaction_transfer_id`  (`transfer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支记录表';

-- ============================================================
-- 表 5: budget（预算表 · P1）
-- ============================================================
DROP TABLE IF EXISTS `budget`;
CREATE TABLE `budget` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '预算主键',
  `user_id`     BIGINT        NOT NULL                 COMMENT '所属用户ID（N:1 → user）',
  `category_id` BIGINT        NOT NULL                 COMMENT '关联分类ID（N:1 → category，仅支出分类）',
  `month`       VARCHAR(7)    NOT NULL                 COMMENT '预算月份（格式YYYY-MM，如2026-05）',
  `amount`      DECIMAL(12,2) NOT NULL                 COMMENT '预算金额（必须>0，精度2位，禁用FLOAT/DOUBLE）',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_user_category_month` (`user_id`, `category_id`, `month`),
  KEY `idx_budget_user_month` (`user_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';

-- ============================================================
-- 表 6: recurring_bill（周期性账单表 · P1）
-- ============================================================
DROP TABLE IF EXISTS `recurring_bill`;
CREATE TABLE `recurring_bill` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '账单主键',
  `user_id`       BIGINT        NOT NULL                 COMMENT '所属用户ID（N:1 → user）',
  `account_id`    BIGINT        NOT NULL                 COMMENT '关联账户ID（N:1 → account）',
  `category_id`   BIGINT        NOT NULL                 COMMENT '关联分类ID（N:1 → category）',
  `name`          VARCHAR(30)   NOT NULL                 COMMENT '账单名称（1-30字符，如：房租、工资）',
  `amount`        DECIMAL(12,2) NOT NULL                 COMMENT '金额（必须>0，精度2位，禁用FLOAT/DOUBLE）',
  `type`          TINYINT       NOT NULL                 COMMENT '类型：1=收入, 2=支出',
  `period`        VARCHAR(10)   NOT NULL                 COMMENT '周期：monthly=每月, weekly=每周',
  `next_due_date` DATE          NOT NULL                 COMMENT '下次到期日（@Scheduled日检到期依据）',
  `status`        TINYINT       NOT NULL DEFAULT 1       COMMENT '状态：1=活跃, 0=停用（软删除，停用后不可恢复）',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_recurring_bill_user_id` (`user_id`),
  KEY `idx_recurring_bill_account_id` (`account_id`),
  KEY `idx_recurring_bill_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期性账单表';

-- ============================================================
-- 测试数据（按外键依赖顺序: user → account → transaction → budget → recurring_bill）
-- ============================================================

-- 测试用户（2 条，密码均为 123456 的 BCrypt 哈希）
INSERT INTO `user` (`username`, `password`) VALUES
  ('zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
  ('lisi',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- 测试账户（4 条，覆盖 4 种类型，均为用户 zhangsan 的账户）
INSERT INTO `account` (`user_id`, `name`, `type`, `initial_balance`, `currency`) VALUES
  (1, '现金钱包',   1,  5000.00, 'CNY'),
  (1, '招商银行卡', 2, 30000.00, 'CNY'),
  (1, '支付宝',     3,  8000.00, 'CNY'),
  (1, '微信零钱',   4,  2000.00, 'CNY');

-- 测试收支记录（5 条 + 1 组转账共 6 条）
-- 注：category_id=13 为「其他(收入)」，转账无专属分类归入「其他」
-- 注：实际转账接口使用 UUID.randomUUID()，此处为测试固定值
INSERT INTO `transaction` (`user_id`, `account_id`, `category_id`, `type`, `amount`, `note`, `time`, `transfer_id`) VALUES
  (1, 1, 1,  2,   50.00, '午餐外卖',       '2026-05-16 12:30:00', NULL),
  (1, 3, 9,  1, 8000.00, '5月工资',         '2026-05-10 09:00:00', NULL),
  (1, 2, 4,  2, 2500.00, '5月房租',         '2026-05-01 08:00:00', NULL),
  (1, 2, 5,  2,  120.00, '电影票',          '2026-05-15 20:00:00', NULL),
  -- 转账组：从银行卡转 200 元到现金（两条记录共享 transfer_id）
  (1, 2, 13, 2,  200.00, '银行卡→现金(转出)', '2026-05-14 10:00:00', 't-001-uuid-test'),
  (1, 1, 13, 1,  200.00, '银行卡→现金(转入)', '2026-05-14 10:00:00', 't-001-uuid-test');

-- 测试预算（3 条）
INSERT INTO `budget` (`user_id`, `category_id`, `month`, `amount`) VALUES
  (1, 1, '2026-05', 2000.00),  -- 餐饮 2000
  (1, 2, '2026-05',  500.00),  -- 交通 500
  (1, 3, '2026-05', 1000.00);  -- 购物 1000

-- 测试周期性账单（3 条）
INSERT INTO `recurring_bill` (`user_id`, `account_id`, `category_id`, `name`, `amount`, `type`, `period`, `next_due_date`) VALUES
  (1, 2, 4, '月房租',  2500.00, 2, 'monthly', '2026-06-01'),
  (1, 2, 9, '月工资',  8000.00, 1, 'monthly', '2026-06-10'),
  (1, 3, 5, '网费',     120.00, 2, 'monthly', '2026-06-15');
