# SQL 脚本

这个目录没有 Flyway，也没有别的工具会按文件名自动跑 `upgrade_*.sql`。顺序以本文为准，手工执行。

## 全新安装

用这两个文件，不要再跑 `upgrade_*.sql`：

| 顺序 | 文件 | 做什么 |
|---|---|---|
| 1 | `schema.sql` | 建表。不创建数据库，执行前先 `USE manager`。表结构里已经包含后面升级脚本要补的列和外键 |
| 2 | `data.sql` | 初始演示数据：人员 + 各模块全状态业务数据。密码已经是 `MD5(明文 + 盐值)` |

`upgrade_admin.sql`、`upgrade_alarm.sql` 的文件头写了：全新安装走 `schema.sql`，不必再执行本脚本。`upgrade_password.sql` 和 `upgrade_fk.sql` 同样不要在新库上跑。

## 已有库：清空业务数据并重灌演示集

人员账号（`person`）不动，只重建公告、项目、服务器、应用、部署、日志、执行、告警时：

```sql
SOURCE demo_seed.sql;
```

`demo_seed.sql` 会 `TRUNCATE` 上述业务表后写入与 `data.sql` 一致的演示集（覆盖审查验收、运维审批、自动与手动部署、失败回滚、告警闭环；部署与告警时间用相对今天的日期，保证「发布分析」近 7/30 天曲线有密度）。结构须已含 SSH、`execute_mode` 等列（新库用 `schema.sql`；旧库先跑下文升级脚本）。

`data.sql` 与 `demo_seed.sql` 由 `_gen_demo_seed.py` 生成。生成器写出 `person` 单表和角色账号。要改演示数据，改生成器再运行；不要在生成器之外改完 SQL 又重跑，否则手改会被覆盖。

## 真实虚机实验室（清空历史单据）

三台 CentOS：`hadoop101/102/103`（`192.168.116.131/132/133`），SSH `kang/centos7`，部署目录 `/opt/module`。

```sql
SOURCE lab_real_vm.sql;
```

会清空发布单、告警与演示服务器，写入上述三台主机及应用实例；人员账号保留。虚机目录可用 `_prep_real_vms.py` 检查并创建 `/opt/module`。

## 已有库升级

下面的脚本只给**还没合并人员表的旧库**用。按顺序手工执行。每一条执行前都先备份。

步骤 1（`upgrade_admin.sql`）和步骤 3（`upgrade_password.sql`）会改 `admin` / `dev` / `ops`。库里已经只有 `person` 时再跑会报 1146，应跳过。步骤 11 会删除这三张旧表，不可逆。

| 顺序 | 文件 | 做什么 | 执行前 |
|---|---|---|---|
| 1 | `upgrade_admin.sql` | 给 `admin` 补 `status`、`create_time`、`update_time`、`deleted`，并回填空值。幂等 | 备份。它会 `ALTER TABLE` |
| 2 | `upgrade_alarm.sql` | 给 `alarm` 补可空列 `deploy_no`。幂等 | 备份。它会 `ALTER TABLE` |
| 3 | `upgrade_password.sql` | 把 `admin` 用户 `admin` 的密码写成 `MD5(admin + 盐值)`，并把 `dev`、`ops` 全部行的密码写成 `MD5(123456 + 盐值)` | 必须备份。改过的密码会被盖回初始口令 |
| 4 | `upgrade_fk.sql` | 给 `app`、`deploy`、`deploy_log`、`alarm` 补 7 条物理外键。缺了才加，重复执行会跳过 | 必须备份。如果有对不上的 `project_id`、`server_id`、`app_id`、`deploy_id`，加外键会失败 |

在库里的执行方式（把脚本路径换成你机器上的实际路径）：

```sql
SOURCE upgrade_admin.sql;
SOURCE upgrade_alarm.sql;
SOURCE upgrade_password.sql;
SOURCE upgrade_fk.sql;
```

## 为什么是这个顺序

这是读完 4 个脚本后的依赖，不是按文件名猜的。

- `upgrade_admin.sql` 只改 `admin`，不读其他升级脚本的结果。
- `upgrade_alarm.sql` 只给 `alarm` 加 `deploy_no`。它和 admin 脚本没有互相依赖。外键脚本用的是 `alarm.app_id`、`alarm.server_id`，并不引用 `deploy_no`。两个补列脚本仍放在外键前面：先把列补齐，再加约束。
- `upgrade_password.sql` 只更新已有的 `password` 列，不建列、不建外键。数据订正放在补列之后。它不依赖 `status` 或 `deploy_no`。
- `upgrade_fk.sql` 添加的 7 条外键引用的是这些表原来就有的主键列，不依赖 admin 的新列，也不依赖 `deploy_no`。外键放在最后，避免先套上约束再改表。

## 后续升级（第 5–10 步，新增功能）

接在上面 4 个脚本后面，按这个顺序手工执行。每一条执行前先备份。不要跑 `upgrade_password.sql` 来“顺便升级”，它会把密码改回初始口令。

| 顺序 | 文件 | 做什么 | 依赖 |
|---|---|---|---|
| 5 | `upgrade_server_ssh.sql` | 给 `server` 补 SSH 端口、用户、认证方式、密文密码、私钥、部署目录 | 只改 `server` |
| 6 | `upgrade_deploy_log.sql` | 给 `deploy_log` 补 `log_type`、`seq`、`source`，把 `detail` 扩到 1024，并加 `(deploy_id, log_type, seq)` 索引 | 只改 `deploy_log` |
| 7 | `upgrade_deploy_version.sql` | 给 `deploy` 补 `rollback_version` 和乐观锁 `row_version`。`version` 仍是发布版本号 | 只改 `deploy` |
| 8 | `upgrade_deploy_execution.sql` | 新建 `deploy_execution`，外键指向 `deploy.id` | 先有 `deploy` |
| 9 | `upgrade_deploy_artifact.sql` | 删除已废弃的 `deploy_artifact`（若存在）。发布已改为按仓库地址拉取版本 | 可单独执行 |
| 10 | `upgrade_deploy_execute_mode.sql` | 给 `deploy` 补 `execute_mode`，已有行默认 `AUTO` | 只改 `deploy` |
| 11 | `upgrade_person.sql` | 把 `admin` / `dev` / `ops` 合并进 `person` 后删除旧表。用户名冲突时保留已写入的 `person` 行 | 旧三张表还在时才搬数据。人员主键会重排，已登录 token 需重新登录 |
| 12 | `upgrade_relation.sql` | 人员用户名、公告发布人、发布单号收成外键；应用必须属于项目，发布单必须有应用和目标环境，流转记录必须属于发布单 | 先有 `person.username` 唯一约束和已有的 7 条外键。对不上的负责人、操作人、单号会被清空后再加约束 |

全新安装用 `schema.sql` 即可，这些升级脚本不要再跑。
