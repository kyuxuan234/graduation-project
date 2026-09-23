-- ============================================================
-- 为已有 manager.server 补齐 SSH 与部署目录（幂等）
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 5 个（接在 upgrade_fk.sql 之后）
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_server_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_server_column_if_missing(
    IN p_column VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'server'
          AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL add_server_column_if_missing('ssh_port',
    'ALTER TABLE `server` ADD COLUMN `ssh_port` INT NOT NULL DEFAULT 22 COMMENT ''SSH 端口'' AFTER `remark`');
CALL add_server_column_if_missing('ssh_user',
    'ALTER TABLE `server` ADD COLUMN `ssh_user` VARCHAR(64) NULL DEFAULT NULL COMMENT ''SSH 登录用户名'' AFTER `ssh_port`');
CALL add_server_column_if_missing('ssh_auth_type',
    'ALTER TABLE `server` ADD COLUMN `ssh_auth_type` VARCHAR(16) NOT NULL DEFAULT ''PASSWORD'' COMMENT ''PASSWORD=密码；KEY=私钥'' AFTER `ssh_user`');
CALL add_server_column_if_missing('ssh_password',
    'ALTER TABLE `server` ADD COLUMN `ssh_password` VARCHAR(512) NULL DEFAULT NULL COMMENT ''SSH 密码（应用层加密存储）'' AFTER `ssh_auth_type`');
CALL add_server_column_if_missing('ssh_private_key',
    'ALTER TABLE `server` ADD COLUMN `ssh_private_key` TEXT NULL COMMENT ''SSH 私钥内容（应用层加密存储）'' AFTER `ssh_password`');
CALL add_server_column_if_missing('deploy_path',
    'ALTER TABLE `server` ADD COLUMN `deploy_path` VARCHAR(256) NULL DEFAULT NULL COMMENT ''应用部署根目录'' AFTER `ssh_private_key`');

DROP PROCEDURE IF EXISTS add_server_column_if_missing;
