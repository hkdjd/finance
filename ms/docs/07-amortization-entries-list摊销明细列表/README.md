# 摊销明细接口文档

## 步骤2：摊销明细管理

根据需求文档，步骤2要求输出形式为List<Entity>形式交给前端，并且后端支持对数据进行调整（修改，加行或删除行）。

## 接口列表

### 1. 查询合同摊销明细列表
- **URL**: `/amortization-entries/contract/{contractId}`
- **Method**: GET
- **Description**: 查询指定合同的所有摊销明细
- **返回格式**: List<AmortizationEntry>

### 2. 查询单个摊销明细
- **URL**: `/amortization-entries/{entryId}`
- **Method**: GET
- **Description**: 查询指定ID的摊销明细

### 3. 统一操作接口（增删改）
- **URL**: `/amortization-entries/operate`
- **Method**: POST
- **Description**: 通过operate字段区分操作类型：CREATE、UPDATE、DELETE

### 4. 批量操作接口
- **URL**: `/amortization-entries/batch-operate`
- **Method**: POST
- **Description**: 支持批量增删改操作

## 响应格式
返回包装格式的数据，包含合同信息和摊销明细数组，避免合同信息在每个摊销明细中重复。

```json
{
  "contract": {
    "id": 1,
    "totalAmount": 4000.00,
    "startDate": "2025-01-01",
    "endDate": "2025-04-30",
    "vendorName": "供应商A",
    "customFields": "{\"法定代表人\":\"张明\",\"项目经理\":\"李华\"}"
  },
  "amortization": [
    {
      "id": 1,
      "amortizationPeriod": "2025-01",
      "accountingPeriod": "2025-01",
      "amount": 1000.00,
      "periodDate": "2025-01-01",
      "paymentStatus": "COMPLETED",
      "paymentDate": "2025-01-15",
      "createdAt": "2024-12-24T14:30:52.123456",
      "updatedAt": "2025-01-15T10:20:30.123456",
      "createdBy": "system",
      "updatedBy": "admin"
    },
    {
      "id": 2,
      "amortizationPeriod": "2025-02",
      "accountingPeriod": "2025-02",
      "amount": 1000.00,
      "periodDate": "2025-02-01",
      "paymentStatus": "PENDING",
      "paymentDate": null,
      "createdAt": "2024-12-24T14:30:52.123456",
      "updatedAt": "2024-12-24T14:30:52.123456",
      "createdBy": "system",
      "updatedBy": "system"
    }
  ]
}
```

## 数据结构

### AmortizationEntry实体字段
```json
{
  "id": 1,
  "contract": {
    "id": 1,
    "totalAmount": 4000.00,
    "startDate": "2025-01-01",
    "endDate": "2025-04-30",
    "vendorName": "供应商A",
    "createdAt": "2024-12-20T09:30:00.123456",
    "customFields": "{\"法定代表人\":\"张明\",\"项目经理\":\"李华\"}"
  },
  "amortizationPeriod": "2025-01",
  "accountingPeriod": "2025-01",
  "amount": 1000.00,
  "periodDate": "2025-01-01",
  "paymentStatus": "PENDING",
  "paymentDate": null,
  "createdAt": "2024-12-24T14:30:52",
  "updatedAt": "2024-12-24T14:30:52",
  "createdBy": "system",
  "updatedBy": "system"
}
```

### 合同字段说明
- **id**: 合同ID
- **totalAmount**: 合同总金额
- **startDate**: 合同开始日期（格式 yyyy-MM-dd）
- **endDate**: 合同结束日期（格式 yyyy-MM-dd）
- **vendorName**: 供应商名称
- **createdAt**: 合同创建时间（格式 yyyy-MM-ddTHH:mm:ss.SSSSSS）
- **customFields**: 自定义字段（JSON字符串格式），包含AI提取或用户录入的自定义信息，如：`{"法定代表人":"张明","项目经理":"李华"}`

### 摊销明细字段说明
- **id**: 摊销明细ID
- **amortizationPeriod**: 预提/摊销期间，格式 yyyy-MM
- **accountingPeriod**: 入账期间，格式 yyyy-MM
- **amount**: 预提/摊销金额
- **periodDate**: 期间日期（月份第一天）
- **paymentStatus**: 付款状态
  - `PENDING`: 待付款
  - `COMPLETED`: 已完成
- **paymentDate**: 支付时间（格式 yyyy-MM-dd）
  - 当 `paymentStatus` 为 `COMPLETED` 时，该字段有值，表示实际支付日期
  - 当 `paymentStatus` 为 `PENDING` 时，该字段为 `null`
- **createdAt**: 创建时间
- **updatedAt**: 更新时间
- **createdBy**: 创建人
- **updatedBy**: 更新人

```
```

## 业务逻辑

### 摊销规则
根据需求文档的场景要求：

#### 场景1：当前时间小于合同开始时间
- 合同总金额自动平均摊销到合同期间的每个月
- 预提/摊销期间 = 入账期间

#### 场景2：当前时间在合同开始时间内
- 合同未开始期间的每个月摊销，分别记录到当前月份
- 预提/摊销期间 ≠ 入账期间（记录到当前月份）

#### 场景3：当前时间大于合同结束时间
- 不用摊销到每个月，记当前月份的会计分录即可

### 示例数据
合同期间：2025年1月-2025年4月，总金额4000元

```
预提/摊销期间    入账期间    预提/摊销金额
2025年1月       2025年1月  1000.00
2025年2月       2025年2月  1000.00
2025年3月       2025年3月  1000.00
2025年4月       2025年4月  1000.00
```

## 统一操作接口使用

### 请求格式
```json
{
  "operate": "CREATE|UPDATE|DELETE",
  "id": 1,
  "data": {
    // AmortizationEntry实体数据
  }
}
```

### 操作示例

#### 创建摊销明细
```bash
curl -X POST /amortization-entries/operate \
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

#### 更新摊销明细
```bash
curl -X POST /amortization-entries/operate \
  -H "Content-Type: application/json" \
  -d '{
    "operate": "UPDATE",
    "id": 1,
    "data": {
      "amount": 1200.00
    }
  }'
```

#### 删除摊销明细
```bash
curl -X POST /amortization-entries/operate \
  -H "Content-Type: application/json" \
  -d '{
    "operate": "DELETE",
    "id": 1
  }'
```

## 前端操作支持

### 表格编辑功能
- **查询**: 获取合同的所有摊销明细
- **修改**: 通过operate=UPDATE操作修改金额等字段
- **新增行**: 通过operate=CREATE操作添加新的摊销明细行
- **删除行**: 通过operate=DELETE操作删除摊销明细行
- **批量保存**: 通过batch-operate接口批量保存到数据库

### 数据验证
- 摊销期间格式验证（yyyy-MM）
- 入账期间格式验证（yyyy-MM）
- 金额必须大于0
- 同一合同的摊销期间不能重复

## 使用场景

### 步骤2工作流程
1. 系统根据合同信息自动计算摊销明细
2. 前端以List<Entity>格式接收数据
3. 用户在页面上查看和编辑摊销明细表格
4. 支持增删改操作
5. 用户点击保存按钮保存到数据库
6. 为步骤3生成会计分录做准备

## 错误处理
- 合同不存在: 404 Not Found
- 摊销明细不存在: 404 Not Found
- 数据验证失败: 400 Bad Request
- 重复的摊销期间: 409 Conflict
