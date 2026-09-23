-- ============================================================
-- 为现有 manager 库补齐 7 条物理外键（幂等）
-- 枢纽：app；约束策略：ON DELETE RESTRICT，禁止物理删除被引用的父记录
-- 执行顺序：第 4 个
-- ============================================================

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_fk_if_missing $$
CREATE PROCEDURE add_fk_if_missing(
    IN p_table VARCHAR(64),
    IN p_name VARCHAR(64),
    IN p_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND CONSTRAINT_NAME = p_name
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        SET @ddl = p_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_fk_if_missing('app', 'fk_app_project',
    'ALTER TABLE `app` ADD CONSTRAINT `fk_app_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('app', 'fk_app_server',
    'ALTER TABLE `app` ADD CONSTRAINT `fk_app_server` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('deploy', 'fk_deploy_app',
    'ALTER TABLE `deploy` ADD CONSTRAINT `fk_deploy_app` FOREIGN KEY (`app_id`) REFERENCES `app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('deploy', 'fk_deploy_server',
    'ALTER TABLE `deploy` ADD CONSTRAINT `fk_deploy_server` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('deploy_log', 'fk_deploy_log_deploy',
    'ALTER TABLE `deploy_log` ADD CONSTRAINT `fk_deploy_log_deploy` FOREIGN KEY (`deploy_id`) REFERENCES `deploy` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('alarm', 'fk_alarm_app',
    'ALTER TABLE `alarm` ADD CONSTRAINT `fk_alarm_app` FOREIGN KEY (`app_id`) REFERENCES `app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('alarm', 'fk_alarm_server',
    'ALTER TABLE `alarm` ADD CONSTRAINT `fk_alarm_server` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE');

DROP PROCEDURE IF EXISTS add_fk_if_missing;
