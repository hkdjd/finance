# 执行付款接口

## 接口信息
- **URL**: `/payments/execute`
- **Method**: POST
- **Description**: 执行付款（步骤4付款阶段），根据用户填写的付款金额和付款时间生成会计分录并保存到数据库

## 请求参数
```json
{
  "contractId": 1,
  "paymentAmount": 2000.00,
  "paymentDate": "2024-02-15",
  "paymentMethod": "银行转账",
  "description": "部分付款"
}
```

## 响应格式
```json
{
  "paymentId": 1,
  "contractId": 1,
  "paymentAmount": 2000.00,
  "paymentDate": "2024-02-15",
  "paymentMethod": "银行转账",
  "status": "COMPLETED",
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
  ]
}
```

## 业务逻辑
1. 创建付款记录
2. 生成付款会计分录（entryType=PAYMENT）
3. 保存会计分录到journal_entries表
4. 更新摊销明细的付款状态
5. 返回付款信息和生成的会计分录

## 会计分录规则
- **借方**: 应付账款（冲减应付）
- **贷方**: 活期存款（减少银行存款）
- **记账日期**: 付款日期
- **分录类型**: PAYMENT

## 使用场景
- 步骤4：执行实际付款操作
- 生成付款相关的会计分录
- 更新合同的付款状态

## 错误处理
- 合同不存在：404 Not Found
- 付款金额超过应付金额：400 Bad Request
- 付款日期无效：400 Bad Request
