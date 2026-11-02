-- ============================================================
-- DevOps 运维管理系统 - 建表脚本
-- 目标库：manager（本脚本不创建数据库，请先 use manager）
-- 说明：
--   1. 统一使用 CREATE TABLE IF NOT EXISTS，不会覆盖已存在的表
--   2. 每张表统一包含 id / create_time / update_time / deleted 四个公共字段
--   3. 状态类字段统一 VARCHAR(20)，取值为英文枚举，前后端保持一致
--   4. 外键以人员用户名、发布单号和主键串联。应用必须属于项目；发布单必须指向应用和目标环境
--   5. 开发负责人、申请人、审批人、操作人、告警处理人、公告发布人都指向 person.username
--   6. 系统自动动作的操作人为空。告警的发布单号可空，但必须是已有单号；告警至少指向应用或环境之一
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 0. 人员（管理员 / 开发 / 运维同一张表，用 role 区分）
-- ----------------------------
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

-- ----------------------------
-- 2.1 系统公告（脚手架公告页仍使用 time / user 字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notice` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` VARCHAR(1000) NULL DEFAULT NULL COMMENT '内容',
  `time` VARCHAR(50) NULL DEFAULT NULL COMMENT '展示用创建时间',
  `user` VARCHAR(50) NOT NULL COMMENT '发布人用户名',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_title`(`title`) USING BTREE,
  CONSTRAINT `fk_notice_user` FOREIGN KEY (`user`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统公告' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 3. 项目
-- ----------------------------
CREATE TABLE IF NOT EXISTS `project` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` VARCHAR(100) NOT NULL COMMENT '项目名',
  `code` VARCHAR(50) NOT NULL COMMENT '项目编号',
  `tech_stack` VARCHAR(200) NULL DEFAULT NULL COMMENT '技术栈',
  `leader` VARCHAR(50) NULL DEFAULT NULL COMMENT '开发负责人用户名，由技术负责人从在职开发人员中分配',
  `status` VARCHAR(20) NULL DEFAULT 'NOT_STARTED' COMMENT '状态：NOT_STARTED-未开始 IN_PROGRESS-进行中 PAUSED-已暂停 FINISHED-已完结',
  `start_date` DATE NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` DATE NULL DEFAULT NULL COMMENT '结束日期',
  `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '项目描述',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_code`(`code`) USING BTREE,
  INDEX `idx_project_name`(`name`) USING BTREE,
  CONSTRAINT `fk_project_leader` FOREIGN KEY (`leader`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4. 运行环境（开发 / 测试 / 生产）。一台服务器只属于一个环境。
-- ----------------------------
CREATE TABLE IF NOT EXISTS `environment` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `code` VARCHAR(20) NOT NULL COMMENT '环境编码：DEV/TEST/PROD',
  `name` VARCHAR(50) NOT NULL COMMENT '环境名称',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT '说明',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_environment_code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '运行环境' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4.1 服务器
-- ----------------------------
CREATE TABLE IF NOT EXISTS `server` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `hostname` VARCHAR(50) NOT NULL COMMENT '主机名',
  `ip` VARCHAR(50) NOT NULL COMMENT 'IP地址',
  `os` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作系统',
  `cpu` VARCHAR(20) NULL DEFAULT NULL COMMENT 'CPU核数',
  `memory` VARCHAR(20) NULL DEFAULT NULL COMMENT '内存',
  `disk` VARCHAR(20) NULL DEFAULT NULL COMMENT '磁盘',
  `env` VARCHAR(20) NULL DEFAULT NULL COMMENT '环境：DEV-开发 TEST-测试 PROD-生产',
  `status` VARCHAR(20) NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING-运行中 STOPPED-已停机 MAINTENANCE-维护中',
  `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
  `ssh_port` INT NOT NULL DEFAULT 22 COMMENT 'SSH 端口',
  `ssh_user` VARCHAR(64) NULL DEFAULT NULL COMMENT 'SSH 登录用户名',
  `ssh_auth_type` VARCHAR(16) NOT NULL DEFAULT 'PASSWORD' COMMENT 'PASSWORD=尚未生成登录私钥；KEY=已按登记密码生成私钥',
  `ssh_password` VARCHAR(512) NULL DEFAULT NULL COMMENT 'SSH 密码（应用层加密存储，非明文）',
  `ssh_private_key` TEXT NULL COMMENT 'SSH 私钥内容（应用层加密存储）',
  `deploy_path` VARCHAR(256) NULL DEFAULT NULL COMMENT '应用部署根目录，如 /opt/apps',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_server_hostname`(`hostname`) USING BTREE,
  UNIQUE INDEX `uk_server_ip`(`ip`) USING BTREE,
  INDEX `idx_server_env`(`env`) USING BTREE,
  CONSTRAINT `fk_server_env` FOREIGN KEY (`env`) REFERENCES `environment` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务器' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 5. 应用
-- ----------------------------
CREATE TABLE IF NOT EXISTS `app` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` VARCHAR(100) NOT NULL COMMENT '应用名',
  `code` VARCHAR(50) NOT NULL COMMENT '应用编号',
  `project_id` INT NOT NULL COMMENT '所属项目id',
  `tech_stack` VARCHAR(200) NULL DEFAULT NULL COMMENT '技术栈',
  `repo_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '代码仓库地址',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_app_code`(`code`) USING BTREE,
  INDEX `idx_app_name`(`name`) USING BTREE,
  INDEX `idx_app_project_id`(`project_id`) USING BTREE,
  CONSTRAINT `fk_app_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 5.1 应用实例：一个应用在一个环境一条，环境内可有多台服务器，实例指定其中一台
-- ----------------------------
CREATE TABLE IF NOT EXISTS `app_instance` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` INT NOT NULL COMMENT '应用id',
  `env_code` VARCHAR(20) NOT NULL COMMENT '环境编码',
  `server_id` INT NOT NULL COMMENT '该环境上的目标服务器',
  `port` INT NULL DEFAULT NULL COMMENT '端口',
  `version` VARCHAR(50) NULL DEFAULT NULL COMMENT '该环境当前版本',
  `status` VARCHAR(20) NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE/DEPLOYING/OFFLINE',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_instance_app_env`(`app_id`, `env_code`) USING BTREE,
  INDEX `idx_instance_server`(`server_id`) USING BTREE,
  CONSTRAINT `fk_instance_app` FOREIGN KEY (`app_id`) REFERENCES `app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_instance_env` FOREIGN KEY (`env_code`) REFERENCES `environment` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_instance_server` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用实例' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 6. 部署单
-- ----------------------------
CREATE TABLE IF NOT EXISTS `deploy` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `deploy_no` VARCHAR(50) NOT NULL COMMENT '部署单号',
  `app_id` INT NOT NULL COMMENT '应用id',
  `server_id` INT NOT NULL COMMENT '目标服务器id',
  `env_code` VARCHAR(20) NULL DEFAULT NULL COMMENT '目标环境，提交时按服务器写入',
  `version` VARCHAR(50) NULL DEFAULT NULL COMMENT '目标版本',
  `deploy_type` VARCHAR(20) NULL DEFAULT NULL COMMENT '类型：FULL-全量 INCREMENT-增量 ROLLBACK-回滚',
  `execute_mode` VARCHAR(16) NOT NULL DEFAULT 'AUTO' COMMENT '执行方式：AUTO-自动SSH MANUAL-人工确认',
  `status` VARCHAR(20) NULL DEFAULT 'PENDING' COMMENT '状态：REVIEW-待技术审查 TESTING-待测试验收 ACCEPTING-待产品验收 PENDING-待运维审批 APPROVED-已批准 REJECTED-已驳回 DEPLOYING-部署中 SUCCESS-成功 FAILED-失败',
  `applicant` VARCHAR(50) NOT NULL COMMENT '申请人用户名',
  `approver` VARCHAR(50) NULL DEFAULT NULL COMMENT '审批人用户名，未审批为空',
  `apply_time` DATETIME NULL DEFAULT NULL COMMENT '申请时间',
  `approve_time` DATETIME NULL DEFAULT NULL COMMENT '审批时间',
  `finish_time` DATETIME NULL DEFAULT NULL COMMENT '完成时间',
  `reason` VARCHAR(255) NULL DEFAULT NULL COMMENT '申请原因',
  `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
  `rollback_version` VARCHAR(50) NULL DEFAULT NULL COMMENT '回滚目标版本',
  `row_version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_deploy_no`(`deploy_no`) USING BTREE,
  INDEX `idx_deploy_app_id`(`app_id`) USING BTREE,
  INDEX `idx_deploy_server_id`(`server_id`) USING BTREE,
  INDEX `idx_deploy_status`(`status`) USING BTREE,
  INDEX `idx_deploy_app_env`(`app_id`, `env_code`, `status`) USING BTREE,
  CONSTRAINT `fk_deploy_app` FOREIGN KEY (`app_id`) REFERENCES `app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_deploy_env` FOREIGN KEY (`env_code`) REFERENCES `environment` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_deploy_server` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_deploy_applicant` FOREIGN KEY (`applicant`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_deploy_approver` FOREIGN KEY (`approver`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部署单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 7. 部署流转记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `deploy_log` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `deploy_id` INT NOT NULL COMMENT '部署单id',
  `action` VARCHAR(50) NULL DEFAULT NULL COMMENT '动作：APPLY-提交申请 APPROVE-审批通过 REJECT-审批驳回 START_DEPLOY-开始部署 FINISH-部署成功 FAIL-部署失败 ROLLBACK-回滚 EXEC-执行输出 HEALTH_CHECK_FAIL-探活失败',
  `operator` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作人用户名，系统自动动作为空',
  `detail` VARCHAR(1024) NULL DEFAULT NULL COMMENT '操作说明',
  `log_type` VARCHAR(16) NOT NULL DEFAULT 'STATE' COMMENT 'STATE-状态流转 EXEC-执行输出',
  `seq` INT NOT NULL DEFAULT 0 COMMENT 'EXEC 行序从 1 递增，STATE 固定 0',
  `source` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL-人工 AUTO-系统自动',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_deploy_log_deploy_id`(`deploy_id`) USING BTREE,
  INDEX `idx_log_deploy_type_seq`(`deploy_id`, `log_type`, `seq`) USING BTREE,
  CONSTRAINT `fk_deploy_log_deploy` FOREIGN KEY (`deploy_id`) REFERENCES `deploy` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_deploy_log_operator` FOREIGN KEY (`operator`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部署流转记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 8. 告警记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `alarm` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `alarm_no` VARCHAR(50) NOT NULL COMMENT '告警编号',
  `app_id` INT NULL DEFAULT NULL COMMENT '关联应用id',
  `server_id` INT NULL DEFAULT NULL COMMENT '关联服务器id',
  `deploy_no` VARCHAR(50) NULL DEFAULT NULL COMMENT '关联发布单号，可空，有值时必须是已有发布单',
  `level` VARCHAR(20) NULL DEFAULT NULL COMMENT '级别：INFO-提示 WARNING-警告 CRITICAL-严重',
  `content` VARCHAR(500) NULL DEFAULT NULL COMMENT '告警内容',
  `status` VARCHAR(20) NULL DEFAULT 'UNHANDLED' COMMENT '状态：UNHANDLED-未处理 HANDLING-处理中 RESOLVED-已解决',
  `handler` VARCHAR(50) NULL DEFAULT NULL COMMENT '处理人用户名，未接手为空',
  `handle_time` DATETIME NULL DEFAULT NULL COMMENT '接手时间',
  `resolve_time` DATETIME NULL DEFAULT NULL COMMENT '解决时间',
  `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_alarm_no`(`alarm_no`) USING BTREE,
  INDEX `idx_alarm_app_id`(`app_id`) USING BTREE,
  INDEX `idx_alarm_server_id`(`server_id`) USING BTREE,
  INDEX `idx_alarm_level`(`level`) USING BTREE,
  CONSTRAINT `fk_alarm_app` FOREIGN KEY (`app_id`) REFERENCES `app` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_alarm_server` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_alarm_deploy` FOREIGN KEY (`deploy_no`) REFERENCES `deploy` (`deploy_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_alarm_handler` FOREIGN KEY (`handler`) REFERENCES `person` (`username`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '告警记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 9. 部署执行记录（一次 startDeploy 对应一条，重试更新同一条）
-- ----------------------------
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
  INDEX `idx_execution_deploy`(`deploy_id`) USING BTREE,
  CONSTRAINT `fk_execution_deploy` FOREIGN KEY (`deploy_id`) REFERENCES `deploy` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部署执行记录' ROW_FORMAT = DYNAMIC;
