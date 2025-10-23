# 21-audit-log审计日志接口

## 接口概述
提供摊销明细的审计日志查询功能，记录付款操作的历史记录。

## 接口详情

### 1. 查询摊销明细审计日志

**接口路径：** `GET /audit-logs/amortization-entry/{amortizationEntryId}`

**功能描述：** 根据摊销明细ID查询该明细的所有审计日志记录

**请求参数：**
- `amortizationEntryId` (路径参数): 摊销明细ID，必填

**响应格式：** JSON

**成功响应示例：**
```json
{
  "auditLogs": [
    {
      "id": 1,
      "amortizationEntryId": 123,
      "operationType": "PAYMENT",
      "operationTypeDesc": "付款",
      "operatorId": "user001",
      "operationTime": "2024-10-23 10:15:30",
      "paymentAmount": 1000.00,
      "paymentDate": "2024-10-23",
      "paymentStatus": "PAID",
      "paymentStatusDesc": "已付款",
      "oldPaymentAmount": null,
      "oldPaymentDate": null,
      "oldPaymentStatus": null,
      "oldPaymentStatusDesc": null,
      "remark": "付款操作：期间2024-10，付款金额1000.00",
      "createdAt": "2024-10-23 10:15:30",
      "createdBy": "user001"
    }
  ],
  "totalCount": 1,
  "amortizationEntryId": 123,
  "message": "查询成功"
}
```

**错误响应示例：**
```json
{
  "auditLogs": [],
  "totalCount": 0,
  "amortizationEntryId": 123,
  "message": "查询审计日志失败: 摊销明细不存在"
}
```

## 数据字段说明

### AuditLogInfo 对象字段

| 字段名 | 类型 | 描述 | 示例 |
|--------|------|------|------|
| id | Long | 审计日志ID | 1 |
| amortizationEntryId | Long | 摊销明细ID | 123 |
| operationType | String | 操作类型 | "PAYMENT" |
| operationTypeDesc | String | 操作类型描述 | "付款" |
| operatorId | String | 操作人ID | "user001" |
| operationTime | String | 操作时间 | "2024-10-23 10:15:30" |
| paymentAmount | BigDecimal | 支付金额 | 1000.00 |
| paymentDate | String | 付款时间 | "2024-10-23" |
| paymentStatus | String | 付款状态 | "PAID" |
| paymentStatusDesc | String | 付款状态描述 | "已付款" |
| oldPaymentAmount | BigDecimal | 修改前支付金额 | null |
| oldPaymentDate | String | 修改前付款时间 | null |
| oldPaymentStatus | String | 修改前付款状态 | null |
| oldPaymentStatusDesc | String | 修改前付款状态描述 | null |
| remark | String | 备注 | "付款操作：期间2024-10，付款金额1000.00" |
| createdAt | String | 创建时间 | "2024-10-23 10:15:30" |
| createdBy | String | 创建人 | "user001" |

### 操作类型枚举

| 值 | 描述 |
|----|------|
| PAYMENT | 付款 |
| UPDATE | 更新 |
| DELETE | 删除 |

### 付款状态枚举

| 值 | 描述 |
|----|------|
| PENDING | 待付款 |
| PAID | 已付款 |
| CANCELLED | 已取消 |

## 使用场景

1. **合同详情页面**：在摊销明细表格中，对于已付款的行，显示"audit log"超链接
2. **审计追踪**：查看某个摊销明细的所有付款操作历史
3. **问题排查**：当付款状态异常时，通过审计日志追踪操作历史

## 前端集成说明

1. 在摊销明细表格的操作列中，对于状态为"已付款"的行，添加"audit log"超链接
2. 点击超链接时，调用此接口获取审计日志数据
3. 在弹窗中展示审计日志列表，按操作时间倒序排列
4. 显示操作人、操作时间、付款金额、付款状态等关键信息

## 注意事项

1. 只有状态为"已付款"的摊销明细才显示audit log链接
2. 审计日志按操作时间倒序排列，最新的操作在前
3. 审计日志一旦生成不可修改，确保数据的完整性和可追溯性
4. 操作人ID应该从当前登录用户获取
