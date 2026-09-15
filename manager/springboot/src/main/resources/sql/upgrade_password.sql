-- ============================================================
-- 密码加密升级脚本：明文密码 -> MD5(明文 + 盐值)
-- 目标库：manager
-- 盐值必须与 com.example.common.Constants.PASSWORD_SALT 保持一致
-- 本脚本为幂等脚本，重复执行结果不变
-- ============================================================

SET NAMES utf8mb4;

-- admin 表：原明文密码为 admin，加密后仍用 admin 登录
UPDATE `admin` SET `password` = MD5(CONCAT('admin', 'devops_salt_2026')) WHERE `username` = 'admin';

-- dev 表：原明文密码为 123456，加密后仍用 123456 登录
UPDATE `dev` SET `password` = MD5(CONCAT('123456', 'devops_salt_2026'));

-- ops 表：原明文密码为 123456，加密后仍用 123456 登录
UPDATE `ops` SET `password` = MD5(CONCAT('123456', 'devops_salt_2026'));
