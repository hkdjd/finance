#!/bin/bash

# PostgreSQL数据库快速启动脚本

set -e

echo "🐘 启动PostgreSQL数据库..."

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装，请先安装Docker"
    exit 1
fi

# 检查Docker Compose是否可用
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    echo "❌ Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 停止现有容器
echo "🛑 停止现有数据库容器..."
$COMPOSE_CMD -f docker-compose-db.yml down

# 启动PostgreSQL数据库
echo "🚀 启动PostgreSQL数据库..."
$COMPOSE_CMD -f docker-compose-db.yml up -d postgres

# 等待数据库启动
echo "⏳ 等待数据库启动..."
for i in {1..30}; do
    if $COMPOSE_CMD -f docker-compose-db.yml exec postgres pg_isready -U finance2_user -d finance2 > /dev/null 2>&1; then
        echo "✅ 数据库启动成功！"
        break
    else
        echo "⏳ 等待数据库启动... ($i/30)"
        sleep 2
    fi
    
    if [ $i -eq 30 ]; then
        echo "❌ 数据库启动超时"
        $COMPOSE_CMD -f docker-compose-db.yml logs postgres
        exit 1
    fi
done

# 显示连接信息
echo ""
echo "🎉 PostgreSQL数据库已成功启动！"
echo ""
echo "📊 数据库连接信息:"
echo "  主机: localhost"
echo "  端口: 5432"
echo "  数据库: finance2"
echo "  用户名: finance2_user"
echo "  密码: finance2_password"
echo ""
echo "🔗 JDBC连接字符串:"
echo "  jdbc:postgresql://localhost:5432/finance2"
echo ""
echo "💡 管理命令:"
echo "  查看日志: $COMPOSE_CMD -f docker-compose-db.yml logs -f postgres"
echo "  连接数据库: $COMPOSE_CMD -f docker-compose-db.yml exec postgres psql -U finance2_user -d finance2"
echo "  停止数据库: $COMPOSE_CMD -f docker-compose-db.yml down"
echo "  启动pgAdmin: $COMPOSE_CMD -f docker-compose-db.yml --profile admin up -d"
echo ""
echo "📚 建表脚本已自动执行，表结构已创建完成！"
