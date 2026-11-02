-- 发布主路径改为按仓库地址在目标机拉取版本后，deploy_artifact 已废弃。
-- 本脚本删除遗留表（幂等）。全新安装走 schema.sql，不必执行本脚本。
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `deploy_artifact`;
SET FOREIGN_KEY_CHECKS = 1;
