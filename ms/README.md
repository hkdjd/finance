# Finance2 Transaction Processing Service

基于 Spring Boot 3.x 的交易处理微服务，采用 MVC 架构模式，实现合同摊销计算、会计分录预览与付款处理。

## 项目特性

- **Spring Boot 3.x** - 最新的 Spring Boot 框架
- **MVC 架构** - 清晰的分层架构（Controller/Service/Repository/Model/DTO）
- **RESTful API** - 标准的 REST 接口
- **Maven 构建** - 标准的 Maven 项目结构
- **JPA/Hibernate** - 数据持久化与审计
- **H2 数据库** - 开发环境内存数据库

## 环境要求

- Java 21
- Maven 3.6+
- 端口 8081

## 快速启动

```bash
# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run

# 健康检查
curl http://localhost:8081/health

# H2 控制台（可选）
# 访问 http://localhost:8081/h2-console
# JDBC URL: jdbc:h2:mem:finance2
# 用户名: sa，密码: 空
```

## 核心业务流程

### 步骤1：创建合同并初始化摊销台账

```bash
curl -X POST http://localhost:8081/contracts \
  -H 'Content-Type: application/json' \
  -d '{
    "totalAmount": 6000,
    "startDate": "2024-01",
    "endDate": "2024-06",
    "taxRate": 0,
    "vendorName": "供应商A"
  }'
```

**响应示例：**
```json
{
  "totalAmount": 6000.00,
  "startDate": "2024-01",
  "endDate": "2024-06",
  "scenario": null,
  "generatedAt": "2025-09-22T16:47:00+08:00",
  "entries": [
    {
      "id": 1,
      "amortizationPeriod": "2024-01",
      "accountingPeriod": "2024-01",
      "amount": 1000.00
    }
  ]
}
```

### 步骤2：查询合同摊销台账

```bash
curl http://localhost:8081/contracts/1/amortization
```

### 步骤3：摊销明细修改（支持增删改操作）

```bash
# 修改摊销明细（包含该合同所有摊销明细）
curl -X POST http://localhost:8081/amortization-entries/operate \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 1,
    "amortization": [
      {
        "id": 1,
        "amortizationPeriod": "2025-01",
        "accountingPeriod": "2025-01",
        "amount": 1200.00,
        "periodDate": "2025-01-01",
        "paymentStatus": "PENDING",
        "createdAt": "2024-12-24T14:30:52.123456",
        "updatedAt": "2024-12-24T14:30:52.123456",
        "createdBy": "system",
        "updatedBy": "system"
      },
      {
        "id": 2,
        "amortizationPeriod": "2025-02",
        "accountingPeriod": "2025-02",
        "amount": 1000.00,
        "periodDate": "2025-02-01",
        "paymentStatus": "PENDING",
        "createdAt": "2024-12-24T14:30:52.123456",
        "updatedAt": "2024-12-24T14:30:52.123456",
        "createdBy": "system",
        "updatedBy": "system"
      }
    ]
  }'
```

### 步骤4：会计分录预览

```bash
curl -X POST http://localhost:8081/journals/preview \
  -H 'Content-Type: application/json' \
  -d '{
    "totalAmount": 6000.00,
    "entries": [
      {
        "id": 1,
        "amortizationPeriod": "2024-01",
        "accountingPeriod": "2024-01",
        "amount": 1000.00
      }
    ]
  }'
```

**响应示例：**
```json
[
  {
    "bookingDate": "2024-01-27",
    "account": "费用",
    "dr": 1000.00,
    "cr": 0.00,
    "memo": "amort:2024-01 acct:2024-01"
  },
  {
    "bookingDate": "2024-01-27",
    "account": "应付",
    "dr": 0.00,
    "cr": 1000.00,
    "memo": "amort:2024-01 acct:2024-01"
  }
]
```

### 步骤5：付款预览

```bash
curl -X POST http://localhost:8081/payments/preview \
  -H 'Content-Type: application/json' \
  -d '{
    "amortization": {
      "entries": [
        {"id": 1, "amortizationPeriod": "2024-01", "accountingPeriod": "2024-01", "amount": 1000.00},
        {"id": 2, "amortizationPeriod": "2024-02", "accountingPeriod": "2024-02", "amount": 1000.00}
      ]
    },
    "paymentAmount": 2001,
    "bookingDate": "2024-03-20",
    "selectedPeriods": ["2024-01", "2024-02"]
  }'
```

**响应示例（情形2.2：多付1元）：**
```json
{
  "paymentAmount": 2001.00,
  "entries": [
    {
      "bookingDate": "2024-01-27",
      "account": "应付",
      "dr": 1000.00,
      "cr": 0.00,
      "memo": "period:2024-01"
    },
    {
      "bookingDate": "2024-02-27",
      "account": "应付",
      "dr": 1000.00,
      "cr": 0.00,
      "memo": "period:2024-02"
    },
    {
      "bookingDate": "2024-03-20",
      "account": "费用",
      "dr": 1.00,
      "cr": 0.00,
      "memo": "over small"
    },
    {
      "bookingDate": "2024-03-20",
      "account": "活期存款",
      "dr": 0.00,
      "cr": 2001.00,
      "memo": "payment"
    }
  ]
}
```

## API 接口文档

### 合同管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/contracts` | 创建合同并初始化摊销台账 |
| GET | `/contracts/{id}/amortization` | 查询合同摊销台账 |

### 摊销计算

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/amortization/calculate/{contractId}` | 根据合同ID计算摊销表（不落库） |

### 摊销明细管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/amortization-entries/contract/{contractId}` | 查询合同摊销明细列表 |
| POST | `/amortization-entries/operate` | 摊销明细更新（支持增删改操作） |

### 会计分录

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/journals/preview` | 基于摊销结果预览会计分录 |

### 付款处理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/payments/preview` | 付款预览（含差额处理） |

### 系统

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/health` | 健康检查 |

## 摊销计算规则

### 场景1：当前时间 < 合同开始时间
- 合同总金额平均摊销到每个月
- 入账期间 = 摊销期间

### 场景2：合同开始时间 ≤ 当前时间 ≤ 合同结束时间
- 合同开始到当前月（含）的摊销集中入账到当前月
- 未来月份按各自月份入账

### 场景3：当前时间 > 合同结束时间
- 不按月摊销，全部金额记当前月份

## 付款差额处理规则

- **平账**：付款金额 = 勾选期间摊销总额
- **多付**：付款金额 > 勾选期间摊销总额
  - 小额差异（≤ 最小月摊销额的10%）：借记"费用"
  - 大额差异：借记"预付"
- **少付**：付款金额 < 勾选期间摊销总额
  - 小额差异：贷记"费用"
  - 大额差异：贷记"预付"

## 项目结构

```
src/main/java/com/ocbc/finance/
├── controller/          # 控制器层
├── service/            # 业务服务层
│   └── calculation/    # 计算服务
├── repository/         # 数据访问层
├── model/             # 实体模型
├── dto/               # 数据传输对象
└── config/            # 配置类
```

## 开发说明

- 使用 H2 内存数据库，重启后数据清空
- 自动建表，无需手动创建数据库结构
- 支持 JPA 审计（创建时间、修改时间、创建人、修改人）
- 统一异常处理，友好的错误信息返回

## 生产部署建议

1. 切换到 PostgreSQL 数据库
2. 配置连接池和事务管理
3. 添加日志配置和监控
4. 配置 HTTPS 和安全认证
5. 添加 API 限流和缓存
