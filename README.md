# DevOps 发布管理系统

毕业设计题目：《DevOps 发布管理系统的设计与实现——面向中小软件开发团队》。用资产台账把 **项目 → 服务器 → 应用 → 部署单 → 告警** 串成闭环，部署单走审批状态机，成功后才把应用置为在线并反写版本。

| 端 | 技术栈 | 目录 | 端口 |
|---|---|---|---|
| 后端 | Spring Boot 2.5.9 + MyBatis + MySQL + JWT | `manager/springboot` | `9090` |
| 前端 | Vue 2 + Element UI + axios + Vue Router | `manager/vue` | `8080` |
| 数据库 | MySQL 8，库名 `manager` | `manager/springboot/src/main/resources/sql` | `3306` |

---

## 项目架构

### 目录

```
graduation-project/
├── manager/
│   ├── springboot/          # 后端
│   │   └── src/main/
│   │       ├── java/com/example/
│   │       │   ├── controller/     # REST
│   │       │   ├── service/        # 业务与状态机落地
│   │       │   ├── mapper/         # MyBatis
│   │       │   ├── entity/
│   │       │   ├── common/         # Result、枚举、DeployStatusMachine、JWT/CORS
│   │       │   └── exception/
│   │       └── resources/sql/      # schema.sql / data.sql
│   └── vue/                 # 前端 SPA
└── manager.sql              # 早期脚手架，仅 admin + notice，勿与新脚本混跑
```

### 分层

```
浏览器 (Vue2 :8080)
    │  axios + Header: token
    ▼
Spring MVC (JwtInterceptor)
    │  统一 Result{code,msg,data}
    ▼
Service（角色校验 + 事务 + 状态机）
    ▼
MyBatis Mapper  ↔  MySQL manager
```

- 前端直连 `http://localhost:9090`（`.env.development`），不做 webpack 代理；后端 `CorsFilter` 放行跨域。
- JWT 放在请求头 `token`，签名密钥为配置项 `jwt.secret`（本地见 `application-dev.yml`，生产见环境变量 `JWT_SECRET`），有效期 2 小时。
- 登录按用户名查 `person` 表，角色存在该行的 `role` 字段（`ADMIN` / `DEV` / `TL` / `QA` / `PM` / `OPS`）；**新密码用 BCrypt 存储**，库里历史遗留的 32 位 `MD5(明文 + devops_salt_2026)` 密文仍可登录（兼容读，见 `utils/PasswordUtils.java`）。
- 自动部署按应用上的仓库地址在目标机拉取对应版本。生产构建未配置 `VUE_APP_API_BASE_URL` 时，前端按当前主机名访问 9090。

### 资产闭环（以应用为枢纽）

```
project ──► app ◄── server
              │
              ├── deploy ──► deploy_log / deploy_execution
              └── alarm
```

页面上也按这个链路跳转：项目编号 → 主机名 → 应用编号 → 部署单号 → 告警编号。

### 角色与菜单

| 角色 | 账号 | 能做什么 |
|---|---|---|
| ADMIN | `person.role = ADMIN` | 全量菜单；绕过角色校验；管团队成员与发布通知 |
| DEV | `person.role = DEV` | 发起发布单；技术审查前只能改自己的单 |
| TL | `person.role = TL` | 审查生产发布变更，通过后进入产品验收 |
| QA | `person.role = QA` | 验收测试环境的发布单，通过后交给运维 |
| PM | `person.role = PM` | 验收技术审查已通过的生产发布单，通过后交给运维 |
| OPS | `person.role = OPS` | 审批已验收的发布单；执行、回滚；接手并闭环告警 |

侧边栏分三组：发布看板全员可见；项目资产里业务项目为 ADMIN+DEV+TL（开发维护资料，技术负责人分配开发负责人），应用服务为 ADMIN+DEV+OPS，部署环境为 ADMIN+OPS；发布管控里发布单对参与流程的角色可见，发布告警和发布报表仅 ADMIN+OPS；团队设置为 ADMIN。人员账号在同一张 `person` 表。路由 `meta.roles` 与接口层再各卡一次。接口层的角色校验由 `AuthUtils.requireRole` 把关，其中 **ADMIN 会被无条件放行（旁路角色校验）**，便于演示与排障（前端登录与 WebSocket 握手同样接受 ADMIN）。

### 主要页面

`/dashboard` · `/project` · `/server` · `/app` · `/deploy` · `/alarm` · `/analysis` · `/person` `/notice` · `/login` `/register`

---

## 设计思想

### 1. 状态机约束部署，而不是靠页面按钮

部署单只允许合法跳转，集中在 `DeployStatusMachine`：

新建发布单的起点由目标服务器环境决定：

```
开发环境 DEV：  提交 ──► PENDING
测试环境 TEST： 提交 ──► TESTING ──通过──► PENDING
生产环境 PROD： 提交 ──► REVIEW ──通过──► ACCEPTING ──通过──► PENDING

PENDING ──运维审批──► APPROVED ──开始──► DEPLOYING ──完成──► SUCCESS
   │                                      │
   └──驳回──► REJECTED                     └──完成──► FAILED

任一审查或验收环节驳回，也进入 REJECTED。
FAILED ──回滚──► DEPLOYING（类型改为 ROLLBACK）──完成──► SUCCESS / FAILED
```

技术审查只处理生产环境的 `REVIEW`，通过后进入产品验收。测试验收只处理测试环境的 `TESTING`，通过后交给运维。产品验收只处理生产环境的 `ACCEPTING`。非法跳转直接抛业务异常。每次流转在同一事务里写 `deploy_log`。

### 2. 应用台账跟部署结果对齐

开始部署时结果未知，不能提前把应用写成「在线」。

| 时机 | 应用状态 | 版本 |
|---|---|---|
| `startDeploy` / `rollback` | `DEPLOYING` 部署中 | 不改 |
| `finishDeploy(SUCCESS)` | `ONLINE` 在线 | 反写目标版本 |
| `finishDeploy(FAILED)` | `OFFLINE` 离线 | 不改 |

这样失败后资产台账不会永久停留在「在线」。

### 3. 权限前后端各守一层

前端按角色藏菜单、藏按钮；后端 `AuthUtils.requireRole` 卡写操作。DEV 不能审批执行，OPS 不能替别人改待审批单。ADMIN 走旁路，方便演示和排障。

### 4. 软删除 + 外键 + 业务锁

业务表用 `deleted` 逻辑删，查询默认 `deleted = 0`。物理外键挡住误删有依赖的项目/服务器/应用。在线应用、进行中项目、停机/维护中的服务器等再加一层业务锁。

### 5. 真实 SSH 部署（可选手动）

开始部署时选执行方式：

| 模式 | 行为 |
|---|---|
| `AUTO` | 事务提交后异步 SSH 到目标机跑脚本，按退出码判成功/失败 |
| `MANUAL` | 只进入「部署中」；页面可开 WebSocket 终端，运维自行操作后点成功/失败 |

服务器保存时：用登记的 SSH 密码登录一次，在目标机写入公钥、本地密文保存私钥，之后探活与自动部署都走密钥（`ssh_auth_type=KEY`）。生产凭据用 `CREDENTIAL_KEY` 加密入库。

---

## 怎么使用

### 环境

- JDK 8、Maven、Node.js / npm
- MySQL 8（本机服务名一般是 `MySQL`，须处于 Running）

`application-dev.yml` 默认：库 `manager`，用户 `root` / `123456`，以及本地 `jwt.secret`。不一致就改开发配置或环境变量。

### 建库（只做一次）

不要执行仓库根目录的 `manager.sql`（早期两张表，会 `DROP`）。用新脚本：

```sql
CREATE DATABASE manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE manager;
SOURCE manager/springboot/src/main/resources/sql/schema.sql;
SOURCE manager/springboot/src/main/resources/sql/data.sql;
```

已有旧库升级顺序见 `manager/springboot/src/main/resources/sql/README.md`。`data.sql` 和 `demo_seed.sql` 由同目录 `_gen_demo_seed.py` 生成，改演示数据请改生成器再运行。全新安装不要再跑升级脚本。生产启动要设置 `SPRING_PROFILES_ACTIVE=prod`；不设置时默认是 `dev`。

### 启动（本机 IDE / 命令行）

```bash
# 后端（默认 spring.profiles.active=dev）
cd manager/springboot
mvn spring-boot:run

# 前端（另开终端）
cd manager/vue
npm install
npm run serve
```

浏览器打开 [http://localhost:8080/](http://localhost:8080/)。后端健康检查：`http://localhost:9090/actuator/health`。

MySQL 没开时，登录会报 JDBC `Communications link failure`。Windows 可先 `net start MySQL`（通常要管理员权限）。

### 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 超级管理员 | `admin` | `admin` |
| 开发 | `dev1` | `123456` |
| 技术负责人 | `tl1` | `123456` |
| 测试 | `qa1` | `123456` |
| 产品经理 | `pm1` | `123456` |
| 运维 | `ops1` | `123456` |

登录页可直接点角色预填。开发/运维也可走 `/register` 注册（不能注册 ADMIN）。

### 推荐演示路径

1. **OPS**（或 ADMIN）→ 服务器管理：填好 SSH 用户/端口/登记密码/部署目录并保存，确认认证方式变为密钥。
2. **DEV** → 部署管理 → 发起申请（选应用、目标版本）。应用需先填写代码仓库；自动部署会生成并执行拉取该版本代码的脚本。
3. **OPS** → 审批通过 → 开始部署，选「自动部署」：应用变为「部署中」，SSH 跑完后自动成功/失败并反写版本。
4. 也可选「手动部署」：打开终端操作后，再点成功/失败。
5. 成功单可回滚（自动模式同样走 SSH）。告警：接手 → 解决；运维分析页看统计。

### 常用接口

CRUD 统一：`POST /{模块}/add`、`PUT /update`、`DELETE /delete/{id}`、`GET /selectPage`。部署与告警的写操作：

| 方法 | 路径 | 角色 |
|---|---|---|
| POST | `/login` | 匿名 |
| PUT | `/deploy/gate/{id}?result=PASS\|REJECTED` | TL / QA / PM（按当前状态） |
| PUT | `/deploy/approve/{id}?result=APPROVED\|REJECTED` | OPS |
| PUT | `/deploy/startDeploy/{id}?executeMode=AUTO\|MANUAL` | OPS |
| PUT | `/deploy/finishDeploy/{id}?result=SUCCESS\|FAILED` | OPS（手动收尾） |
| PUT | `/deploy/rollback/{id}` | OPS |
| PUT | `/alarm/handle/{id}`、`/alarm/resolve/{id}` | OPS |

注册：`POST /register`。修改密码：`PUT /updatePassword`。统计与分析：`/statistics/*`（含部署趋势、告警状态、错误 TOP 等）。WebSocket 终端：`ws://主机:9090/ssh/terminal?deployId={部署单id}&token={登录token}`，仅 OPS/ADMIN 且必须挂在一张部署单上（见 `common/config/SshWebSocketConfig.java`、`SshHandshakeInterceptor`）。开发环境前端把地址从 `VUE_APP_API_BASE_URL` 推导，不再拼页面自己的 `/api`。

### 自测

```bash
cd manager/springboot
mvn test
```

当前共 **11 个测试类 / 52 个用例**，覆盖部署状态机、CAS 审批、SSH 主机密钥校验、凭据加密、密码兼容、上传文件名安全等（如 `DeployServiceStatusTest`、`DeployMapperCasApproveXmlTest`、`DeployStatusMachineTest`、`SshHostKeyVerifierTest`）。前端改完后用 `npm run serve` 看页面即可。
