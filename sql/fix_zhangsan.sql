-- Reset zhangsan password to BCrypt hash of "123456" (cost=10)
-- Reset admin password to BCrypt hash of "123456" (cost=10) if truncated
UPDATE user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username = 'zhangsan';
UPDATE user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username = 'admin' AND LENGTH(password) < 50;
-- Verify
SELECT id, username, role, LENGTH(password) AS pwd_len FROM user WHERE username IN ('zhangsan','admin','zhangsan2','admin_fix');
