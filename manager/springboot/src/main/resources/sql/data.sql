-- ============================================================
-- DevOps 运维管理系统 - 测试数据脚本
-- 目标库：manager（请先执行 schema.sql）
-- 说明：
--   1. 显式指定主键 id，保证 app / deploy / deploy_log / alarm 的关联关系稳定
--   2. 密码存储为 MD5(明文 + 盐值)，盐值须与 Constants.PASSWORD_SALT 一致
--   3. create_time / update_time 交由数据库默认值填充
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 0. 超级管理员（登录：admin / admin）
-- ----------------------------
INSERT INTO `admin` (`id`, `username`, `password`, `name`, `phone`, `email`, `role`, `status`) VALUES
(1, 'admin', MD5(CONCAT('admin', 'devops_salt_2026')), '超级管理员', '13677889922', 'admin@xm.com', 'ADMIN', 'ACTIVE');

-- ----------------------------
-- 0.1 系统公告（3条）
-- ----------------------------
INSERT INTO `notice` (`id`, `title`, `content`, `time`, `user`) VALUES
(1, '系统进入答辩演示环境', 'DevOps 运维管理系统已就绪，请按 项目编号 → 主机名 → 应用编号 → 部署单号 → 告警编号 追溯主线。', '2026-09-13', 'admin'),
(2, '三角色分工', '超级管理员拥有全部权限；开发人员发起部署申请；运维人员审批发布并闭环告警。', '2026-09-13', 'admin'),
(3, '账号说明', '演示账号：admin/admin、dev1/123456、ops1/123456，密码均已加盐存储。', '2026-09-13', 'admin');

-- ----------------------------
-- 1. 开发人员（2条）
-- ----------------------------
INSERT INTO `dev` (`id`, `username`, `password`, `name`, `phone`, `email`, `status`) VALUES
(1, 'dev1', MD5(CONCAT('123456', 'devops_salt_2026')), '张伟', '13800010001', 'dev1@devops.com', 'ACTIVE'),
(2, 'dev2', MD5(CONCAT('123456', 'devops_salt_2026')), '李娜', '13800010002', 'dev2@devops.com', 'ACTIVE');

-- ----------------------------
-- 2. 运维人员（2条）
-- ----------------------------
INSERT INTO `ops` (`id`, `username`, `password`, `name`, `phone`, `email`, `status`) VALUES
(1, 'ops1', MD5(CONCAT('123456', 'devops_salt_2026')), '王强', '13900020001', 'ops1@devops.com', 'ACTIVE'),
(2, 'ops2', MD5(CONCAT('123456', 'devops_salt_2026')), '赵敏', '13900020002', 'ops2@devops.com', 'ACTIVE');

-- ----------------------------
-- 3. 项目（5条，覆盖 NOT_STARTED / IN_PROGRESS / PAUSED / FINISHED 四种状态）
-- ----------------------------
INSERT INTO `project` (`id`, `name`, `code`, `tech_stack`, `leader`, `status`, `start_date`, `end_date`, `description`) VALUES
(1, '电商交易平台', 'PRJ-001', 'Spring Boot + Vue2 + MySQL + Redis', '张伟', 'IN_PROGRESS', '2025-03-01', '2025-12-31', '面向 C 端的电商交易平台，包含商品、订单、支付、库存等核心链路'),
(2, '统一用户中心', 'PRJ-002', 'Spring Cloud + MySQL + Redis + JWT', '李娜', 'IN_PROGRESS', '2025-01-15', '2025-09-30', '为集团各业务线提供统一的账号注册、登录鉴权与权限管理能力'),
(3, '数据可视化大屏', 'PRJ-003', 'Vue2 + ECharts + Nginx', '王强', 'FINISHED', '2024-06-01', '2024-12-20', '经营指标实时可视化大屏，已完成验收并交付运营部门使用'),
(4, '移动端APP后台', 'PRJ-004', 'Spring Boot + MongoDB + RabbitMQ', '赵敏', 'PAUSED', '2025-02-10', '2025-11-30', '移动端 APP 服务端项目，因产品需求调整暂停开发'),
(5, '智能运维监控平台', 'PRJ-005', 'Spring Boot + 自研探活', '陈磊', 'NOT_STARTED', '2026-01-05', '2026-08-31', '统一采集主机与应用指标，提供告警与自愈能力，尚未启动开发');

-- ----------------------------
-- 4. 服务器（8条，DEV / TEST / PROD 三种环境均有，状态各异）
-- ----------------------------
INSERT INTO `server` (`id`, `hostname`, `ip`, `os`, `cpu`, `memory`, `disk`, `env`, `status`, `remark`) VALUES
(1, 'app-dev-01', '192.168.10.11', 'CentOS 7.9', '4', '8GB', '100GB', 'DEV', 'RUNNING', '开发环境应用服务器，日常联调使用'),
(2, 'app-dev-02', '192.168.10.12', 'Ubuntu 20.04', '4', '8GB', '100GB', 'DEV', 'STOPPED', '开发环境备用机，暂未启用'),
(3, 'app-test-01', '192.168.20.11', 'CentOS 7.9', '8', '16GB', '200GB', 'TEST', 'RUNNING', '测试环境应用服务器，承载回归测试'),
(4, 'app-test-02', '192.168.20.12', 'Ubuntu 22.04', '8', '16GB', '200GB', 'TEST', 'MAINTENANCE', '测试环境服务器，正在升级内核'),
(5, 'app-prod-01', '10.0.1.11', 'CentOS 7.9', '16', '32GB', '500GB', 'PROD', 'RUNNING', '生产环境应用主节点'),
(6, 'app-prod-02', '10.0.1.12', 'CentOS 7.9', '16', '32GB', '500GB', 'PROD', 'RUNNING', '生产环境应用从节点，与主节点负载均衡'),
(7, 'db-prod-01', '10.0.1.21', 'CentOS 7.9', '32', '64GB', '1TB', 'PROD', 'RUNNING', '生产环境数据库主库'),
(8, 'db-prod-02', '10.0.1.22', 'CentOS 7.9', '32', '64GB', '1TB', 'PROD', 'MAINTENANCE', '生产环境数据库备库，正在做数据校验');

-- ----------------------------
-- 5. 应用（8条，关联已有项目与服务器）
-- ----------------------------
INSERT INTO `app` (`id`, `name`, `code`, `project_id`, `server_id`, `port`, `version`, `tech_stack`, `repo_url`, `status`) VALUES
(1, '交易服务', 'APP-001', 1, 5, 8081, 'v2.3.1', 'Spring Boot + MyBatis + Redis', 'http://git.devops.com/shop/trade-service.git', 'ONLINE'),
(2, '订单服务', 'APP-002', 1, 6, 8082, 'v2.1.0', 'Spring Boot + MyBatis + RabbitMQ', 'http://git.devops.com/shop/order-service.git', 'ONLINE'),
(3, '前端门户', 'APP-003', 1, 3, 8080, 'v1.9.4', 'Vue2 + Element UI + Nginx', 'http://git.devops.com/shop/shop-portal.git', 'ONLINE'),
(4, '统一认证服务', 'APP-004', 2, 5, 8083, 'v1.5.2', 'Spring Boot + JWT + Redis', 'http://git.devops.com/uc/auth-service.git', 'ONLINE'),
(5, '用户中心服务', 'APP-005', 2, 1, 8084, 'v1.4.0', 'Spring Boot + MyBatis + MySQL', 'http://git.devops.com/uc/user-service.git', 'OFFLINE'),
(6, '数据大屏服务', 'APP-006', 3, 3, 8085, 'v1.0.6', 'Vue2 + ECharts + Nginx', 'http://git.devops.com/bi/dashboard.git', 'ONLINE'),
(7, '移动端网关', 'APP-007', 4, 2, 8086, 'v0.9.1', 'Spring Cloud Gateway + MongoDB', 'http://git.devops.com/app/mobile-gateway.git', 'OFFLINE'),
(8, '监控采集服务', 'APP-008', 5, 4, 8087, 'v0.1.0', 'Spring Boot + 自研探活', 'http://git.devops.com/ops/monitor-agent.git', 'OFFLINE');

-- ----------------------------
-- 6. 部署单（10条，覆盖 PENDING / APPROVED / REJECTED / DEPLOYING / SUCCESS / FAILED 全部状态）
-- ----------------------------
INSERT INTO `deploy` (`id`, `deploy_no`, `app_id`, `server_id`, `version`, `deploy_type`, `status`, `applicant`, `approver`, `apply_time`, `approve_time`, `finish_time`, `reason`, `remark`) VALUES
(1, 'DP-20250901-001', 1, 5, 'v2.3.1', 'FULL', 'SUCCESS', 'dev1', 'ops1', '2025-09-01 09:10:00', '2025-09-01 09:30:00', '2025-09-01 10:05:00', '交易服务新增优惠券核销功能，需全量发布', '发布顺利，已完成线上验证'),
(2, 'DP-20250903-002', 2, 6, 'v2.1.0', 'INCREMENT', 'SUCCESS', 'dev1', 'ops1', '2025-09-03 10:00:00', '2025-09-03 10:20:00', '2025-09-03 10:50:00', '修复订单超时未关闭的问题', '增量发布，仅替换业务 jar'),
(3, 'DP-20250905-003', 3, 3, 'v1.9.4', 'FULL', 'SUCCESS', 'dev2', 'ops2', '2025-09-05 14:00:00', '2025-09-05 14:15:00', '2025-09-05 14:45:00', '前端门户改版首页并适配移动端', '静态资源已刷新 CDN 缓存'),
(4, 'DP-20250908-004', 4, 5, 'v1.5.2', 'INCREMENT', 'FAILED', 'dev2', 'ops1', '2025-09-08 09:00:00', '2025-09-08 09:20:00', '2025-09-08 09:55:00', '认证服务升级 JWT 签名算法', '启动时读取不到新密钥配置，发布失败'),
(5, 'DP-20250909-005', 1, 5, 'v2.3.0', 'ROLLBACK', 'SUCCESS', 'ops1', 'ops2', '2025-09-09 11:00:00', '2025-09-09 11:05:00', '2025-09-09 11:25:00', '线上出现批量下单失败，回滚至上一稳定版本', '回滚完成，业务指标恢复正常'),
(6, 'DP-20250910-006', 5, 1, 'v1.4.0', 'FULL', 'REJECTED', 'dev1', 'ops2', '2025-09-10 15:00:00', '2025-09-10 15:30:00', NULL, '用户中心服务提测部署', '缺少数据库变更脚本与回滚方案，驳回重提'),
(7, 'DP-20250911-007', 6, 3, 'v1.0.6', 'INCREMENT', 'DEPLOYING', 'dev2', 'ops1', '2025-09-11 09:30:00', '2025-09-11 09:45:00', NULL, '大屏新增经营指标看板', '正在灰度发布中'),
(8, 'DP-20250912-008', 7, 2, 'v0.9.1', 'FULL', 'APPROVED', 'dev1', 'ops1', '2025-09-12 10:00:00', '2025-09-12 10:20:00', NULL, '移动端网关首次部署至开发环境', '已批准，等待发布窗口'),
(9, 'DP-20250912-009', 8, 4, 'v0.1.0', 'FULL', 'PENDING', 'dev2', NULL, '2025-09-12 16:00:00', NULL, NULL, '监控采集服务初版部署验证', '等待运维审批'),
(10, 'DP-20250913-010', 2, 6, 'v2.1.1', 'INCREMENT', 'PENDING', 'dev1', NULL, '2025-09-13 09:00:00', NULL, NULL, '订单服务修复金额计算精度问题', '等待运维审批');

-- ----------------------------
-- 7. 部署流转记录（20条，与部署单一一对应，覆盖 7 种动作）
-- ----------------------------
INSERT INTO `deploy_log` (`id`, `deploy_id`, `action`, `operator`, `detail`) VALUES
(1, 1, 'APPLY', 'dev1', '提交交易服务 v2.3.1 全量部署申请'),
(2, 1, 'APPROVE', 'ops1', '变更方案与回滚预案齐全，审批通过'),
(3, 1, 'FINISH', 'ops1', '部署成功，健康检查通过，接口可正常访问'),
(4, 2, 'APPLY', 'dev1', '提交订单服务 v2.1.0 增量部署申请'),
(5, 2, 'FINISH', 'ops1', '增量包替换完成，订单超时任务恢复正常'),
(6, 3, 'APPLY', 'dev2', '提交前端门户 v1.9.4 全量部署申请'),
(7, 3, 'FINISH', 'ops2', '静态资源发布完成，CDN 缓存已刷新'),
(8, 4, 'APPLY', 'dev2', '提交统一认证服务 v1.5.2 增量部署申请'),
(9, 4, 'APPROVE', 'ops1', '影响面评估为低风险，审批通过'),
(10, 4, 'FAIL', 'ops1', '服务启动失败：未找到 jwt.secret 配置项，已保留现场日志'),
(11, 5, 'APPLY', 'ops1', '线上批量下单失败，紧急提交回滚至 v2.3.0'),
(12, 5, 'ROLLBACK', 'ops2', '回滚执行完成，下单成功率恢复至 99.9%'),
(13, 6, 'APPLY', 'dev1', '提交用户中心服务 v1.4.0 全量部署申请'),
(14, 6, 'REJECT', 'ops2', '驳回：缺少 DDL 变更脚本与回滚方案，请补充后重提'),
(15, 7, 'APPLY', 'dev2', '提交数据大屏服务 v1.0.6 增量部署申请'),
(16, 7, 'START_DEPLOY', 'ops1', '开始灰度发布，先更新 1 个节点观察 30 分钟'),
(17, 8, 'APPLY', 'dev1', '提交移动端网关 v0.9.1 全量部署申请'),
(18, 8, 'APPROVE', 'ops1', '开发环境部署风险可控，审批通过，等待发布窗口'),
(19, 9, 'APPLY', 'dev2', '提交监控采集服务 v0.1.0 全量部署申请'),
(20, 10, 'APPLY', 'dev1', '提交订单服务 v2.1.1 增量部署申请');

-- ----------------------------
-- 8. 告警记录（12条，INFO / WARNING / CRITICAL 三级与三种状态均有，resolve_time 与 handle_time 存在时间差）
-- ----------------------------
INSERT INTO `alarm` (`id`, `alarm_no`, `app_id`, `server_id`, `level`, `content`, `status`, `handler`, `handle_time`, `resolve_time`) VALUES
(1, 'AL-20250901-001', 1, 5, 'CRITICAL', '交易服务 CPU 使用率持续 5 分钟超过 95%，请求排队严重', 'RESOLVED', 'ops1', '2025-09-01 08:15:00', '2025-09-01 08:52:00'),
(2, 'AL-20250902-002', 2, 6, 'WARNING', '订单服务 JVM 老年代内存使用率达到 82%，存在 FullGC 风险', 'RESOLVED', 'ops1', '2025-09-02 10:20:00', '2025-09-02 11:05:00'),
(3, 'AL-20250903-003', 3, 3, 'INFO', '前端门户 Nginx 配置已重新加载，新增 gzip 压缩规则生效', 'RESOLVED', 'ops2', '2025-09-03 14:30:00', '2025-09-03 14:35:00'),
(4, 'AL-20250905-004', 4, 5, 'CRITICAL', '统一认证服务健康检查连续 3 次失败，接口 5xx 比例达到 37%', 'RESOLVED', 'ops2', '2025-09-05 09:05:00', '2025-09-05 10:12:00'),
(5, 'AL-20250908-005', 4, 5, 'WARNING', '统一认证服务接口平均响应时间上升至 1200ms，超过 800ms 阈值', 'HANDLING', 'ops1', '2025-09-08 09:40:00', NULL),
(6, 'AL-20250909-006', 1, 5, 'CRITICAL', '交易服务数据库连接池耗尽，等待获取连接的线程数达到 48', 'HANDLING', 'ops1', '2025-09-09 11:10:00', NULL),
(7, 'AL-20250910-007', 5, 1, 'INFO', '用户中心服务已按计划下线，进入停机维护窗口', 'RESOLVED', 'ops2', '2025-09-10 18:00:00', '2025-09-10 18:03:00'),
(8, 'AL-20250911-008', 6, 3, 'WARNING', '数据大屏服务所在磁盘使用率达到 85%，请及时清理历史日志', 'HANDLING', 'ops2', '2025-09-11 10:15:00', NULL),
(9, 'AL-20250912-009', 7, 2, 'INFO', '移动端网关已完成灰度流量切换，当前灰度比例 10%', 'UNHANDLED', NULL, NULL, NULL),
(10, 'AL-20250912-010', 8, 4, 'WARNING', '监控采集服务采集任务积压 1200 条，消费速度低于生产速度', 'UNHANDLED', NULL, NULL, NULL),
(11, 'AL-20250913-011', 2, 6, 'CRITICAL', '订单服务节点心跳丢失，实例已被注册中心自动摘除', 'UNHANDLED', NULL, NULL, NULL),
(12, 'AL-20250913-012', 6, 3, 'INFO', '数据大屏服务定时任务执行完成，本次耗时 42s', 'UNHANDLED', NULL, NULL, NULL);
