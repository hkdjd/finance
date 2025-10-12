#!/bin/bash

# 本地运行财务系统脚本（无需Docker）

set -e

echo "🚀 启动财务系统（本地模式）..."

# 检查jar文件是否存在
if [ ! -f "target/finance2-service-0.0.1-SNAPSHOT.jar" ]; then
    echo "📦 构建项目..."
    mvn package -DskipTests
fi

echo "✅ 项目已构建完成"

# 设置环境变量
export SPRING_PROFILES_ACTIVE=default
export SERVER_PORT=8081

echo "🌐 启动应用服务器..."
echo "📍 应用将在 http://localhost:8081 启动"
echo "📊 H2控制台: http://localhost:8081/h2-console"
echo "❤️ 健康检查: http://localhost:8081/health"
echo ""
echo "💡 按 Ctrl+C 停止服务"
echo ""

# 启动应用
java -jar target/finance2-service-0.0.1-SNAPSHOT.jar
