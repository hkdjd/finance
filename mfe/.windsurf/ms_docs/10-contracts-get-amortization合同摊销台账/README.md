# 查询合同摊销台账接口

## 接口信息
- **URL**: `/contracts/{id}/amortization`
- **Method**: GET
- **Description**: 查询指定合同的摊销台账信息

## 请求参数
- **路径参数**: `id` - 合同ID

## 响应格式
```json
{
  "contractId": 1,
  "contract": {
    "id": 1,
    "totalAmount": 4000.00,
    "startDate": "2025-01-01",
    "endDate": "2025-04-30",
    "vendorName": "供应商A"
  },
  "amortizationEntries": [
    {
      "id": 1,
      "amortizationPeriod": "2025-01",
      "accountingPeriod": "2025-01",
      "amount": 1000.00,
      "periodDate": "2025-01-01",
      "paymentStatus": "PENDING"
    },
    {
      "id": 2,
      "amortizationPeriod": "2025-02",
      "accountingPeriod": "2025-02",
      "amount": 1000.00,
      "periodDate": "2025-02-01",
      "paymentStatus": "PENDING"
    }
  ],
  "totalEntries": 4,
  "totalAmount": 4000.00
}
```

## 使用场景
- 步骤2：查看合同的摊销台账
- 前端展示摊销明细列表
- 为步骤3生成会计分录做准备

## 错误处理
- 合同不存在：404 Not Found
- 摊销台账未生成：404 Not Found
