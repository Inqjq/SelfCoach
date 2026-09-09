# SelfCoach 后端镜像：多阶段构建（编译 + 精简运行）

# --- 第一阶段：用带 Maven + JDK17 的镜像编译 ---
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先把 pom.xml 拷进来，利用镜像层缓存依赖下载
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
# 再拷源码编译打包
COPY src ./src
RUN mvn -B -DskipTests package

# --- 第二阶段：用精简 JRE 运行 ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]