# DevOps 改造 · 从这里开始

> 这套提示词用来把 `manager` 这个运维管理系统改造成**能上内网、能自动部署、能自己报警、能做数据分析**的平台。
> 一共 53 条，分成 4 个文件。**一次只做一个文件，做完再开下一个。**

---

## 路径约定（先看这一条，不然后面会找错地方）

| 提示词里写的 | 实际在哪 |
|---|---|
| `springboot/…`、`vue/…`、`files/` | 相对 `D:\Encoding\IntelliJProject\graduation-project\manager\` |
| git 仓库（`.git`）、`.gitignore`、`.gitattributes` | 在**上一级** `D:\Encoding\IntelliJProject\graduation-project\`，**不在 `manager/` 下** |
| 启动脚本 `start.sh` / `start.bat`、`docker-compose.yml`、`DEPLOY.md` | 按第 2 步的约定建在 `manager/` 下 |

也就是说：凡是提示词里写「项目根目录」又没特别说明的，指的是 `manager/`；只有 `.gitignore`、`.gitattributes` 这两个文件例外，它们在 `graduation-project/` 这一层。

---

## 文件在哪、按什么顺序做

| 顺序 | 文件 | 多少条 | 做完能怎样 | 大概要多久 |
|---|---|---|---|---|
| 1 | `devops-prompts-1-堵洞.md` | 9 条 | 答辩/评审时经得起追问 | 半天到 1 天 |
| 2 | `devops-prompts-2-跑起来.md` | 11 条 | 换台服务器一条命令起全栈 | 1.5-2 天 |
| 3 | `devops-prompts-3-自动部署.md` | 20 条（16 必须 + 4 余力） | 点一下，机器真的去部署 | 4.5-6.5 天 |
| 4 | `devops-prompts-4-看得见.md` | 13 条（9 核心 + 4 增强） | 出问题系统自己报警，且运维数据可统计、可分析、可可视化 | 3.5-5 天 |

**为什么是这个顺序**：每一步都依赖上一步。

- 第 1 步不改架构，纯补漏，**风险最低**，先做能立刻看到效果
- 第 2 步要有第 1 步的配置外置才能容器化（不然密码写死在镜像里）
- 第 3 步要有第 2 步的容器环境才能演示"自动部署到测试机"
- 第 4 步的探活要用第 3 步写的 SSH 连接能力；数据分析（4-10 ~ 4-13）吃的是第 3 步落库的部署记录和执行日志，没有第 3 步就没有真数据可分析

**中间可以停**：第 1、2 步各自独立完整，完成任一步都是可交付的状态。第 3 步要把 **16 条必须项全部做完**，才是完整可演示的自动部署。

**总时间估算：10.5-14.5 天**（四个里程碑合计，含验证与返工缓冲）。最大变数不是代码量，是第 3 步要用的 SSH 测试机——进第 3 步前先把它就绪。

---

## 选题与专业匹配说明

- 你的专业是**大数据技术**，学校**不强制要求 Hadoop/Spark 生态组件**，接受"数据采集 / 统计分析 / 可视化"方向。
- 本版在第 4 步（4-10 ~ 4-13）落地了完整的「运维数据采集（部署记录 / 执行日志 / 探活结果 / 告警）→ 存储（MySQL）→ 统计分析（聚合 SQL + 移动平均）→ 可视化（ECharts）」链路，这就是专业契合点，答辩时直接照这条链讲。
- 题目二选一，由你定夺：《DevOps 运维管理系统的设计与实现——面向中小软件开发团队》，或加上数据分析定位的《DevOps 运维管理系统的设计与实现——面向中小软件开发团队的运维数据采集与分析》。
- 答辩划界话术不变："覆盖持续交付环路的部署执行与可观测环节，CI 不在范围。"

---

## v2 修订说明（评审后定稿版）

本版合并了架构评审报告 `devops-plan-review.md` 的全部 P0/P1 修正，与初版的差异：

1. **3-09 整段重写**（评审时编号 3-07）：原写法把线程池提交放在事务方法内部，工作线程可能在事务提交前就开始跑（竞态），而且原验收方法（看两行日志先后）证明不了事务边界。新版改为「无事务入口方法 → 事务方法返回（即提交）后再提交线程池」，验收同步改为在入口方法的 submit 前后打日志。
2. **新增 3-10 启动恢复**（评审时编号 3-08）：进程重启会硬杀在途 SSH 执行，部署单永久卡在 DEPLOYING（状态机不能从 DEPLOYING 重入，人工也推不动）。启动时把悬挂的 RUNNING 执行判定 FAILED，可重新发起。
3. **原 3-14（回滚真实执行）、原 3-15（制品管理最小版）从"有余力"升为必做**——没有它们，制品靠手工预放到目标机、回滚只改状态，"自动部署"是假闭环，"回滚是真的回滚"也没有条目支撑。文件 3 因此全量重排编号（旧→新映射表见文件 3 开头）。
4. **其余修补**：1-04 的 mock 指令改写（原指令与项目纯 Mockito 测试风格自相矛盾）、新增 1-09（`.gitattributes` 锁 LF 换行）、2-03 加请求体上限、2-04 加 MySQL 时区与 restart 策略、2-11 加离线部署一节、4-06 加时区前提提示。

**v3 修订（2026-09-19）**：里程碑 4 扩充数据分析模块——新增 4-10 ~ 4-13 四条核心提示词（部署成败多维统计接口、执行日志错误 TOP-N、告警趋势与 7 日移动平均、数据分析页签整合可视化），全部基于真实落库数据；条目 47 → 51，里程碑 4 估时 2-3 天 → 3.5-5 天，总估时 9-12 天 → 10-14 天。

**v4 修订（对齐真实代码）**：本版把提示词里**引用了但实际不存在**的东西全部改掉——改之前执行者照做会直接卡死。变化清单：

1. **CAS 乐观锁不是"已存在"，是本轮新增**。`DeployService.startDeploy()` / `finishDeploy()` / `rollback()` 现在用的是 `deployMapper.updateById(updateDeploy)`，`DeployMapper.xml` 里没有 `casUpdateStatus`，`deploy` 表没有 `row_version` 字段（git 全部 3 次提交里从未出现过 `row_version`）。原提示词里的"casUpdate() 一个字都不要改"全部作废，改为**新增 3-06 一条**：加 `row_version` 列 + CAS 更新语句 + 状态变更改走 CAS。
2. **新增 3-05 一条**：`deploy_log` 表实际只有 8 个字段（`id`/`deploy_id`/`action`/`operator`/`detail`/`create_time`/`update_time`/`deleted`），**没有** `log_type` / `seq` / `source`。后续所有 `log_type='EXEC'`、`seq`、`source='AUTO'` 的写法都建立在 3-05 加完列之后。
3. **SQL 脚本名改为项目风格**：项目没有 Flyway（`resources/sql/` 里是 `schema.sql`、`data.sql`、`upgrade_fk.sql`、`upgrade_password.sql`、`upgrade_admin.sql`、`upgrade_alarm.sql`，手动执行）。原 `V5__server_ssh.sql` / `V6__deploy_execution.sql` / `V7__deploy_artifact.sql` 改为 `upgrade_server_ssh.sql` / `upgrade_deploy_execution.sql` / `upgrade_deploy_artifact.sql`；第 2 步 2-10 也不再改成 `V1__`~`V4__`。
4. **不再引用不存在的 `upgrade_rollback.sql`**（第 2、3、4 步里共有 5 处），改成真实存在的 `upgrade_alarm.sql` / `upgrade_fk.sql` / `upgrade_admin.sql`，行号一并改掉。
5. **4-10 从"新建统计接口"改为"扩展已有的 StatisticsController"**：`StatisticsController` / `StatisticsService` 已存在，已有 7 个端点（`count`、`deployTrend`、`alarmLevel`、`serverEnv`、`deployStatus`、`alarmStatus`、`appRisk`），不要新建 Controller/Service。
6. **1-02、1-09 标注为"已做"**：`.gitignore`、`.gitattributes` 在项目根（`graduation-project/`）已存在，先读一遍确认，满足就跳过。
7. **Dashboard 的「部署后 24 小时内新告警」指标当前没有**，从"已有指标"改为"本轮要做"（并入 4-06）。已完成的交付成功率 / 告警闭环率仍然是已有指标。

条目 51 → 53（里程碑 3 新增 2 条），里程碑 3 估时 4-6 天 → 4.5-6.5 天，总估时 10-14 天 → 10.5-14.5 天。

---

## 怎么用（每条都长这样）

```
## 3-01 · 加 SSH 依赖

改什么：springboot/pom.xml（1 个文件）

提示词：
（这一整段直接复制，粘给 Cursor）

怎么验证：
（敲什么命令、看什么现象，通过了再进下一条）
```

**工作节奏**：
1. 复制「提示词」那一整段，粘给 Cursor
2. 让它改完，**看 diff**（不要不看就接受）
3. 按「怎么验证」自己验一遍
4. 验过了再做下一条

**如果验证不过**：先撤回（`git checkout .`），把报错信息贴给我，不要硬往下做。

---

## 有件事你得先知道

下面这些**当前代码里真有**，做后面的条目时不要再改一遍：

| 已经做好的 | 在哪 |
|---|---|
| 生产环境数据库密码/JWT 密钥/CORS 源走环境变量 | `application-prod.yml`，强制注入，无默认值 |
| `.env` 读进系统属性，并接到 SMTP | `EnvFileLoader.load()` 在 `SpringbootApplication.main` 里、Spring 启动前调用；`application.yml` 的 `spring.mail.password` 用 `${MAIL_PASSWORD:}` |
| 状态变更落流转日志（同事务） | `DeployService.saveLog()`，约第 271 行。文件一共大约 280 行 |
| 开发/生产配置分离 | `application.yml`、`application-dev.yml`、`application-prod.yml` |
| 回滚只改状态 | `DeployService.rollback()`：成功单回到 `DEPLOYING`，没有版本合法性三重校验 |
| **交付成功率 / 告警闭环率看板** | `Dashboard.vue`「交付与风险」面板，带进度条 |
| 邮件告警服务 | `AlarmMailService` / `PersonMailService`。没配 SMTP 时降级，不阻断启动 |
| 文件上传安全校验 | `FileSafeNames`，有单测 |

下面这些**旧稿写成「已经好了」，当前仓库里没有**。文件 1–4 如果点名它们、或引用旧行号，按「还没做」处理，不要照着不存在的方法改：

| 旧稿写法 | 当前代码 |
|---|---|
| `casUpdate()` + `row_version` 乐观锁 | 全仓库搜不到。**本轮新增 3-06 会把它建出来**，在那之前状态变更走 `deployMapper.updateById(updateDeploy)` |
| `DeployService.execLogs()`、`DeployLogLinesVO` | 不存在 |
| `deploy_log.seq` / `log_type` / `source` | 表只有 `id`、`deploy_id`、`action`、`operator`、`detail`、`create_time`、`update_time`、`deleted` |
| `upgrade_rollback.sql` | `resources/sql/` 里是 `schema.sql`、`data.sql`、`upgrade_fk.sql`、`upgrade_password.sql`、`upgrade_alarm.sql` |
| `HEALTH_CHECK_FAIL` | `DeployActionEnum` 里没有 |
| `saveLog()` 在第 367–380 行 | 实际在约第 271 行 |
| 「部署后 24 小时内新告警」 | `Dashboard.vue` 没有这条指标 |

**文件 1–4 里其余条目仍是待做缺口。** 遇到「这条写着我应该已经做过了，但代码里没有」，以代码为准，把那条当成还没做。

---

## 答辩时现在能说的

执行链路只定死了一小截，**不能**说表结构、索引和 `execLogs` 都已经留好：

| 预留物 | 位置 | 现状 |
|---|---|---|
| `EXEC` | `DeployLogTypeEnum` | 枚举值在，注释写「执行输出（预留）」。`deploy_log` 没有 `log_type` 列，也没有写入——**本轮 3-05 加列后才可写入** |
| `AUTO` | `DeployLogSourceEnum` | 枚举值在，注释写「系统自动（预留）」。表上没有 `source` 列——**本轮 3-05 加列后才可写入** |
| 执行摘要 VO | `DeployExecSummaryVO` | 空壳，`empty()` 把 `status` 设为 `NONE`。`DeployTimelineVO` 挂着它 |
| 时间线 VO | `DeployTimelineVO` | 结构在。`DeployService` 没有对外的时间线查询 |

答辩不要说：日志表已经用 `log_type` / `seq` 建好索引、`execLogs` 返回空、`HEALTH_CHECK_FAIL` 已预留。这些还没进代码。

---

## 关于"还能再做什么"

四个文件做完，系统就已经是完整的 DevOps 运维平台了。如果还想往前走，方向是：

1. **真正的 CI**（代码提交触发自动构建）—— 需要接 Git 服务，工程量不小
2. **多环境管理**（测试环境/预发/生产的隔离）—— 现在的 `env` 字段可以扩展
3. **告警聚合**（一批故障合并成一封邮件）—— 现在的告警是一条一条发的
4. **权限细化**（现在只有 ADMIN/DEV/OPS 三角色）—— 需要数据级权限

这些都不在本次范围内，**不要在做四个文件的时候顺手做**，容易把已验证的东西改坏。
