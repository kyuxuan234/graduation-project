# 内网部署（Docker）

本机 IDE / 命令行直连本机 MySQL 的用法见仓库根目录 `README.md`。本文只写 Docker Compose 一键起全栈。

以下命令都在 `manager/` 目录执行（本文件所在目录）。镜像名在 `docker-compose.yml` 里写成 `manager-backend:latest`、`manager-frontend:latest`。

曾在 WSL2（Ubuntu 24.04，Docker 29）跑通：三个容器 `healthy`，`/actuator/health` 为 `UP`，`http://localhost:8080/login` 返回 200，经 nginx 的 `/api/login` 用 `admin` / `admin` 能登录。备份命令按 compose 写法核对过语法。

## 1. 前置要求

- Docker 20.10 或更高。`depends_on` 的 `condition: service_healthy` 需要 Compose V2（命令是 `docker compose`，不是带横线的旧命令）。
- 需要能访问的端口：
  - `8080`：前端
  - `9090`：后端
  - `3306`：数据库。默认**不**映射到宿主机，只在 Compose 网络里用服务名 `mysql` 访问，避免和本机已有的 MySQL 抢端口。要从别的机器连库，确认宿主机 3306 空闲后，再给 `mysql` 服务加上 `ports: ["3306:3306"]`。
- 服务器建议至少 2 核 4G。后端 JVM 是 `-Xms256m -Xmx512m`，还要留给 MySQL 和前端。
- 首次构建要能访问外网：拉 `mysql:8.0`、`maven:3.8-openjdk-8`、`eclipse-temurin:8-jre`、`node:16-alpine`、`nginx:1.21-alpine`，并下载 Maven、npm 依赖。当前镜像站对官方 `openjdk:8-jre-slim` 返回 403，所以运行阶段不用它。不能出网时走第 6 节。

## 2. 首次部署

1. 把代码拷到服务器，进入 `manager/`。

2. 复制环境文件并填写：

```bash
cp .env.example .env
```

Windows：

```bat
copy .env.example .env
```

| 变量 | 怎么填 |
|---|---|
| `DB_HOST` / `DB_PORT` | 只给本机不用 Docker 的开发配置。容器里的后端固定连 `mysql:3306`，改这两项不会改变容器里的数据库地址 |
| `DB_NAME` | 库名，示例是 `manager`。要和容器里新建的库一致 |
| `DB_USERNAME` | 填 `root`。容器用这个账号连库 |
| `DB_PASSWORD` | MySQL root 密码。不能空 |
| `JWT_SECRET` | 随机串，不能空，不能用示例值。只用于签发/校验登录 token，与账号密码无关 |
| `CREDENTIAL_KEY` | SSH 密码与私钥入库前的加密密钥。生产必填；留空则凭据按明文存，探活会记配置错误 |
| `CORS_ALLOWED_ORIGINS` | 浏览器直接访问后端时的来源。经前端 `/api` 转发时是同源，这项不参与页面请求。示例 `http://localhost:8080` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | 发告警邮件用。密码留空则不发信，不阻止启动 |

生成 `JWT_SECRET`：

```bash
openssl rand -base64 32
```

Windows PowerShell：

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))
```

3. 启动：

```bash
./start.sh
```

Windows 双击 `start.bat`，或在 `manager/` 下执行 `start.bat`。

4. 验证：

- 浏览器打开 `http://localhost:8080`，应看到登录页。演示账号在 `data.sql`：`admin` / `admin`，`dev1` / `123456`，`ops1` / `123456`。
- 健康检查不需要登录：

```bash
curl http://localhost:9090/actuator/health
```

应返回 `{"status":"UP"}`。

首次构建要拉镜像、下依赖，20 秒后页面可能还没起来。用 `docker compose ps` 看 `backend` 是否变成 `healthy`。

## 3. 日常运维

看后端日志：

```bash
docker compose logs -f backend
```

只重启后端：

```bash
docker compose restart backend
```

备份（在 `manager/` 执行，密码用容器里的 `MYSQL_ROOT_PASSWORD`，不会写进命令历史里的明文）：

```bash
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" manager' > backup.sql
```

库名如果改过，把 `manager` 换成 `.env` 里的 `DB_NAME`。

恢复：

```bash
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" manager' < backup.sql
```

停止但保留数据：

```bash
./stop.sh
```

Windows 用 `stop.bat`。这是 `docker compose down`，数据卷还在。

## 4. 升级已有环境

`schema.sql` 只给**全新安装**用。Docker 只在 `mysql-data` 卷为空的第一次启动时执行 `/docker-entrypoint-initdb.d/` 里的脚本。卷里已经有数据时，改了 `schema.sql` 再 `docker compose up`，脚本不会重跑。

已有库要升级时：

1. 先按上一节备份。
2. 打开 `springboot/src/main/resources/sql/README.md`，按里面的顺序手工执行 `upgrade_*.sql`。
3. 不要在全新库上再跑这些升级脚本。

改动要进已有卷，只有两条路：手工执行 SQL，或者 `docker compose down -v` 清空卷后重来。`-v` 会删掉 MySQL 数据。`stop.bat` / `stop.sh` 不会加 `-v`。

## 5. 常见问题

容器起不来，先看日志：

```bash
docker compose logs
docker compose logs backend
docker compose ps
```

数据库连不上：

- `.env` 里 `DB_PASSWORD`、`JWT_SECRET` 是否为空。生产配置没有默认值，空着后端会退出。`CREDENTIAL_KEY` 为空能启动，但 SSH 凭据不会加密。
- `docker compose ps` 里 `mysql` 是否 `healthy`。不健康时看 `docker compose logs mysql`。
- 后端连的是服务名 `mysql`，不是 `.env` 里的 `DB_HOST=localhost`。

页面能开，接口 404：

- 生产包的接口前缀必须是 `/api`。`vue/.env.production` 里是 `VUE_APP_API_BASE_URL=/api`。
- `vue/nginx.conf` 的 `location /api/` 会把前缀去掉再转到 `http://backend:9090/`。例如浏览器请求 `/api/login`，后端收到的是 `/login`。
- 改完 nginx 或前端包后要重新构建：`docker compose up -d --build frontend`。

上传的文件在宿主机 `manager/data/files`。这个目录已加入 `.gitignore`。

## 6. 离线部署

开发机可以出网、目标机不能出网时，在开发机的 `manager/` 执行：

```bash
docker compose build
docker save -o manager-images.tar manager-backend:latest manager-frontend:latest mysql:8.0
```

`docker save` 之前先 `docker images`，名字必须和上面三行一致。把 `manager-images.tar`、代码和填好的 `.env` 拷到目标机，在目标机执行：

```bash
docker load -i manager-images.tar
```

然后按第 2 节启动。`docker-compose.yml` 里仍然保留 `build`。本地已经有同名镜像时，`docker compose up` 不会为了找基础镜像再去外网拉。目标机上如果镜像不存在，Compose 仍会尝试构建，那一步需要外网。

能出网的机器不需要这一节，按第 1 节直接构建即可。

## 7. 探活

后端默认每 2 分钟检查一台状态为运行中的服务器，半分钟后再检查在线应用。线程名前缀是 `health-check-`。本地 `application-dev.yml` 里 `healthcheck.enabled` 是 false，容器里的生产配置是 true。

服务器要填 SSH 用户、端口、登记密码和部署目录，否则记一条配置不完整的告警，不会去连。保存时系统会用登记密码登录一次，生成私钥并写入目标机 `authorized_keys`，之后探活与自动部署走密钥（`ssh_auth_type=KEY`）。生产环境的 SSH 密码和私钥用 `CREDENTIAL_KEY` 加密后入库；密钥为空时仍能启动，但凭据按明文保存，探活会把这种情况当成配置错误。

同一台机器或同一个应用，未处理和处置中的自动告警在 `healthcheck.auto-alarm-cooldown-minutes`（默认 30 分钟）内不重复创建。冷却结束后如果还是失败，会再记一条「故障复发」。探活恢复只在告警内容末尾补「探活已恢复」，不会自动改成已解决。

应用优先用它自己的端口做本机连通检查。没有端口时，改为检查部署目录下的 `current` 是否存在。单台失败不会中断其他主机。
