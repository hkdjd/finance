# 摊销计算接口

## 接口信息
- **URL**: `/amortization/calculate/{contractId}`
- **Method**: GET
- **Description**: 根据合同ID计算摊销明细，支持三种场景的摊销计算

## 请求参数
- **路径参数**: `contractId` (Long) - 合同ID

**示例**: `GET /amortization/calculate/1`

## 响应格式
```json
{
  "totalAmount": 4000.00,
  "startDate": "2025-01",
  "endDate": "2025-04",
  "scenario": "SCENARIO_1",
  "generatedAt": "2025-01-15T10:35:20.456789+08:00",
  "entries": [
    {
      "id": null,
      "amortizationPeriod": "2025-01",
      "accountingPeriod": "2025-01",
      "amount": 1000.00
    },
    {
      "id": null,
      "amortizationPeriod": "2025-02",
      "accountingPeriod": "2025-02",
      "amount": 1000.00
    },
    {
      "id": null,
      "amortizationPeriod": "2025-03",
      "accountingPeriod": "2025-03",
      "amount": 1000.00
    },
    {
      "id": null,
      "amortizationPeriod": "2025-04",
      "accountingPeriod": "2025-04",
      "amount": 1000.00
    }
  ]
}
```

## 摊销场景
### 场景1：当前时间小于合同开始时间
- 合同总金额自动平均摊销到合同期间的每个月
- 预提/摊销期间 = 入账期间

### 场景2：当前时间在合同开始时间内
- 合同未开始期间的每个月摊销，分别记录到当前月份
- 预提/摊销期间 ≠ 入账期间（记录到当前月份）

### 场景3：当前时间大于合同结束时间
- 不用摊销到每个月，记当前月份的会计分录即可

## 接口特点
- **简化参数**: 只需要传入合同ID，无需重复传入合同信息
- **自动获取**: 从数据库自动获取合同的总金额、开始时间、结束时间等信息
- **GET方法**: 使用GET方法，符合查询操作的RESTful设计
- **缓存友好**: GET请求可以被浏览器和代理服务器缓存

## 使用场景
- 合同创建后的摊销明细预览
- 步骤2的摊销台账生成前的计算验证
- 为步骤3的会计分录生成做准备
- 前端展示摊销计算结果

## 调用示例
```bash
# 计算合同ID为1的摊销明细
curl http://localhost:8081/amortization/calculate/1
```
