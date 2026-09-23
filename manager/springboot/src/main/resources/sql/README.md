# SQL 脚本

这个目录没有 Flyway，也没有别的工具会按文件名自动跑 `upgrade_*.sql`。顺序以本文为准，手工执行。

## 全新安装

用这两个文件，不要再跑 `upgrade_*.sql`：

| 顺序 | 文件 | 做什么 |
|---|---|---|
| 1 | `schema.sql` | 建表。不创建数据库，执行前先 `USE manager`。表结构里已经包含后面升级脚本要补的列和外键 |
| 2 | `data.sql` | 初始数据。密码已经是 `MD5(明文 + 盐值)` |

`upgrade_admin.sql`、`upgrade_alarm.sql` 的文件头写了：全新安装走 `schema.sql`，不必再执行本脚本。`upgrade_password.sql` 和 `upgrade_fk.sql` 同样不要在新库上跑。

Docker 首次启动（`mysql-data` 卷是空的）只会挂载并执行 `schema.sql`、`data.sql`。

## 已有库升级

下面 4 个脚本针对**已经存在的旧库**。按顺序手工执行。每一条执行前都先备份。

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

## 第 3 步新增

接在上面 4 个脚本后面，按这个顺序手工执行。每一条执行前先备份。不要跑 `upgrade_password.sql` 来“顺便升级”，它会把密码改回初始口令。

| 顺序 | 文件 | 做什么 | 依赖 |
|---|---|---|---|
| 5 | `upgrade_server_ssh.sql` | 给 `server` 补 SSH 端口、用户、认证方式、密文密码、私钥、部署目录 | 只改 `server` |
| 6 | `upgrade_deploy_log.sql` | 给 `deploy_log` 补 `log_type`、`seq`、`source`，把 `detail` 扩到 1024，并加 `(deploy_id, log_type, seq)` 索引 | 只改 `deploy_log` |
| 7 | `upgrade_deploy_version.sql` | 给 `deploy` 补 `rollback_version` 和乐观锁 `row_version`。`version` 仍是发布版本号 | 只改 `deploy` |
| 8 | `upgrade_deploy_execution.sql` | 新建 `deploy_execution`，外键指向 `deploy.id` | 先有 `deploy` |
| 9 | `upgrade_deploy_artifact.sql` | 新建 `deploy_artifact`，外键指向 `app.id` | 先有 `app` |
| 10 | `upgrade_deploy_execute_mode.sql` | 给 `deploy` 补 `execute_mode`，已有行默认 `AUTO` | 只改 `deploy` |

全新安装用 `schema.sql` 即可，这些升级脚本不要再跑。Docker 空卷初始化也只执行 `schema.sql` 和 `data.sql`。
