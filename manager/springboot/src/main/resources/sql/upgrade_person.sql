-- ============================================================
-- 把 admin / dev / ops 合并进 person（幂等）
-- 全新安装走 schema.sql，不必再执行本脚本
-- 执行顺序：第 11 个。人员主键会重新生成，已登录的 token 需重新登录
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `person` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NULL DEFAULT NULL COMMENT '密码：新密码 BCrypt，兼容历史 32 位 MD5(明文 + 盐值)',
  `name` VARCHAR(50) NULL DEFAULT NULL COMMENT '姓名',
  `phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：ADMIN-超级管理员 DEV-开发 TL-技术负责人 QA-测试 PM-产品经理 OPS-运维',
  `status` VARCHAR(20) NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常 DISABLED-禁用',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_person_username`(`username`) USING BTREE,
  INDEX `idx_person_role`(`role`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '人员' ROW_FORMAT = DYNAMIC;

DROP PROCEDURE IF EXISTS migrate_person_tables;
DELIMITER $$
CREATE PROCEDURE migrate_person_tables()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin'
    ) THEN
        INSERT INTO person (username, password, name, phone, email, avatar, role, status, create_time, update_time, deleted)
        SELECT a.username, a.password, a.name, a.phone, a.email, a.avatar,
               IFNULL(NULLIF(a.role, ''), 'ADMIN'),
               IFNULL(NULLIF(a.status, ''), 'ACTIVE'),
               IFNULL(a.create_time, CURRENT_TIMESTAMP),
               IFNULL(a.update_time, CURRENT_TIMESTAMP),
               IFNULL(a.deleted, 0)
        FROM admin a
        WHERE NOT EXISTS (SELECT 1 FROM person p WHERE p.username = a.username);
        DROP TABLE admin;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dev'
    ) THEN
        INSERT INTO person (username, password, name, phone, email, role, status, create_time, update_time, deleted)
        SELECT d.username, d.password, d.name, d.phone, d.email, 'DEV',
               IFNULL(NULLIF(d.status, ''), 'ACTIVE'),
               IFNULL(d.create_time, CURRENT_TIMESTAMP),
               IFNULL(d.update_time, CURRENT_TIMESTAMP),
               IFNULL(d.deleted, 0)
        FROM dev d
        WHERE NOT EXISTS (SELECT 1 FROM person p WHERE p.username = d.username);
        DROP TABLE dev;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ops'
    ) THEN
        INSERT INTO person (username, password, name, phone, email, role, status, create_time, update_time, deleted)
        SELECT o.username, o.password, o.name, o.phone, o.email, 'OPS',
               IFNULL(NULLIF(o.status, ''), 'ACTIVE'),
               IFNULL(o.create_time, CURRENT_TIMESTAMP),
               IFNULL(o.update_time, CURRENT_TIMESTAMP),
               IFNULL(o.deleted, 0)
        FROM ops o
        WHERE NOT EXISTS (SELECT 1 FROM person p WHERE p.username = o.username);
        DROP TABLE ops;
    END IF;
END$$
DELIMITER ;

CALL migrate_person_tables();
DROP PROCEDURE IF EXISTS migrate_person_tables;
