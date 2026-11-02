-- 准备 learn-git 真实发布：改仓库、结束卡住的发布中、补齐各单流转日志，再建两张待执行单
SET NAMES utf8mb4;

UPDATE `app` SET `repo_url` = 'https://github.com/kyuxuan234/learn-git.git';

-- 结束卡住的发布中，避免同环境互斥
UPDATE `deploy` SET `status` = 'FAILED',
  `finish_time` = NOW(),
  `remark` = '截图前结束卡住的发布中'
WHERE `id` IN (18, 19) AND `status` = 'DEPLOYING';

UPDATE `app_instance` SET `status` = 'OFFLINE'
WHERE `app_id` = 5 AND `env_code` = 'DEV' AND `status` = 'DEPLOYING';

-- 为已有单据补流转/执行日志（原先现场单日志过少）
DELETE FROM `deploy_log` WHERE `deploy_id` BETWEEN 1 AND 21;

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`)
SELECT d.id, 'APPLY', d.applicant, CONCAT('提交 ', IFNULL(d.reason, '')), 'STATE', 0, 'MANUAL',
       IFNULL(d.apply_time, NOW())
FROM `deploy` d WHERE d.id BETWEEN 1 AND 21;

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`)
SELECT d.id,
       CASE WHEN d.status = 'REJECTED' THEN 'REJECT' ELSE 'APPROVE' END,
       IFNULL(d.approver, 'ops1'),
       CASE WHEN d.status = 'REJECTED' THEN IFNULL(d.remark, '已驳回') ELSE '审批通过' END,
       'STATE', 0, 'MANUAL',
       IFNULL(d.approve_time, DATE_ADD(IFNULL(d.apply_time, NOW()), INTERVAL 15 MINUTE))
FROM `deploy` d
WHERE d.id BETWEEN 1 AND 21
  AND d.status NOT IN ('REVIEW', 'TESTING', 'PENDING');

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`)
SELECT d.id, 'START_DEPLOY', IFNULL(d.approver, 'ops1'),
       CONCAT('开始部署 executeMode=', IFNULL(d.execute_mode, 'AUTO')),
       'STATE', 0, 'MANUAL',
       DATE_ADD(IFNULL(d.approve_time, IFNULL(d.apply_time, NOW())), INTERVAL 5 MINUTE)
FROM `deploy` d
WHERE d.id BETWEEN 1 AND 21
  AND d.status IN ('DEPLOYING', 'SUCCESS', 'FAILED')
  AND d.deploy_type <> 'ROLLBACK';

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`)
SELECT d.id, 'FINISH', IFNULL(d.approver, 'ops1'), IFNULL(d.remark, '发布成功'),
       'STATE', 0, IF(d.execute_mode = 'MANUAL', 'MANUAL', 'AUTO'),
       IFNULL(d.finish_time, NOW())
FROM `deploy` d WHERE d.id BETWEEN 1 AND 21 AND d.status = 'SUCCESS' AND d.deploy_type <> 'ROLLBACK';

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`)
SELECT d.id, 'FAIL', IFNULL(d.approver, 'ops1'), IFNULL(d.remark, '发布失败'),
       'STATE', 0, 'AUTO', IFNULL(d.finish_time, NOW())
FROM `deploy` d WHERE d.id BETWEEN 1 AND 21 AND d.status = 'FAILED';

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`)
SELECT d.id, 'EXEC', NULL,
       CASE
         WHEN d.status = 'SUCCESS' AND d.execute_mode = 'AUTO' THEN '[auto] deploy ok md5 verified'
         WHEN d.status = 'SUCCESS' AND d.execute_mode = 'MANUAL' THEN '[manual] operator confirmed SUCCESS'
         WHEN d.status = 'FAILED' THEN '[auto] Permission denied writing /opt/module/releases'
         ELSE '[auto] fetching repository ...'
       END,
       'EXEC', 1, IF(d.execute_mode = 'MANUAL', 'MANUAL', 'AUTO'),
       IFNULL(d.finish_time, NOW())
FROM `deploy` d
WHERE d.id BETWEEN 1 AND 21
  AND d.status IN ('DEPLOYING', 'SUCCESS', 'FAILED');

-- 两张新的待执行单：自动 / 人工，版本 master，仓库 learn-git
DELETE FROM `deploy_log` WHERE `deploy_id` IN (30, 31);
DELETE FROM `deploy_execution` WHERE `deploy_id` IN (30, 31);
DELETE FROM `deploy` WHERE `id` IN (30, 31);

INSERT INTO `deploy` (`id`, `deploy_no`, `app_id`, `server_id`, `env_code`, `version`, `deploy_type`, `execute_mode`, `status`,
                      `applicant`, `approver`, `apply_time`, `approve_time`, `finish_time`, `reason`, `remark`,
                      `rollback_version`, `row_version`) VALUES
(30, 'DP-GIT-AUTO', 1, 1, 'DEV', 'master', 'FULL', 'AUTO', 'APPROVED',
 'dev1', 'ops1', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL,
 'learn-git 自动发布演示', '待开始自动发布', NULL, 2),
(31, 'DP-GIT-MANUAL', 2, 1, 'DEV', 'master', 'FULL', 'MANUAL', 'APPROVED',
 'dev1', 'ops1', DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE), NULL,
 'learn-git 人工发布演示', '待开始人工发布', NULL, 2);

INSERT INTO `deploy_log` (`deploy_id`, `action`, `operator`, `detail`, `log_type`, `seq`, `source`, `create_time`) VALUES
(30, 'APPLY', 'dev1', '提交 learn-git 自动发布演示', 'STATE', 0, 'MANUAL', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(30, 'APPROVE', 'ops1', '审批通过', 'STATE', 0, 'MANUAL', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(31, 'APPLY', 'dev1', '提交 learn-git 人工发布演示', 'STATE', 0, 'MANUAL', DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(31, 'APPROVE', 'ops1', '审批通过', 'STATE', 0, 'MANUAL', DATE_SUB(NOW(), INTERVAL 15 MINUTE));

ALTER TABLE `deploy` AUTO_INCREMENT = 200;
