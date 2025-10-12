# 财务系统部署指南

## 🎉 容器运行测试结果

✅ **测试成功！** 财务系统可以正常运行，所有核心功能都已验证通过。

## 🚀 快速启动

### 方式1: 本地运行（推荐）
```bash
# 构建并启动应用
./run-local.sh
```

### 方式2: Docker容器运行
```bash
# 如果系统已安装Docker
./build-and-run.sh
```

## 📋 测试验证

运行完整的API测试：
```bash
./test-apis.sh
```

## 🌐 访问地址

- **主应用**: http://localhost:8081
- **健康检查**: http://localhost:8081/health  
- **H2数据库控制台**: http://localhost:8081/h2-console
- **API文档**: 查看 `docs/` 目录

## ✅ 已验证功能

### 核心业务流程
1. **步骤1**: 摊销计算 - `GET /amortization/calculate/{contractId}`
2. **步骤2**: 合同创建 - `POST /contracts`
3. **步骤3**: 会计分录预览 - `POST /journals/preview`
4. **步骤4**: 付款执行 - `POST /payments/execute` ⭐

### 管理功能
- 摊销明细管理（修改查询，不支持增删）
- 合同摊销台账查询
- 付款记录管理
- 会计分录查询

## 🧪 测试结果摘要

```
📊 API接口测试结果:
  ✅ 健康检查接口 - 正常
  ✅ 摊销计算接口 - 正常  
  ✅ 合同创建接口 - 正常
  ✅ 摊销台账查询 - 正常
  ✅ 付款执行接口 - 正常（步骤4核心功能）
  ✅ 付款记录查询 - 正常
  ✅ 付款详情查询 - 正常
  ✅ 会计分录预览 - 正常
```

## 🏗️ 技术架构

- **框架**: Spring Boot 3.2.0
- **Java版本**: JDK 21
- **数据库**: H2内存数据库（开发）/ PostgreSQL（生产）
- **构建工具**: Maven
- **容器化**: Docker + Docker Compose

## 📊 数据库配置

### H2内存数据库（默认）
- **URL**: `jdbc:h2:mem:finance2`
- **用户名**: `sa`
- **密码**: 空
- **控制台**: http://localhost:8081/h2-console

### PostgreSQL（生产环境）
```bash
# 启动PostgreSQL服务
docker compose --profile postgres up -d
```

## 🔧 配置文件

- `application.properties` - 默认配置
- `application-docker.properties` - Docker环境配置
- `docker-compose.yml` - 容器编排配置
- `Dockerfile` - 容器构建配置

## 📚 API文档结构

```
docs/
├── README.md                          # API文档总览
├── api.md                            # 接口列表和详细说明
├── health/                           # 健康检查接口
├── amortization-calculate/           # 摊销计算接口
├── contracts-create/                 # 合同创建接口
├── payments-execute/                 # 付款执行接口（核心）
└── ...                              # 其他接口文档
```

每个接口目录包含：
- `note.txt` - API地址和简要说明
- `request.json` - 请求示例
- `response.json` - 响应示例

## 🎯 核心业务逻辑

### 付款执行逻辑（步骤4）
系统根据需求文档实现了完整的付款业务逻辑：

1. **平账情况**: 付款金额 = 选择期间摊销总额
2. **多付情况**: 付款金额 > 选择期间摊销总额
   - 小额差异 → 费用科目
   - 大额差异 → 预付科目
3. **少付情况**: 付款金额 < 选择期间摊销总额
   - 小额差异 → 贷记费用科目
   - 大额差异 → 贷记预付科目

## 🛠️ 开发工具

- **构建**: `mvn clean package`
- **测试**: `mvn test`
- **启动**: `java -jar target/finance2-service-0.0.1-SNAPSHOT.jar`
- **日志**: 查看控制台输出或 `logs/` 目录

## 🔍 故障排除

### 常见问题
1. **端口占用**: 确保8081端口未被占用
2. **Java版本**: 需要JDK 21或更高版本
3. **内存不足**: 建议至少2GB可用内存

### 日志查看
```bash
# 实时查看应用日志
tail -f logs/application.log

# Docker环境查看日志
docker compose logs -f finance2-app
```

## 📈 性能监控

- **健康检查**: GET /health
- **应用指标**: GET /actuator/metrics
- **数据库监控**: H2控制台

---

🎉 **恭喜！** 财务系统已成功部署并通过所有测试验证。系统现在可以处理完整的财务业务流程，从摊销计算到付款执行的全链路功能都已就绪。
