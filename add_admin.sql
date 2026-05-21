INSERT INTO user (username, password, role) VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1) ON DUPLICATE KEY UPDATE role=1;
SELECT id, username, role FROM user;