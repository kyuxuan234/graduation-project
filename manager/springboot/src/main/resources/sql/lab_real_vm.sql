-- ============================================================
-- 真实虚机实验室种子：清空业务数据后写入 hadoop101/102/103
-- 账号 kang / centos7，部署目录 /opt/module
-- 保留人员账号；不灌历史发布单与告警，便于真实发布
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `deploy_execution`;
TRUNCATE TABLE `deploy_log`;
TRUNCATE TABLE `alarm`;
TRUNCATE TABLE `deploy`;
TRUNCATE TABLE `app_instance`;
TRUNCATE TABLE `app`;
TRUNCATE TABLE `server`;
TRUNCATE TABLE `project`;
TRUNCATE TABLE `notice`;

SET FOREIGN_KEY_CHECKS = 1;

INSERT IGNORE INTO `environment` (`id`, `code`, `name`, `sort_no`, `remark`) VALUES
(1, 'DEV', '开发环境', 1, 'hadoop101'),
(2, 'TEST', '测试环境', 2, 'hadoop102'),
(3, 'PROD', '生产环境', 3, 'hadoop103');

INSERT INTO `notice` (`id`, `title`, `content`, `time`, `user`) VALUES
(1, '真实虚机已接入', '三台 CentOS 虚机：hadoop101=开发、hadoop102=测试、hadoop103=生产。SSH 用户 kang，部署目录 /opt/module。', DATE_FORMAT(CURDATE(), '%Y-%m-%d'), 'admin'),
(2, '发布注意', '自动发布会在目标机 /opt/module/releases/<版本> 拉取仓库代码并切换 current。请先在应用上填好可访问的仓库地址与版本标签。', DATE_FORMAT(CURDATE(), '%Y-%m-%d'), 'admin'),
(3, '登录账号', 'admin/admin；dev1、tl1、qa1、pm1、ops1 密码均为 123456。', DATE_FORMAT(CURDATE(), '%Y-%m-%d'), 'admin');

INSERT INTO `project` (`id`, `name`, `code`, `tech_stack`, `leader`, `status`, `start_date`, `end_date`, `description`) VALUES
(1, '电商交易平台', 'PRJ-001', 'Spring Boot + Vue2 + MySQL', 'dev1', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 180 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), '挂载到真实虚机，用于联调发布'),
(2, '统一用户中心', 'PRJ-002', 'Spring Boot + JWT', 'dev2', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 200 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), '认证与用户服务'),
(3, '经营数据大屏', 'PRJ-003', 'Vue2 + ECharts', 'dev1', 'FINISHED', DATE_SUB(CURDATE(), INTERVAL 400 DAY), DATE_SUB(CURDATE(), INTERVAL 200 DAY), '已交付示例项目');

-- 三台真实虚机：PASSWORD 明文口令（开发密钥下可读；页面保存时会改写成登录私钥）
INSERT INTO `server` (`id`, `hostname`, `ip`, `os`, `cpu`, `memory`, `disk`, `env`, `status`, `remark`,
                      `ssh_port`, `ssh_user`, `ssh_auth_type`, `ssh_password`, `ssh_private_key`, `deploy_path`) VALUES
(1, 'hadoop101', '192.168.116.131', 'CentOS 7', '4', '8GB', '100GB', 'DEV',  'RUNNING', '开发环境虚机', 22, 'kang', 'PASSWORD', 'centos7', NULL, '/opt/module'),
(2, 'hadoop102', '192.168.116.132', 'CentOS 7', '4', '8GB', '100GB', 'TEST', 'RUNNING', '测试环境虚机', 22, 'kang', 'PASSWORD', 'centos7', NULL, '/opt/module'),
(3, 'hadoop103', '192.168.116.133', 'CentOS 7', '4', '8GB', '100GB', 'PROD', 'RUNNING', '生产环境虚机', 22, 'kang', 'PASSWORD', 'centos7', NULL, '/opt/module');

INSERT INTO `app` (`id`, `name`, `code`, `project_id`, `tech_stack`, `repo_url`) VALUES
(1, '交易服务', 'APP-001', 1, 'Spring Boot + Redis', 'https://github.com/spring-projects/spring-petclinic.git'),
(2, '订单服务', 'APP-002', 1, 'Spring Boot + MQ', 'https://github.com/spring-projects/spring-petclinic.git'),
(3, '商城门户', 'APP-003', 1, 'Vue2 + Nginx', 'https://github.com/spring-projects/spring-petclinic.git'),
(4, '统一认证', 'APP-004', 2, 'Spring Boot + JWT', 'https://github.com/spring-projects/spring-petclinic.git'),
(5, '用户中心', 'APP-005', 2, 'Spring Boot + MySQL', 'https://github.com/spring-projects/spring-petclinic.git'),
(6, '经营大屏', 'APP-006', 3, 'Vue2 + ECharts', 'https://github.com/spring-projects/spring-petclinic.git');

-- 每个应用挂三环境，全部指向对应虚机；初始离线，等真实发布成功后再回写版本
INSERT INTO `app_instance` (`id`, `app_id`, `env_code`, `server_id`, `port`, `version`, `status`) VALUES
(1,  1, 'DEV',  1, 8081, NULL, 'OFFLINE'),
(2,  1, 'TEST', 2, 8081, NULL, 'OFFLINE'),
(3,  1, 'PROD', 3, 8081, NULL, 'OFFLINE'),
(4,  2, 'DEV',  1, 8082, NULL, 'OFFLINE'),
(5,  2, 'TEST', 2, 8082, NULL, 'OFFLINE'),
(6,  2, 'PROD', 3, 8082, NULL, 'OFFLINE'),
(7,  3, 'DEV',  1, 8080, NULL, 'OFFLINE'),
(8,  3, 'TEST', 2, 8080, NULL, 'OFFLINE'),
(9,  3, 'PROD', 3, 8080, NULL, 'OFFLINE'),
(10, 4, 'DEV',  1, 8083, NULL, 'OFFLINE'),
(11, 4, 'TEST', 2, 8083, NULL, 'OFFLINE'),
(12, 4, 'PROD', 3, 8083, NULL, 'OFFLINE'),
(13, 5, 'DEV',  1, 8084, NULL, 'OFFLINE'),
(14, 5, 'TEST', 2, 8084, NULL, 'OFFLINE'),
(15, 5, 'PROD', 3, 8084, NULL, 'OFFLINE'),
(16, 6, 'DEV',  1, 8085, NULL, 'OFFLINE'),
(17, 6, 'TEST', 2, 8085, NULL, 'OFFLINE'),
(18, 6, 'PROD', 3, 8085, NULL, 'OFFLINE');

ALTER TABLE `project` AUTO_INCREMENT = 100;
ALTER TABLE `server` AUTO_INCREMENT = 100;
ALTER TABLE `app` AUTO_INCREMENT = 100;
ALTER TABLE `app_instance` AUTO_INCREMENT = 100;
ALTER TABLE `deploy` AUTO_INCREMENT = 200;
ALTER TABLE `deploy_log` AUTO_INCREMENT = 2000;
ALTER TABLE `deploy_execution` AUTO_INCREMENT = 200;
ALTER TABLE `alarm` AUTO_INCREMENT = 200;
ALTER TABLE `notice` AUTO_INCREMENT = 100;
