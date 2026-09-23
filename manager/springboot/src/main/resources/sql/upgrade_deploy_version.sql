-- ============================================================
-- 为已有 manager.deploy 补 rollback_version 与乐观锁 row_version（幂等）
-- version 仍是发布版本号，不要改成乐观锁列
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 7 个
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

CALL add_deploy_column_if_missing('rollback_version',
    'ALTER TABLE `deploy` ADD COLUMN `rollback_version` VARCHAR(50) NULL DEFAULT NULL COMMENT ''回滚目标版本'' AFTER `remark`');
CALL add_deploy_column_if_missing('row_version',
    'ALTER TABLE `deploy` ADD COLUMN `row_version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本'' AFTER `rollback_version`');

DROP PROCEDURE IF EXISTS add_deploy_column_if_missing;
