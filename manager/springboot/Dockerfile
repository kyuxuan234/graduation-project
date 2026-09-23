# 产物名来自 pom：artifactId=springboot，version=0.0.1-SNAPSHOT，没有 finalName。
FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
# Maven 3.8 会拦截 pom 里的 http 仓库。构建时改走 HTTPS 镜像，不改 pom。
RUN mkdir -p /root/.m2 \
    && printf '%s\n' \
        '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' \
        > /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# 镜像站对官方 openjdk:8-jre-slim 返回 403，改用仍是 Java 8、且带 apt 的 Temurin。
FROM eclipse-temurin:8-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget passwd \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --no-create-home appuser \
    && mkdir -p /app/files \
    && chown -R appuser:appuser /app
WORKDIR /app
COPY --from=build /build/target/springboot-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar
USER appuser
VOLUME /app/files
EXPOSE 9090
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:9090/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Duser.timezone=Asia/Shanghai", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
