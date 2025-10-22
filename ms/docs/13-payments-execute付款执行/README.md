# 执行付款接口

## 接口信息
- **URL**: `/payments/execute`
- **Method**: POST
- **Description**: 执行付款（步骤4付款阶段），根据用户填写的付款金额和付款时间生成会计分录并保存到数据库

## 请求参数
```json
{
  "contractId": 1,
  "paymentAmount": 6000.00,
  "paymentDate": "2025-10-20 19:17:04",
  "selectedPeriods": [1, 2, 3, 4, 5, 6]
}
```

### 参数说明
- `contractId` (Long, 必填): 合同ID
- `paymentAmount` (BigDecimal, 必填): 付款金额，必须大于0
- `paymentDate` (LocalDateTime, 可选): 支付时间，格式为YYYY-MM-DD HH:mm:ss，如为空则默认为当前时间
- `selectedPeriods` (List<Long>, 必填): 选择的摊销明细ID列表

## 响应格式
```json
{
  "paymentId": 1,
  "contractId": 1,
  "paymentAmount": 6000.00,
  "paymentDate": "2025-10-20 19:17:04",
  "status": "CONFIRMED",
  "selectedPeriods": "2025-01,2025-02,2025-03,2025-04,2025-05,2025-06",
  "journalEntries": [
    {
      "id": 10,
      "entryType": "PAYMENT",
      "bookingDate": "2024-03-20",
      "accountName": "应付",
      "debitAmount": 6000.00,
      "creditAmount": 0.00,
      "description": "付款冲减应付"
    },
    {
      "id": 11,
      "entryType": "PAYMENT", 
      "bookingDate": "2024-03-20",
      "accountName": "活期存款",
      "debitAmount": 0.00,
      "creditAmount": 6000.00,
      "description": "付款减少银行存款"
    }
  ]
}
```

## 业务逻辑
1. 验证合同存在性
2. 根据选择的摊销明细ID获取对应的期间信息
3. 创建付款记录，保存支付时间到数据库
4. 计算并生成付款会计分录（entryType=PAYMENT）
5. 保存会计分录到journal_entries表
6. 更新摊销明细的付款状态和付款日期
7. 返回付款信息和生成的会计分录

## 会计分录规则
- **借方**: 应付账款（冲减应付）
- **贷方**: 活期存款（减少银行存款）
- **记账日期**: 根据支付时间和摊销入账时间计算得出
- **分录类型**: PAYMENT
- **支付时间**: 作为付款记录的重要字段保存到数据库

## 使用场景
- 步骤4：执行实际付款操作
- 生成付款相关的会计分录
- 更新合同的付款状态

## 错误处理
- 合同不存在：IllegalArgumentException "未找到合同，ID=xxx"
- 摊销明细不存在：IllegalArgumentException "未找到摊销明细，ID=xxx"
- 付款金额无效：400 Bad Request "付款金额必须大于0"
- 支付时间格式错误：400 Bad Request
- 选择的摊销明细ID为空：400 Bad Request "选择的摊销明细ID不能为空"
