# OCBC财务合同管理系统

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9.0-red.svg)](https://maven.apache.org/)

## 📋 项目简介

OCBC财务合同管理系统是一个企业级的合同订单财务管理后端系统，提供完整的合同生命周期管理功能，包括合同上传、信息提取、财务计算、时间表管理和会计分录生成等核心业务功能。

## 🚀 核心功能

### 📄 合同管理
- **智能合同上传**: 支持PDF文件上传和AI驱动的信息提取
- **多条件查询**: 支持按合同编号、别名、状态等条件查询
- **关联状态展示**: 实时显示各关联表的数据状态
- **合同预览**: 在线预览原始合同文件

### 💰 财务信息管理
- **分类信息存储**: 8大类财务信息（基础、财务、时间、结算、费用、税务、风险）
- **JSONB灵活存储**: 支持动态扩展字段
- **数据完整性验证**: 确保财务数据的准确性

### 📊 预提/待摊时间表
- **智能摊销算法**: 按月摊销，自动处理除不尽情况
- **审批时间处理**: 考虑审批时间对入账时间的影响
- **状态管理**: 完整的时间表状态跟踪

### 💳 支付时间表
- **灵活支付计划**: 根据合同期间智能生成支付计划
- **多种支付模式**: 支持短期、长期合同的不同支付策略
- **里程碑支付**: 支持基于里程碑的支付条件

### 📚 会计分录
- **8种分录场景**: 完整覆盖未付款和已付款的各种业务场景
- **自动借贷平衡**: 确保会计分录符合会计准则
- **编号管理**: 智能生成符合规范的会计分录编号

## 🛠️ 技术栈

- **后端框架**: Spring Boot 3.1.0
- **数据库**: PostgreSQL 15
- **ORM框架**: Spring Data JPA + Hibernate
- **构建工具**: Maven 3.9.0
- **Java版本**: JDK 17
- **AI服务**: DeepSeek、Gemini、Gemma3
- **文档处理**: Apache PDFBox
- **JSON处理**: Jackson
- **日志框架**: SLF4J + Logback

## 📁 项目结构

```
ms-finance-contract/
├── src/main/java/com/ocbc/finance/
│   ├── controller/          # REST API控制器
│   ├── service/            # 业务服务层
│   ├── repository/         # 数据访问层
│   ├── entity/             # 实体类
│   ├── dto/                # 数据传输对象
│   ├── config/             # 配置类
│   ├── util/               # 工具类
│   └── exception/          # 异常处理
├── src/main/resources/
│   ├── db/migration/       # 数据库迁移脚本
│   └── application.yml     # 应用配置
├── logs/                   # 日志文件
├── uploads/                # 上传文件存储
└── test-*.sh              # API测试脚本
```

## 🔧 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 15+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE ocbc_finance;
CREATE SCHEMA ocbc_finance_contract;
CREATE USER tangwei;
GRANT ALL PRIVILEGES ON DATABASE ocbc_finance TO tangwei;
GRANT ALL PRIVILEGES ON SCHEMA ocbc_finance_contract TO tangwei;
```

2. 修改 `application.yml` 中的数据库连接配置

### 启动应用

```bash
# 克隆项目
git clone https://github.com/YOUR_USERNAME/ocbc-finance-contract-system.git
cd ocbc-finance-contract-system

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动

## 📚 API文档

### 核心接口

| 模块 | 接口数量 | 基础路径 |
|------|----------|----------|
| 合同管理 | 4个 | `/api/v1/contracts` |
| 财务信息 | 3个 | `/api/v1/finance` |
| 预提待摊 | 3个 | `/api/v1/amortization` |
| 支付时间表 | 3个 | `/api/v1/payment` |
| 会计分录 | 3个 | `/api/v1/accounting` |

### 测试脚本

```bash
# 测试合同上传
./test-api.sh

# 测试会计分录场景
./test-accounting-scenarios.sh

# 测试合同状态API
./test-contract-status-api.sh
```

## 🏗️ 部署说明

### Docker部署

```bash
# 构建镜像
docker build -t ocbc-finance-contract .

# 运行容器
docker run -p 8080:8080 ocbc-finance-contract
```

### 生产环境配置

1. 配置生产数据库连接
2. 设置AI服务API密钥
3. 配置日志级别和文件路径
4. 设置文件上传路径和大小限制

## 🧪 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# API功能测试
chmod +x *.sh
./comprehensive-api-test.sh
```

## 📊 项目统计

- **代码行数**: 约12,000行Java代码
- **API接口**: 16个RESTful接口
- **业务服务**: 8个核心服务类
- **数据库表**: 6个核心业务表
- **业务场景**: 8种会计分录场景 + 多种摊销和支付场景

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 开发团队

- **OCBC Finance Team** - *项目开发* - [OCBC](https://github.com/ocbc)

## 📞 联系我们

- 项目链接: [https://github.com/YOUR_USERNAME/ocbc-finance-contract-system](https://github.com/YOUR_USERNAME/ocbc-finance-contract-system)
- 问题反馈: [Issues](https://github.com/YOUR_USERNAME/ocbc-finance-contract-system/issues)

---

⭐ 如果这个项目对你有帮助，请给它一个星标！
