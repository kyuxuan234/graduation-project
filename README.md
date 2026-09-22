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
- JWT 放在请求头 `token`，签名密钥是当前用户的密码哈希，改密后旧 token 立即失效，有效期 2 小时。
- 登录按 `admin` → `dev` → `ops` 三张账号表解析角色；密码为 `MD5(明文 + devops_salt_2026)`。

### 资产闭环（以应用为枢纽）

```
project ──► app ◄── server
              │
              ├── deploy ──► deploy_log
              └── alarm
```

页面上也按这个链路跳转：项目编号 → 主机名 → 应用编号 → 部署单号 → 告警编号。

### 角色与菜单

| 角色 | 账号表 | 能做什么 |
|---|---|---|
| ADMIN | `admin` | 全量菜单；绕过 `AuthUtils.requireRole`；管开发/运维/公告 |
| DEV | `dev` | 发起部署申请；只能改自己的待审批单 |
| OPS | `ops` | 审批、开始/结束部署、回滚；接手并闭环告警 |

侧边栏：所有人可见仪表盘 / 服务器 / 应用 / 部署 / 告警；ADMIN+DEV 可见项目；人员与公告仅 ADMIN。接口层再校验一次。

### 主要页面

`/dashboard` 仪表盘 · `/project` 项目 · `/server` 服务器 · `/app` 应用 · `/deploy` 部署（含流转日志） · `/alarm` 告警 · `/dev` `/ops` `/notice` · `/login` `/register`

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

### 5. 模拟发布，不接真实流水线

没有 SSH / K8s / Jenkins。OPS 在页面上点「开始 / 成功 / 失败」推进状态，用来把审批、台账、告警、统计跑通，适合毕业设计演示。

---

## 怎么使用

### 环境

- JDK 8、Maven、Node.js / npm
- MySQL 8（本机服务名一般是 `MySQL`，须处于 Running）

`application.yml` 默认：库 `manager`，用户 `root` / `123456`。不一致就改这一处。

### 建库（只做一次）

不要执行仓库根目录的 `manager.sql`（早期两张表，会 `DROP`）。用新脚本：

```sql
CREATE DATABASE manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE manager;
SOURCE manager/springboot/src/main/resources/sql/schema.sql;
SOURCE manager/springboot/src/main/resources/sql/data.sql;
```

已有旧库需要补外键、把明文密码刷成加盐 MD5、给 `alarm` 补 `deploy_no`、或给旧 `admin` 表补 `status` / `deleted` 时，再用同目录下的 `upgrade_fk.sql`、`upgrade_password.sql`、`upgrade_alarm.sql`、`upgrade_admin.sql`。

### 启动

```bash
# 后端
cd manager/springboot
mvn spring-boot:run

# 前端（另开终端）
cd manager/vue
npm install
npm run serve
```

浏览器打开 [http://localhost:8080/](http://localhost:8080/)。后端健康检查：`http://localhost:9090/`。

MySQL 没开时，登录会报 JDBC `Communications link failure`。Windows 可先 `net start MySQL`（通常要管理员权限）。

### 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 超级管理员 | `admin` | `admin` |
| 开发 | `dev1` | `123456` |
| 运维 | `ops1` | `123456` |

登录页可直接点角色预填。开发/运维也可走 `/register` 注册（不能注册 ADMIN）。

### 推荐演示路径

1. **DEV** 登录 → 部署管理 → 发起申请（选应用、目标版本）。
2. **OPS** 登录 → 审批通过 → 开始部署：应用变为「部署中」，版本仍是旧值。
3. 点成功：应用「在线」，当前版本 = 目标版本；点失败：应用「离线」，不再显示在线。
4. 成功单可回滚，应用再次进入「部署中」，再走一次完成。
5. 告警管理：接手 → 解决，看仪表盘闭环率变化。

### 常用接口

CRUD 统一：`POST /{模块}/add`、`PUT /update`、`DELETE /delete/{id}`、`GET /selectPage`。部署与告警的写操作：

| 方法 | 路径 | 角色 |
|---|---|---|
| POST | `/login` | 匿名 |
| PUT | `/deploy/approve/{id}?result=APPROVED\|REJECTED` | OPS |
| PUT | `/deploy/startDeploy/{id}` | OPS |
| PUT | `/deploy/finishDeploy/{id}?result=SUCCESS\|FAILED` | OPS |
| PUT | `/deploy/rollback/{id}` | OPS |
| PUT | `/alarm/handle/{id}`、`/alarm/resolve/{id}` | OPS |

统计：`/statistics/count`、`/deployTrend`、`/deployStatus`、`/alarmStatus`、`/appRisk`。

### 自测

```bash
cd manager/springboot
mvn test
```

当前覆盖部署状态机合法/非法跳转。前端改完后用 `npm run serve` 看页面即可。
