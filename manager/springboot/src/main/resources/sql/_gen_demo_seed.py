# -*- coding: utf-8 -*-
"""Generate demo_seed.sql + data.sql with dense analysis-friendly history."""
from pathlib import Path

SQL_DIR = Path(__file__).resolve().parent

# Historical finished deploys: (day_ago, hour, app_id, server_id, version, dtype, mode, status, applicant, approver, reason, remark, rollback, fail_keyword)
# fail_keyword used only for FAILED → EXEC log text containing keyword
HISTORY = [
    # last ~28 days: mix of success / fail, denser near recent week
    (28, 10, 1, 5, "v2.1.0", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "交易服务周版本", "探活通过", None, None),
    (27, 14, 2, 6, "v2.0.5", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "订单超时修复", "成功", None, None),
    (26, 9, 3, 3, "v1.8.0", "FULL", "MANUAL", "SUCCESS", "dev2", "ops2", "门户静态资源", "手动完成", None, None),
    (25, 11, 4, 5, "v1.4.8", "INCREMENT", "AUTO", "FAILED", "dev2", "ops1", "认证热更", "密钥缺失", None, "error"),
    (25, 16, 1, 5, "v2.1.1", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "优惠券补丁", "成功", None, None),
    (24, 10, 6, 3, "v1.0.3", "INCREMENT", "AUTO", "SUCCESS", "dev2", "ops1", "大屏指标", "成功", None, None),
    (23, 15, 5, 1, "v1.3.5", "FULL", "AUTO", "FAILED", "dev1", "ops2", "用户中心提测", "连接被拒", None, "refused"),
    (22, 9, 2, 6, "v2.0.6", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "金额精度", "成功", None, None),
    (22, 18, 4, 5, "v1.4.9", "FULL", "AUTO", "FAILED", "dev2", "ops1", "认证全量", "启动异常", None, "exception"),
    (21, 11, 1, 6, "v2.1.2", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops2", "从节点同步", "成功", None, None),
    (20, 10, 7, 1, "v0.8.5", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "网关开发环境", "成功", None, None),
    (19, 14, 3, 3, "v1.8.2", "INCREMENT", "MANUAL", "SUCCESS", "dev2", "ops2", "文案热修", "成功", None, None),
    (18, 9, 5, 1, "v1.3.6", "INCREMENT", "AUTO", "FAILED", "dev1", "ops1", "用户中心配置", "执行超时", None, "timeout"),
    (18, 15, 2, 6, "v2.0.7", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "订单全量", "成功", None, None),
    (17, 11, 1, 5, "v2.2.0", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "交易大版本", "成功", None, None),
    (16, 10, 4, 5, "v1.5.0", "INCREMENT", "AUTO", "FAILED", "dev2", "ops2", "JWT 算法切换", "鉴权失败", None, "failed"),
    (16, 16, 6, 3, "v1.0.4", "INCREMENT", "AUTO", "SUCCESS", "dev2", "ops1", "大屏缓存", "成功", None, None),
    (15, 9, 8, 4, "v0.0.9", "FULL", "AUTO", "FAILED", "dev2", "ops1", "探活初装", "权限拒绝", None, "denied"),
    (14, 13, 1, 5, "v2.2.1", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "交易补丁", "成功", None, None),
    (13, 10, 2, 6, "v2.0.8", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops2", "死信队列", "成功", None, None),
    (13, 17, 5, 1, "v1.3.7", "FULL", "AUTO", "FAILED", "dev1", "ops1", "用户中心全量", "探活失败", None, "error"),
    (12, 11, 3, 3, "v1.9.0", "FULL", "MANUAL", "SUCCESS", "dev2", "ops2", "门户改版", "成功", None, None),
    (11, 9, 4, 5, "v1.5.1", "INCREMENT", "AUTO", "SUCCESS", "dev2", "ops1", "认证修复后重发", "成功", None, None),
    (10, 14, 1, 5, "v2.2.2", "INCREMENT", "AUTO", "FAILED", "dev1", "ops1", "交易灰度", "上游超时", None, "timeout"),
    (10, 16, 1, 5, "v2.2.1", "ROLLBACK", "AUTO", "SUCCESS", "ops1", "ops2", "灰度失败回滚", "回滚完成", "v2.2.1", None),
    (9, 10, 2, 6, "v2.0.9", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "订单监控", "成功", None, None),
    (8, 15, 6, 3, "v1.0.5", "FULL", "AUTO", "SUCCESS", "dev2", "ops1", "大屏全量", "成功", None, None),
    (7, 9, 5, 1, "v1.3.8", "INCREMENT", "AUTO", "FAILED", "dev1", "ops2", "用户中心热更", "连接拒绝", None, "refused"),
    (7, 14, 7, 1, "v0.9.0", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "网关中间版", "成功", None, None),
    (6, 11, 3, 3, "v1.9.1", "INCREMENT", "MANUAL", "SUCCESS", "dev2", "ops2", "门户微调", "成功", None, None),
    (5, 10, 4, 5, "v1.5.1", "INCREMENT", "AUTO", "FAILED", "dev2", "ops1", "认证压测包", "异常堆栈", None, "exception"),
    (5, 16, 2, 6, "v2.1.0", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "订单主版本", "成功", None, None),
    (4, 9, 1, 5, "v2.3.0", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "交易预发", "成功", None, None),
    (4, 15, 8, 4, "v0.0.9", "FULL", "AUTO", "FAILED", "dev2", "ops2", "探活重试", "权限 denied", None, "denied"),
    (3, 10, 6, 3, "v1.0.6", "INCREMENT", "AUTO", "SUCCESS", "dev2", "ops1", "经营指标", "成功", None, None),
    (3, 14, 5, 1, "v1.3.9", "FULL", "AUTO", "FAILED", "dev1", "ops1", "用户中心配置热更新", "TIMEOUT", None, "timeout"),
    (2, 9, 1, 5, "v2.3.1", "FULL", "AUTO", "SUCCESS", "dev1", "ops1", "交易优惠券核销", "自动部署成功", None, None),
    (2, 11, 2, 6, "v2.1.0", "INCREMENT", "AUTO", "SUCCESS", "dev1", "ops1", "订单超时关闭", "增量完成", None, None),
    (2, 14, 3, 3, "v1.9.4", "FULL", "MANUAL", "SUCCESS", "dev2", "ops2", "门户首页改版", "手动标记成功", None, None),
    (1, 9, 4, 5, "v1.5.2", "INCREMENT", "AUTO", "FAILED", "dev2", "ops1", "认证 JWT 升级", "缺少 jwt.secret", None, "error"),
    (1, 11, 1, 5, "v2.3.0", "ROLLBACK", "AUTO", "SUCCESS", "ops1", "ops2", "下单失败率升高回滚", "回滚完成", "v2.3.0", None),
]

# Live showcase (not finished / in-flight)
SHOWCASE = [
    # rejected
    (1, 15, 5, 1, "v1.4.0", "FULL", "AUTO", "REJECTED", "dev1", "ops2", "用户中心提测部署", "驳回：缺少 DDL 与回滚方案", None),
    # deploying auto
    (0, 9, 7, 1, "v0.9.1", "FULL", "AUTO", "DEPLOYING", "dev1", "ops1", "移动网关开发环境首发", "自动部署执行中", None),
    # approved waiting
    (0, 10, 6, 3, "v1.0.7", "INCREMENT", "MANUAL", "APPROVED", "dev2", "ops1", "大屏新增经营指标", "已批准，等待晚间窗口", None),
    # pending x2
    (0, 9, 8, 4, "v0.1.0", "FULL", "AUTO", "PENDING", "dev2", None, "探活采集初版验证", "等待运维审批", None),
    (0, 10, 2, 6, "v2.1.1", "INCREMENT", "AUTO", "PENDING", "dev1", None, "订单金额精度修复", "等待运维审批", None),
    # deploying manual
    (0, 16, 3, 3, "v1.9.5", "INCREMENT", "MANUAL", "DEPLOYING", "dev2", "ops2", "门户热修文案", "已进入手动部署", None),
]

ALARM_DAYS = [
    # (day_ago, hour, app, server, level, content, status, handler, deploy_no_or_None)
    (28, 11, 1, 5, "INFO", "交易服务周发后探活通过", "RESOLVED", "ops1", None),
    (25, 12, 4, 5, "CRITICAL", "认证服务启动失败，5xx 升高", "RESOLVED", "ops1", None),
    (23, 16, 5, 1, "WARNING", "用户中心端口连接被拒", "RESOLVED", "ops2", None),
    (22, 19, 4, 5, "CRITICAL", "认证服务异常退出", "RESOLVED", "ops1", None),
    (20, 11, 7, 1, "INFO", "网关首发完成", "RESOLVED", "ops1", None),
    (18, 10, 5, 1, "WARNING", "用户中心部署超时", "RESOLVED", "ops1", None),
    (16, 11, 4, 5, "CRITICAL", "JWT 切换后登录失败率升高", "RESOLVED", "ops2", None),
    (15, 10, 8, 4, "WARNING", "探活采集权限不足", "RESOLVED", "ops1", None),
    (13, 18, 5, 1, "CRITICAL", "用户中心探活连续失败", "RESOLVED", "ops1", None),
    (12, 12, 3, 3, "INFO", "门户 CDN 刷新完成", "RESOLVED", "ops2", None),
    (10, 15, 1, 5, "CRITICAL", "交易灰度失败率升高，已回滚", "RESOLVED", "ops2", None),
    (10, 17, 1, 5, "WARNING", "回滚后观察期内存偏高", "RESOLVED", "ops1", None),
    (9, 11, 2, 6, "INFO", "订单监控埋点生效", "RESOLVED", "ops1", None),
    (8, 16, 6, 3, "WARNING", "大屏磁盘使用率 82%", "RESOLVED", "ops2", None),
    (7, 10, 5, 1, "WARNING", "用户中心热更连接拒绝", "HANDLING", "ops2", None),
    (7, 15, 7, 1, "INFO", "网关中间版发布完成", "RESOLVED", "ops1", None),
    (5, 11, 4, 5, "CRITICAL", "认证压测包异常堆栈", "RESOLVED", "ops1", None),
    (5, 17, 2, 6, "INFO", "订单主版本上线", "RESOLVED", "ops1", None),
    (4, 10, 1, 5, "INFO", "交易预发探活通过", "RESOLVED", "ops1", None),
    (4, 16, 8, 4, "WARNING", "探活重试仍权限不足", "HANDLING", "ops2", None),
    (3, 11, 6, 3, "WARNING", "大屏节点磁盘 86%", "HANDLING", "ops2", None),
    (3, 15, 5, 1, "WARNING", "用户中心超时失败后仍离线", "UNHANDLED", None, None),
    (2, 10, 1, 5, "INFO", "交易服务发布后 QPS 正常", "RESOLVED", "ops1", None),
    (2, 12, 2, 6, "WARNING", "订单服务老年代内存 78%", "RESOLVED", "ops1", None),
    (1, 10, 4, 5, "CRITICAL", "认证服务启动失败", "RESOLVED", "ops1", None),
    (1, 12, 1, 5, "CRITICAL", "下单失败率升高已回滚", "RESOLVED", "ops2", None),
    (0, 9, 7, 1, "INFO", "移动网关自动部署进行中", "HANDLING", "ops1", None),
    (0, 10, 2, 6, "CRITICAL", "订单节点心跳丢失，注册中心已摘除", "UNHANDLED", None, None),
    (0, 10, 8, 4, "WARNING", "探活采集所在测试机处于维护中", "UNHANDLED", None, None),
    (0, 11, 6, 3, "INFO", "门户缓存巡检正常", "RESOLVED", "ops2", None),
]

KEYWORD_DETAIL = {
    "error": "[auto] ERROR: jwt.secret missing / health check error",
    "exception": "[auto] Uncaught exception: NullPointerException in AuthFilter",
    "failed": "[auto] deploy failed: health check returned non-200",
    "timeout": "[auto] gateway timeout while calling upstream / exceed max-timeout",
    "denied": "[auto] Permission denied writing /opt/apps/releases",
    "refused": "[auto] connection refused by app port 8084",
}


def ts(day_ago, hour, minute=0):
    return f"DATE_ADD(DATE_SUB(CURDATE(), INTERVAL {day_ago} DAY), INTERVAL {hour * 3600 + minute * 60} SECOND)"


def sql_str(s):
    if s is None:
        return "NULL"
    return "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"


def build_business_body():
    lines = []
    lines.append("-- ----------------------------")
    lines.append("-- 系统公告（展示产品价值与操作主线）")
    lines.append("-- ----------------------------")
    lines.append("INSERT INTO `notice` (`id`, `title`, `content`, `time`, `user`) VALUES")
    lines.append("(1, '发布闭环已就绪', '请按 项目→应用→部署单→告警 追溯。开发提交申请，运维审批后可选自动部署（SSH）或手动终端。', DATE_FORMAT(CURDATE(), '%Y-%m-%d'), 'admin'),")
    lines.append("(2, '今天演示看什么', '① 待审批单（运维 inbox）② 部署中进度与执行日志 ③ 失败单与回滚成功单 ④ 发布分析：成败趋势/失败分布/错误 TOP/告警走势。', DATE_FORMAT(CURDATE(), '%Y-%m-%d'), 'admin'),")
    lines.append("(3, '账号与注意', 'admin/admin、dev1/123456、ops1/123456。SSH 已开通的服务器显示「已开通」；自动部署需制品。', DATE_FORMAT(CURDATE(), '%Y-%m-%d'), 'admin');")
    lines.append("")
    lines.append("-- ----------------------------")
    lines.append("-- 项目：4 种状态全覆盖")
    lines.append("-- ----------------------------")
    lines.append("""INSERT INTO `project` (`id`, `name`, `code`, `tech_stack`, `leader`, `status`, `start_date`, `end_date`, `description`) VALUES
(1, '电商交易平台', 'PRJ-001', 'Spring Boot + Vue2 + MySQL + Redis', '张伟', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 180 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), '核心交易链路，演示全量/增量/回滚与生产发布'),
(2, '统一用户中心', 'PRJ-002', 'Spring Boot + JWT + Redis', '李娜', 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 200 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), '认证与用户服务，演示失败发布与配置类告警'),
(3, '经营数据大屏', 'PRJ-003', 'Vue2 + ECharts + Nginx', '王强', 'FINISHED', DATE_SUB(CURDATE(), INTERVAL 400 DAY), DATE_SUB(CURDATE(), INTERVAL 200 DAY), '已交付项目，演示稳定在线与历史成功单'),
(4, '移动端网关', 'PRJ-004', 'Spring Cloud Gateway', '赵敏', 'PAUSED', DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_ADD(CURDATE(), INTERVAL 120 DAY), '需求调整暂停，演示 APPROVED 等待窗口'),
(5, '智能探活平台', 'PRJ-005', 'Spring Boot + 探活', '张伟', 'NOT_STARTED', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY), '尚未启动，演示 PENDING 初版申请');""")
    lines.append("")
    lines.append("-- ----------------------------")
    lines.append("-- 服务器：DEV/TEST/PROD + RUNNING/STOPPED/MAINTENANCE + KEY/PASSWORD")
    lines.append("-- ----------------------------")
    lines.append("""INSERT INTO `server` (`id`, `hostname`, `ip`, `os`, `cpu`, `memory`, `disk`, `env`, `status`, `remark`,
                      `ssh_port`, `ssh_user`, `ssh_auth_type`, `ssh_password`, `ssh_private_key`, `deploy_path`) VALUES
(1, 'app-dev-01',  '192.168.10.11', 'CentOS 7.9',  '4',  '8GB',  '100GB', 'DEV',  'RUNNING',     '开发联调主节点', 22, 'deploy', 'KEY',      NULL, NULL, '/opt/apps'),
(2, 'app-dev-02',  '192.168.10.12', 'Ubuntu 22.04','4',  '8GB',  '100GB', 'DEV',  'STOPPED',     '开发备用机未启用', 22, 'deploy', 'PASSWORD', NULL, NULL, '/opt/apps'),
(3, 'app-test-01', '192.168.20.11', 'CentOS 7.9',  '8',  '16GB', '200GB', 'TEST', 'RUNNING',     '回归测试机', 22, 'deploy', 'KEY',      NULL, NULL, '/opt/apps'),
(4, 'app-test-02', '192.168.20.12', 'Ubuntu 22.04','8',  '16GB', '200GB', 'TEST', 'MAINTENANCE', '内核升级维护中', 22, 'deploy', 'PASSWORD', NULL, NULL, '/opt/apps'),
(5, 'app-prod-01', '10.0.1.11',     'CentOS 7.9',  '16', '32GB', '500GB', 'PROD', 'RUNNING',     '生产主节点', 22, 'deploy', 'KEY',      NULL, NULL, '/opt/apps'),
(6, 'app-prod-02', '10.0.1.12',     'CentOS 7.9',  '16', '32GB', '500GB', 'PROD', 'RUNNING',     '生产从节点', 22, 'deploy', 'KEY',      NULL, NULL, '/opt/apps'),
(7, 'ops-jump-01', '10.0.1.5',      'Ubuntu 22.04','2',  '4GB',  '50GB',  'PROD', 'RUNNING',     '跳板机（待开通密钥）', 22, 'ops', 'PASSWORD', NULL, NULL, '/opt/jump');""")
    lines.append("")
    lines.append("-- ----------------------------")
    lines.append("-- 应用：ONLINE / DEPLOYING / OFFLINE")
    lines.append("-- ----------------------------")
    lines.append("""INSERT INTO `app` (`id`, `name`, `code`, `project_id`, `server_id`, `port`, `version`, `tech_stack`, `repo_url`, `status`) VALUES
(1, '交易服务',     'APP-001', 1, 5, 8081, 'v2.3.1', 'Spring Boot + Redis',     'http://git.example.com/shop/trade.git',   'ONLINE'),
(2, '订单服务',     'APP-002', 1, 6, 8082, 'v2.1.0', 'Spring Boot + MQ',        'http://git.example.com/shop/order.git',   'ONLINE'),
(3, '商城门户',     'APP-003', 1, 3, 8080, 'v1.9.4', 'Vue2 + Nginx',            'http://git.example.com/shop/portal.git',  'ONLINE'),
(4, '统一认证',     'APP-004', 2, 5, 8083, 'v1.5.1', 'Spring Boot + JWT',       'http://git.example.com/uc/auth.git',      'OFFLINE'),
(5, '用户中心',     'APP-005', 2, 1, 8084, 'v1.4.0', 'Spring Boot + MySQL',     'http://git.example.com/uc/user.git',      'OFFLINE'),
(6, '经营大屏',     'APP-006', 3, 3, 8085, 'v1.0.6', 'Vue2 + ECharts',          'http://git.example.com/bi/dash.git',      'ONLINE'),
(7, '移动网关',     'APP-007', 4, 1, 8086, 'v0.9.1', 'Spring Cloud Gateway',    'http://git.example.com/app/gateway.git',  'DEPLOYING'),
(8, '探活采集',     'APP-008', 5, 4, 8087, 'v0.1.0', 'Spring Boot',             'http://git.example.com/ops/agent.git',    'OFFLINE');""")
    lines.append("")
    lines.append("-- ----------------------------")
    lines.append("-- 制品（自动部署依赖）")
    lines.append("-- ----------------------------")
    lines.append("""INSERT INTO `deploy_artifact` (`id`, `app_id`, `version`, `file_name`, `file_path`, `file_size`, `md5`, `uploader`) VALUES
(1, 1, 'v2.3.1', 'trade-v2.3.1.jar', '/data/artifacts/trade-v2.3.1.jar', 52428800, 'a1b2c3d4e5f60718293a4b5c6d7e8f90', 'dev1'),
(2, 1, 'v2.3.0', 'trade-v2.3.0.jar', '/data/artifacts/trade-v2.3.0.jar', 51380224, 'b2c3d4e5f60718293a4b5c6d7e8f90a1', 'dev1'),
(3, 2, 'v2.1.0', 'order-v2.1.0.jar', '/data/artifacts/order-v2.1.0.jar', 41943040, 'c3d4e5f60718293a4b5c6d7e8f90a1b2', 'dev1'),
(4, 2, 'v2.1.1', 'order-v2.1.1.jar', '/data/artifacts/order-v2.1.1.jar', 42000000, 'd4e5f60718293a4b5c6d7e8f90a1b2c3', 'dev1'),
(5, 4, 'v1.5.2', 'auth-v1.5.2.jar',  '/data/artifacts/auth-v1.5.2.jar',  31457280, 'e5f60718293a4b5c6d7e8f90a1b2c3d4', 'dev2'),
(6, 7, 'v0.9.1', 'gateway-v0.9.1.jar','/data/artifacts/gateway-v0.9.1.jar',20971520, 'f60718293a4b5c6d7e8f90a1b2c3d4e5', 'dev1'),
(7, 6, 'v1.0.6', 'dash-v1.0.6.zip',  '/data/artifacts/dash-v1.0.6.zip',  8388608, '018293a4b5c6d7e8f90a1b2c3d4e5f60', 'dev2');""")
    lines.append("")
    lines.append("-- ----------------------------")
    lines.append("-- 部署单：近 30 天成败密度 + 现场状态单（分析页 / 列表页都能看出价值）")
    lines.append("-- finish_time / create_time 用相对今天的日期，刷新种子后曲线不空")
    lines.append("-- ----------------------------")
    lines.append("""INSERT INTO `deploy` (`id`, `deploy_no`, `app_id`, `server_id`, `version`, `deploy_type`, `execute_mode`, `status`,
                      `applicant`, `approver`, `apply_time`, `approve_time`, `finish_time`, `reason`, `remark`,
                      `rollback_version`, `row_version`) VALUES""")

    deploy_rows = []
    did = 1
    for day, hour, app, srv, ver, dtype, mode, status, applicant, approver, reason, remark, rb, kw in HISTORY:
        apply_t = ts(day, hour)
        approve_t = ts(day, hour, 15)
        finish_t = ts(day, hour, 40)
        deploy_no = f"DP-H{did:03d}"
        deploy_rows.append(
            f"({did}, '{deploy_no}', {app}, {srv}, '{ver}', '{dtype}', '{mode}', '{status}', "
            f"{sql_str(applicant)}, {sql_str(approver)}, {apply_t}, {approve_t}, {finish_t}, "
            f"{sql_str(reason)}, {sql_str(remark)}, {sql_str(rb)}, 3)"
        )
        did += 1

    for day, hour, app, srv, ver, dtype, mode, status, applicant, approver, reason, remark, rb in SHOWCASE:
        apply_t = ts(day, hour)
        approve_t = "NULL" if status == "PENDING" else ts(day, hour, 15)
        finish_t = "NULL"
        row_v = 0 if status == "PENDING" else (2 if status in ("APPROVED", "DEPLOYING", "REJECTED") else 3)
        deploy_no = f"DP-L{did:03d}"
        deploy_rows.append(
            f"({did}, '{deploy_no}', {app}, {srv}, '{ver}', '{dtype}', '{mode}', '{status}', "
            f"{sql_str(applicant)}, {sql_str(approver)}, {apply_t}, {approve_t}, {finish_t}, "
            f"{sql_str(reason)}, {sql_str(remark)}, {sql_str(rb)}, {row_v})"
        )
        did += 1

    lines.append(",\n".join(deploy_rows) + ";")
    lines.append("")

    # executions for finished + running showcase
    lines.append("-- ----------------------------")
    lines.append("-- 部署执行记录")
    lines.append("-- ----------------------------")
    lines.append("INSERT INTO `deploy_execution` (`id`, `deploy_id`, `status`, `exit_code`, `retry_count`, `start_time`, `end_time`, `error_message`) VALUES")
    exec_rows = []
    eid = 1
    for i, row in enumerate(HISTORY, start=1):
        day, hour, *rest = row
        status = row[7]
        kw = row[13]
        start_t = ts(day, hour, 16)
        end_t = ts(day, hour, 39)
        if status == "SUCCESS":
            exec_rows.append(f"({eid}, {i}, 'SUCCESS', 0, 0, {start_t}, {end_t}, NULL)")
        else:
            msg = KEYWORD_DETAIL.get(kw, "failed")
            exec_rows.append(f"({eid}, {i}, 'FAILED', 1, 1, {start_t}, {end_t}, {sql_str(msg[:120])})")
        eid += 1
    # showcase deploying ids = len(HISTORY)+2 and last deploying
    # SHOWCASE indices: 0 REJECTED, 1 DEPLOYING, 2 APPROVED, 3 PENDING, 4 PENDING, 5 DEPLOYING
    hist_n = len(HISTORY)
    for idx, sc in enumerate(SHOWCASE):
        if sc[7] != "DEPLOYING":
            continue
        did_sc = hist_n + idx + 1
        day, hour = sc[0], sc[1]
        exec_rows.append(f"({eid}, {did_sc}, 'RUNNING', NULL, 0, {ts(day, hour, 16)}, NULL, NULL)")
        eid += 1
    lines.append(",\n".join(exec_rows) + ";")
    lines.append("")

    # logs
    lines.append("-- ----------------------------")
    lines.append("-- 流转日志 + EXEC（关键字覆盖 error/exception/failed/timeout/denied/refused）")
    lines.append("-- ----------------------------")
    lines.append("INSERT INTO `deploy_log` (`id`, `deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`) VALUES")
    log_rows = []
    lid = 1
    for i, row in enumerate(HISTORY, start=1):
        day, hour, app, srv, ver, dtype, mode, status, applicant, approver, reason, remark, rb, kw = row
        t0 = ts(day, hour)
        t1 = ts(day, hour, 15)
        t2 = ts(day, hour, 20)
        t3 = ts(day, hour, 40)
        log_rows.append(f"({lid}, {i}, 'APPLY', {sql_str(applicant)}, {sql_str('提交 ' + reason)}, 'STATE', 0, 'MANUAL', {t0})")
        lid += 1
        log_rows.append(f"({lid}, {i}, 'APPROVE', {sql_str(approver)}, '审批通过', 'STATE', 0, 'MANUAL', {t1})")
        lid += 1
        log_rows.append(f"({lid}, {i}, 'START_DEPLOY', {sql_str(approver)}, {sql_str('开始部署 executeMode=' + mode)}, 'STATE', 0, 'MANUAL', {t2})")
        lid += 1
        if status == "SUCCESS":
            action = "ROLLBACK" if dtype == "ROLLBACK" else "FINISH"
            log_rows.append(f"({lid}, {i}, '{action}', {sql_str(approver)}, {sql_str(remark or '完成')}, 'STATE', 0, 'AUTO', {t3})")
            lid += 1
            log_rows.append(f"({lid}, {i}, 'EXEC', 'system', '[auto] deploy ok md5 verified', 'EXEC', 1, 'AUTO', {t3})")
            lid += 1
        else:
            log_rows.append(f"({lid}, {i}, 'FAIL', {sql_str(approver)}, {sql_str(remark or '失败')}, 'STATE', 0, 'AUTO', {t3})")
            lid += 1
            detail = KEYWORD_DETAIL[kw]
            log_rows.append(f"({lid}, {i}, 'EXEC', 'system', {sql_str(detail)}, 'EXEC', 1, 'AUTO', {t3})")
            lid += 1
            # second keyword line for density on some failures
            if kw in ("error", "timeout", "exception"):
                extra = {
                    "error": "[auto] exit_code=1 retry failed",
                    "timeout": "[auto] health check timeout after 60s",
                    "exception": "[auto] nested exception is BindException",
                }[kw]
                log_rows.append(f"({lid}, {i}, 'EXEC', 'system', {sql_str(extra)}, 'EXEC', 2, 'AUTO', {t3})")
                lid += 1

    for idx, sc in enumerate(SHOWCASE):
        did_sc = hist_n + idx + 1
        day, hour, app, srv, ver, dtype, mode, status, applicant, approver, reason, remark, rb = sc
        t0 = ts(day, hour)
        log_rows.append(f"({lid}, {did_sc}, 'APPLY', {sql_str(applicant)}, {sql_str('提交 ' + reason)}, 'STATE', 0, 'MANUAL', {t0})")
        lid += 1
        if status == "PENDING":
            continue
        t1 = ts(day, hour, 15)
        if status == "REJECTED":
            log_rows.append(f"({lid}, {did_sc}, 'REJECT', {sql_str(approver)}, {sql_str(remark)}, 'STATE', 0, 'MANUAL', {t1})")
            lid += 1
            continue
        log_rows.append(f"({lid}, {did_sc}, 'APPROVE', {sql_str(approver)}, '审批通过', 'STATE', 0, 'MANUAL', {t1})")
        lid += 1
        if status == "DEPLOYING":
            t2 = ts(day, hour, 20)
            log_rows.append(f"({lid}, {did_sc}, 'START_DEPLOY', {sql_str(approver)}, '部署进行中', 'STATE', 0, 'MANUAL', {t2})")
            lid += 1
            log_rows.append(f"({lid}, {did_sc}, 'EXEC', 'system', '[auto] uploading artifact ...', 'EXEC', 1, 'AUTO', {t2})")
            lid += 1

    lines.append(",\n".join(log_rows) + ";")
    lines.append("")

    # alarms with create_time
    lines.append("-- ----------------------------")
    lines.append("-- 告警：跨近 30 天分布，形成走势 + 未闭环")
    lines.append("-- ----------------------------")
    lines.append("INSERT INTO `alarm` (`id`, `alarm_no`, `app_id`, `server_id`, `deploy_no`, `level`, `content`, `status`, `handler`, `handle_time`, `resolve_time`, `create_time`) VALUES")
    alarm_rows = []
    for i, (day, hour, app, srv, level, content, status, handler, dno) in enumerate(ALARM_DAYS, start=1):
        ct = ts(day, hour)
        ht = "NULL"
        rt = "NULL"
        if status in ("HANDLING", "RESOLVED"):
            ht = ts(day, hour, 20)
        if status == "RESOLVED":
            rt = ts(day, hour, 50)
        alarm_rows.append(
            f"({i}, 'AL-D{i:03d}', {app}, {srv}, {sql_str(dno)}, '{level}', {sql_str(content)}, '{status}', "
            f"{sql_str(handler)}, {ht}, {rt}, {ct})"
        )
    lines.append(",\n".join(alarm_rows) + ";")
    lines.append("")
    lines.append("-- 复位自增")
    lines.append("ALTER TABLE `project` AUTO_INCREMENT = 100;")
    lines.append("ALTER TABLE `server` AUTO_INCREMENT = 100;")
    lines.append("ALTER TABLE `app` AUTO_INCREMENT = 100;")
    lines.append("ALTER TABLE `deploy` AUTO_INCREMENT = 200;")
    lines.append("ALTER TABLE `deploy_log` AUTO_INCREMENT = 2000;")
    lines.append("ALTER TABLE `deploy_execution` AUTO_INCREMENT = 200;")
    lines.append("ALTER TABLE `deploy_artifact` AUTO_INCREMENT = 100;")
    lines.append("ALTER TABLE `alarm` AUTO_INCREMENT = 200;")
    lines.append("ALTER TABLE `notice` AUTO_INCREMENT = 100;")
    return "\n".join(lines)


SEED_HEADER = """-- ============================================================
-- 演示数据重置：保留人员账号（admin/dev/ops），清空并重建业务数据
-- 用法：在已有 schema（含 SSH / execute_mode 等列）的 manager 库执行
-- 日期用 CURDATE()/DATE_SUB，刷新后「发布分析」近 7/30 天曲线仍有密度
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `deploy_execution`;
TRUNCATE TABLE `deploy_artifact`;
TRUNCATE TABLE `deploy_log`;
TRUNCATE TABLE `alarm`;
TRUNCATE TABLE `deploy`;
TRUNCATE TABLE `app`;
TRUNCATE TABLE `server`;
TRUNCATE TABLE `project`;
TRUNCATE TABLE `notice`;

SET FOREIGN_KEY_CHECKS = 1;

"""

DATA_HEADER = """-- ============================================================
-- DevOps 发布管理系统 - 初始/演示数据
-- 目标库：manager（请先执行 schema.sql）
-- 说明：
--   1. 人员账号保留为演示账号；业务表覆盖各模块全状态，便于答辩展示
--   2. 密码为 MD5(明文 + 盐值)，盐值与 Constants.PASSWORD_SALT 一致
--   3. 已有库仅重置业务数据时，用同目录 demo_seed.sql（会 TRUNCATE 业务表，不动人员）
--   4. 部署/告警时间用相对今天的表达式，保证「发布分析」近 7/30 天有曲线
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 0. 超级管理员（登录：admin / admin）
-- ----------------------------
INSERT INTO `admin` (`id`, `username`, `password`, `name`, `phone`, `email`, `role`, `status`) VALUES
(1, 'admin', MD5(CONCAT('admin', 'devops_salt_2026')), '超级管理员', '13677889922', 'admin@xm.com', 'ADMIN', 'ACTIVE');

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

"""


def main():
    body = build_business_body()
    (SQL_DIR / "demo_seed.sql").write_text(SEED_HEADER + body, encoding="utf-8")
    (SQL_DIR / "data.sql").write_text(DATA_HEADER + body, encoding="utf-8")
    print("history", len(HISTORY), "showcase", len(SHOWCASE), "alarms", len(ALARM_DAYS))
    print("wrote demo_seed.sql and data.sql")


if __name__ == "__main__":
    main()
