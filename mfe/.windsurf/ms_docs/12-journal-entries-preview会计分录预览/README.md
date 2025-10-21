# 预览会计分录接口

## 接口信息
- **URL**: `/journal-entries/preview`
- **Method**: POST
- **Description**: 预览会计分录，不保存到数据库，用于前端展示和确认

## 请求参数
```json
{
  "contractId": 1,
  "previewType": "AMORTIZATION"
}
```

## 响应格式
```json
{
  "contractId": 1,
  "previewEntries": [
    {
      "entryType": "AMORTIZATION",
      "bookingDate": "2024-01-31",
      "accountName": "费用",
      "debitAmount": 1000.00,
      "creditAmount": 0.00,
      "description": "摊销费用预览"
    },
    {
      "entryType": "AMORTIZATION",
      "bookingDate": "2024-01-31",
      "accountName": "应付",
      "debitAmount": 0.00,
      "creditAmount": 1000.00,
      "description": "摊销应付预览"
    }
  ]
}
```

## 预览类型
- **AMORTIZATION**: 预览摊销会计分录
- **PAYMENT**: 预览付款会计分录

## 使用场景
- 步骤3：生成会计分录前的预览
- 步骤4：付款前的会计分录预览
- 用户确认会计分录内容

## 特点
- 不保存到数据库
- 实时计算和展示
- 支持多种预览类型
