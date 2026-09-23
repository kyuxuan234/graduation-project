-- ============================================================
-- 为已有 manager.alarm 补 deploy_no（幂等）
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 2 个
-- ============================================================

SET NAMES utf8mb4;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'alarm'
      AND COLUMN_NAME = 'deploy_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `alarm` ADD COLUMN `deploy_no` VARCHAR(50) NULL DEFAULT NULL COMMENT ''关联部署单编号'' AFTER `server_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
