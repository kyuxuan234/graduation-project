# 第 2 步 · 跑起来

> 这个文件是干嘛的：让这套系统能在内网服务器上用一条命令装起来、跑起来，不再靠手工拷贝 jar 包。
> 做完能怎样：换一台干净的服务器，一条命令起全栈；改配置不用重新打包。
> 怎么做：从 2-01 往下按顺序做。这一步会新建文件，**不会改动已有业务代码**，风险低。

---

## 先说清楚现状

| 项 | 现状 |
|---|---|
| 容器化 | **完全没有**。没有 Dockerfile、没有 docker-compose.yml、没有 .dockerignore |
| 数据库初始化 | 有 6 个 SQL 文件在 `springboot/src/main/resources/sql/`：`schema.sql`（建表）、`data.sql`（初始数据）、`upgrade_fk.sql`、`upgrade_password.sql`、`upgrade_admin.sql`、`upgrade_alarm.sql` |
| 数据库版本管理 | **手工执行**、**没有 Flyway**。改表靠人记得跑哪个 `upgrade_*.sql`；脚本本身用存储过程 + `information_schema` 判断，可重复执行 |
| 配置分环境 | 已做好（`application.yml` / `-dev.yml` / `-prod.yml`） |
| 前端部署 | 手工 `npm run build` 后拷贝 |

**关于那 4 个 upgrade 脚本**：它们的质量比一般毕设高——用存储过程 + `information_schema` 判断列/索引是否存在，**可以重复执行不出错**，还带历史数据回填。所以第 2 步不是"重写脚本"，而是"给它加上自动执行和顺序说明"。这个区别很重要。

**命名约定（重要）**：这个项目**没有 Flyway**，脚本都是手动执行的 `upgrade_*.sql`。
所以**不要**把它们改成 `V1__xxx.sql` / `V2__xxx.sql` 这种 Flyway 风格命名——
那会让人误以为有版本迁移框架在管顺序，实际没有。顺序靠 `sql/README.md` 写清楚即可。

---

## 2-01 · 给后端写 Dockerfile

**改什么**：新建 `springboot/Dockerfile`（1 个新文件）

**提示词**：
```
在 D:\Encoding\IntelliJProject\graduation-project\manager\springboot\ 下新建 Dockerfile。
这是一个 Spring Boot 2.5.9 / Java 8 项目，用 maven 打包，产物是 target/*.jar。

要求：
1. 用多阶段构建：
   - 第一阶段用 maven:3.8-openjdk-8 编译，只复制 pom.xml 和 src，先跑 mvn dependency:go-offline
     再跑 mvn package -DskipTests（分层是为了让依赖缓存住，改代码不用重下依赖）
   - 第二阶段用 openjdk:8-jre-slim，只复制第一阶段的 jar
2. 运行用户不要用 root，创建一个 appuser 再切过去
3. 暴露 9090 端口（项目 server.port=9090）
4. 启动命令用 ENTRYPOINT，设置合理 JVM 参数：
   -Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8
5. 不要硬编码任何密码、数据库地址；这些都靠环境变量注入（application-prod.yml 已支持）
6. 声明一个 VOLUME /app/files，因为上传的文件存在 user.dir/files 下

先在 pom.xml 里确认 finalName 或 artifactId，确保 COPY 的 jar 路径写对。
```

**怎么验证**：
```
cd springboot
docker build -t manager-backend:test .
```
构建成功（第一次会慢，要下依赖）。然后用 `docker images` 能看到 `manager-backend` 这个镜像，体积在 200-300MB 之间（多阶段构建的效果）。

---

## 2-02 · 给前端写 Dockerfile

**改什么**：新建 `vue/Dockerfile`（1 个新文件）

**提示词**：
```
在 D:\Encoding\IntelliJProject\graduation-project\manager\vue\ 下新建 Dockerfile。
这是 Vue 2.6 + vue-cli 5 项目，构建产物在 dist/。

要求：
1. 多阶段构建：
   - 第一阶段用 node:16-alpine（Vue 2 + vue-cli 5 在 node 16 上最稳），
     先 COPY package*.json 跑 npm install，再 COPY 全部源码跑 npm run build
   - 第二阶段用 nginx:1.21-alpine，把 dist/ 复制到 /usr/share/nginx/html
2. 复制一份自定义 nginx 配置（见下一条，本步骤先写好引用路径 /etc/nginx/conf.d/default.conf）
3. 暴露 80 端口
4. 不要在这个 Dockerfile 里写死后端地址——nginx 配置里会处理转发

注意：先读 vue/package.json 确认 build 脚本名字，不要假设一定是 "build"。
```

**怎么验证**：
```
cd vue
docker build -t manager-frontend:test .
```
构建成功。用 `docker run --rm -p 8081:80 manager-frontend:test` 起一个，浏览器打开 `localhost:8081`，应该能看到登录页（此时接口调不通是正常的，下一步解决）。

---

## 2-03 · 写 nginx 配置，把接口请求转发给后端

**改什么**：新建 `vue/nginx.conf`（1 个新文件）

**提示词**：
```
新建 D:\Encoding\IntelliJProject\graduation-project\manager\vue\nginx.conf。

背景：前端是 Vue Router 的 history 模式（先读 vue/src/router/index.js 确认 mode 是不是 history），
直接访问子路由（如 /manager/deploy）会 404，必须配置 try_files 回退到 index.html。
接口路径统一走 /api（先读 vue/src/utils/request.js 确认 baseURL 到底是什么，以实际值为准）。

要求：
1. server 监听 80
2. server 块里加 client_max_body_size 100m;
   nginx 默认请求体上限只有 1MB——系统的文件上传、以及第 3 步的制品上传
   （jar 包几十 MB）都走 /api 转发，超过 1MB 会直接 413。
   这个问题现在自测不会暴露（不传大文件），到第 3 步才炸，必须现在配掉。
3. location / 用 try_files $uri $uri/ /index.html 处理前端路由
4. 把接口路径转发到后端容器：
   location /api/ {
       proxy_pass http://backend:9090/;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
   }
   注意：backend 是 docker-compose 里的服务名，9090 是后端端口。
   如果 request.js 里的 baseURL 不是 /api 开头，请告诉我实际值，不要擅自改前端代码。
5. gzip 开启，压缩 js/css/json
6. 静态资源加缓存：location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ 设 7 天过期
7. 不要配置 SSL（内网环境，先用 HTTP）

如果前端 baseURL 是硬编码的完整地址（如 http://localhost:9090），不要改它——
在第 2-05 步会用环境变量处理，这一步只要保证 nginx 能正确转发。
```

**怎么验证**：
1. 确认 `vue/src/utils/request.js` 里的 baseURL 是 `/api` 或空（相对路径）
2. 如果它是硬编码的 `http://localhost:9090`，**停下来记下来**，第 2-05 步会处理

---

## 2-04 · 写 docker-compose，把三个服务串起来

**改什么**：新建项目根 `docker-compose.yml` + `.env.example`（2 个新文件）

**提示词**：
```
在 D:\Encoding\IntelliJProject\graduation-project\manager\ 根目录新建 docker-compose.yml。
包含三个服务：mysql、backend、frontend。

要求：

【mysql 服务】
- 镜像 mysql:8.0
- 环境变量：MYSQL_ROOT_PASSWORD、MYSQL_DATABASE（默认 manager）、TZ=Asia/Shanghai
  关于 TZ 必须说明：容器默认 UTC，而 DATETIME 的默认值（CURRENT_TIMESTAMP）
  由 MySQL 会话时区决定——JVM 侧设时区管不到它。不配的话 create_time 整体偏 8 小时，
  第 4 步的 14 天趋势按 DATE_FORMAT(create_time) 分组会把日期切错位。
- 字符集参数：--character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
- 数据持久化：把 mysql-data 卷挂到 /var/lib/mysql
- 初始化脚本挂载：
  ./springboot/src/main/resources/sql/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
  ./springboot/src/main/resources/sql/data.sql:/docker-entrypoint-initdb.d/02-data.sql
  注意：mysql 官方镜像会按文件名顺序自动执行 /docker-entrypoint-initdb.d/ 下的脚本，
  加数字前缀是为了保证 schema 先于 data。
  那 4 个 upgrade_*.sql 不要挂进去（它们针对的是已有库升级，全新安装用 schema.sql 就够——
  这是 upgrade_alarm.sql / upgrade_admin.sql 文件头注释里写明的
  "全新安装走 schema.sql，不必再执行本脚本"）
- 健康检查：mysqladmin ping，间隔 10s，重试 5 次

**⚠️ 关于初始化脚本的一个重要前提，请写进 compose 文件的注释里：**
MySQL 官方镜像**只在数据卷为空的首次启动时**执行 /docker-entrypoint-initdb.d/ 下的脚本。
一旦 mysql-data 卷已经有数据，这些脚本**再也不会自动执行**，即使你改了 schema.sql。
所以：
- 首次部署（卷是空的）→ 脚本自动执行，建表 + 灌数据 ✓
- 之后改了 schema.sql，重新 up → **不会生效**，需要手工执行，或先 down -v 清空卷（会丢数据）
- 这一点必须写在 compose 文件的注释里，并且要在 DEPLOY.md（第 2-11 步）里明说

【backend 服务】
- build 上下文指向 ./springboot
- 依赖 mysql，并且要等 mysql 健康后再启动（depends_on 用 condition: service_healthy）
- 环境变量从 .env 读：SPRING_PROFILES_ACTIVE=prod、DB_HOST=mysql、DB_PORT=3306、
  DB_NAME、DB_USERNAME、DB_PASSWORD、JWT_SECRET、CORS_ALLOWED_ORIGINS、
  MAIL_HOST、MAIL_PORT、MAIL_USERNAME、MAIL_PASSWORD
- 端口映射 9090:9090
- 挂载上传目录：./data/files:/app/files
  （注意：FileController 用的是 user.dir/files，所以在 Dockerfile 里 WORKDIR 必须是 /app）
- 日志：json-file 驱动，max-size 10m，max-file 3

【frontend 服务】
- build 上下文指向 ./vue
- 端口映射 8080:80
- 依赖 backend

【顶层】
- 不要写 version 字段（新版本 compose 已废弃）
- 定义 named volume: mysql-data
- 所有密码类环境变量用 ${VAR} 形式引用，不要写默认值

【三个服务都要】
- 加 restart: unless-stopped
  原因：Spring Boot 启动时数据源连不上会直接退出——depends_on 的 service_healthy
  只能缓解大部分时序问题，mysql 健康检查通过 ≠ 一定可接受连接（初始化窗口尾部）。
  backend 一旦退出，没有重启策略 compose 不会拉它起来，start.sh 跑完留下一具尸体。
  mysql 和 frontend 同理加上，保证"一条命令起全栈"真的可靠。

同时新建 .env.example（如果已存在就补充），列出 compose 需要的全部变量。
```

**怎么验证**：
```
cd D:\Encoding\IntelliJProject\graduation-project\manager
copy .env.example .env
（编辑 .env，填上真实密码和 JWT_SECRET）
docker compose config
```
`docker compose config` 应该能正常输出解析后的配置，没有语法错误、没有 `${VAR}` 未替换的警告。

---

## 2-05 · 让前端接口地址能通过环境变量切换

**改什么**：`vue/src/utils/request.js`、`vue/src/utils/axios.js`（**两个文件都要看一下**）、新建 `vue/.env.production`、`vue/.env.development`

**提示词**：
```
前端请求层不是只有一个文件，请先把这几个文件都读一遍再动手：
- vue/src/utils/request.js
- vue/src/utils/axios.js（如果存在）
- vue/src/api/ 目录下的文件（如果存在）

找出 baseURL / 后端地址到底是定义在哪个文件里的。

情况 A：如果 baseURL 已经是 '/api' 或空字符串（相对路径）→ 本步骤不需要改动，
        请直接回报"无需改动"，不要为了改而改。

情况 B：如果 baseURL 是硬编码的（如 'http://localhost:9090'）→ 按下面改：
1. 把定义 baseURL 的地方改为从环境变量读：
   baseURL: process.env.VUE_APP_API_BASE_URL || '/api'
   **只在真正定义 baseURL 的那个文件里改**，不要两个文件都改（会互相覆盖）
2. 新建 vue/.env.production，内容：
   VUE_APP_API_BASE_URL=/api
3. 新建 vue/.env.development，内容：
   VUE_APP_API_BASE_URL=http://localhost:9090
   （保持本地开发时直连后端，不用起 nginx）
4. 不要改动请求拦截器里 token 的注入逻辑
5. 不要改动响应拦截器对错误码的处理

注意：vue-cli 只暴露 VUE_APP_ 开头的变量给前端代码，所以名字不能改。
```

**怎么验证**：
1. 本地跑 `npm run serve`，打开页面，能正常登录、能正常调到后端（dev 环境变量生效）
2. 跑 `npm run build`，进 `dist/` 目录，用编辑器全局搜索 `localhost:9090`，
   应该**搜不到**——说明生产打包没有把开发地址带进去
3. 改完后再看一遍所有 `vue/src` 下的请求相关文件，确认 baseURL 只有一处定义
   （如果发现有两处，说明改漏了，第二处会把第一处覆盖掉）

---

## 2-06 · 加一个健康检查接口

**改什么**：`springboot/pom.xml` + `application.yml`（2 个文件）

**提示词**：
```
给项目加 Spring Boot Actuator，只开放健康检查端点（不要开放全部端点，那会泄露信息）。

1. 在 springboot/pom.xml 的 <dependencies> 里加：
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   （版本由 parent 管理，不要写 version）

2. 在 application.yml 里加：
   management:
     endpoints:
       web:
         exposure:
           include: health
     endpoint:
       health:
         show-details: when-authorized
     server:
       port: 9090

3. 重要：检查 springboot/src/main/java/com/example/common/config/JwtInterceptor.java，
   看它的拦截路径是怎么配的（先读 WebConfig.java 确认）。
   /actuator/health 必须**不被**登录拦截，否则 docker 健康检查永远失败。
   如果 JwtInterceptor 拦了所有路径，需要把 /actuator/** 加进排除列表。

4. 不要改动其他 pom 依赖，不要升级 Spring Boot 版本
```

**怎么验证**：
```
（本地启动应用）
curl http://localhost:9090/actuator/health
```
应该返回 `{"status":"UP"}`，**且不需要登录**。如果返回 401 或"未登录"，说明拦截器没排除掉，需要修正。

---

## 2-07 · 给 docker-compose 配上后端健康检查

**改什么**：`docker-compose.yml` + `springboot/Dockerfile`（2 个文件）

**提示词**：
```
上一步已经让后端暴露了 /actuator/health。

1. 在 springboot/Dockerfile 里加健康检查指令：
   HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
     CMD wget -qO- http://localhost:9090/actuator/health | grep -q '"status":"UP"' || exit 1
   注意：openjdk:8-jre-slim 镜像里可能没有 wget 或 curl，需要先 apt-get install 安装 wget
   （如果基础镜像本身没有包管理器，改用 curl，或换成 eclipse-temurin:8-jre 镜像）

2. 在 docker-compose.yml 的 frontend 服务里，把 depends_on 改成等待 backend 健康：
   depends_on:
     backend:
       condition: service_healthy

3. 不要改动已有的 mysql 健康检查
```

**怎么验证**：
```
docker compose up -d
docker compose ps
```
等待约 60 秒，`backend` 服务的 STATUS 列应该显示 `(healthy)`。如果一直显示 `(health: starting)` 然后变成 `(unhealthy)`，用 `docker compose logs backend` 看日志——大概率是 wget 不存在或健康检查路径被拦截。

---

## 2-08 · 写一个一键启动脚本

**改什么**：新建 `start.sh` + `start.bat`（2 个新文件）

**提示词**：
```
在项目根目录新建两个启动脚本，给不熟悉 docker 的人用。

【start.bat】（Windows）
1. 检查 docker 是否可用：docker --version，失败就提示"请先安装 Docker Desktop"并退出
2. 检查 .env 是否存在，不存在就提示"请复制 .env.example 为 .env 并填写配置"并退出
3. 执行 docker compose up -d --build
4. 等待 20 秒后打印访问地址：
   前端 http://localhost:8080
   后端 http://localhost:9090
5. 执行 docker compose ps 显示服务状态

【start.sh】（Linux/macOS）
功能同上，用 #!/bin/bash，加 set -e

要求：
1. 脚本里不要写任何密码
2. 用中文提示信息（面向的是你自己的答辩演示场景）
3. 不要自动执行 docker compose down（避免误删数据卷）
```

**怎么验证**：
```
（Windows）双击 start.bat
```
应该自动构建并启动三个容器，最后打印出访问地址。浏览器打开 `http://localhost:8080`，能看到登录页，能正常登录。

---

## 2-09 · 写停止和清理脚本

**改什么**：新建 `stop.bat` + `stop.sh`（2 个新文件）

**提示词**：
```
在项目根目录新建停止脚本。

【stop.bat】和【stop.sh】
1. 执行 docker compose down（停止并移除容器，**保留数据卷**）
2. 提示"服务已停止，数据已保留。如需彻底清理请执行：docker compose down -v"

要求：
1. 绝对不要在脚本里执行 docker compose down -v
   （-v 会删掉 mysql 数据卷，你的演示数据会全丢）
2. 不要执行 docker system prune
```

**怎么验证**：
```
（Windows）双击 stop.bat
docker compose ps
```
应该没有任何容器在跑。再执行 `docker volume ls | findstr mysql`，mysql 数据卷**应该还在**。

---

## 2-10 · 把 4 个升级脚本的执行顺序写清楚

**改什么**：新建 `springboot/src/main/resources/sql/README.md` + 给 4 个脚本文件头各加一行顺序注释（1 个新文件 + 4 个文件各加注释，**不改任何 SQL 逻辑**）

**提示词**：
```
现在 sql/ 目录下有 4 个升级脚本，命名看不出执行顺序：
upgrade_fk.sql
upgrade_password.sql
upgrade_admin.sql
upgrade_alarm.sql

**不要改成 V1__xxx.sql 这种 Flyway 命名**——项目没有 Flyway，
也没有任何工具会按文件名顺序自动执行它们。改成 Flyway 风格只会误导人。
正确做法：保留 upgrade_*.sql 命名，把顺序写进 README。

**先做这一步——确认真实执行顺序**：
不要凭文件名猜顺序。请逐个读完这 4 个文件，按"谁依赖谁的产物"排序：
- upgrade_admin.sql：给 admin 补 status / create_time / update_time / deleted 四列
- upgrade_alarm.sql：给 alarm 补 deploy_no 列
- upgrade_password.sql：把 admin / dev / ops 的明文密码改成 MD5(明文 + 盐值)（数据订正）
- upgrade_fk.sql：给 app / deploy / deploy_log / alarm 建 7 条物理外键
判断原则：加列的脚本要排在建外键的脚本之前（外键依赖被引用的列已存在）；
数据订正排在建列之后。把自己读出来的依赖理由写在回报里。

**如果线索不足、无法确定顺序**：
不要自己编一个顺序然后当成事实。请**停下来告诉我**，
把四个脚本各自做了什么列出来，让我来判断。
顺序错了会导致升级脚本执行失败，这个不能猜。

**确认顺序后**，做两件事：
1. 在 sql/README.md 里写清楚：
   - schema.sql 是全新安装用的（建表），data.sql 是初始数据
   - upgrade_*.sql 是针对已有库的升级脚本，**按下面确认的顺序手工执行**
   - **全新安装不要执行 upgrade_*.sql**（upgrade_alarm.sql / upgrade_admin.sql
     文件头注释里已写明"全新安装走 schema.sql，不必再执行本脚本"）
   - 逐个写清每个脚本做了什么、执行前要不要备份
   - 第 3 步会新增 upgrade_server_ssh.sql / upgrade_deploy_log.sql /
     upgrade_deploy_execution.sql / upgrade_deploy_artifact.sql，
     它们同样是手动执行的 upgrade 脚本，也要登记进这个 README
2. 给每个脚本文件头的注释块里加一行「执行顺序：第 N 个」，不改脚本正文

要求：
1. 不要重命名任何文件（重命名会让 git 历史和别人的笔记对不上，收益却为零）
2. 不要改动任何脚本的 SQL 内容
3. 不要引入 Flyway / Liquibase
```

**怎么验证**：
```
git status
```
应该只看到 `sql/README.md` 是新增，4 个脚本是 `modified`（只多了注释行），
**不应该**出现 `renamed:` 或 `deleted`。打开 README，照着它在一台已有库上从 1 到 N 执行一遍，全部不报错。

---

## 2-11 · 写内网部署文档

**改什么**：新建 `DEPLOY.md`（1 个新文件）

**提示词**：
```
在项目根目录新建 DEPLOY.md，面向"拿到这套代码要部署到内网服务器"的人。

必须包含这几节，每节都要有可直接复制的命令：

1. 前置要求
   - Docker 版本要求（20.10+，因为用了 compose 的 condition: service_healthy）
   - 需要开放的端口：8080（前端）、9090（后端）、3306（数据库，仅内网）
   - 服务器最低配置建议：2 核 4G

2. 首次部署（5 步）
   - 拷贝代码
   - 复制 .env.example 为 .env，并说明每个变量怎么填
   - 特别强调 JWT_SECRET 必须换成随机值，给出生成命令：
     Linux: openssl rand -base64 32
     Windows PowerShell: [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))
   - 执行 ./start.sh 或 start.bat
   - 验证：打开前端地址、访问健康检查接口

3. 日常运维
   - 看日志：docker compose logs -f backend
   - 重启单个服务：docker compose restart backend
   - 备份数据库：docker exec <容器名> mysqldump -uroot -p manager > backup.sql
   - 恢复数据库

4. 升级已有环境
   - 说明 schema.sql 是全新安装用的
   - 已有库升级要按 sql/README.md 里写的顺序手工执行 upgrade_*.sql
   - 提醒先备份
   - **必须写清这个坑**：MySQL 容器只在数据卷为空的首次启动时执行
     /docker-entrypoint-initdb.d/ 下的脚本。如果 mysql-data 卷已有数据，
     就算你改了 schema.sql 再 docker compose up，**脚本也不会重跑**，改动不生效。
     解决办法只有两个：手工执行 SQL，或者 docker compose down -v 清空卷重来（会丢数据）。

5. 常见问题
   - 容器起不来先看什么（docker compose logs）
   - 数据库连接失败怎么查
   - 前端页面能开但接口 404 怎么查（nginx 转发配置）

6. 离线部署（内网服务器无外网时）
   - 在开发机执行 docker compose build 后，把三个镜像导出：
     docker save -o manager-images.tar <backend镜像名> <frontend镜像名> mysql:8.0
     （镜像名以 docker images 实际输出为准）
   - 用 U 盘 / 内网传输把 tar 拷到目标机，执行 docker load -i manager-images.tar
   - 之后按「首次部署」流程走；compose 里保留 build 配置即可，
     docker 会优先使用本地已有镜像，不会去外网拉
   - 如果内网可以出外网，不需要这一节，但要在「前置要求」里写明：
     首次部署需要能访问 Docker Hub 拉基础镜像、maven/npm 仓库拉依赖

要求：命令要准确，不要写你没验证过的命令。不确定的地方标注"待验证"。
```

**怎么验证**：
按文档从头走一遍**全新部署流程**（可以先 `docker compose down -v` 清空再做，确认你真的能重建起来）。任何一步命令跑不通，就修正文档。文档写不对比没有文档更浪费时间。

---

## 做完这 11 条，你得到什么

| 能力 | 效果 |
|---|---|
| 一条命令起全栈 | `./start.sh` → 前端、后端、数据库全部就位 |
| 换服务器不用重打包 | 改 `.env` 就行 |
| 数据库自动初始化 | 全新环境自动建表 + 灌初始数据 |
| 升级脚本有顺序 | `sql/README.md` 写明先跑哪个、全新安装不用跑 |
| 服务状态可查 | `docker compose ps` 能看到健康状态 |
| 有部署文档 | 别人（或半年后的你）能照着装起来 |

**下一步**：进 `devops-prompts-3-自动部署.md`，这是最大的一步——把部署动作从"手工点按钮"改成"机器真的去执行"。
