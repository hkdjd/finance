# 查询付款详情接口

## 接口信息
- **URL**: `/payments/{paymentId}`
- **Method**: GET
- **Description**: 查询指定付款的详细信息

## 请求参数
- **路径参数**: `paymentId` - 付款ID

## 响应格式
```json
{
  "id": 1,
  "contract": {
    "id": 1,
    "totalAmount": 4000.00,
    "vendorName": "供应商A"
  },
  "paymentAmount": 2000.00,
  "paymentDate": "2024-02-15",
  "paymentMethod": "银行转账",
  "status": "COMPLETED",
  "description": "部分付款",
  "journalEntries": [
    {
      "id": 10,
      "entryType": "PAYMENT",
      "bookingDate": "2024-02-15",
      "accountName": "应付",
      "debitAmount": 2000.00,
      "creditAmount": 0.00,
      "description": "付款冲减应付"
    },
    {
      "id": 11,
      "entryType": "PAYMENT",
      "bookingDate": "2024-02-15",
      "accountName": "活期存款",
      "debitAmount": 0.00,
      "creditAmount": 2000.00,
      "description": "付款减少银行存款"
    }
  ],
  "createdAt": "2024-02-15T10:30:00",
  "updatedAt": "2024-02-15T10:30:00",
  "createdBy": "user123",
  "updatedBy": "user123"
}
```

## 包含信息
- 付款基本信息
- 关联的合同信息
- 生成的会计分录
- 审计字段

## 使用场景
- 付款详情查看
- 会计分录确认
- 付款记录审计

## 错误处理
- 付款不存在：404 Not Found
- 无权限访问：403 Forbidden
