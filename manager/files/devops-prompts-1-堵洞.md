# 第 1 步 · 堵洞

> 这个文件是干嘛的：把答辩和内网评审时**会被追问、而现在确实答不上来**的地方补上。
> 做完能怎样：老师打开代码随便点，你都能接住。
> 怎么做：从 1-01 往下按顺序做，每条做完自己验证一遍再进下一条。一条只改一两个文件，做坏了直接撤回。

> **路径约定**：本文件里的 `springboot/…`、`vue/…` 都相对 `manager\`（`D:\Encoding\IntelliJProject\graduation-project\manager\`）。
> 唯一例外是 **1-02 的 `.gitignore` 和 1-09 的 `.gitattributes`**——它们在**上一级** `D:\Encoding\IntelliJProject\graduation-project\`（git 仓库所在的那一层），不在 `manager/` 下。

---

## 先说清楚：这一步为什么只有 9 条

你上一轮改造已经把大部分洞堵完了。我逐条核过代码，下面这些**已经是好的，别再动**：

| 已经做好的 | 证据 |
|---|---|
| 生产环境数据库密码走环境变量 | `application-prod.yml` 写的是 `${DB_PASSWORD}`，**没有默认值**，缺了直接启动失败 |
| 生产环境会话密钥走环境变量 | 同上，`${JWT_SECRET}` |
| 跨域来源可配置 | `cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}` |
| `.env` 文件自动加载 | `common/EnvFileLoader.java`，在 `SpringbootApplication.main` 第 13 行、`SpringApplication.run` **之前**调用；优先级是 系统环境变量 > 系统属性 > .env；读不到文件不影响启动 |
| 状态变更落流转日志 | `DeployService.saveLog()` 约第 271 行（`DeployService.java` 共约 280 行），注释写明"与状态变更处于同一事务" |
| 开发/生产配置分离 | `application.yml` + `application-dev.yml` + `application-prod.yml` 三个文件 |

所以第 1 步只做真正缺的。做完这 9 条，你的工程底子就干净了。

---

## 1-01 · 关掉控制台 SQL 打印

**改什么**：`springboot/src/main/resources/application.yml`（1 个文件）

**提示词**：
```
把 springboot/src/main/resources/application.yml 里 mybatis.configuration.log-impl 这一行删掉。

原因：org.apache.ibatis.logging.stdout.StdOutImpl 会把每一条 SQL 和参数打印到控制台。
生产环境下这既拖慢性能，又会把数据内容（包括密码字段）写进日志文件。

要求：
1. 直接删除 log-impl 这一行，不要改成别的实现类
2. 同级的 map-underscore-to-camel-case: true 必须保留
3. 不要动 mybatis 下面其他配置
4. 删除后如果需要在本地调试看 SQL，请提示我通过 application-dev.yml 单独开启，不要动主配置文件
```

**怎么验证**：
启动应用，执行任意一次查询（比如打开服务器列表页）。控制台的 SQL 输出应该消失。同时 `map-underscore-to-camel-case` 还在生效——表现为列表页的字段能正常显示，不会出现 `{{null}}` 或空列。

---

## 1-02 · 让 `.env` 文件不进版本库

> **当前项目已存在，先读一遍确认内容是否满足，满足就跳过本条。**
> `.gitignore` 在**项目根** `D:\Encoding\IntelliJProject\graduation-project\`（git 仓库那一层），
> **不在 `manager/` 下**；它已经特意保留了 `manager/files` 里的提示词文件。
> `.env.example` 也已经存在于 `manager/.env.example`。所以本条大概率只需要核对，不需要新建。

**改什么**：核对 `.gitignore` + 核对 `.env.example`（2 个文件，可能都不用改）

**提示词**：
```
先到 D:\Encoding\IntelliJProject\graduation-project\ 下把 .gitignore 完整读一遍，
再到 D:\Encoding\IntelliJProject\graduation-project\manager\ 下把 .env.example 读一遍。

1. 核对 .gitignore 是否包含这几行（防止真实密钥被提交）：
   .env
   .env.local
   *.log
   files/
   node_modules/
   target/
   缺哪一行补哪一行；都有了就在回报里写"已满足，无需改动"，不要为了改而改。
   （注意：.gitignore 里如果已经有保留 manager/files 的规则，不要删掉它）

2. 核对 manager\.env.example 是否作为模板存在（这个文件要提交，给协作者看）：
   # 复制为 .env 后填入真实值
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=manager
   DB_USERNAME=root
   DB_PASSWORD=
   JWT_SECRET=
   CORS_ALLOWED_ORIGINS=http://localhost:8080
   MAIL_HOST=smtp.yeah.net
   MAIL_PORT=465
   MAIL_USERNAME=
   MAIL_PASSWORD=
   缺哪个变量补哪个；已经齐全就回报"已满足，无需改动"。

要求：
1. .env.example 里所有密码类字段留空，绝不填真实值
2. 每行一个，不要加引号
3. 注释用 # 开头
4. 两个文件都只读不改也能满足要求时，就什么也别改
```

**怎么验证**：
```
git status
```
应该看不到 `.env`（如果本地有的话）。同时能看到 `.env.example` 出现在待提交列表里。用记事本打开 `.env.example`，确认密码字段全空。

---

## 1-03 · 上传目录改成可配置

**改什么**：`springboot/src/main/java/com/example/controller/FileController.java` + `application.yml`（2 个文件）

**提示词**：
```
现在 springboot/src/main/java/com/example/controller/FileController.java 里
文件存储目录写死为：Paths.get(System.getProperty("user.dir"), "files")

要改成从配置读取，默认值保持现在这个路径（保证现有数据不受影响）。

要求：
1. 在 FileController 里 ，把 FILE_ROOT 从静态常量改为通过 @Value 注入：
   @Value("${file.upload-dir:files}")
   private String uploadDir;
   然后用 Paths.get(System.getProperty("user.dir"), uploadDir) 构造根路径
   注意：@Value 注入的是实例字段，原来如果 FILE_ROOT 是 static final，需要改成实例字段，
   同时检查所有引用它的地方（包括 resolveInside 方法）一并改成实例访问
2. 在 application.yml 里新增：
   file:
     upload-dir: ${FILE_UPLOAD_DIR:files}
3. 不要改动 resolveInside()、FileSafeNames.MAX_BYTES、assertMagic() 的校验逻辑
4. 不要改动文件读取和删除的权限校验（DELETE 仍必须是 ADMIN）
```

**怎么验证**：
1. 不配置任何环境变量直接启动，上传一个文件——功能正常，文件出现在项目目录的 `files/` 下
2. 停掉服务，设置环境变量 `FILE_UPLOAD_DIR=D:\test-upload`，重启，再上传一个文件——文件出现在 `D:\test-upload` 下
3. 用 DEV 角色的账号去调删除接口，应该仍然返回权限错误

---

## 1-04 · 给"状态变更必落日志"补一个回归测试

**改什么**：`springboot/src/test/java/com/example/service/DeployServiceStatusTest.java`（1 个文件，追加测试方法）

**提示词**：
```
springboot/src/main/java/com/example/service/DeployService.java 里，
私有方法 saveLog(...)（约第 271 行，文件共约 280 行）负责写流转记录，
注释声明"与状态变更处于同一事务"。这个方法目前没有测试保护。

注意两个真实情况，别照着旧稿写：
- 状态更新走的是 deployMapper.updateById(updateDeploy)，**项目里目前没有 casUpdate()，
  也没有乐观锁**（CAS 要到第 3 步 3-06 才新增）。本条测试断言的是 updateById 的调用顺序。
- DeployLog 实体现在只有 id / deployId / action / operator / detail /
  createTime / updateTime / deleted 八个字段，**没有 logType / seq / source**
  （这三列要等第 3 步 3-05 加）。所以下面第 1 条断言 logType='STATE' 的前提是 3-05 已做完；
  3-05 之前请把该断言换成"断言 action='APPROVE' + detail 非空"，不要断言不存在的字段。

请在已有的 DeployServiceStatusTest 里**追加**测试方法，不要新建测试类、不要改动已有方法。

注意：本项目的测试是**纯 Mockito 风格**（没有 @SpringBootTest、没有真实数据库）。
所以"验证日志真的写进去了"在这里的正确做法是 **verify + 参数捕获**，
不是连真库查询。DeployLogMapper 本来就该 mock——不要去引入 Spring 上下文或真实数据库。

要测三件事：
1. 审批通过后，verify(deployLogMapper).insert(...) 被调用一次；
   用 ArgumentCaptor 捕获参数，断言 action = 'APPROVE'
   （3-05 做完后可再断言 logType = 'STATE'；3-05 之前不要断言它）
2. 开始部署后，同样捕获 insert 参数，断言 action = 'DEPLOY_START'
3. 日志与状态变更的先后关系：用 Mockito 的 InOrder 断言——
   先发生状态更新（deployMapper.updateById 调用），后发生 deployLogMapper.insert，
   且两者在同一方法内完成。这是"同一事务"在单测层面能做的最接近的验证

要求：
1. 沿用该测试类现有的框架和断言风格（先读一遍这个文件，照它的写法来）
2. 测试方法名用中文或英文都行，但要能看懂测的是什么
3. 每个断言都要针对捕获到的真实参数，不要只 verify 调用次数
```

**怎么验证**：
```
cd springboot
mvn test -Dtest=DeployServiceStatusTest
```
三个新测试方法全绿。

**再做一个"测试有效性"验证**（确认测试真的在验东西，不是摆设）：
把 `saveLog` 里 `deployLog.setAction(...)` 这一行的值从 `APPROVE` 改成 `"XXX"`，
重跑测试——断言 `action = 'APPROVE'` 的那条应该变红（参数捕获到的值对不上）。
（旧稿写的是改 `setLogType`，但 `DeployLog` 实体现在没有 `logType` 字段，改不了，用 `action` 代替。）
验完记得改回来。

也可以换一种验法：把 saveLog 里的 insert 调用临时注释掉，verify 会报
WantedButNotInvoked——同样说明测试在盯梢。两种验法任选，验完都要改回来。

---

## 1-05 · 异常返回前端前做脱敏

**改什么**：`springboot/src/main/java/com/example/exception/GlobalExceptionHandler.java`（1 个文件）

**提示词**：
```
springboot/src/main/java/com/example/exception/GlobalExceptionHandler.java
现在处理未知异常时，很可能把 e.getMessage() 直接返回给前端。
这会把 SQL 报错、表名、字段名、甚至连接串泄露给浏览器。

请改造这个类：

1. 先读一遍现有实现，看清它现在分几类处理（BusinessException / 参数异常 / 兜底 Exception）
2. 对 BusinessException：保持原样返回业务错误码和提示（这些是给人看的，安全）
3. 对所有**非** BusinessException 的异常（含兜底 Exception）：
   - 用 log.error("未预期异常", e) 把完整堆栈打到服务端日志
   - 返回给前端的 message 固定为「系统繁忙，请稍后重试」，不要拼接任何 e.getMessage()
   - 返回的错误码用一个已有的通用失败码（先查 ResultCodeEnum，挑一个语义是"系统错误"的，
     比如 SYSTEM_ERROR("500")；如果项目要求统一用 6xxx 业务码段，
     就在 ResultCodeEnum 末尾新增 6034，值为「系统繁忙，请稍后重试」）
4. 不要改动 BusinessException 分支的逻辑，现有业务错误码必须原样保留

注意：动手前先读一遍 ResultCodeEnum，确认当前最大的码是多少再决定新增几。
（当前是 6027 MAIL_SEND_ERROR；6034 是本条为"系统繁忙"预留的，
6036 留给第 3 步 3-06 的并发冲突、6035 留给 3-19 的并发互斥，都不要撞。）
```

**怎么验证**：
1. 正常走一遍业务流程（提交部署单 → 审批 → 部署），确认业务错误提示还在，
   比如用 DEV 账号去审批（应该报"只有OPS角色才能审批部署"）。
   **错误码以 ResultCodeEnum 里的实际值为准**（旧稿写的 6026 实际是"邮件-未配置个人邮箱"，
   和驳回无关，不要拿它当断言）
2. 制造一个非业务异常——最简单的方法：把数据库连接地址临时改错，重启，然后调任意接口。前端应该看到「系统繁忙，请稍后重试」，而不是 `Communications link failure...`
3. 同时看服务端控制台，完整的异常堆栈应该在日志里（这条很重要，不然你就看不见问题了）
4. 验完把数据库配置改回来

---

## 1-06 · 确认打包产物不含上传文件

> 说明：这条原本是"在 pom.xml 里排除 files/ 目录"，但那个写法是错的——
> `files/` 是运行时在程序工作目录下创建的，根本不在 `src/main/resources` 里，压根不会被打进 jar。
> 所以这条改成"检查一下、确认没问题"，不需要改代码。

**改什么**：无（只做检查）

**提示词**：
```
不需要改代码，请帮我确认一件事。

背景：springboot/src/main/java/com/example/controller/FileController.java 里，
文件存储根目录是 Paths.get(System.getProperty("user.dir"), "files")。
这是**运行时**在工作目录下创建/读取的目录，理论上不应该被打进 jar 包。

请确认：
1. 检查 springboot/src/main/resources/ 下有没有 files/ 目录或任何 .log 文件
   ——如果有，告诉我，那才需要处理
2. 检查 springboot/.gitignore 里有没有 files/ 和 *.log
   ——没有就加上（这是为了防止误提交，不是为了打包）
3. 执行 mvn clean package，然后告诉我：
   - 生成的 jar 文件完整路径和大小
   - jar 里有没有 files/ 目录（可以用 unzip -l target/*.jar | grep files 检查）

不要修改 pom.xml。不要动 <build> 段。
```

**怎么验证**：
```
cd springboot
mvn clean package
unzip -l target/*.jar | grep -i "files/\|\.log"
```
应该没有任何输出（说明 jar 里没有这些内容）。如果有输出，说明真的有文件混进了资源目录，需要单独处理。

---

## 1-07 · 给文件上传路径穿越加一条明确的测试

**改什么**：`springboot/src/test/java/com/example/utils/FileSafeNamesTest.java`（1 个文件，追加测试方法）

**提示词**：
```
文件上传的安全校验分散在两个地方：
- springboot/src/main/java/com/example/utils/FileSafeNames.java
- springboot/src/main/java/com/example/controller/FileController.java 的 resolveInside() 方法

现在已有 FileSafeNamesTest，但可能没覆盖"路径穿越"这类攻击输入。

请在 FileSafeNamesTest 里**追加**测试方法（不要新建测试类），至少覆盖：

1. 文件名包含 ../ 时被拒绝
2. 文件名包含 ..\ （Windows 反斜杠）时被拒绝
3. 文件名包含绝对路径（如 C:\Windows\x.txt 或 /etc/passwd）时被拒绝
4. 文件名包含空字节 %00 时被拒绝
5. 正常文件名（如 myapp-v1.2.0.jar）能通过

要求：
1. 先读 FileSafeNames.java 看清它实际暴露了哪些方法，按真实签名写测试
2. 如果某个攻击输入在现有实现下**没有被拒绝**，不要改测试去迁就它——
   这说明发现了真漏洞，请在回报里明确告诉我哪个输入没被拦住
3. 沿用该测试类现有风格
```

**怎么验证**：
```
cd springboot
mvn test -Dtest=FileSafeNamesTest
```
全绿。**如果出现红**，说明可能真存在路径穿越漏洞——这种情况不要自己修，先告诉我具体是哪个输入没被拦住。

---

## 1-08 · 关掉开发环境的默认弱口令提示

**改什么**：`springboot/src/main/resources/application-dev.yml`（1 个文件）

**提示词**：
```
springboot/src/main/resources/application-dev.yml 第 6 行是：
password: ${DB_PASSWORD:123456}

这个默认值 123456 明文躺在版本库里。生产环境已经强制走环境变量（application-prod.yml），
所以这不是安全漏洞，但答辩时容易被抓住问。

改法（保持开发便利性，只加提示）：
1. 保留 ${DB_PASSWORD:123456} 不动（删掉会导致每个协作者本地都要配环境变量，得不偿失）
2. 在该行上方加一行注释：
   # 默认值为本地开发占位口令，生产环境见 application-prod.yml（强制环境变量注入）
3. 不要动第 5 行的 ${DB_USERNAME:root}
4. 不要动 url 那一长串连接参数
```

**怎么验证**：
打开 `application-dev.yml`，确认注释已加、`${DB_PASSWORD:123456}` 原样保留、url 没被动过。启动应用，本地开发不需要配任何环境变量就能连上数据库。

---

## 1-09 · 用 `.gitattributes` 锁住 shell 脚本的换行符

> **当前项目已存在，先读一遍确认内容是否满足，满足就跳过本条。**
> `.gitattributes` 在**项目根** `D:\Encoding\IntelliJProject\graduation-project\`（git 仓库那一层），
> **不在 `manager/` 下**。它现在的内容就是 `*.sh text eol=lf` 这一行——
> 如果读出来确实如此，本条直接跳过；只需补做下面的 `git add --renormalize .`。

**改什么**：核对 `.gitattributes`（1 个文件，可能不用改）

**提示词**：
```
先到 D:\Encoding\IntelliJProject\graduation-project\ 下读一遍 .gitattributes。
如果有 *.sh text eol=lf 这一行就什么都不用加；没有就新建/追加这一行规则：

*.sh text eol=lf

为什么必须有这个文件：
Windows 上 git 默认 core.autocrlf=true，checkout 时会把仓库里 LF 换行的 .sh
转成 CRLF。CRLF 的脚本打进 jar、传到 Linux 目标机执行时，bash 会报
$'\r': command not found 或 bad interpreter——
第 2 步的 start.sh、第 3 步的 deploy.sh / rollback.sh 全都会中招。
.gitattributes 在仓库层面锁死 .sh 始终用 LF，不依赖每个人本地的 git 配置。

要求：
1. 只加 *.sh text eol=lf 这一条规则，不要顺手加其他文件的规则
2. 加完后执行 git add --renormalize . 让已入库的 .sh 也按新规则归一化
3. 把 .gitattributes 本身提交进版本库
```

**怎么验证**：
在 Git Bash 里对仓库中任一 .sh 文件执行：
```
file start.sh
```
输出应包含 `LF line terminators`，且**不含** `CRLF`、**不含** `with BOM`。
（第 3 步新建 deploy.sh / rollback.sh 后，用同样的命令再各验一次——
它和 3-09 里 Java 侧的 `\r\n → \n` 运行时归一化构成三道保险。）

---

## 做完这 9 条，你得到什么

| 问题 | 状态 |
|---|---|
| "你的 SQL 日志会不会泄露数据？" | 不会，已关闭 |
| "密钥会不会被提交上去？" | 不会，`.gitignore` + `.env.example` 已就位 |
| "上传目录写死了怎么部署？" | 可以配置了 |
| "状态变了但日志没记怎么办？" | 有测试盯着 |
| "报错会不会把库表结构暴露出去？" | 不会，已脱敏 |
| "打包是不是把垃圾都带上了？" | 已确认没有（1-06） |
| "上传有没有路径穿越风险？" | 有测试兜住 |
| "开发口令明文躺着，怎么说？" | 有注释说明边界 |
| "Windows 上脚本换行被 git 改坏怎么办？" | `.gitattributes` 锁死 LF，运行时还有兜底（见 3-09） |

**下一步**：进 `devops-prompts-2-跑起来.md`，让系统能在服务器上装起来。
