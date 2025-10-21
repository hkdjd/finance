# 取消付款接口

## 接口信息
- **URL**: `/payments/operate`
- **Method**: POST
- **Description**: 通过operate字段取消付款操作

## 请求参数
```json
{
  "operate": "DELETE",
  "id": 1,
  "data": {
    "cancelReason": "付款错误，需要重新处理"
  }
}
```

## 响应格式
```json
{
  "success": true,
  "message": "付款已成功取消",
  "paymentId": 1,
  "cancelledAt": "2024-02-16T09:15:00"
}
```

## 业务逻辑
1. 验证付款状态（只能取消COMPLETED状态的付款）
2. 生成冲销会计分录
3. 更新付款状态为CANCELLED
4. 更新相关摊销明细的付款状态

## 冲销会计分录
取消付款时会生成相反的会计分录：
```json
[
  {
    "entryType": "PAYMENT",
    "accountName": "应付",
    "debitAmount": 0.00,
    "creditAmount": 2000.00,
    "description": "取消付款-恢复应付"
  },
  {
    "entryType": "PAYMENT",
    "accountName": "活期存款",
    "debitAmount": 2000.00,
    "creditAmount": 0.00,
    "description": "取消付款-恢复银行存款"
  }
]
```

## 使用场景
- 付款错误的纠正
- 财务调整需要
- 合同变更导致的付款取消

## 错误处理
- 付款不存在：404 Not Found
- 付款已取消：400 Bad Request
- 无权限操作：403 Forbidden

## 注意事项
- 此接口已统一到 `/payments/operate`
- 通过operate=DELETE实现取消功能
