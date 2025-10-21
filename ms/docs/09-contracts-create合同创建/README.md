# 创建合同接口

## 接口信息
- **URL**: `/contracts`
- **Method**: POST
- **Description**: 创建合同并初始化摊销台账，将计算结果保存到数据库

## 请求参数
```json
{
  "totalAmount": 4000.00,
  "startDate": "2025-01-01",
  "endDate": "2025-04-30",
  "vendorName": "供应商A",
  "description": "合同描述"
}
```

## 响应格式
```json
{
  "id": 1,
  "totalAmount": 4000.00,
  "startDate": "2025-01-01",
  "endDate": "2025-04-30",
  "vendorName": "供应商A",
  "description": "合同描述",
  "createdAt": "2024-12-24T14:30:52",
  "updatedAt": "2024-12-24T14:30:52",
  "createdBy": "system",
  "updatedBy": "system"
}
```

## 业务逻辑
1. 创建合同记录
2. 自动计算摊销明细
3. 保存摊销明细到数据库
4. 返回完整的合同信息

## 使用场景
- 步骤1：合同创建和摊销台账初始化
- 为后续步骤2和步骤3做数据准备

## 错误处理
- 合同信息验证失败：400 Bad Request
- 摊销计算失败：500 Internal Server Error
