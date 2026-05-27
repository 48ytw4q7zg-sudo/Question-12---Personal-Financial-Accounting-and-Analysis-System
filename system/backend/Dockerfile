# Q-CR v21 — creator qxw · 2501060122
# 修复：添加多阶段构建，集成 Maven 编译步骤（此前需要手动 mvn package 前置）
# 安全加固：非 root 用户运行 + JVM 容器感知内存参数

# 第一阶段：使用 Maven + JDK 21 构建 JAR
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src/ src/
RUN mvn package -DskipTests -B -q

# 第二阶段：使用最小 JRE 运行（减小镜像体积）
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安全加固：创建非 root 用户运行应用（防容器逃逸提权）
# 安装 curl：供 docker-compose.yml 的 healthcheck 使用（eclipse-temurin:21-jre-alpine 默认不含 curl）
RUN apk add --no-cache curl && addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# JVM 内存参数：堆 512MB，启用容器感知支持（防止 OOMKilled）
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
