# 第 4 步 · 看得见

> 这个文件是干嘛的：让系统在出问题的时候**自己发现、自己报警**，而不是等人打开页面看；再把系统攒下的运维数据**统计出来、分析出来、画出来**。
> 做完能怎样：应用挂了、服务器连不上了，系统几分钟内自己产生一条告警并推邮件给你；部署记录、执行日志、告警这些数据能做多维统计、错误 TOP-N、趋势分析，并在一个数据分析页里可视化。
> 怎么做：这一步有 13 条。**核心是 4-01 到 4-05**（告警自动化）**加 4-10 到 4-13**（数据分析，4 条全是核心）。4-06 到 4-09 是锦上添花。

---

## 先说清楚现状（你比你想的做得多）

我先读了你现有的代码，有一个重要发现：**你的 Dashboard 已经是一个很成熟的运维看板了**。

`vue/src/views/manager/Dashboard.vue` 里已经有 5 个面板：

| 面板 | 已有内容 |
|---|---|
| 业务总览 | 各类资产计数卡片 |
| 资产分布 | CentOS/Ubuntu 分布、运行中服务器数、环境分布 |
| **交付与风险** | **交付成功率**（带进度条）、**告警闭环率**（带进度条）、待审批部署数、未处理告警数 |
| 部署交付闭环 | 各状态部署单数量分布，点击可筛选 |
| 应用变更与风险 | 按应用排序的风险表（部署次数 + 告警次数 + 风险等级，**不含**"部署后 24 小时内新告警"） |

**注意一处旧稿的错误**：旧稿说「应用变更与风险」面板里有"最近一次成功部署后 24 小时内新告警"
这个指标——**当前 `Dashboard.vue` 里没有它**（`appRisk` 的数据只到"部署次数 + 告警次数 + 风险等级"为止）。
这个指标本身是变更失败率的正确算法（关联到具体那次变更之后的告警），
所以本轮**要把它做出来**，见 **4-06**。

所以第 4 步**不需要重做看板**。真正的缺口有五个：

| 缺口 | 现状 | 本次要做的 |
|---|---|---|
| **告警靠人工录入** | `AlarmService` 里**没有任何 `@Scheduled` 方法**，没有任何探活逻辑。告警只能靠人在页面上手动录入 | 4-01 ~ 4-05（核心） |
| **只有快照，没有趋势** | 所有指标都是"当前值"，看不到"最近两周怎么变的" | 4-06 |
| **指标不给外部看** | 第 2 步加了 `/actuator/health`，但没有业务指标 | 4-07、4-08（可选） |
| **数据采了但没有分析** | 部署记录、执行日志、`deploy_execution`、告警都已经真实落库，但没有任何统计与分析能力 | 4-10 ~ 4-13（核心，数据分析模块） |
| **没有"变更→故障"关联指标** | 「部署后 24 小时内新告警」这个指标当前不存在 | 4-06（并入趋势图那条一起做） |

已经有的好东西**不要动**：`AlarmService.bindDeploy()`（录入告警时能从部署单自动带出应用和服务器，第 183-197 行）、`handle` / `resolve` 状态流转、邮件链路（`AlarmMailService` / `PersonMailService`）、`AlarmResolution` 相关字段。

---

# 核心（5 条）

## 4-01 · 加一个定时任务的基础配置

**改什么**：`springboot/src/main/java/com/example/SpringbootApplication.java` + 新建线程池配置（2 个文件）

**提示词**：
```
项目现在没有任何定时任务。要开启定时任务能力。

要求：
1. 在 SpringbootApplication 类上加 @EnableScheduling
   （现在这个类只有 @SpringBootApplication 和 @MapperScan）
2. 新建 springboot/src/main/java/com/example/common/config/SchedulingConfig.java：
   - 实现 SchedulingConfigurer
   - 配置一个 TaskScheduler，线程池大小 2（探活任务不需要多线程）
   - 线程名前缀 "health-check-"，方便看日志时辨认
   - 加 @Configuration
3. 不要用 @Scheduled 的默认单线程调度器（默认只有一个线程，
   一个任务卡住其他全不跑）
4. 从配置读一个总开关：
   healthcheck:
     enabled: ${HEALTHCHECK_ENABLED:true}
     如果关了，所有探活任务不执行（本地开发时可以关掉，避免一直连不上测试机刷日志）
   在 application-dev.yml 里默认设成 false（本地开发不需要探活），
   application-prod.yml 里设成 true
```

**怎么验证**：
启动应用（dev profile），控制台不应该有探活相关的日志（因为 dev 默认关闭）。改成 `HEALTHCHECK_ENABLED=true` 重启，能正常启动不报错。

---

## 4-02 · 实现服务器探活

**改什么**：新建 `springboot/src/main/java/com/example/service/HealthCheckService.java`（1 个新文件）

**提示词**：
```
新建 HealthCheckService，负责定时检查服务器和应用的健康状态。

要求：
1. 用 @Scheduled 起一个方法，每 2 分钟跑一次（cron 从配置读，默认 "0 */2 * * * ?"）：
   @Scheduled(cron = "${healthcheck.server-cron:0 */2 * * * ?}")
2. 检查逻辑：
   - 查出所有 status='RUNNING'（运行中）且未删除的服务器
   - 对每一台，复用第 3 步写的 SshDeployExecutor.testConnection(server)
   - 连不上 → 判定为异常
   - 注意：这里的探测是"能不能连上 SSH"，不是"服务好不好"。
     这对你自己的运维系统来说是合理的——你要管的就是这些机器
3. **重要：探活必须容错**。单台机器连不上不能影响其他机器，也不能影响整个任务。
   每台单独 try-catch，失败只记日志 + 生成告警，不要往外抛
4. 每台机器的检查间隔不能太短（SSH 连接本身有开销）。
   如果服务器数量超过 50 台，需要分批检查（每批之间 sleep 100ms），避免瞬间打满 CPU
5. 探活结果暂时只记日志（log.warn），生成告警的逻辑放在下一步
6. **不要在探活里做数据库写操作**（下一步统一处理），这个类只负责"探测"和"判断"

注意：
- 服务器表里如果没有配 SSH 信息的机器（ssh_user 为空等），**记为"配置不全"并跳过**，
  不要因为配置缺失就报错刷屏
- 关于**历史明文密码**（上一轮改造前存的、没经过 CredentialCipher 加密的数据）：
  `CredentialCipher.decrypt()` 遇到非加密格式会返回 null。
  **不要简单地跳过这种机器**——那样它会永远不被检查，也永远不会报警，
  等于从监控里消失了。正确做法是**把它当成"配置异常"来报告**：
  - 记一条 WARN 日志，写明"服务器 {hostname} 的凭据未加密，无法用于探活，请重新保存凭据"
  - 并且在第 4-03 步里，给这台机器产生一条**"配置异常"类型的告警**
    （提示运维人员去重新填写凭据），而不是静默跳过
  - 这样机器不会从视野里消失，人也知道该做什么
- 更进一步（有余力再做）：在探活开始前做一次**一次性迁移**——
  遍历所有服务器，如果 ssh_password 不是加密格式且非空，就加密后回写。
  如果做这个，要单独写个方法并记日志"已迁移 N 台服务器的凭据"，
  不要混在探活逻辑里
```

**怎么验证**：
1. 在服务器表里配一台**能连上**的机器，和一台**故意填错 IP** 的机器
2. 把 `HEALTHCHECK_ENABLED` 设为 true，`healthcheck.server-cron` 临时改成 `0/10 * * * * ?`（每 10 秒一次）
3. 启动应用，观察日志：
   - 能连上的机器：没有异常日志
   - 连不上的机器：有 warn 日志，且**不影响**能连上的那台的检查
4. 验证完把 cron 改回 2 分钟

---

## 4-03 · 探活失败自动产生告警

**改什么**：`HealthCheckService`（1 个文件，追加逻辑）

**提示词**：
```
上一步的 HealthCheckService 探到失败只记日志，现在要让它生成真实的告警记录。

背景：AlarmService 已有 add(Alarm) 方法可以创建告警。
Alarm 实体已经有关联字段 appId / serverId / deployId，
并且 AlarmService.bindDeploy() 能从 deployId 自动带出 appId 和 serverId。
AlarmStatusEnum 有 UNHANDLED 等状态，AlarmLevelEnum 有告警级别。

要求：
1. 探活失败的机器，调 AlarmService.add() 创建一条告警，内容：
   - 告警级别：从 AlarmLevelEnum 里选合适的（服务器连不上应该是较高级别）
   - alarmNo：交给 AlarmService 自动生成（它有 generateAlarmNo()）
   - serverId：设为该服务器 id
   - 标题/描述：写清楚"服务器 {hostname}({ip}) SSH 连接失败"这类具体信息
   - 来源标识：标记这条是系统自动产生的，不是人工录入的
     （先看 Alarm 实体有没有类似 source / origin 的字段。
      没有就用描述里加前缀"【自动检测】"，
      并在回报里说明——不要为了这个去改表结构）
2. **必须做去重**！这是本条最重要的要求：
   同一个服务器连续探活失败，**不能每 2 分钟产生一条新告警**，否则告警列表会被刷爆。
   做法：生成告警前先查——该 serverId 是否已存在状态为 UNHANDLED 或 HANDLING 的
   "自动检测"类告警。有就不再新建，只更新它的描述（比如追加"仍在异常，已持续 N 次"）
   并把 update_time 刷新

   **⚠️ 这里有个容易踩的坑，必须处理**：
   如果告警被人工「解决」/「关闭」了，但机器**其实还是坏的**，
   按上面的逻辑（只查 UNHANDLED/HANDLING）就会判定"已有告警"而不新建，
   结果**再也不报警了**——人被误导以为问题解决了，实际故障还在。
   正确做法是加**冷却期**而不是永久抑制：
   - 查最近一条自动告警，看它的解决时间（或最后更新时间）
   - 如果**还没被关闭** → 只更新描述，不新建（原逻辑）
   - 如果**已被关闭，但关闭时间在 30 分钟内** → 不新建（给运维一点处理时间，避免刷屏）
   - 如果**已被关闭，且超过 30 分钟，而探活仍然失败** → **新建一条告警**，
     描述里写明"故障复发"（这说明之前的处理没生效）
   冷却期时长从配置读：healthcheck.auto-alarm-cooldown-minutes，默认 30

3. **恢复时也要处理**：
   - 如果该服务器之前有自动产生的未处理告警，现在探活成功了，
     应该在描述里标注"已恢复"（或调用 AlarmService 的相应方法推进状态）
   - 不要自动把告警标记为"已解决"——那是人工确认的动作，系统不该替人做决定。
     但要让人能看出来"问题已经不存在了"
4. 生成告警后调 AlarmMailService 发邮件（复用已有能力，不要重新写邮件逻辑）
   - 邮件的收件人由 AlarmMailService 自己决定，你不要传收件人
   - 邮件发送失败不能影响探活流程（try-catch 包住，只记日志）

注意：AlarmService.add() 上有校验逻辑（看 AlarmService 第 42 行的实现），
如果它要求某些字段必填，要按它的要求填全。
```

**怎么验证**：
1. 保留一台连不上的测试机，把 cron 改成每 10 秒
2. 启动应用，等 30 秒
3. **去告警列表页看**：应该只有 **1 条**该服务器的告警，不是 3 条（验证去重生效）
4. 看邮件收件箱：应该收到告警邮件
5. **验证"关掉后仍故障会复发报警"这个场景**（这是最容易漏的）：
   - 在页面上把这条告警标记为「已解决」
   - 保持机器仍然是坏的，等一个探活周期
   - 因为冷却期是 30 分钟，**此时不应该立刻新建告警**（这是对的，避免刷屏）
   - 把 `healthcheck.auto-alarm-cooldown-minutes` 临时改成 0，再等一个周期
   - **应该新建一条告警**，描述里有"故障复发"
   - 验完把冷却期配置去掉（回到默认 30 分钟）
5. 把测试机的 IP 改对（或配一台能连的），再等 30 秒：
   - 应该是**自动恢复**——不产生新告警，原告警的描述里能看到"已恢复"之类信息
6. 把一台能连的机器的 SSH 密码改错，触发失败，确认能正常产生告警

---

## 4-04 · 应用状态探活

**改什么**：`HealthCheckService`（1 个文件，追加方法）

**提示词**：
```
现在只探服务器。还要探应用——应用的 status 是 ONLINE / DEPLOYING / OFFLINE。

要求：
1. 新增一个 @Scheduled 方法，检查所有 status='ONLINE' 的应用
   （OFFLINE 是人工置离线的，不该报警；DEPLOYING 是执行中，也不报）
2. 怎么判断一个应用"活着"：
   应用表里有 serverId，说明它部署在哪台机器上。
   最实用的判断方式是：SSH 到那台机器，检查应用进程或端口是否在。
   - 优先用端口检查：如果应用表里有端口字段就用它，
     没有就用 deploy_path 检查软链是否存在（current 指向的目录还在不在）
   - 命令行示例：test -L "$DEPLOY_PATH/current" && echo OK
3. 探到异常时，产生告警的逻辑和 4-03 完全一样（同样要去重、同样发邮件）
   - 告警关联 appId 和 serverId
   - 描述写清楚"{应用名} 在 {服务器} 上状态异常"
4. 抽出一个公共方法给 4-03 和本条复用：
   private void raiseAlarm(Integer appId, Integer serverId, String title, String detail)
   不要把去重逻辑写两遍
5. 应用探活和应用数量可能很多，同样要分批 + 单独容错

注意：如果应用表里没有端口字段，不要为了这个改表——
用 deploy_path + current 软链判断就够了，而且更贴合你第 3 步的部署方式
（第 3 步的 deploy.sh 就是建 current 软链）。
```

**怎么验证**：
1. 启一个部署完成的应用（current 软链存在），探活应该正常
2. 登到目标机上手工删掉 current 软链（`rm /opt/apps/{应用}/current`）
3. 等一个探活周期，应该产生一条"应用状态异常"的告警
4. 恢复软链（重新指回去），应该标注"已恢复"
5. 确认这个应用的告警关联到了正确的 appId 和 serverId（点进去能看到应用和服务器）

---

## 4-05 · 探活结果写进部署日志

**改什么**：`springboot/src/main/java/com/example/service/HealthCheckService.java` + `DeployService.java`（2 个文件）

> 说明：`DeployLogTypeEnum` 已有 `STATE` 和 `EXEC` 两个值，`HEALTH_CHECK_FAIL` 是 `action` 的值
> （不是 `logType` 的值），所以**不需要改任何枚举**。只需在两处加代码。

**提示词**：
```
HEALTH_CHECK_FAIL 这个动作值目前只出现在 `DeployLog.java` 的 `action` 字段注释里，
`DeployActionEnum` 里并没有它，也没有任何地方用它。现在探活有结果了，可以用上。
（它是 `action` 的值，不是 `log_type` 的值，所以不用改枚举，直接写字符串即可。
注意 `log_type` 这一列要等第 3 步 3-05 补完才存在。）

要求：
1. 探活失败时，**如果该服务器上有正在部署的单据**，要往那条部署单的日志里写一条
   - 这是"变更关联性"的体现：如果部署完成后探活失败，说明很可能是这次变更导致的
   - 用 DeployLogTypeEnum.STATE（这是流程性事件，不是执行输出）
   - action 用 "HEALTH_CHECK_FAIL"
   - detail 写清楚"部署后探活失败：{具体原因}"
2. 实现方式：在 HealthCheckService 里注入 DeployLogService 或 DeployService，
   但注意——DeployLogController 的类注释写明「记录只由 Deploy 模块内部写入」。
   所以**不要**直接调 DeployLogMapper.insert，而是通过 DeployService 暴露的公共方法写入
   （如果 DeployService 的 saveLog 是私有的，需要新增一个 public 方法包装，
   比如 public void saveHealthCheckLog(Integer deployId, String detail)）
3. 怎么找"该服务器上正在部署的单据"：
   查 deploy 表，serverId = 该服务器 且 status = 'DEPLOYING' 或最近 24 小时内 SUCCESS 的
4. 只在这个服务器**第一次**探活失败时写（和告警去重一样的逻辑），不要每次探活都写

**⚠️ 关于循环依赖的说明（重要）：**
- 按约定，`deploy_log` 的写入归 Deploy 模块。但如果 HealthCheckService 注入 DeployService，
  而 DeployService 又（在第 3 步里）依赖了执行相关的东西，可能形成循环依赖，
  导致**应用起不来**。
- **推荐解法**：不要注入 DeployService。改为：
  1. 在 DeployService 里新增一个很薄的公共方法：
     `public void saveHealthCheckLog(Integer deployId, String detail)`
     转发给已有的私有 saveLog
  2. HealthCheckService **用 `@Lazy` 注入** DeployService，打破循环：
     `@Lazy @Resource private DeployService deployService;`
     （@Lazy 让 Spring 注入代理而不是立即实例化，能解掉环）
- 这样既守住了"日志只由 Deploy 模块写"的约定，又不会造成启动失败。

**这条是可选条目**：如果你做完发现无论怎么调都起不来，
或者在回报里说明遇到循环依赖无法解开，**可以跳过这一条**，
只保留告警（告警本身已经能表达"部署后出问题了"）。
**不要为了这一条去大改 DeployService 的结构。**
```

**怎么验证**：
1. 完成一次部署（SUCCESS），确认应用在跑
2. 立刻破坏应用（删掉 current 软链）
3. 等探活触发
4. 打开那条部署单的详情，时间线里应该能看到一条"部署后探活失败"的记录
5. 这条记录能说明"这次部署可能导致了问题"——这是答辩时的好素材

---

# 增强（4 条）

## 4-06 · 加一张趋势图，并补上"部署后 24 小时新告警"指标

**改什么**：`vue/src/views/manager/Dashboard.vue` + `StatisticsService`（2 个文件）

**提示词**：
```
Dashboard.vue 现在全是"当前快照"指标，看不到变化趋势。加一张按天的趋势图，
并补一个当前还没有的指标。

要求：
1. 后端：在已有的 StatisticsService 里新增方法，查"最近 14 天每天的部署单数量"：
   - 按 create_time 的日期分组统计 deploy 表
   - 返回 List<Map>，每项含 date（yyyy-MM-dd）和 count
   - 没有数据的日期要补 0（不要漏掉空白天，否则折线会跳）
   - 同时在 StatisticsMapper.xml 里加对应 SQL
   - 注意 MySQL 的 DATE_FORMAT(create_time, '%Y-%m-%d') 分组

2. 在已有的 StatisticsController 里加一个端点返回这个数据
   （这个类已有 7 个端点：count / deployTrend / alarmLevel / serverEnv /
   deployStatus / alarmStatus / appRisk，照同样风格加，**不要新建 Controller**）

3. 前端：在 Dashboard.vue 的「交付与风险」面板下方，加一张 ECharts 折线图
   - 项目已经用了 ECharts（先确认 vue/src 下有没有已封装好的图表组件，
     有就复用，不要重新引入一套）
   - X 轴是日期（14 个点），Y 轴是部署单数
   - 折线用项目主色调
   - 鼠标悬停显示具体数值
   - 图的高度控制在 200px 以内（这是看板，不是独立分析页）

4. 补一个当前不存在的指标：**「最近一次成功部署后 24 小时内新告警」**
   - 后端：在 StatisticsService / StatisticsMapper 里加聚合——对每个应用，
     取它最近一次 status='SUCCESS' 的部署的 finish_time（没有就用 create_time），
     统计该时间点之后 24 小时内 alarm 表里这个应用的新增告警数
   - 端点同样加在已有的 StatisticsController 上
   - 前端：放进「应用变更与风险」面板，在风险表里加一列展示（没有数据的应用显示 0）
   - 这个指标是"变更失败率"的正确算法（关联到具体那次变更之后的告警），
     答辩时可以专门讲

5. 不要改动 Dashboard.vue 已有的 5 个面板和其他指标

补充说明：为什么是这两样？
14 天部署趋势折线 = 部署频率；"部署后 24 小时新告警" = 变更失败率；
配合已有的"交付成功率"，DORA 的核心指标就能在答辩时直接说出来。
```

**前提提醒**：按日分组的前提是 MySQL 会话时区正确——第 2 步 2-04 已给 mysql 容器配了 `TZ: Asia/Shanghai`。如果实际部署没走这套 compose（比如直连外部 MySQL），要先确认数据库时区，否则 `DATE_FORMAT(create_time)` 分组会把日期切错位 8 小时，趋势图整体错一天。

**怎么验证**：
1. 先在数据库里造一些不同日期的部署单数据（直接 INSERT 几行，日期分散在最近 14 天）
2. 打开 Dashboard，折线图应该能看到有起伏的曲线，最近几天有数据的点不为 0
3. 把某个中间日期的一条数据删掉，确认那天显示 0 而不是断线
4. 窗口缩放到手机宽度，图应该自适应不溢出

---

## 4-07 · 暴露业务指标给 Prometheus（可选）

**改什么**：`springboot/pom.xml` + 新建 `MetricsConfig.java`（2 个文件）

**提示词**：
```
项目第 2 步已经加了 Actuator，只开了 health 端点。现在加业务指标。

要求：
1. pom.xml 加 Micrometer 的 Prometheus 支持：
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   （版本由 Spring Boot parent 管理，不要写 version）

2. application.yml 把 prometheus 端点加进暴露列表：
   management.endpoints.web.exposure.include: health,prometheus

3. 新建 MetricsConfig.java，注册几个业务指标（用 Micrometer 的 Gauge）：
   - manager.deploy.total：部署单总数
   - manager.deploy.success.total：成功部署数
   - manager.alarm.unhandled.total：未处理告警数
   - manager.server.online.total：在线服务器数
   这些值从对应的 Service 或 Mapper 查。注意 Gauge 是基于回调的，
   每次被抓取时才查库（不要在启动时计算一次就固定住）

4. 记得把 /actuator/prometheus 加进 JwtInterceptor 的排除列表
   （和第 2 步处理 /actuator/health 一样）

注意：这条是给"将来接 Prometheus + Grafana"准备的。
如果答辩不打算展示这个，可以跳过——不影响其他功能。
```

**怎么验证**：
```
curl http://localhost:9090/actuator/prometheus
```
输出里能搜到 `manager_deploy_total` 等指标行，且数值和页面上显示的一致。

---

## 4-08 · 告警恢复通知（可选）

**改什么**：`HealthCheckService`（1 个文件）

**提示词**：
```
现在探活恢复时只在告警描述里标注，不通知人。补上恢复通知。

要求：
1. 探活从"失败"变为"成功"时，除了更新告警描述，还发一封**恢复通知**邮件
2. 邮件内容要包含：
   - 什么恢复了（服务器 hostname / 应用名）
   - 故障持续了多久（从告警创建时间到现在）
   - 关联的告警编号（方便去系统里看详情）
3. 复用 AlarmMailService，不要重写邮件发送逻辑
4. 恢复通知**每个故障只发一次**，不要反复发
   （判断依据：该告警是否已经发过恢复通知，可以在描述里加标记，或看告警状态）
5. 邮件发送失败不影响主流程

注意：真正的生产系统会做"告警聚合"（一批故障合并成一封邮件），
这里不做——毕设单机场景，一个一个发就够了，而且更清楚。
```

**怎么验证**：
1. 制造一次探活失败，收到告警邮件
2. 恢复机器，等一个探活周期
3. 应该收到一封"已恢复"邮件，里面有故障持续时长
4. 再等两个周期，确认**没有**重复的恢复邮件

---

## 4-09 · 把探活配置写进部署文档

**改什么**：`DEPLOY.md`（1 个文件，追加章节）

**提示词**：
```
在第 2 步写的 DEPLOY.md 里追加一节「健康检查配置」。

要包含：
1. 探活相关的全部环境变量及默认值：
   - HEALTHCHECK_ENABLED（默认 true；本地开发建议 false）
   - healthcheck.server-cron（默认 0 */2 * * * ?，即每 2 分钟）
   - healthcheck.auto-alarm-cooldown-minutes（默认 30，告警复发冷却期）
   **注意**：先读一遍 4-02 / 4-03 / 4-04 实际写进代码的配置项，
   以代码里的真实配置名为准。不要凭记忆写文档——
   如果 4-04 的应用探活没有单独配 cron（复用了 server-cron），
   就不要在文档里写一个不存在的 healthcheck.app-cron。
2. 说明探活需要什么前置条件：
   - 服务器表里必须配好 SSH 信息（端口、用户名、密码或私钥）
   - 探活的账号需要有执行 echo 和 test 命令的权限
3. 说明告警去重逻辑（避免运维人员不理解为什么只收到一条）：
   - 同一故障持续存在 → 只保留一条告警，更新描述
   - 告警被人关闭后 30 分钟内 → 不重复报警（冷却期）
   - 关闭超过 30 分钟且故障仍在 → 会新建一条"故障复发"告警
4. 常见问题：
   - 告警一直不产生 → 检查 HEALTHCHECK_ENABLED、检查服务器表有没有配 SSH
   - 告警刷屏 → 不该发生（有去重 + 冷却期），如果发生了说明去重逻辑有 bug，
     检查同一服务器的告警是否被误关闭导致冷却期反复触发
   - 机器"从监控里消失"了 → 检查该服务器凭据是否未加密（历史数据），
     这种情况会产生"配置异常"告警，重新保存一次凭据即可
   - 邮件收不到 → 检查 MAIL_ 开头的环境变量

要求：命令和变量名要准确，不要写没验证过的。
```

**怎么验证**：
按文档里的说明，用一个新手视角走一遍：只看文档，能不能配好探活、能不能判断"为什么没报警"。

---

# 核心 · 数据分析（4 条）

> 这一组是数据分析模块的全部：4-10 打统计地基，4-11 做日志分析，4-12 做趋势分析，4-13 整合可视化。
> **红线：所有统计都基于真实落库数据（`deploy` / `deploy_log` / `deploy_execution` / `alarm` 表），禁止造假数据、禁止在前端硬编码演示数字。** 验证时需要造数，就按各条的验证步骤手工插真实表结构的行。

## 4-10 · 部署成败多维统计接口（扩展已有的 StatisticsController）

> **不要新建 Controller/Service**。`com.example.controller.StatisticsController`
> 和 `StatisticsService` **已经存在**，已有 7 个端点：
> `count`、`deployTrend`（近 7 天）、`alarmLevel`、`serverEnv`、`deployStatus`、
> `alarmStatus`、`appRisk`（各应用部署次数与告警次数）。本条是在它们上面**加端点**。
>
> **与现有端点的差异（答辩会被问，先想清楚）**：
> 现有 7 个端点都是**全局分布、无时间参数**（"当前一共有多少 / 各状态各多少"）；
> 本条新增的是**带时间范围参数（days，默认 30）的成功率多维聚合**（按应用 / 按服务器 / 按日），
> 回答的是"最近这段时间谁老失败"，不是"现在有多少"。

**改什么**：`DeployMapper.xml` 加聚合查询 + `StatisticsService` 加方法 + `StatisticsController` 加端点（3 个文件，都不新建类）

**提示词**：
```
给数据分析模块打地基：统计近 N 天的部署成败数据。

背景：只用已有表（deploy、server、app），不建新表。
部署成败以 deploy.status 为准（SUCCESS / FAILED），时间字段用 create_time。

要求：
1. DeployMapper.xml 新增三个聚合查询：
   ① 按应用：应用名、成功次数、失败次数、成功率（成功/(成功+失败)，无部署的不列）
   ② 按服务器：服务器名（关联 deploy.server_id → server）、成功次数、失败次数
   ③ 按日：DATE_FORMAT(create_time,'%Y-%m-%d') 分组的部署总次数、失败次数，
      没有数据的日期要补 0（同 4-06 的做法，否则图会断）
2. 天数参数化：days 从 Controller 传入，默认 30；Service 里校验 1 ≤ days ≤ 365，
   非法值返回参数错误，不要裸奔
3. 方法加在已有的 StatisticsService 里，端点加在已有的 StatisticsController 里
   （RequestMapping 是 /statistics，照现有端点的写法加，风格保持一致）；
   时间范围用 create_time >= NOW() - INTERVAL days DAY
4. 返回结构：一个 VO 装三组数组（byApp / byServer / byDay），一次请求全拿，
   前端不要为一张图发一次请求

为什么先做这条：这是数据分析模块的地基接口，4-13 的四张图里有三张吃它。
```

**怎么验证**：
1. Swagger（或接口工具带 token）调统计端点，`days=30` 默认值生效
2. 与手工 SQL 逐组比对，结果必须一致：
```
mysql -uroot -p manager -e "SELECT app_id, SUM(status='SUCCESS') ok, SUM(status='FAILED') fail FROM deploy WHERE deleted=0 AND create_time >= NOW() - INTERVAL 30 DAY GROUP BY app_id;"
```
3. `days=7`、`days=365` 各试一次；`days=0` 和 `days=400` 应报参数错误
4. 用一个没有部署记录的库（或把 days 范围外推）再调，三组数据都是空数组，不报错

---

## 4-11 · 执行日志错误 TOP-N 统计

**改什么**：`StatisticsService` 加方法 + `DeployLogMapper.xml` 聚合查询 + 新建关键词常量类（3 个文件；端点挂已有的 `StatisticsController` 上）

> **前提**：这条依赖 `deploy_log.log_type` 列，而该列由第 3 步 **3-05** 补建。
> 3-05 没做的话，`log_type='EXEC'` 这个条件在库里根本不成立，先回去做 3-05。

**提示词**：
```
从执行日志里统计"哪些部署单错得最多"——答辩讲"日志数据分析"最直观的一张图。

要求：
1. DeployLogMapper.xml 新增聚合查询：从 deploy_log 里
   - 只统计 log_type = 'EXEC' 的行（STATE 是流程日志，不算执行输出）
   - 近 N 天（参数 days，默认 30，校验同 4-10）
   - detail 包含错误关键词：'ERROR'、'Exception'、'FAILED'（LIKE 匹配，大小写不敏感）
   - 按 deploy_id 聚合命中的行数，取 TOP 10
   - 返回：部署单号、部署单号对应的应用名（deploy 关联 app）、错误行数
2. 关键词列表放到常量类（如 common/constant/LogErrorKeywords.java），
   SQL 里用 foreach 拼接，以后想加 'FATAL' 只改常量，不动代码逻辑
3. 方法加在已有的 StatisticsService 里，端点加在已有的 StatisticsController 上
   （和 4-10 一样，不要新建 Controller）

取舍说明（答辩和论文里都可以讲）：
- 关键词匹配用 SQL LIKE，在毕设数据量下完全可接受；
- 生产级做法是 3-12 落库时顺带打"是否错误行"的标记列，查询走索引。
  那是第 3 步的既有条目，本条不改它——论文里写明这个演进思路即可。

补充：失败部署的最终错误原因记录在 deploy_execution.error_message
（3-07 建表、3-09 落值的既有约定），返回 VO 里可以顺带带上这个字段，
验证时与 deploy_log 的错误行交叉核对。
```

**怎么验证**：
1. 先按 3-11 真跑一次"含错误"的部署：把部署单的版本号指向一个不存在的包（3-11 验证第 6 步同款场景），部署会 FAILED，执行日志里有 ERROR 相关输出；或者临时在 deploy.sh 里加一行 `echo "[deploy] ERROR: 故意的测试错误"`（验完删掉）
2. 调接口，这个部署单应出现在 TOP 列表里，错误行数与手工 SQL 一致：
```
mysql -uroot -p manager -e "SELECT deploy_id, COUNT(*) FROM deploy_log WHERE log_type='EXEC' AND (detail LIKE '%ERROR%' OR detail LIKE '%Exception%' OR detail LIKE '%FAILED%') GROUP BY deploy_id ORDER BY 2 DESC LIMIT 10;"
```
3. 没有任何含错误关键词的日志时（新库），返回空列表不报错

---

## 4-12 · 告警趋势与 7 日移动平均

**改什么**：`AlarmMapper.xml` 按日聚合 + `StatisticsService` 计算移动平均（2 个文件；端点同样加在已有的 `StatisticsController` 上）

**提示词**：
```
统计近 30 天每日告警数量，并计算 7 日移动平均——论文里最容易讲清的"趋势分析"方法。

动手前先确认 MySQL 版本（SELECT VERSION();）：
- 8.0 及以上：可用窗口函数 OVER (ORDER BY 日期 ROWS 6 PRECEDING)
- 5.7：不支持窗口函数，用自关联子查询（对每一天取它前面共 7 行的数量求平均）
两种写法选一种，在回报里写明用的是哪种、为什么。

要求：
1. AlarmMapper.xml 新增按日聚合：近 30 天（参数 days，默认 30，校验同 4-10）
   每天按 DATE_FORMAT(create_time,'%Y-%m-%d') 分组 COUNT(*)，没有告警的日期补 0（同 4-06）
2. 移动平均规则：
   - 窗口不足 7 天（比如序列开头）用"已有天数平均"，并在条目回报里说明这个行为
   - 结果保留 1 位小数
3. 移动平均放 Service 层（Java 算）或 SQL 里实现均可，二选一；
   Java 实现则 Mapper 只返回每日数量，SQL 实现则聚合和移动平均写在同一个查询里
4. 加一个端点，一次返回三个等长数组：dates、counts、movingAvg（一一对应，供 4-13 双线图直接用）
```

**怎么验证**：
1. 手工插几条不同日期的告警（用 `INSERT INTO alarm ...` 真实表结构和字段，分散在近 30 天，含连续 7 天以上的区间）
2. 调接口，手工核对 2-3 个点：某天的 7 日移动平均 = 它前面共 7 天的数量之和 ÷ 7
3. 核对序列开头不足 7 天的点用的是"已有天数平均"（比如第 3 天 = 前 3 天之和 ÷ 3）
4. 等做完 4-13，趋势图能看到两条线（实际值 + 移动平均），移动平均线明显更平滑

---

## 4-13 · 数据分析页签整合可视化

**改什么**：前端新增"数据分析"页签 + api 定义文件（2 个文件；页签实现方式先读代码再定）

**提示词**：
```
把 4-10 ~ 4-12 的接口数据整合成一个"数据分析"页面——这是论文"数据可视化"章节的全部素材，也是答辩现场演示的主页面。

动手前先读 vue/src 的路由和菜单组织方式（router 配置 + 菜单在哪注册），
新建页面就按现有页面的方式注册路由和菜单，不要发明新结构。
用 Vue 2 + Element-UI 2.x 语法（同 3-14 的约定）。

要求：
1. 页面（在 Dashboard.vue 加 tab，或新建 Analysis.vue 注册路由，按读到的现状选更顺的一种）
   放四张图，全部用 ECharts（项目已有 6.1.0，
   复用 Dashboard.vue 里现成的初始化 / resize 自适应写法，不要重新封装一套）：
   ① 部署成败按日：柱状（总次数）+ 折线（失败次数）双系列，数据来自 4-10 的 byDay
   ② 失败分布饼图：按应用、按服务器两个维度可切换，数据来自 4-10 的 byApp / byServer
   ③ 错误 TOP-N 横向柱状图，数据来自 4-11
   ④ 告警趋势双线图（实际值 + 移动平均），数据来自 4-12
2. 页面顶部放一个天数选择（7 / 30 / 90），切换时四张图一起重新请求
3. 接口调用走 request.js 封装（自动带 token），不要手写 axios
4. 现有 4-06 的 14 天部署趋势保留在总览页不动——本页是深度分析页，
   在页头加一行说明文字，避免答辩时被问"这两个图是不是重复的"
5. 每张图要处理空数据状态（没数据显示"暂无数据"，不要白屏）
6. 不要改动 Dashboard.vue 已有的 5 个面板和 4-06 的趋势图
```

**怎么验证**：
1. 页面四张图都有**真数据**（数据来自前面真实跑出来的部署、执行日志、告警，不是硬编码）
2. 切换 7 / 30 / 90，四张图跟着变
3. 把某应用连跑两次失败部署，饼图里它的失败占比变大（验证图吃的是活数据）
4. 浏览器控制台无报错；窗口缩放时图表自适应不溢出（同 4-06 的验证标准）

---

## 做完之后

| 你得到什么 | 答辩怎么讲 |
|---|---|
| 服务器连不上，系统自己报警 | "不用等人发现，2 分钟内自动产生告警并推邮件" |
| 报警不会刷屏 | "同一个故障只报一条，恢复时通知一次，这是运维系统的基本素养" |
| 应用探活 | "应用挂了我能知道，而且是按部署路径判断的，贴合我的部署方式" |
| 部署后探活失败会记到部署单上 | "故障能追溯到是哪次变更引起的" |
| 14 天部署趋势 | "这是部署频率，配合交付成功率，就是 DORA 的前两个指标" |
| 部署成败多维统计（4-10） | "按应用、按服务器、按日三个维度统计成败，一个接口全给" |
| 错误日志 TOP-N（4-11） | "执行日志不是存着看的，能聚合出'哪些部署错得最多'" |
| 告警趋势 + 7 日移动平均（4-12） | "移动平均滤掉单点毛刺，趋势一眼看清" |
| 数据分析页四张真数据图表（4-13） | "采集→存储→分析→可视化整条链路现场演示" |

**到这里，四个里程碑全部完成。** 你的系统从"能跑的管理后台"变成了"会自己看自己、自己报警的运维平台"，而且部署记录、执行日志、告警这些运维数据都能统计、能分析、能可视化——这正是"大数据技术"专业在毕设里的落点。
