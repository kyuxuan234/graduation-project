-- ============================================================
-- 为已有 manager.deploy 补执行方式 execute_mode（幂等）
-- AUTO：批准后通过 SSH 自动执行
-- MANUAL：批准后只进入部署中，由运维在页面确认成功或失败
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 10 个
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_deploy_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_deploy_column_if_missing(
    IN p_column VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'deploy'
          AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL add_deploy_column_if_missing('execute_mode',
    'ALTER TABLE `deploy` ADD COLUMN `execute_mode` VARCHAR(16) NOT NULL DEFAULT ''AUTO'' COMMENT ''执行方式：AUTO-自动SSH MANUAL-人工确认'' AFTER `deploy_type`');

DROP PROCEDURE IF EXISTS add_deploy_column_if_missing;
