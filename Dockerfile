# 使用官方的OpenJDK 21运行时作为基础镜像
FROM openjdk:21-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制Maven构建的jar文件到容器中
COPY target/finance2-service-0.0.1-SNAPSHOT.jar app.jar

# 暴露应用程序端口
EXPOSE 8081

# 设置JVM参数和启动命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/health || exit 1
