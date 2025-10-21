#!/bin/bash

# 财务系统Docker构建和运行脚本

set -e

echo "🚀 开始构建财务系统..."

# 清理之前的构建
echo "📦 清理Maven项目..."
mvn clean

# 编译和打包项目
echo "🔨 编译和打包项目..."
mvn package -DskipTests

# 检查jar文件是否存在
if [ ! -f "target/finance2-service-0.0.1-SNAPSHOT.jar" ]; then
    echo "❌ 构建失败：找不到jar文件"
    exit 1
fi

echo "✅ 项目构建成功"

# 停止并删除现有容器
echo "🛑 停止现有容器..."
docker-compose down

# 构建Docker镜像
echo "🐳 构建Docker镜像..."
docker-compose build

# 启动服务
echo "🚀 启动服务..."
docker-compose up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

# 检查健康状态
echo "❤️ 检查应用健康状态..."
for i in {1..30}; do
    if curl -f http://localhost:8081/health > /dev/null 2>&1; then
        echo "✅ 服务启动成功！"
        echo "🌐 应用访问地址: http://localhost:8081"
        echo "📊 H2控制台: http://localhost:8081/h2-console"
        echo "📋 API文档: 查看 docs/ 目录"
        break
    else
        echo "⏳ 等待服务启动... ($i/30)"
        sleep 2
    fi
    
    if [ $i -eq 30 ]; then
        echo "❌ 服务启动超时"
        echo "📋 查看日志:"
        docker-compose logs finance2-app
        exit 1
    fi
done

echo ""
echo "🎉 财务系统已成功启动！"
echo ""
echo "📚 可用的API接口:"
echo "  - GET  /health                           - 健康检查"
echo "  - GET  /amortization/calculate/{id}      - 根据合同ID计算摊销明细"
echo "  - POST /contracts                        - 创建合同"
echo "  - GET  /contracts/{id}/amortization      - 查询合同摊销台账"
echo "  - POST /journals/preview                 - 预览会计分录"
echo "  - POST /payments/execute                 - 执行付款（步骤4）"
echo "  - GET  /payments/contracts/{contractId}  - 查询付款记录"
echo ""
echo "💡 使用 'docker-compose logs -f finance2-app' 查看实时日志"
echo "💡 使用 'docker-compose down' 停止服务"
