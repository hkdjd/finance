# 精简API接口文档

## 设计原则

所有的PUT、DELETE接口都合并到POST接口中，通过请求字段`operate`来区分增删改操作：
- `operate`的值可为：`CREATE`、`UPDATE`、`DELETE`
- 统一使用POST方法，简化接口设计
- 支持单个操作和批量操作

## 精简后的接口列表

| API | Method | Description | URL |
|-----|--------|-------------|-----|
| localhost:8081/health | GET | 健康检查接口 | /health |
| localhost:8081/contracts | GET | 查询合同列表 | /contracts |
| localhost:8081/contracts/list | GET | 分页查询合同列表 | /contracts/list |
| localhost:8081/contracts/upload | POST | 上传合同文件（步骤1） | /contracts/upload |
| localhost:8081/contracts/{contractId} | GET | 查询合同信息 | /contracts/{contractId} |
| localhost:8081/contracts/operate | POST | 合同操作（增删改） | /contracts/operate |
| localhost:8081/amortization/calculate/{contractId} | GET | 根据合同ID计算摊销明细 | /amortization/calculate/{contractId} |
| localhost:8081/amortization-entries/contract/{contractId} | GET | 查询合同摊销明细列表 | /amortization-entries/contract/{contractId} |
| localhost:8081/amortization-entries/operate | POST | 摊销明细操作（支持增删改操作） | /amortization-entries/operate |
| localhost:8081/contracts | POST | 创建合同并初始化摊销台账 | /contracts |
| localhost:8081/contracts/{id}/amortization | GET | 查询合同摊销台账 | /contracts/{id}/amortization |
| localhost:8081/journal-entries/generate/{contractId} | POST | 生成会计分录（步骤3摊销） | /journal-entries/generate/{contractId} |
| localhost:8081/journal-entries/contract/{contractId} | GET | 查询合同会计分录列表 | /journal-entries/contract/{contractId} |
| localhost:8081/journal-entries/operate | POST | 会计分录操作（增删改） | /journal-entries/operate |
| localhost:8081/journal-entries/preview | POST | 预览会计分录 | /journal-entries/preview |
| localhost:8081/payments/execute | POST | 执行付款（步骤4） | /payments/execute |
| localhost:8081/payments/contracts/{contractId} | GET | 查询合同付款记录 | /payments/contracts/{contractId} |
| localhost:8081/payments/{paymentId} | GET | 查询付款详情 | /payments/{paymentId} |
| localhost:8081/payments/operate | POST | 付款操作（增删改、取消） | /payments/operate |

## 会计分录统一设计

### 重要说明
**会计分录是统一的实体和数据表**，不区分"合同会计分录"和"付款会计分录"：

- **步骤3（摊销）**: 通过 `/journal-entries/generate/{contractId}` 生成摊销相关的会计分录
- **步骤4（付款）**: 通过 `/payments/execute` 执行付款时，同样生成会计分录到同一个表
- **查询和操作**: 所有会计分录都通过 `/journal-entries/*` 接口进行统一管理

### 会计分录类型区分
通过JournalEntry实体中的字段来区分不同业务场景：
- `entryType`: 分录类型（如：AMORTIZATION摊销、PAYMENT付款）
- `description`: 业务描述
- `contract`: 关联的合同
- `bookingDate`: 记账日期

## 统一操作接口设计

### 请求数据结构

```json
{
  "operate": "CREATE|UPDATE|DELETE",
  "id": 1,
  "data": {
    // 实体数据
  }
}
```

### 字段说明

- **operate**: 操作类型，必填
  - `CREATE`: 创建新记录
  - `UPDATE`: 更新现有记录
  - `DELETE`: 删除记录

- **id**: 实体ID，UPDATE和DELETE操作时必填

- **data**: 实体数据
  - CREATE操作：包含完整的实体数据
  - UPDATE操作：包含需要更新的字段
  - DELETE操作：可为空

### 操作示例

#### 1. 创建摊销明细
```bash
curl -X POST http://localhost:8081/amortization-entries/operate \
  -H "Content-Type: application/json" \
  -d '{
    "operate": "CREATE",
    "data": {
      "contract": {"id": 1},
      "amortizationPeriod": "2024-01",
      "accountingPeriod": "2024-01",
      "amount": 1000.00
    }
  }'
```

#### 2. 更新摊销明细
```bash
curl -X POST http://localhost:8081/amortization-entries/operate \
  -H "Content-Type: application/json" \
  -d '{
    "operate": "UPDATE",
    "id": 1,
    "data": {
      "amount": 1200.00
    }
  }'
```

#### 3. 删除摊销明细
```bash
curl -X POST http://localhost:8081/amortization-entries/operate \
  -H "Content-Type: application/json" \
  -d '{
    "operate": "DELETE",
    "id": 1
  }'
```

## 批量操作接口

### 批量操作URL
- 摊销明细：`/amortization-entries/batch-operate`
- 会计分录：`/journal-entries/batch-operate`

### 批量请求格式
```json
[
  {
    "operate": "CREATE",
    "data": { /* 实体数据1 */ }
  },
  {
    "operate": "UPDATE",
    "id": 2,
    "data": { /* 更新数据 */ }
  },
  {
    "operate": "DELETE",
    "id": 3
  }
]
```

## 响应格式

### 成功响应
- **CREATE/UPDATE**: 返回实体对象
- **DELETE**: 返回空或成功消息

### 错误响应
```json
{
  "error": "不支持的操作类型: INVALID_OPERATION",
  "message": "详细错误信息"
}
```

## 优势

1. **接口简化**: 减少接口数量，统一操作方式
2. **易于维护**: 统一的请求格式和处理逻辑
3. **批量支持**: 天然支持批量操作
4. **扩展性强**: 新增操作类型只需扩展枚举
5. **前端友好**: 统一的调用方式，减少前端代码复杂度

## 兼容性说明

- 原有的GET查询接口保持不变
- 专用的POST接口（如上传、生成等）保持不变
- 只合并了通用的增删改操作
