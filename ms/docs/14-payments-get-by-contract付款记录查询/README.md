# 查询合同付款记录接口

## 接口信息
- **URL**: `/payments/contracts/{contractId}`
- **Method**: GET
- **Description**: 查询指定合同的所有付款记录

## 请求参数
- **路径参数**: `contractId` - 合同ID

## 响应格式
```json
{
  "contractId": 1,
  "payments": [
    {
      "id": 1,
      "paymentAmount": 2000.00,
      "paymentDate": "2024-02-15",
      "paymentMethod": "银行转账",
      "status": "COMPLETED",
      "description": "部分付款",
      "createdAt": "2024-02-15T10:30:00"
    },
    {
      "id": 2,
      "paymentAmount": 1500.00,
      "paymentDate": "2024-03-10",
      "paymentMethod": "银行转账",
      "status": "COMPLETED",
      "description": "剩余付款",
      "createdAt": "2024-03-10T14:20:00"
    }
  ],
  "totalPayments": 2,
  "totalPaidAmount": 3500.00,
  "remainingAmount": 500.00
}
```

## 字段说明
- **totalPayments**: 付款记录总数
- **totalPaidAmount**: 已付款总金额
- **remainingAmount**: 剩余应付金额

## 使用场景
- 步骤4：查看合同的付款历史
- 财务对账和核实
- 付款进度跟踪

## 错误处理
- 合同不存在：404 Not Found
- 无付款记录：返回空列表
