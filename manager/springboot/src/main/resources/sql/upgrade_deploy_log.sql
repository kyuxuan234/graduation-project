-- ============================================================
-- 为已有 manager.deploy_log 补执行日志列，并把 detail 扩到 1024（幂等）
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 6 个
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_deploy_log_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_deploy_log_column_if_missing(
    IN p_column VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'deploy_log'
          AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL add_deploy_log_column_if_missing('log_type',
    'ALTER TABLE `deploy_log` ADD COLUMN `log_type` VARCHAR(16) NOT NULL DEFAULT ''STATE'' COMMENT ''STATE-状态流转 EXEC-执行输出'' AFTER `detail`');
CALL add_deploy_log_column_if_missing('seq',
    'ALTER TABLE `deploy_log` ADD COLUMN `seq` INT NOT NULL DEFAULT 0 COMMENT ''EXEC 行序从 1 递增，STATE 固定 0'' AFTER `log_type`');
CALL add_deploy_log_column_if_missing('source',
    'ALTER TABLE `deploy_log` ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''MANUAL-人工 AUTO-系统自动'' AFTER `seq`');

ALTER TABLE `deploy_log` MODIFY COLUMN `detail` VARCHAR(1024) NULL DEFAULT NULL COMMENT '操作说明';

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'deploy_log'
      AND INDEX_NAME = 'idx_log_deploy_type_seq'
);
SET @idx_sql = IF(@idx_exists = 0,
    'ALTER TABLE `deploy_log` ADD INDEX `idx_log_deploy_type_seq`(`deploy_id`, `log_type`, `seq`)',
    'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

DROP PROCEDURE IF EXISTS add_deploy_log_column_if_missing;
