# DevOps 运维管理系统

毕业设计：面向开发 / 运维 / 管理员的发布部署管理系统。用资产台账把 **项目 → 服务器 → 应用 → 部署单 → 告警** 串成闭环，部署单走审批状态机，成功后才把应用置为在线并反写版本。

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
- 登录按 `admin` → `dev` → `ops` 三张账号表解析角色；密码为 `MD5(明文 + devops_salt_2026)`。

### 资产闭环（以应用为枢纽）

```
project ──► app ◄── server
              │
              ├── deploy ──► deploy_log / deploy_execution / deploy_artifact
              └── alarm
```

页面上也按这个链路跳转：项目编号 → 主机名 → 应用编号 → 部署单号 → 告警编号。

### 角色与菜单

| 角色 | 账号表 | 能做什么 |
|---|---|---|
| ADMIN | `admin` | 全量菜单；绕过 `AuthUtils.requireRole`；管开发/运维/公告 |
| DEV | `dev` | 发起部署申请；只能改自己的待审批单；可见项目 / 应用 / 部署 / 告警 / 分析 |
| OPS | `ops` | 管服务器与 SSH 凭据；审批、开始/结束部署、回滚；接手并闭环告警 |

侧边栏：仪表盘 / 应用 / 部署 / 告警 / 运维分析全员可见；**服务器仅 ADMIN+OPS**；项目 ADMIN+DEV；人员与公告仅 ADMIN。路由 `meta.roles` 与接口层再各卡一次。

### 主要页面

`/dashboard` · `/project` · `/server` · `/app` · `/deploy` · `/alarm` · `/analysis` · `/dev` `/ops` `/notice` · `/login` `/register`

---

## 设计思想

### 1. 状态机约束部署，而不是靠页面按钮

部署单只允许合法跳转，集中在 `DeployStatusMachine`：

```
PENDING ──审批──► APPROVED ──开始──► DEPLOYING ──完成──► SUCCESS
   │                                    │
   └──驳回──► REJECTED                   └──完成──► FAILED

SUCCESS ──回滚──► DEPLOYING（类型改为 ROLLBACK）──完成──► SUCCESS / FAILED
```

非法跳转直接抛业务异常。每次流转在同一事务里写 `deploy_log`（申请 / 通过 / 驳回 / 开始 / 成功 / 失败 / 回滚）。

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

服务器保存时：用登记的 SSH 密码登录一次，在目标机写入公钥、本地密文保存私钥，之后探活与自动部署都走密钥（`ssh_auth_type=KEY`）。生产凭据用 `CREDENTIAL_KEY` 加密入库。内网 Docker 一键起见 `manager/DEPLOY.md`。

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

已有旧库升级顺序见 `manager/springboot/src/main/resources/sql/README.md`（含 SSH、部署执行、制品、`execute_mode` 等 `upgrade_*.sql`）。全新安装不要再跑升级脚本。

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

要用 Docker 起全栈（含 MySQL），看 `manager/DEPLOY.md`，不要和本机 MySQL 抢 3306。

### 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 超级管理员 | `admin` | `admin` |
| 开发 | `dev1` | `123456` |
| 运维 | `ops1` | `123456` |

登录页可直接点角色预填。开发/运维也可走 `/register` 注册（不能注册 ADMIN）。

### 推荐演示路径

1. **OPS**（或 ADMIN）→ 服务器管理：填好 SSH 用户/端口/登记密码/部署目录并保存，确认认证方式变为密钥。
2. **DEV** → 部署管理 → 发起申请（选应用、目标版本）；制品需先上传（自动部署依赖制品）。
3. **OPS** → 审批通过 → 开始部署，选「自动部署」：应用变为「部署中」，SSH 跑完后自动成功/失败并反写版本。
4. 也可选「手动部署」：打开终端操作后，再点成功/失败。
5. 成功单可回滚（自动模式同样走 SSH）。告警：接手 → 解决；运维分析页看统计。

### 常用接口

CRUD 统一：`POST /{模块}/add`、`PUT /update`、`DELETE /delete/{id}`、`GET /selectPage`。部署与告警的写操作：

| 方法 | 路径 | 角色 |
|---|---|---|
| POST | `/login` | 匿名 |
| PUT | `/deploy/approve/{id}?result=APPROVED\|REJECTED` | OPS |
| PUT | `/deploy/startDeploy/{id}?executeMode=AUTO\|MANUAL` | OPS |
| PUT | `/deploy/finishDeploy/{id}?result=SUCCESS\|FAILED` | OPS（手动收尾） |
| PUT | `/deploy/rollback/{id}` | OPS |
| PUT | `/alarm/handle/{id}`、`/alarm/resolve/{id}` | OPS |

统计与分析：`/statistics/*`（含部署趋势、告警状态、错误 TOP 等）。WebSocket 终端：`/ssh/{serverId}`。

### 自测

```bash
cd manager/springboot
mvn test
```

当前覆盖部署状态机等。前端改完后用 `npm run serve` 看页面即可。
