-- ============================================================
-- 为已有 manager.admin 补齐 status / create_time / update_time / deleted（幂等）
-- 旧脚手架只有 id、username、password、name、avatar、role、phone、email
-- 全新安装走 schema.sql，不必再执行本脚本
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_admin_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_admin_column_if_missing(
    IN p_column VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'admin'
          AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL add_admin_column_if_missing('status',
    'ALTER TABLE `admin` ADD COLUMN `status` VARCHAR(20) NULL DEFAULT ''ACTIVE'' COMMENT ''状态：ACTIVE-正常 DISABLED-禁用'' AFTER `role`');
CALL add_admin_column_if_missing('create_time',
    'ALTER TABLE `admin` ADD COLUMN `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `status`');
CALL add_admin_column_if_missing('update_time',
    'ALTER TABLE `admin` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `create_time`');
CALL add_admin_column_if_missing('deleted',
    'ALTER TABLE `admin` ADD COLUMN `deleted` TINYINT NULL DEFAULT 0 COMMENT ''逻辑删除：0-未删除 1-已删除'' AFTER `update_time`');

DROP PROCEDURE IF EXISTS add_admin_column_if_missing;

UPDATE `admin` SET `status` = 'ACTIVE' WHERE `status` IS NULL;
UPDATE `admin` SET `deleted` = 0 WHERE `deleted` IS NULL;
