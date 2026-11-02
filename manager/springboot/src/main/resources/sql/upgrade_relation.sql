-- ============================================================
-- 把人员、公告和发布单号收进外键，并收紧上线时必须有的归属
-- 适用：已经按 schema 或 upgrade_fk 建过表的 manager 库
-- 全新安装不要执行本脚本，直接用 schema.sql
-- ============================================================

SET NAMES utf8mb4;

-- 开发负责人统一存用户名。演示数据里的姓名改到对应开发账号。
UPDATE `project` SET `leader` = NULL WHERE `leader` = '';
UPDATE `project` SET `leader` = 'dev1' WHERE `leader` IN ('张伟', '王强');
UPDATE `project` SET `leader` = 'dev2' WHERE `leader` IN ('李娜', '赵敏');
UPDATE `project` p
    LEFT JOIN `person` pe ON pe.`username` = p.`leader` AND pe.`deleted` = 0
SET p.`leader` = NULL
WHERE p.`leader` IS NOT NULL AND pe.`id` IS NULL;

-- 系统自动动作不挂人员账号
UPDATE `deploy_log` SET `operator` = NULL WHERE `operator` IN ('system', '');
UPDATE `deploy_log` l
    LEFT JOIN `person` pe ON pe.`username` = l.`operator` AND pe.`deleted` = 0
SET l.`operator` = NULL
WHERE l.`operator` IS NOT NULL AND pe.`id` IS NULL;

UPDATE `deploy` SET `approver` = NULL WHERE `approver` = '';
UPDATE `deploy` d
    LEFT JOIN `person` pe ON pe.`username` = d.`approver` AND pe.`deleted` = 0
SET d.`approver` = NULL
WHERE d.`approver` IS NOT NULL AND pe.`id` IS NULL;

UPDATE `alarm` SET `handler` = NULL WHERE `handler` = '';
UPDATE `alarm` a
    LEFT JOIN `person` pe ON pe.`username` = a.`handler` AND pe.`deleted` = 0
SET a.`handler` = NULL
WHERE a.`handler` IS NOT NULL AND pe.`id` IS NULL;

UPDATE `alarm` SET `deploy_no` = NULL WHERE `deploy_no` = '';
UPDATE `alarm` a
    LEFT JOIN `deploy` d ON d.`deploy_no` = a.`deploy_no`
SET a.`deploy_no` = NULL
WHERE a.`deploy_no` IS NOT NULL AND d.`id` IS NULL;

UPDATE `notice` n
    LEFT JOIN `person` pe ON pe.`username` = n.`user` AND pe.`deleted` = 0
SET n.`user` = 'admin'
WHERE n.`user` IS NULL OR n.`user` = '' OR pe.`id` IS NULL;

ALTER TABLE `app`
    MODIFY `project_id` INT NOT NULL COMMENT '所属项目id',
    MODIFY `server_id` INT NULL DEFAULT NULL COMMENT '当前挂载服务器id，可与发布单目标环境不同';

ALTER TABLE `deploy`
    MODIFY `app_id` INT NOT NULL COMMENT '应用id',
    MODIFY `server_id` INT NOT NULL COMMENT '目标服务器id',
    MODIFY `applicant` VARCHAR(50) NOT NULL COMMENT '申请人用户名',
    MODIFY `approver` VARCHAR(50) NULL DEFAULT NULL COMMENT '审批人用户名，未审批为空';

ALTER TABLE `project`
    MODIFY `leader` VARCHAR(50) NULL DEFAULT NULL COMMENT '开发负责人用户名，由技术负责人从在职开发人员中分配';

ALTER TABLE `deploy_log`
    MODIFY `deploy_id` INT NOT NULL COMMENT '部署单id',
    MODIFY `operator` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作人用户名，系统自动动作为空';

ALTER TABLE `alarm`
    MODIFY `deploy_no` VARCHAR(50) NULL DEFAULT NULL COMMENT '关联发布单号，可空，有值时必须是已有发布单',
    MODIFY `handler` VARCHAR(50) NULL DEFAULT NULL COMMENT '处理人用户名，未接手为空';

ALTER TABLE `notice`
    MODIFY `user` VARCHAR(50) NOT NULL COMMENT '发布人用户名';

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

CALL add_fk_if_missing('notice', 'fk_notice_user',
    'ALTER TABLE `notice` ADD CONSTRAINT `fk_notice_user` FOREIGN KEY (`user`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('project', 'fk_project_leader',
    'ALTER TABLE `project` ADD CONSTRAINT `fk_project_leader` FOREIGN KEY (`leader`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('deploy', 'fk_deploy_applicant',
    'ALTER TABLE `deploy` ADD CONSTRAINT `fk_deploy_applicant` FOREIGN KEY (`applicant`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('deploy', 'fk_deploy_approver',
    'ALTER TABLE `deploy` ADD CONSTRAINT `fk_deploy_approver` FOREIGN KEY (`approver`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('deploy_log', 'fk_deploy_log_operator',
    'ALTER TABLE `deploy_log` ADD CONSTRAINT `fk_deploy_log_operator` FOREIGN KEY (`operator`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('alarm', 'fk_alarm_deploy',
    'ALTER TABLE `alarm` ADD CONSTRAINT `fk_alarm_deploy` FOREIGN KEY (`deploy_no`) REFERENCES `deploy` (`deploy_no`) ON DELETE RESTRICT ON UPDATE CASCADE');
CALL add_fk_if_missing('alarm', 'fk_alarm_handler',
    'ALTER TABLE `alarm` ADD CONSTRAINT `fk_alarm_handler` FOREIGN KEY (`handler`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE');

DROP PROCEDURE IF EXISTS add_fk_if_missing;
