-- ============================================================
-- 新建 deploy_artifact（幂等）
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 9 个。依赖 app 表已存在
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `deploy_artifact` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` INT NOT NULL COMMENT '应用id',
  `version` VARCHAR(50) NOT NULL COMMENT '版本号',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path` VARCHAR(512) NOT NULL COMMENT '服务器本地路径',
  `file_size` BIGINT NULL DEFAULT NULL COMMENT '字节数',
  `md5` VARCHAR(32) NULL DEFAULT NULL COMMENT 'MD5',
  `uploader` VARCHAR(50) NULL DEFAULT NULL COMMENT '上传人',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_artifact_app_version`(`app_id`, `version`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部署制品' ROW_FORMAT = DYNAMIC;

SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'deploy_artifact'
      AND CONSTRAINT_NAME = 'fk_artifact_app'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @fk_sql = IF(@fk_exists = 0,
    'ALTER TABLE `deploy_artifact` ADD CONSTRAINT `fk_artifact_app` FOREIGN KEY (`app_id`) REFERENCES `app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE',
    'SELECT 1');
PREPARE fk_stmt FROM @fk_sql;
EXECUTE fk_stmt;
DEALLOCATE PREPARE fk_stmt;
