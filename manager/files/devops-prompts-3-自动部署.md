# 第 3 步 · 自动部署

> 这个文件是干嘛的：把部署动作从"人工在页面上点一下、系统只改个状态"改成"机器真的连上服务器去执行"。
> 做完能怎样：点「开始部署」后，系统自己 SSH 到目标机跑脚本，执行过程实时回显，成功失败自动判定，回滚也是真的回滚。
> 怎么做：**这一步有 20 条，其中前 16 条是必须做的，后 4 条有余力再做**。16 条必须项全部做完，才是完整可演示的自动部署。
> **注意**：3-05、3-06 是 v4 版新增的两条「补地基」条目（原方案误以为它们已经存在），它们必须先做——3-12、3-13、3-16 和里程碑 4 的数据分析全都依赖它们。

---

## 开工前置：先就绪一台可 SSH 的 Linux 测试机

**进入本里程碑之前，必须有一台能 SSH 连上的 Linux 测试机。** 本文件一半条目的验证都要登到目标机上看结果，没有测试机等于盲飞。

Windows 开发机上最省事的两种搞法（任选其一）：
1. **docker 起 sshd 容器（推荐）**：`docker run -d -p 2222:22 ...` 起一个带 sshd 的容器。好处是可以反复销毁重建，正好顺带演示"可重复部署"。
2. **WSL2 + openssh-server**：装好后启动 ssh 服务即可。

⚠️ 初版里个别条目写过"没有可用的测试机就先跳过验证"——**在本版作废，验证不能跳**。机器没就绪就先就绪机器，不要往下做。

---

## v2 修订说明（评审后定稿版）

本版合并了架构评审的修正，相对初版的变化：

| 变化 | 说明 |
|---|---|
| 3-09 整段重写 | 原写法把线程池提交放在事务方法内部，工作线程可能在事务提交前就开始跑（竞态）；且"看两行日志先后"的验收证明不了事务边界。改为：无事务入口方法 → 事务方法返回（即提交）后再提交线程池 |
| 新增 3-10 启动恢复 | 进程重启会硬杀在途 SSH 执行，部署单永久卡在 DEPLOYING（状态机不能从 DEPLOYING 重入，人工也推不动）。启动时把悬挂的 RUNNING 执行判 FAILED，可重新发起 |
| 原 3-17 制品管理 → 最小版升为必做（新 3-15） | 没有它制品靠手工预放到目标机，"自动部署"是假闭环。砍掉前端表单联动，只留 表 + 上传 + scp + MD5 |
| 原 3-16 回滚真实执行 → 升为必做（新 3-16） | 总览卖点"回滚是真的回滚"必须有条目支撑 |
| 其余修补 | deploy.sh 末尾加幂等启动步骤（新 3-11）；Java 读脚本模板统一 `\r\n → \n` 归一化（与 1-09 的 .gitattributes、file 命令验证构成三道保险） |

**v4 修订（对齐真实代码）**：原方案把 CAS 乐观锁、`deploy_log` 的三列、游标索引
都当成"已经做好了"来写禁令（"一个字都不要改"），实际上**它们在仓库里都不存在**
——`DeployService` 用的是 `deployMapper.updateById(updateDeploy)`，`DeployMapper.xml`
里没有 `casUpdateStatus`，`deploy` 表没有 `row_version`，`deploy_log` 表只有 8 个字段。
所以 v4 把"不要改"改成"本轮新增"：**新增 3-05（给 deploy_log 补 log_type / seq / source
三列 + 索引）和 3-06（给 deploy 加 row_version + 状态变更改走 CAS）**，
原有 3-05 ~ 3-18 顺延为 3-07 ~ 3-20。同时把 SQL 脚本名从 Flyway 风格
（`V5__` / `V6__` / `V7__`）改回项目风格 `upgrade_*.sql`——项目没有 Flyway，脚本手动执行。

**编号映射**（评审前版本 → 当前版本）：

| 评审前编号 | 当前编号 | 条目 |
|---|---|---|
| 3-01 ~ 3-04 | 3-01 ~ 3-04 | 不变 |
| —（v4 新增） | 3-05 | 给 `deploy_log` 补 log_type / seq / source 三列 + 游标索引 |
| —（v4 新增） | 3-06 | 给 `deploy` 加 row_version，状态变更改走 CAS |
| 3-05 | 3-07 | 执行任务表 `deploy_execution` |
| 3-06 | 3-08 | 部署执行线程池 |
| 3-07 | 3-09 | 执行动作移出事务（**整段重写**） |
| —（v2 新增） | 3-10 | 启动恢复 |
| 3-08 | 3-11 | deploy.sh 真执行（加幂等启动 + CRLF 兜底） |
| 3-09 | 3-12 | 执行输出落库 |
| 3-10 | 3-13 | 新建 execLogs（旧稿写成"填数据"，但方法不存在） |
| 3-11 | 3-14 | 前端执行日志抽屉 |
| 3-15 | 3-15 | 制品管理（最小版，升必做） |
| 3-14 | 3-16 | 回滚真实执行（升必做） |
| 3-12 | 3-17 | 自动重试（余力） |
| 3-13 | 3-18 | 超时可终止（余力） |
| 3-16 | 3-19 | 并发互斥（余力） |
| 3-17 | 3-20 | 日志自动刷新（余力） |

---

## 先说清楚两件事

### 一、哪些是真预留、哪些得本轮新建（**以代码为准，别照旧稿说**）

原作者（你）确实留了一些口子，但**没有旧稿说的那么多**。逐条核对过当前代码后的真实清单：

**真的已经存在的**：

| 预留物 | 位置 | 现状 |
|---|---|---|
| `EXEC` 枚举值 | `DeployLogTypeEnum` | 枚举值在，注释写「执行输出（预留）」。**但 `deploy_log` 表没有 `log_type` 列，写不进去** |
| `AUTO` 枚举值 | `DeployLogSourceEnum` | 枚举值在。**但表上没有 `source` 列** |
| `EXEC` 动作值 | `DeployLog.action` 字段注释里已列 | 定义了，无人使用 |
| `DeployExecSummaryVO` | `dto/` | 类存在，注释原文：「本轮恒空，字段结构先定死，接执行引擎时只填数据。」 |
| `DeployTimelineVO` / `DeployTimelineStateVO` | `dto/` | 结构在，`DeployService` 没有对外的时间线查询 |

**旧稿写成"已经好了"、实际仓库里没有的**（这些是本轮要新建的，见 3-05 / 3-06 / 3-13）：

| 旧稿说法 | 真实情况 |
|---|---|
| `deploy_log.seq` 字段（「EXEC 行序，STATE 固定 0」） | **不存在**。`DeployLog` 实体只有 id / deployId / action / operator / detail / createTime / updateTime / deleted 八个字段 |
| `deploy_log.log_type` / `source` 列 | **不存在**，表上和实体上都没有 |
| 游标索引 `idx_log_deploy_type_seq (deploy_id, log_type, seq)` | **不存在**，没有任何脚本建过它 |
| `DeployService.execLogs()` 方法 | **不存在**（`DeployService.java` 共约 280 行，没有这个方法） |
| `DeployLogLinesVO` | **不存在**（`dto/` 下只有 DeployExecSummaryVO / DeployTimelineVO / DeployTimelineStateVO） |
| `GET /deploy/{id}/logs` 端点 | **不存在**（`DeployController` 只有 add / delete / select / approve / startDeploy / finishDeploy / rollback） |
| `HEALTH_CHECK_FAIL` 动作值 | `DeployActionEnum` 里没有，只在 `DeployLog.action` 的字段注释里被提到 |
| `casUpdate()` + `row_version` 乐观锁 | **不存在**，状态变更走 `deployMapper.updateById(updateDeploy)` |

**所以这一步的活儿，是"先补地基、再填空"。** 答辩时按真实情况说：

> "执行这块我上一轮定了数据模型和接口契约：日志类型、来源的枚举先留好了，执行摘要 VO 也留好了。
> 这一轮我按契约把 `log_type` / `seq` / `source` 三列和对应的索引补上（3-05），
> 再给状态变更加上 `row_version` 乐观锁（3-06），然后往里填数据。"

### 二、有一条设计红线，违反了会出大问题

`DeployService.startDeploy()` 上有 `@Transactional(rollbackFor = Exception.class)`，它现在做三件事然后立刻结束：改单据状态、把应用置为"部署中"、写一条流转日志。

**绝对不能把 SSH 执行塞进这个方法里。** 因为 SSH 跑一个脚本可能几十秒到几分钟，如果放在事务里：

- 数据库连接会被一直占着（内网小机器连接池本来就小）
- 更致命的是：事务没提交，单据状态就还是旧值，**别人看到的是"没在部署"**，
  3-06 加的 CAS 乐观锁也形同虚设（没人能并发读到新状态）

正确做法是：**事务里只改状态、立刻提交；然后在事务外另起一个线程去执行 SSH。** 这个模式叫"事务内只做状态流转，事务外做耗时动作"。

---

# 必须做的（16 条）

## 3-01 · 加 SSH 依赖

**改什么**：`springboot/pom.xml`（1 个文件）

**提示词**：
```
在 springboot/pom.xml 的 <dependencies> 里新增 SSH 客户端依赖：

<dependency>
    <groupId>com.hierynomus</groupId>
    <artifactId>sshj</artifactId>
    <version>0.35.0</version>
</dependency>

选它的理由：纯 Java、无外部依赖、API 简单（几行就能开 session 执行命令并实时读输出）。
不要选 JSch（已停止维护）、不要选 spring-boot-starter-integration 的方案（太重）。

要求：
1. 版本号用 0.35.0，这是兼容 Java 8 的版本
2. 加完后跑 mvn dependency:tree | findstr sshj 确认引入成功
3. 不要动其他依赖，不要改 Spring Boot 版本（保持 2.5.9）
4. 不要升级 java.version（保持 1.8）
```

**怎么验证**：
```
cd springboot
mvn dependency:tree
```
输出里能看到 `com.hierynomus:sshj:jar:0.35.0`。同时项目能正常编译（`mvn compile` 通过）。

---

## 3-02 · 给服务器表加 SSH 连接字段

**改什么**：`springboot/src/main/resources/sql/upgrade_server_ssh.sql`（1 个新文件）

**提示词**：
```
新建 springboot/src/main/resources/sql/upgrade_server_ssh.sql。
（沿用项目风格：upgrade_*.sql，手动执行，不是 Flyway 迁移——项目里没有 Flyway。）

背景：entity/Server.java 目前只有 hostname/ip/os/cpu/memory/disk/env/status/remark 等字段，
没有任何 SSH 连接信息。要给它加连接参数。

要求：
1. 参照同目录下 upgrade_fk.sql / upgrade_admin.sql 的写法——它们用
   DROP PROCEDURE + CREATE PROCEDURE + information_schema 判断列/约束是否存在，
   保证可重复执行。请沿用同样的模式：
   - 定义 add_column_if_missing 存储过程
   - 用它逐列添加
   - 末尾 DROP PROCEDURE 清理
   （upgrade_alarm.sql 是更简单的另一种写法：直接 SELECT information_schema.COLUMNS
     判断后 PREPARE/EXECUTE，也可以照它写）
2. 要加的列（表名 server）：
   - ssh_port INT NOT NULL DEFAULT 22 COMMENT 'SSH 端口'
   - ssh_user VARCHAR(64) NULL COMMENT 'SSH 登录用户名'
   - ssh_auth_type VARCHAR(16) NOT NULL DEFAULT 'PASSWORD' COMMENT 'PASSWORD=密码；KEY=私钥'
   - ssh_password VARCHAR(512) NULL COMMENT 'SSH 密码（应用层加密存储，非明文）'
   - ssh_private_key TEXT NULL COMMENT 'SSH 私钥内容（应用层加密存储）'
   - deploy_path VARCHAR(256) NULL COMMENT '应用部署根目录，如 /opt/apps'
3. 不要给这些字段加索引（不会用来查询）
4. 同时在 schema.sql 里同步加上这几列，保证全新安装也能建全
5. 文件头写注释说明：已有库执行本脚本，全新安装用 schema.sql

注意：ssh_password 和 ssh_private_key 存的是**加密后**的值，不是明文。
加密实现在下一步（3-03）。
```

**怎么验证**：
```
（对已有的 manager 库执行这个脚本，然后）
mysql -uroot -p manager -e "DESC server;"
```
应该能看到新增的 6 个字段。**再执行一次脚本**，应该不报错（幂等验证）。

---

## 3-03 · 写凭据加密工具

**改什么**：新建 `springboot/src/main/java/com/example/utils/CredentialCipher.java` + `application.yml`（2 个文件）

**提示词**：
```
新建 springboot/src/main/java/com/example/utils/CredentialCipher.java。

作用：把 SSH 密码/私钥在存库前加密，读出来时解密。避免数据库里躺着明文密码。

要求：
1. 用 AES/GCM 对称加密（Java 8 自带 javax.crypto，不需要加依赖）
2. 密钥从配置读：
   在 application.yml 里加：
   credential:
     key: ${CREDENTIAL_KEY:}
   如果这个值为空，**启动时打印一条 WARN 日志**说明"未配置凭据加密密钥，SSH 密码将以明文存储"，
   但不要阻止启动（本地开发方便）
3. 提供三个方法：
   - String encrypt(String plain)：入参为空返回 null；返回 Base64(iv + cipherText)
   - String decrypt(String cipherText)：入参为空返回 null；解密失败返回 null 并记 WARN 日志
     （不要抛异常——历史明文数据不能被这个搞崩）
   - boolean isEncrypted(String value)：判断是不是加密后的格式（用于兼容历史明文数据）
4. 每次加密用随机 IV，不要把 IV 写死
5. GCM 的 tag 长度用 128

参考项目里已有的 PasswordUtils（springboot/src/main/java/com/example/utils/PasswordUtils.java）
的代码风格——它已经用了 spring-security-crypto，可以看看它怎么写的，保持风格一致。

同时在 application-dev.yml 里加一个开发默认密钥（方便本地跑），
application-prod.yml 里必须是 ${CREDENTIAL_KEY}（无默认值，强制注入）。
```

**怎么验证**：
```
cd springboot
mvn test
```
先写一个最简单的测试（或用 JShell 也行）：加密 "hello123" 得到一串 Base64，再解密回来应该还是 "hello123"。同一个明文加密两次，两次的密文应该**不一样**（因为随机 IV）。

再验证降级：把 `credential.key` 设为空，启动应用，控制台应该有一条 WARN 提示，但应用能正常启动。

---

## 3-04 · 加执行器接口和 SSH 实现

**改什么**：新建 `common/exec/DeployExecutor.java` + `common/exec/SshDeployExecutor.java`（2 个新文件）

**提示词**：
```
新建两个类，实现"连上服务器跑脚本并实时回传输出"。

【第一个】springboot/src/main/java/com/example/common/exec/DeployExecutor.java
接口，定义执行契约：

public interface DeployExecutor {
    /**
     * 在目标机上执行命令，输出逐行回调给 consumer（用于实时回显）
     * @return 退出码，0 表示成功
     */
    int execute(Server target, String command, java.util.function.Consumer<String> lineConsumer)
            throws Exception;

    /** 测试连通性 */
    boolean testConnection(Server target);
}

【第二个】springboot/src/main/java/com/example/common/exec/SshDeployExecutor.java
用 sshj 实现上面这个接口。

要求：
1. 用 @Component 注册为 Bean
2. 连不上/认证失败/命令超时，都要抛出带明确信息的异常（不要吞掉）
3. 逐行读输出：sshj 的 Session 拿到 Command 后，用 command.getInputStream() 包成
   BufferedReader，逐行 readLine()，每读到一行就调 lineConsumer.accept(line)。
   **必须是逐行的**，不能等命令跑完一次性读——否则实时回显就没意义了
4. 超时保护：整个命令执行设置一个总超时（从配置读，默认 300 秒），
   超时就断开连接并抛异常
5. 认证方式：
   - 如果 server.getSshAuthType() 是 "KEY"（且私钥非空）→ 用公钥认证
   - 否则用密码认证，密码要先过 CredentialCipher.decrypt()
   - 用户名取 server.getSshUser()，为空则默认 "root"
   - 端口取 server.getSshPort()，为空则默认 22
6. testConnection() 就是执行一个 "echo ok"，看退出码是不是 0

注意：
- 不要在这个类里做任何数据库操作
- 不要在这个类里加 @Transactional
- sshj 的连接用完必须关闭（用 try-with-resources 或 finally）
```

**怎么验证**：
先写一个临时测试（或者直接在这个类里加个 `main` 方法本地跑）：
```
SshDeployExecutor executor = new SshDeployExecutor();
Server s = new Server();
s.setIp("你的测试机IP");
s.setSshUser("root");
s.setSshPort(22);
s.setSshAuthType("PASSWORD");
s.setSshPassword(CredentialCipher.encrypt("你的密码"));
System.out.println(executor.testConnection(s));
```
返回 `true` 说明通了。再改成 `execute(s, "ls -la /", System.out::println)`，应该在控制台**逐行**看到目录列表（不是卡住几秒后突然全出来）。

**这条的验证不能跳**（初版"没有测试机就先跳过验证"的说法已作废）。没有测试机就回到本文件开头的「开工前置」，先把机器就绪再来验。

---

## 3-05 · 给 `deploy_log` 补 log_type / seq / source 三列

> ⚠️ **本条是 v4 新增的补地基条目**（原方案误以为这三列已经存在）。
> 不做这条，3-12（执行输出落库）、3-13（execLogs 填数据）、4-11（错误 TOP-N）
> 全部执行不了——它们都要写/查 `log_type='EXEC'`、`seq`、`source='AUTO'`，
> 而 `deploy_log` 表现在只有 8 个字段。**必须先做这一条。**

**改什么**：新建 `springboot/src/main/resources/sql/upgrade_deploy_log.sql` + 同步改 `schema.sql` + 改 `DeployLog.java`（1 个新文件 + 2 个改动文件）

**提示词**：
```
deploy_log 现在只有 8 列，装不下执行输出。请补三列。

新建 sql/upgrade_deploy_log.sql（沿用 upgrade_fk.sql / upgrade_admin.sql 的
存储过程 + information_schema 幂等写法，手动执行，不是 Flyway）：
1. 加三列：log_type VARCHAR(16) NOT NULL DEFAULT 'STATE'（STATE 状态流转 / EXEC 执行输出）、
   seq INT NOT NULL DEFAULT 0（EXEC 行序从 1 递增；STATE 固定 0）、
   source VARCHAR(16) NOT NULL DEFAULT 'MANUAL'（MANUAL 人工 / AUTO 系统自动）
2. 加索引 idx_log_deploy_type_seq (deploy_id, log_type, seq)，同样先判断是否已存在
3. 回填历史行：log_type='STATE'、seq=0、source='MANUAL'
4. schema.sql 的 deploy_log 建表语句同步加这三列和索引
5. DeployLog 实体加 logType / seq / source 三个字段
```
**怎么验证**：
```
mysql -uroot -p manager -e "DESC deploy_log;"
```
能看到 log_type / seq / source 三列和 idx_log_deploy_type_seq 索引；
**再执行一次脚本**不报错（幂等）；已有行的 log_type 都是 STATE、seq 都是 0。

---

## 3-06 · 给 `deploy` 加乐观锁，状态变更改走 CAS

> ⚠️ **本条是 v4 新增的补地基条目**（原方案误以为 CAS 已经存在，还写了"一个字都不要改"，
> 实际上仓库里既没有 `row_version` 也没有 `casUpdate()`）。
> **若时间不够可以跳过本条**，但代价是：3-09 的并发安全、3-19 的并发互斥失去数据库层支撑，
> 只能靠 3-19 的应用层判断兜底。

**改什么**：新建 `springboot/src/main/resources/sql/upgrade_deploy_version.sql` + 改 `DeployMapper.xml` + 改 `DeployService.java`（1 个新文件 + 2 个改动文件；SQL 与 Java 分两步做，先跑脚本再改代码）

**提示词**：
```
现在状态变更走 deployMapper.updateById(updateDeploy)，没有并发保护。请加乐观锁。

1. 新建 sql/upgrade_deploy_version.sql（幂等写法同 3-05）：给 deploy 表加
   row_version INT NOT NULL DEFAULT 0；schema.sql 同步加；Deploy 实体加 rowVersion
   （不能叫 version——deploy 已有业务字段 version 表示发布版本号）
2. DeployMapper.xml 新增 casUpdateStatus，where 必须带
   id + status(期望状态) + row_version 三个条件，set 里带 row_version = row_version + 1；
   DeployMapper.java 加方法声明
3. startDeploy / finishDeploy / rollback 三处状态变更改调它，更新到 0 行即判冲突
4. 冲突抛错，错误码新增 6036（先读 ResultCodeEnum，当前最大码是 6027；
   6034 给第 1 步"系统繁忙"、6035 给 3-19 并发互斥，别撞号）
5. updateById 仍用于非状态字段的更新，不要全替换
```
**怎么验证**：
1. 对已有库执行脚本，`DESC deploy;` 能看到 row_version；重复执行不报错
2. 手工把某单的 row_version 改成 999，再在页面上点「开始部署」——
   应返回并发冲突提示（不是 500），单据状态不变
3. 正常流程（提交 → 审批 → 开始部署 → 完成）全绿，`mvn test` 不红

---

## 3-07 · 加执行任务表

**改什么**：`springboot/src/main/resources/sql/upgrade_deploy_execution.sql`（1 个新文件）

**提示词**：
```
新建 springboot/src/main/resources/sql/upgrade_deploy_execution.sql。
（沿用项目风格：upgrade_*.sql，手动执行，不是 Flyway 迁移。）

要记录"一次部署的实际执行过程"，需要一张表来存执行状态。

要求：
1. 沿用同目录 upgrade_*.sql 的幂等写法（information_schema 判断 + CREATE TABLE IF NOT EXISTS）
2. 建表 deploy_execution，字段：
   - id INT AUTO_INCREMENT PRIMARY KEY
   - deploy_id INT NOT NULL COMMENT '关联部署单'
   - status VARCHAR(16) NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED/TIMEOUT'
   - command TEXT NULL COMMENT '实际执行的命令（脱敏后）'
   - exit_code INT NULL COMMENT '退出码'
   - started_at DATETIME NULL
   - finished_at DATETIME NULL
   - duration_ms BIGINT NULL
   - retry_count INT NOT NULL DEFAULT 0
   - error_message VARCHAR(512) NULL
   - create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
   - deleted TINYINT NOT NULL DEFAULT 0
3. 索引：
   - idx_exec_deploy (deploy_id)
   - idx_exec_status (status)
4. 同时在 schema.sql 里同步加上这张表

注意：这张表是一对多（一个部署单可能重试多次执行），不是一对一。
```

**怎么验证**：
对已有库执行脚本，`DESC deploy_execution;` 能看到全部字段和两个索引。重复执行不报错。

---

## 3-08 · 加一个部署执行专用线程池

**改什么**：新建 `springboot/src/main/java/com/example/common/config/DeployExecutorConfig.java`（1 个新文件）

**提示词**：
```
部署执行要在后台线程里跑（不能占着请求线程，也不能占着数据库事务）。
先把这个线程池建起来。

新建 springboot/src/main/java/com/example/common/config/DeployExecutorConfig.java：

1. 定义一个 ThreadPoolTaskExecutor Bean，名字 "deployExecutor"：
   - 核心线程数 2
   - 最大线程数 4
   - 队列容量 50
   - 线程名前缀 "deploy-exec-"
   - 拒绝策略 CallerRunsPolicy（队列满了由调用线程自己跑，不要静默丢弃任务）
   - 设置 waitForTasksToCompleteOnShutdown(true)，
     awaitTerminationSeconds(30)——优雅关闭，别让正在跑的部署被硬砍

2. **不要用 Executors.newFixedThreadPool 裸写**。
   Spring 管理的线程池才能在应用关闭时优雅收尾，而且能统一配线程名
   （线程名很重要——线上出问题时，日志里要能一眼看出是部署线程打的）

3. 参考项目里已有的配置类风格（common/config/ 下有 WebConfig、CorsConfig）

4. 从配置读参数，给个默认值：
   deploy.execution.pool-size（默认 2）
   deploy.execution.queue-capacity（默认 50）

做完这条，第 3-09 条会用到这个线程池。
```

**怎么验证**：
启动应用，能正常起来（没有 Bean 定义冲突）。在任意一个 Service 里临时注入 `@Resource(name = "deployExecutor") ThreadPoolTaskExecutor` 并打印它的 corePoolSize，确认是 2。验完删掉临时代码。

---

## 3-09 · 把执行动作移出事务

**改什么**：新建 `springboot/src/main/java/com/example/service/DeployExecutionService.java` + 修改 `DeployService.java`（2 个文件）

> ⚠️ **这是整个第 3 步最需要小心的一条。** 请先读完这条的全部内容再动手。
>
> **前置**：这条会往 `deploy_execution` 表插记录，所以 **`3-07`（建表）必须先做完**。
> 如果 `3-07` 还没做，先回去做。

**提示词**：
```
背景：DeployService.startDeploy() 现在是：
    @Transactional(rollbackFor = Exception.class)
    public void startDeploy(Integer id) {
        Account currentUser = AuthUtils.requireRole(..., RoleEnum.OPS);
        ... 状态更新到 DEPLOYING（3-06 后走 CAS）、置应用部署中、saveLog ...
    }

要改造它：在事务提交**之后**，另起线程去真正执行部署。

⚠️ 动手前先做一件事：完整读一遍 finishDeploy 的实现，
确认它（和它调用的 saveLog）内部有没有用 AuthUtils / ThreadLocal 取当前用户。
异步线程里没有请求上下文，直接依赖会 NPE，执行结果落不了库、单据卡死。
凡涉及当前用户名的：发起时先保存 String operator = currentUser.getUsername()，
闭包捕获传给异步任务，显式传参；EXEC 日志 source=AUTO 本就不该依赖登录态。
把核实结论写在回报里。

要求（技术约束，必须严格遵守）：
1. 新增一个**无 @Transactional** 的入口方法做编排：
   public void startDeployEntry(Integer id)
   它做三步：
   ① 调事务方法 startDeploy(id)（状态流转，保持现有逻辑不变）
   ② startDeploy 返回后（此时事务已提交），打一行日志
   ③ 把 executeDeployAsync(deployId, operator) 提交给 3-08 建好的 deployExecutor 线程池
   **绝不能在 @Transactional 方法内部 submit**——工作线程可能在事务提交前
   就开始跑，读到旧状态；更坏的是提交后事务回滚，异步任务在给一个
   从未进入 DEPLOYING 的单据做真实部署。
2. startDeployEntry 调 startDeploy 是同类自调用，@Transactional 会失效
   （Spring AOP 自调用不走代理）。解法二选一：
   - 注入自身代理（@Lazy @Resource DeployService self），用 self.startDeploy(id)
   - 或把事务方法拆到独立的 DeployExecutionService
   推荐拆独立 Service——下面的 beginExecution/completeExecution 本来也要拆过去。
3. 在 DeployExecutionService 里新增两个 REQUIRES_NEW 事务方法：
   - beginExecution(Integer deployId)：
     插入一条 deploy_execution 记录（status=RUNNING、started_at=now）
   - completeExecution(Integer deployId, boolean success, int exitCode,
                       String errorMessage)：
     更新 deploy_execution 终态（status/finished_at/duration_ms/exit_code/error_message），
     并调用已有的 finishDeploy(deployId, result) 完成状态流转
     （REQUIRES_NEW 是为了让执行结果独立落库，不被外层事务牵连）
4. 新增执行编排方法（**不加 @Transactional**）：
   public void executeDeployAsync(Integer deployId, String operator)
   做三件事：beginExecution → DeployExecutor.execute → completeExecution
   全程 try-catch，任何异常都转成 failed 落库，**不能让它静默丢失**
5. **状态变更必须走 3-06 新增的 `casUpdateStatus`**（id + 期望 status + row_version
   三重条件），不要退回裸 `deployMapper.updateById` 改状态
   （3-06 之前项目里没有乐观锁，本条之后才有了，别再按"没有 CAS"的写法补代码）
6. 现有 approve / finishDeploy / rollback 三个方法的行为不要改变
7. 线程池用 3-08 建好的那个，不要重复新建
```

**怎么验证**：
1. **核心验收——submit 发生在事务提交之后**：
   - 在 `startDeployEntry` 里，事务方法返回后打 `log.info("事务已提交，准备提交异步执行: {}", id)`
   - 在 submit 之后再打 `log.info("异步任务已提交: {}", id)`
   - 点「开始部署」，确认这两行**依次**出现在 startDeploy 的所有 SQL 日志之后。
   - 注意：初版"看状态更新日志和执行线程日志的先后"是**假验收**——
     那只能证明方法内语句顺序，证明不了事务已提交。本条只认入口方法里的日志。
2. **异常路径**：不配 SSH 的情况下（目标服务器信息为空）点「开始部署」，
   部署单应落 FAILED 且 error_message 明确（如"目标服务器未配置 SSH"），
   不允许 500，更不允许悬挂在 DEPLOYING。
3. 现有测试必须全绿：
```
cd springboot
mvn test
```
`DeployStatusMachineTest` 和 `DeployServiceStatusTest` 一个都不能红。

---

## 3-10 · 启动恢复：把悬挂的执行判定为失败

**改什么**：`DeployExecutionService`（1 个文件，追加方法）

**提示词**：
```
应用进程重启（崩溃、OOM、断电，或超过优雅关闭等待时间被硬杀）时，
在途的 SSH 执行会被硬砍：deploy 单永久卡在 DEPLOYING、
deploy_execution 永久卡在 RUNNING。而且状态机不能从 DEPLOYING 重入（3-06 加了 CAS 后同样如此），
人工在页面上也推不动——这条单据就死了。超时终止（3-18）管不了这个，
它只管"命令跑太久"，管不了"进程死了"。

请加一个启动恢复逻辑：
1. 在 DeployExecutionService 里新增方法，加注解
   @EventListener(ApplicationReadyEvent.class)，应用启动完成后执行一次
2. 查 deploy_execution WHERE status='RUNNING' AND deleted=0 的全部记录
3. 每条改为 FAILED，error_message 填"系统重启导致执行中断"，finished_at 填当前时间
4. 对应的 deploy 单同步推 FAILED——复用 3-09 的 completeExecution 路径，
   不要新写一套状态流转
5. 处理了几条就记一行启动日志；一条都没有就记"无悬挂执行，跳过"
6. 不要做成定时任务——这是启动一次性恢复，只在 ApplicationReadyEvent 触发

说明：毕设场景"重启即判失败 + 可重新发起"就够了，不做断点续传。
答辩可以讲"执行器与状态机的故障一致性设计"。
```

**怎么验证**：
1. 把 deploy.sh 临时加一行 `sleep 60`（set -e 之后），发起一次部署
2. 执行进行中直接杀掉应用进程（`taskkill` 或关终端）
3. 查库确认此时 deploy 是 DEPLOYING、deploy_execution 是 RUNNING
4. 重启应用，等启动完成
5. 查库：execution 应变成 FAILED、error_message 是"系统重启导致执行中断"，
   deploy 单也变成 FAILED，且能**重新发起**部署（验证状态机复活了）
6. 验完把 `sleep 60` 删掉

---

## 3-11 · 让部署真执行一条脚本（含启动应用）

**改什么**：`DeployService` 的 `executeDeployAsync` + 新建脚本模板文件（2 个文件）

**提示词**：
```
现在 DeployExecutor 已经能连服务器执行命令，但还没有"要执行什么"。

要求：
1. 新建脚本模板目录：springboot/src/main/resources/deploy-scripts/
   新建 deploy.sh（用 UTF-8，LF 换行，不要 CRLF——Linux 上 CRLF 会导致脚本报 "bad interpreter"）：

#!/bin/bash
set -e
APP_NAME="${APP_NAME}"
VERSION="${VERSION}"
DEPLOY_PATH="${DEPLOY_PATH}"
PACKAGE="${PACKAGE}"

echo "[deploy] 开始部署 $APP_NAME $VERSION"
echo "[deploy] 目标目录: $DEPLOY_PATH"
if [ ! -f "$PACKAGE" ]; then
    echo "[deploy] 错误：安装包不存在 $PACKAGE"
    exit 1
fi
mkdir -p "$DEPLOY_PATH/releases/$VERSION"
cp "$PACKAGE" "$DEPLOY_PATH/releases/$VERSION/"
ln -sfn "$DEPLOY_PATH/releases/$VERSION" "$DEPLOY_PATH/current"
echo "[deploy] 部署完成，当前版本指向 $VERSION"

# 幂等启动：部署完应用要真的跑起来，否则"自动部署"只是把文件挪了个目录。
# 首选 docker compose up -d——演示目标应用也容器化，与第 2 步的技能复用，
# 天然幂等（重复执行结果一致，不破坏 3-17 重试的前提）。
if [ -f "$DEPLOY_PATH/current/docker-compose.yml" ]; then
    echo "[deploy] 检测到 compose 文件，幂等拉起应用"
    docker compose -f "$DEPLOY_PATH/current/docker-compose.yml" up -d
else
    echo "[deploy] 未检测到 compose 文件，跳过启动步骤"
fi
echo "[deploy] 全部完成"
exit 0

   如果演示目标不便容器化，退一档用 systemctl restart 或 nohup java -jar，
   但要在答辩时说明：部署目标的应用进程管理（健康发布/就绪探针）不在本轮范围。

2. 在 DeployService 里实现"组装命令"：
   - 用 ClassPathResource 读 deploy.sh 模板内容，
     **读完后统一 content.replace("\r\n", "\n") 再使用**——这是 CRLF 的运行时兜底，
     与 1-09 的 .gitattributes、file 命令验证构成三道保险，git 配置怎么折腾都不怕
   - 把 APP_NAME / VERSION / DEPLOY_PATH / PACKAGE 四个变量用实际值替换
   - 变量值必须做**转义**：只允许字母数字和 . _ - / ，其他字符一律拒绝并抛异常
     （这是防命令注入——版本号是用户填的，不能直接拼进 shell 命令）
   - 如果目标服务器的 deploy_path 为空，默认用 /opt/apps/{应用名}
   - 实际执行的命令是：把模板内容通过 heredoc 喂给远端 bash，
     形如 bash -s <<'EOF' ... EOF（用引号包 EOF 防止本地变量展开）

3. 把组装好的命令传给 SshDeployExecutor.execute()，
   每一行输出通过 lineConsumer 落库（下一步做）

4. 本步骤先不接实时日志，只要用 log.info 把输出打到服务端控制台，
   确认命令真的发出去了、有回显

注意：不要用 String.format 拼命令字符串后直接执行。必须走上面的 heredoc 方式，
或者把脚本先 scp 到远端再执行。前者更简单，用前者。
```

**怎么验证**：
1. 准备一台测试机，在服务器表里配好它的 SSH 信息和 deploy_path（比如 `/opt/apps/test-app`）
2. 在测试机上先建好 `/opt/apps/test-app/packages/app-1.0.0.jar` 这个文件（随便建个空的）——
   这一步是过渡手段，**3-15 做完后制品由系统上传，就不用手工放了**
3. 在页面上发起一次部署到这个服务器，点「开始部署」
4. **看服务端控制台**，应该能看到逐行的 `[deploy] ...` 输出
5. **登到测试机上验证**：`ls -l /opt/apps/test-app/`，应该看到 `releases/1.0.0/` 目录和 `current` 软链；
   如果 current 下放了 docker-compose.yml，还应看到应用容器被拉起（`docker compose -f ... ps`）
6. 再试一个不存在的包路径，应该看到"安装包不存在"并 exit 1
7. 用 Git Bash 执行 `file springboot/src/main/resources/deploy-scripts/deploy.sh`，
   确认是 LF、无 BOM（1-09 的第三道验证）

---

## 3-12 · 把执行输出存进日志表

**改什么**：新建 `DeployLogService` 的写入方法 + `DeployLogMapper.xml`（2 个文件）

**提示词**：
```
现在执行输出只打到控制台（log.info），要改成存进 deploy_log 表。

前提：**3-05（给 deploy_log 补 log_type / seq / source 三列）必须先做完**，
否则下面这些列根本不存在。
- log_type = 'EXEC'（DeployLogTypeEnum.EXEC 枚举值早已定义，列由 3-05 补）
- seq = 行序号（列由 3-05 补；EXEC 行序从 1 递增，STATE 固定 0）
- source = 'AUTO'（DeployLogSourceEnum.AUTO 枚举值早已定义，列由 3-05 补）
- action = 'EXEC'（action 字段本来就有，注释枚举里已列了这个值）
- detail = 输出行内容（字段本来就有）

要求：
1. 在 DeployLogMapper.java 和 DeployLogMapper.xml 里新增一个批量插入方法：
   int insertBatch(List<DeployLog> logs)
   用 foreach 批量插入，一次插入多行（不要一行一条 insert，输出多的时候会打爆数据库）

2. 在 DeployExecutionService（3-09 里新建的那个）里加写入逻辑：
   - 执行过程中收集 lineConsumer 收到的每一行，**先攒在内存队列里**
   - 每攒够 50 行，或距上次落库超过 1 秒，就批量 insert 一次
     （不要每来一行就 insert 一次——一条命令可能有上千行输出）
   - 执行结束时，把剩余不足 50 行的部分也落库
   - seq 从 1 开始递增，跨批次连续（不要每批都从 1 开始）

3. **不要**在 DeployService 的 saveLog 里改逻辑 —— 那个方法写的是 STATE 类型日志，
   是给状态流转用的，和执行输出是两回事。请**另开**一个方法写 EXEC 日志。

4. DeployLogController 的类注释写明「记录只由 Deploy 模块内部写入，这里只提供查询」——
   请遵守这个约定，**不要在 DeployLogController 里加任何写入接口**。

5. 输出行要做长度截断：单行超过 1000 字符就截断并加省略号
   （防止某条命令输出一个超长字符串把 detail 字段撑爆）
```

**怎么验证**：
执行一次部署，然后查库：
```
mysql -uroot -p manager -e "SELECT seq, action, log_type, source, LEFT(detail,60) FROM deploy_log WHERE deploy_id = 你的部署单ID ORDER BY seq;"
```
应该看到：STATE 记录在前（seq=0），EXEC 记录 seq 从 1 开始连续递增，没有断号。执行一次输出很多行的命令（比如 `ls -laR /`），验证 seq 是连续的。

---

## 3-13 · 新增执行日志明细查询（并让 execSummary 返回真数据）

> 说明：`DeployService` 里**没有** `execLogs()` 方法，`dto/` 下也**没有** `DeployLogLinesVO`
> （旧稿说它们"已预留"，实际不存在）。所以这条是**新建**方法 + 新建 VO + 新建端点。

**改什么**：`DeployService.java` + 新建 `dto/DeployLogLinesVO.java` + `DeployController.java`（3 个文件）

**提示词**：
```
要能按游标翻页查某个部署单的执行输出明细。

要求：
1. 新建 dto/DeployLogLinesVO.java，字段：
   List<String> lines / boolean hasMore / Integer nextCursor，
   带一个 static empty() 工厂（hasMore=false、nextCursor=null）
2. 在 DeployService 里新增：
   public DeployLogLinesVO execLogs(Integer id, Integer cursor)
   - 第一行调 getExistDeploy(id)（校验部署单存在，不存在就抛异常，不要返回空数组）
   - cursor 为 null 时按 0 处理
3. 查询逻辑：
   - 在 DeployLogMapper 里新增方法：按 deploy_id + log_type='EXEC' + seq > cursor 查询，
     按 seq 升序，limit 取 pageSize+1 条（多取一条用来判断还有没有下一页）
   - pageSize 固定用 200（执行输出看的是滚动窗口，不需要用户选页大小）
   - 如果取回的是 pageSize+1 条，说明还有更多：hasMore=true，只返回前 pageSize 条，
     nextCursor 设为第 pageSize 条的 seq
   - 如果不足 pageSize+1 条：hasMore=false，nextCursor=null
4. 查询逻辑：
   - 在 DeployLogMapper 里新增方法：按 deploy_id + log_type='EXEC' + seq > cursor 查询，
     按 seq 升序，limit 取 pageSize+1 条（多取一条用来判断还有没有下一页）
   - pageSize 固定用 200（执行输出看的是滚动窗口，不需要用户选页大小）
   - 如果取回的是 pageSize+1 条，说明还有更多：hasMore=true，只返回前 pageSize 条，
     nextCursor 设为第 pageSize 条的 seq
   - 如果不足 pageSize+1 条：hasMore=false，nextCursor=null
5. **索引由 3-05 新建**：`upgrade_deploy_log.sql` 里建的
   idx_log_deploy_type_seq (deploy_id, log_type, seq)
   查询条件正好对应这个索引。**确认 3-05 已执行**，
   **不要再新建同名索引，也不要改这个索引**
6. 在 DeployController 里加两个端点（现有端点的路径一个都不要改）：
   GET /deploy/{id}/logs（可选参数 @RequestParam(required = false) Integer cursor）
   GET /deploy/{id}/execSummary
   现有 Controller 只有 add / deleteById / deleteBatch / updateById / selectById /
   selectAll / selectPage / approve / startDeploy / finishDeploy / rollback，
   logs 和 execSummary 都是新增，不要以为它们已经存在

另外检查 DeployExecSummaryVO（dto/ 下，这个类**确实存在**）——它的类注释写
「本轮恒空，字段结构先定死，接执行引擎时只填数据」，有 totalLines/status/head/tail
四个字段。请顺便实现它（先读一遍确认真实字段名，以代码为准）：
- totalLines：该部署单的 EXEC 日志总条数
- status：从 deploy_execution 表取最近一条的 status
- head：前 10 行输出
- tail：后 10 行输出
```

**怎么验证**：
1. 执行一次部署后，调接口验证：
```
curl "http://localhost:9090/deploy/1/logs" -H "token: 你的token"
```
应该返回真实的行数据，不再是空数组

2. 翻页验证：如果输出超过 200 行，第一次返回 `hasMore: true` 和非空 `nextCursor`，
   带上 cursor 再请求，应该拿到后续的数据，两次的 seq **不重叠**

3. 不存在的部署单：应该返回部署单不存在的错误（不是空数组）

---

## 3-14 · 前端加执行日志查看入口

**改什么**：`vue/src/views/manager/Deploy.vue` + 可能的 api 定义文件（2 个文件）

**提示词**：
```
vue/src/views/manager/Deploy.vue 已经是一个功能完整的页面（搜索栏、关联筛选、表格、
跨页跳转、canDev(user) 角色判断）。

要在它上面加一个"查看执行日志"的入口，**不要新建页面**。

**先做这一步**：把这个文件完整读一遍，确认以下事实（不要凭猜测写代码）：
- 跨页跳转用的是哪个方法？注意：方法名不是 goAlarms/goApp/goServer 这类，
  实际应该是一个统一的 go(path, query) 方法，请以文件里的真实方法名为准
- 表格的操作列现在有哪些按钮
- 部署状态字段叫什么、状态值有哪些（可能是 status，值是 PENDING/APPROVED/
  DEPLOYING/SUCCESS/FAILED/REJECTED 这类，以文件里的真实值为准）
- api 请求是怎么发的，定义在哪个文件

要求：
1. 在表格的操作列加一个按钮/链接「执行日志」，
   只在状态是 DEPLOYING / SUCCESS / FAILED 时显示（PENDING/APPROVED 还没有执行记录）
2. 点击后打开一个 el-drawer（抽屉，从右侧滑出，宽度 60%），标题是"执行日志 - {部署单号}"
3. 抽屉内容：
   - 顶部一行信息：状态、开始时间、耗时（从 execSummary 接口取）
   - 中间一个深色背景的滚动区域显示日志行，等宽字体，每行前面显示 seq 序号
   - 日志区域自动滚到底部（有新行时）
   - 底部一个「刷新」按钮
4. 数据来源：
   - 调 GET /deploy/{id}/logs 拉取明细（先读 vue/src 下的 api 定义文件，
     看现有接口是怎么定义的，按同样风格加）
   - 调 GET /deploy/{id}/execSummary 拉取摘要信息
5. 如果 hasMore 是 true，显示一个「加载更多」按钮（先不做自动轮询，手动刷新即可）
6. 样式：日志区域用深色底（#1e1e1e）、浅色字（#d4d4d4）、字号 12px、等宽字体，
   这样看起来像终端，演示效果好
7. 不要改动这个页面已有的搜索、表格、跳转逻辑

**⚠️ 关于分页参数的坑**：
这个页面现有的加载方法是类似 load(pageNum = 1) 的形式，
依赖"不传页码时后端返回第一页"这个默认行为。

所以：
- 你新加的"执行日志"抽屉**不要改**现有的 load() 方法签名
- 日志抽屉的翻页用自己独立的方法和参数（cursor），不要复用 load()
- 如果非要动 load()，必须保证不传参时行为完全不变

注意：Vue 2 语法（不是 Vue 3）。Element-UI 2.x 组件（不是 element-plus）。
```

**怎么验证**：
1. `npm run serve` 启动前端
2. 走一遍完整流程：提交部署单 → 审批 → 开始部署
3. 部署完成后，点「执行日志」，抽屉打开，能看到真实的执行输出行，带序号
4. 顶部能看到状态和耗时
5. 部署中的单据点开，也应有日志（可能是部分输出）
6. **回归验证**：点几次部署列表的「查询」按钮、翻页、点关联筛选，
   确认这些**原有功能没被改坏**（这是最容易被顺手改坏的地方）
7. 打开浏览器控制台，确认没有报错

---

## 3-15 · 制品管理：上传 → scp → MD5 校验

**改什么**：新建 `upgrade_deploy_artifact.sql` + `DeployArtifact` 实体/Mapper/Service + 改 `DeployService`（2 类文件）

> 为什么这条是必做：没有它，部署用的制品要靠人事先手工放到目标机上，
> 系统根本不知道制品从哪来——"自动部署"就是个假闭环，答辩一问就破。
> 这是评审判定的结构性缺口，必须补上（最小版）。

**提示词**：
```
现在部署用的安装包靠人事先手工放到目标机上。请做制品管理的最小闭环：
系统内上传制品 → 部署时自动 scp 到目标机 → 远端 MD5 校验。

要求：
1. 新建 springboot/src/main/resources/sql/upgrade_deploy_artifact.sql
   （沿用 upgrade_*.sql 的幂等风格，手动执行，不是 Flyway 迁移），
   建表 deploy_artifact：
   - id, app_id, version, file_name, file_path, file_size, md5, uploaded_by,
     create_time, deleted
   - 索引：idx_artifact_app_version (app_id, version)
   同时在 schema.sql 里同步建这张表
2. 新建实体 DeployArtifact、Mapper、Service
3. 制品上传**复用**已有的 FileController（POST /files/upload，
   已有大小、扩展名、magic number 校验），不要新写一套上传。
   上传成功后把 app_id、version、文件 MD5 等写入 deploy_artifact 表
4. 部署执行时（接进 3-11 的 executeDeployAsync 流程）：
   - 按 app_id + version 从 deploy_artifact 找制品；找不到就让执行 FAILED，
     error_message 写明"未找到版本 {version} 的制品，请先上传"
   - 找到了先用 scp 把制品传到目标机的 $DEPLOY_PATH/packages/ 下
   - 传完在远端执行 md5sum 与入库值比对，不一致就 FAILED（传输完整性校验）
   - 校验通过后再执行 deploy.sh
5. **不要改前端**：部署单的版本号继续手填，不做制品下拉联动——
   系统按 app_id + version 匹配制品即可。这是最小版，前端联动砍掉了
```

**怎么验证**：
1. 上传一个 jar 作为应用 A 的 v1.2.0 制品，查 deploy_artifact 表：有记录、md5 非空
2. 对应用 A 发起 v1.2.0 的部署，执行完成后登到目标机：
   `ls -l $DEPLOY_PATH/packages/` 应看到这个 jar，`md5sum` 结果与库里一致
3. 发起一个**没上传过制品**的版本号，部署单应落 FAILED，错误信息明确

---

## 3-16 · 回滚也走真实执行

**改什么**：`DeployService.rollback` + 新建 `rollback.sh`（2 个文件）

> 为什么这条是必做：只做前 13 条的话，rollback 依然只是改状态 + 人工 finishDeploy，
> 与改造前没有区别。总览卖点表里"回滚是真的回滚"必须有条目支撑。

**提示词**：
```
现在 rollback() 只是改状态（把状态改到 DEPLOYING，patch 里设 deployType=ROLLBACK
和 rollbackVersion），然后等人工调 finishDeploy。要让它也真的去执行回滚。

要求：
1. 先读现有的 rollback() 方法（DeployService 约第 159-180 行，全文件约 280 行），
   理解它现在的流程：校验 STATUS → 校验版本合法性 → 状态改到 DEPLOYING
   （3-06 后这一步走 casUpdateStatus）→ 置应用为部署中 → saveLog
2. 在方法最后挂上异步执行，**复用 3-09 的入口模式**：
   加一个无事务的入口方法（rollbackEntry），事务方法返回（即提交）后再 submit
   到 deployExecutor 线程池——不要在 @Transactional 方法内部提交任务，
   原因和 3-09 完全一样。operator 同样显式传参
   - 回滚执行的是另一个脚本 rollback.sh（新建在 deploy-scripts/ 下）
   - 它做的事：把 $DEPLOY_PATH/current 软链指回目标版本目录
   - **不要改动 rollback() 里已有的版本合法性校验**（那道防线是好的）
   - **不要绕过 3-06 的 casUpdateStatus**——状态变更继续走它，不要退回裸 updateById
3. 回滚脚本模板（UTF-8、LF；Java 读取时同样做 \r\n → \n 归一化，同 3-11）：
#!/bin/bash
set -e
DEPLOY_PATH="${DEPLOY_PATH}"
TARGET_VERSION="${TARGET_VERSION}"
echo "[rollback] 开始回滚至 $TARGET_VERSION"
if [ ! -d "$DEPLOY_PATH/releases/$TARGET_VERSION" ]; then
    echo "[rollback] 错误：目标版本目录不存在 $TARGET_VERSION"
    exit 1
fi
ln -sfn "$DEPLOY_PATH/releases/$TARGET_VERSION" "$DEPLOY_PATH/current"
echo "[rollback] 回滚完成，当前版本 $TARGET_VERSION"
exit 0
4. 回滚执行同样要落 EXEC 日志（复用 3-12 的逻辑）

注意：回滚的 finishDeploy 成功分支会把应用的版本反写成 rollback_version
（现有代码已经处理了，不要改），你只需要把"真实执行"接上。
```

**怎么验证**：
1. 先部署 v1.0.0 成功（在目标机上确认 current 指向 1.0.0）
2. 再部署 v1.1.0 成功（current 指向 1.1.0）
3. 对 v1.1.0 这次部署发起回滚，目标版本选 v1.0.0
4. 执行完成后，**登到目标机上** `ls -l /opt/apps/{应用}/`，current 软链应该指回 `releases/1.0.0`
5. 应用详情页的版本应该显示 v1.0.0
6. 执行日志抽屉里能看到 `[rollback]` 开头的输出

---

# 有余力再做（4 条）

> 上面 16 条必须项做完，自动部署这条链路就是**完整且可演示**的了。
> 下面 4 条是"更像真实生产"的增强，**做不做都不影响答辩**。建议按顺序做，做到哪算哪；时间只剩一处的话优先 3-18（超时兜底）。

## 3-17 · 执行失败自动重试

**改什么**：`DeployExecutionService`（1 个文件）

**提示词**：
```
现在执行失败就直接标记 FAILED。加上重试。

要求：
1. deploy_execution 表已有 retry_count 字段，用它记重试次数
2. 执行失败（退出码非 0 或抛异常）时，如果 retry_count < 2，自动重试：
   - retry_count + 1
   - 等 5 秒（不要用 Thread.sleep 阻塞线程池线程，用 ScheduledExecutorService 延迟调度）
   - 重试前把上一次的输出保留（不要清掉，日志要完整）
   - 新增一条日志行说明"第 N 次重试"
3. 超过 2 次仍失败，才最终标记 FAILED 并走 finishDeploy 失败分支
4. 从配置读重试次数上限：deploy.execution.max-retry，默认 2

注意：重试的必须是**幂等**的操作。3-11 的 deploy.sh 用 cp + ln -sfn 覆盖、
启动用 compose up -d，重复执行结果都一致，所以可以重试。
如果将来改成 systemctl 这类不幂等的启动方式，需要重新考虑。
```

**怎么验证**：
故意让部署失败（指向一个不存在的包路径），观察：
- 服务端日志应该出现 3 次尝试（第 1 次 + 2 次重试），每次间隔约 5 秒
- deploy_execution 表里 retry_count = 2
- 最终状态是 FAILED
- 日志里有"第 1 次重试"、"第 2 次重试"的说明行

---

## 3-18 · 执行超时可终止

**改什么**：`SshDeployExecutor`（1 个文件）

**提示词**：
```
SshDeployExecutor 现在有总超时保护（超时抛异常），但**没有真正终止远端进程**——
断开 SSH 连接后，远端脚本可能还在跑。

要求：
1. 超时后，除了断开连接，还要尽力终止远端进程：
   - 执行命令时记录远端 PID（可以在脚本开头 echo $$ 获取）
   - 超时后新开一个连接，执行 kill -TERM <pid>
   - 等 5 秒，如果进程还在，执行 kill -KILL <pid>
2. 把超时事件的 status 记为 TIMEOUT（不是 FAILED），
   在 error_message 里写明"执行超时，已尝试终止"
3. 超时时间可配置：deploy.execution.timeout-seconds，默认 300
4. 如果终止失败（连接不上等），记 WARN 日志即可，不要因此抛出新的异常
   （原本就是超时失败，不要用另一个异常覆盖掉原因）
```

**怎么验证**：
**先制造一个会超时的场景**——deploy.sh 本身跑得很快，不会超时，
所以要临时在脚本里加一个长命令：

1. 编辑 `springboot/src/main/resources/deploy-scripts/deploy.sh`，
   在开头加一行 `sleep 60`（放在 set -e 之后、任何 echo 之前）
2. 把超时时间临时改成 5 秒（`deploy.execution.timeout-seconds: 5`）
3. 走一次部署，观察：
   - 5 秒后执行结束，状态是 TIMEOUT（不是 FAILED）
   - 登到目标机上执行 `ps aux | grep sleep`，应该**看不到**那个 sleep 进程了
     （说明真的杀掉了，不只是断开连接）
   - deploy_execution 表的 error_message 里有超时说明
4. **验证完做两件事**：
   - 把 deploy.sh 里的 `sleep 60` 删掉
   - 把 timeout-seconds 配置删掉（回到默认 300 秒）

---

## 3-19 · 执行并发互斥

**改什么**：`DeployExecutionService`（1 个文件）

**提示词**：
```
现在同一个应用如果被连续点两次"开始部署"，可能会起两个执行线程同时往同一台机器上部署。

要求：
1. 在 executeDeployAsync 开头加一个互斥判断：
   - 查 deploy_execution 表，看该 deploy_id 或该 app_id 是否有 status='RUNNING' 的记录
   - 有就拒绝，抛异常，错误码新增 6035（ResultCodeEnum 里加，
     提示语"该应用有正在进行的部署，请等待完成"）
     **注意：6034 已被第 1 步的"系统繁忙"占用、6036 已被 3-06 的并发冲突占用，
     这里必须用 6035。新增前先读一遍 ResultCodeEnum 确认当前最大码。**
   - 注意用数据库层面的判断（SELECT ... 或唯一索引），不要只用 Java 内存锁
     （单机部署下内存锁也行，但数据库判断更可靠）
2. 更稳妥的做法：给 deploy_execution 加一个"进行中"的部分唯一索引。
   MySQL 不支持部分索引，可以改用：加一个 running_app_id 字段，
   status=RUNNING 时填 app_id，否则填 NULL，然后给这个字段加唯一索引
3. 加完后要保证：一个应用同时只能有一个 RUNNING 的执行
```

**怎么验证**：
1. 造一个执行很慢的场景（把 deploy.sh 改成 sleep 30 再继续）
2. 用两个浏览器窗口（或两个账号），同时点同一个应用的两个不同部署单的「开始部署」
3. 第二个应该被拒绝，提示"该应用有正在进行的部署"
4. 第一个正常完成
5. 等第一个完成后，第二个重新点，应该能正常执行

---

## 3-20 · 执行日志自动刷新

**改什么**：`vue/src/views/manager/Deploy.vue`（1 个文件）

**提示词**：
```
现在日志抽屉要手动点「刷新」。改成部署中自动刷新。

要求：
1. 抽屉打开时，如果部署单状态是 DEPLOYING，启动一个定时器每 2 秒拉一次最新日志
2. 只拉新增的部分（用 nextCursor / 或记住最大的 seq，请求 seq > lastSeq 的行）
   —— 不要每次拉全量，输出多的时候会卡
3. 状态变成 SUCCESS / FAILED 时，停止定时器，做最后一次全量刷新
4. 抽屉关闭时**必须**清除定时器（在 beforeDestroy 或抽屉的 close 事件里），
   否则会内存泄漏 + 后台一直请求
5. 新行到达时，如果用户没有手动往上滚，自动滚到底部；
   如果用户正在往上翻看历史，不要强制滚下去（判断滚动条是否在底部附近）
6. 不要引入 websocket（毕设没必要，轮询够用且好讲）
```

**怎么验证**：
1. 发起一个部署，抽屉开着
2. 应该看到日志**自动**一行行长出来，像终端一样
3. 部署完成后，日志停止刷新（看浏览器 Network 面板，不再有新请求）
4. 关闭抽屉，Network 面板确认请求停止
5. 往上滚动日志区域，新行到达时不应该把视图拉回底部

---

## 做完之后

| 你得到什么 | 怎么演示 | 支撑条目 |
|---|---|---|
| 点一下，机器真的去部署 | 页面上点「开始部署」，目标机上目录真的出现、应用真的被拉起 | 3-11 |
| 制品不用手工放 | 上传 jar → 自动 scp → 远端 MD5 校验，闭环 | 3-15 |
| 执行过程可回看 | 抽屉里逐行显示，带序号，像终端 | 3-12 ~ 3-14 |
| 回滚是真的回滚 | current 软链真的指回去了 | 3-16 |
| 重启不留尸体 | 杀掉进程再启动，悬挂单据自动判 FAILED，可重新发起 | 3-10 |
| 失败了自动重试（余力） | 故意配错路径，看它试 3 次 | 3-17 |
| 超时能收住（余力） | 不会永远卡在"部署中" | 3-18 |
| 同一个应用不会并发部署（余力） | 两个人同时点，第二个被拦 | 3-19 |

**下一步**：进 `devops-prompts-4-看得见.md`，让系统在出问题时主动告诉你。
