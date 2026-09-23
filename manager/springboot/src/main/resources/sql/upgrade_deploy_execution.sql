-- ============================================================
-- 新建 deploy_execution（幂等）
-- 一次开始部署对应一条记录，重试只更新同一条
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 8 个。依赖 deploy 表已存在
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `deploy_execution` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `deploy_id` INT NOT NULL COMMENT '部署单id',
  `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED/TIMEOUT',
  `exit_code` INT NULL DEFAULT NULL COMMENT '远程脚本退出码',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `start_time` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `error_message` VARCHAR(500) NULL DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_execution_deploy`(`deploy_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部署执行记录' ROW_FORMAT = DYNAMIC;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'deploy_execution'
      AND CONSTRAINT_NAME = 'fk_execution_deploy'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @fk_sql = IF(@fk_exists = 0,
    'ALTER TABLE `deploy_execution` ADD CONSTRAINT `fk_execution_deploy` FOREIGN KEY (`deploy_id`) REFERENCES `deploy` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE',
    'SELECT 1');
PREPARE fk_stmt FROM @fk_sql;
EXECUTE fk_stmt;
DEALLOCATE PREPARE fk_stmt;
