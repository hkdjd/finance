# 摊销明细操作接口

## 接口信息
- **URL**: `/amortization-entries/operate`
- **Method**: POST
- **Description**: 摊销明细更新接口，支持增删改操作（根据amortization列表里子项的id来判定每条数据的操作：若request的id为null，则新增；若request的id与数据库中的id一致，则更新；若数据库中存在的id在request中不存在，则删除）

## 请求参数
请求格式与列表接口响应格式保持一致，包含合同ID和该合同所有摊销明细的完整信息：

```json
{
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
    },
    {
      "id": 3,
      "amortizationPeriod": "2025-03",
      "accountingPeriod": "2025-03",
      "amount": 1000.00,
      "periodDate": "2025-03-01",
      "paymentStatus": "COMPLETED",
      "createdAt": "2024-12-24T14:30:52.123456",
      "updatedAt": "2024-12-24T14:30:52.123456",
      "createdBy": "system",
      "updatedBy": "system"
    },
    {
      "id": 4,
      "amortizationPeriod": "2025-04",
      "accountingPeriod": "2025-04",
      "amount": 800.00,
      "periodDate": "2025-04-01",
      "paymentStatus": "PENDING",
      "createdAt": "2024-12-24T14:30:52.123456",
      "updatedAt": "2024-12-24T14:30:52.123456",
      "createdBy": "system",
      "updatedBy": "system"
    }
  ]
}
```

## 响应格式
返回包装格式的数据，包含合同信息和操作后的摊销明细数组，与列表接口格式保持一致。

```json
{
  "contract": {
    "id": 1,
    "totalAmount": 4000.00,
    "startDate": "2025-01-01",
    "endDate": "2025-04-30",
    "vendorName": "供应商A"
  },
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
    }
  ]
}
```

## 接口特点
- **统一格式**: 请求格式与列表接口响应格式保持一致
- **完整更新**: 包含该合同所有摊销明细的完整信息
- **支持增删改**: 根据ID字段自动判断操作类型
- **批量处理**: 一次请求可处理多个摊销明细的增删改操作

## 操作逻辑
- **新增**: 当摊销明细的 `id` 为 `null` 时，创建新的摊销明细
- **更新**: 当摊销明细的 `id` 与数据库中的ID一致时，更新该摊销明细
- **删除**: 当数据库中存在的摊销明细ID在请求中不存在时，删除该摊销明细

## 使用场景
- 步骤2：摊销明细的完整管理（支持增删改操作）
- 前端表格编辑操作（支持增删行）
- 前端表格整体保存，包含所有行的数据
- 与列表接口数据格式无缝对接
- 动态调整摊销期间和金额分配

## 业务特性
- **灵活管理**: 支持摊销明细的完整生命周期管理
- **智能判断**: 根据ID字段自动识别操作类型
- **数据一致**: 确保请求数据与数据库状态完全同步
- **批量操作**: 一次请求完成所有变更，保证数据一致性

## 错误处理
- 合同不存在：400 Bad Request "未找到合同，ID=xxx"
- 摊销明细不存在：400 Bad Request "未找到摊销明细，ID=xxx"
- 请求格式错误：400 Bad Request
- 数据验证失败：400 Bad Request

## 前端集成建议

### 数据流程
1. **获取数据**: 调用列表接口获取摊销明细数据
2. **编辑操作**: 前端表格直接编辑数据（支持增删行）
3. **保存数据**: 将编辑后的完整数据发送到此接口

### 代码示例
```javascript
// 1. 获取摊销明细列表
const listResponse = await fetch('/amortization-entries/contract/1');
const listData = await listResponse.json();

// 2. 前端编辑后，保存数据（支持增删改）
const updateRequest = {
  contractId: listData.contract.id,
  amortization: [
    // 更新现有摊销明细（有ID）
    {
      id: 1,
      amortizationPeriod: "2025-01",
      accountingPeriod: "2025-01",
      amount: 1500.00, // 修改金额
      periodDate: "2025-01-01",
      paymentStatus: "PENDING"
    },
    // 新增摊销明细（ID为null）
    {
      id: null,
      amortizationPeriod: "2025-05",
      accountingPeriod: "2025-05",
      amount: 800.00,
      periodDate: "2025-05-01",
      paymentStatus: "PENDING"
    }
    // 删除操作：原有ID=2的摊销明细不在此列表中，将被自动删除
  ]
};

const updateResponse = await fetch('/amortization-entries/operate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(updateRequest)
});
```

### 操作示例说明
- **更新**: 包含现有ID的摊销明细将被更新
- **新增**: ID为null的摊销明细将被创建
- **删除**: 数据库中存在但请求中不包含的摊销明细将被删除

## 技术说明
- 使用POST方法，符合RESTful设计原则
- 请求和响应格式与列表接口保持一致
- 支持JSON格式的请求和响应
- 包含完整的审计信息（创建时间、修改时间等）
- 简化的单一接口设计，无需区分单个和批量操作
