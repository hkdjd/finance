# 仪表盘报表接口文档

## 接口信息
- **URL**: `/reports/dashboard`
- **Method**: GET
- **Description**: 获取仪表盘报表数据，用于展示柱状图，包含生效合同数量、本月摊销金额、剩余待付款金额
- **序号**: 19

## 请求参数
无需参数，系统自动统计当前实时数据

## 响应格式

### 成功响应
```json
{
  "activeContractCount": 15,
  "currentMonthAmortization": 12500.00,
  "remainingPayableAmount": 45000.00,
  "statisticsMonth": "2024-10",
  "generatedAt": "2024-10-22T00:00:00"
}
```

## 字段说明

### 响应字段
| 字段名 | 类型 | 说明 |
|--------|------|------|
| activeContractCount | Integer | 生效合同数量（当前日期在合同期间内的合同） |
| currentMonthAmortization | BigDecimal | 本月摊销金额（当前月份所有摊销明细的金额总和） |
| remainingPayableAmount | BigDecimal | 剩余待付款金额（所有待付款状态的摊销明细未付金额总和） |
| statisticsMonth | String | 统计月份（格式：yyyy-MM） |
| generatedAt | String | 数据生成时间（ISO 8601格式） |

## 数据计算规则

### 1. 生效合同数量
- **定义**: 当前日期在合同开始日期和结束日期之间的合同
- **计算公式**: `COUNT(合同) WHERE 今天 >= 开始日期 AND 今天 <= 结束日期`
- **示例**: 
  - 合同A: 2024-01-01 至 2024-12-31 → 生效中 ✓
  - 合同B: 2025-01-01 至 2025-12-31 → 未生效 ✗
  - 合同C: 2023-01-01 至 2023-12-31 → 已过期 ✗

### 2. 本月摊销金额
- **定义**: 摊销期间为当前月份的所有摊销明细金额之和
- **计算公式**: `SUM(摊销明细.金额) WHERE 摊销期间 = 当前月份(yyyy-MM)`
- **示例**: 当前为2024年10月
  - 摊销明细1: 期间=2024-10, 金额=5000 → 计入 ✓
  - 摊销明细2: 期间=2024-09, 金额=5000 → 不计入 ✗

### 3. 剩余待付款金额
- **定义**: 所有付款状态为"待付款"的摊销明细的未付金额之和
- **计算公式**: `SUM(摊销明细.金额 - 摊销明细.已付金额) WHERE 付款状态 = PENDING`
- **示例**:
  - 摊销明细1: 金额=10000, 已付=0, 状态=PENDING → 计入10000 ✓
  - 摊销明细2: 金额=10000, 已付=5000, 状态=PENDING → 计入5000 ✓
  - 摊销明细3: 金额=10000, 已付=10000, 状态=COMPLETED → 不计入 ✗

## 使用场景

### 业务场景
1. **财务仪表盘**: 在首页展示关键财务指标
2. **管理决策**: 快速了解当前合同执行情况
3. **资金规划**: 根据待付款金额安排资金
4. **月度报告**: 生成月度财务摘要

### 前端集成示例

#### 1. 获取报表数据
```javascript
const getDashboardReport = async () => {
  const response = await fetch('/reports/dashboard');
  const data = await response.json();
  return data;
};
```

#### 2. 柱状图展示（使用 ECharts）
```javascript
const renderBarChart = async () => {
  const data = await getDashboardReport();
  
  const option = {
    title: {
      text: `财务仪表盘 - ${data.statisticsMonth}`
    },
    xAxis: {
      type: 'category',
      data: ['生效合同数量', '本月摊销金额', '剩余待付款金额']
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      data: [
        data.activeContractCount,
        data.currentMonthAmortization,
        data.remainingPayableAmount
      ],
      type: 'bar',
      itemStyle: {
        color: function(params) {
          const colors = ['#5470c6', '#91cc75', '#fac858'];
          return colors[params.dataIndex];
        }
      }
    }]
  };
  
  myChart.setOption(option);
};
```

#### 3. 卡片展示
```javascript
const renderDashboardCards = async () => {
  const data = await getDashboardReport();
  
  return (
    <div className="dashboard-cards">
      <Card title="生效合同" value={data.activeContractCount} unit="个" />
      <Card title="本月摊销" value={data.currentMonthAmortization} unit="元" />
      <Card title="待付款" value={data.remainingPayableAmount} unit="元" />
    </div>
  );
};
```

## 错误处理

### 常见错误码
- **500 Internal Server Error**: 数据库查询失败或计算错误

### 错误响应格式
```json
{
  "error": "DATABASE_ERROR",
  "message": "数据库查询失败",
  "timestamp": "2024-10-22T10:30:00.123456"
}
```

## 性能优化

### 缓存策略
- 建议缓存时间: 5-10分钟
- 缓存键: `dashboard_report_{date}`
- 适合场景: 高频访问的首页仪表盘

### 数据库优化
```sql
-- 建议添加的索引
CREATE INDEX idx_contracts_dates ON contracts(start_date, end_date);
CREATE INDEX idx_amortization_period ON amortization_entries(amortization_period);
CREATE INDEX idx_amortization_status ON amortization_entries(payment_status);
```

## 相关接口
- `GET /reports/vendor-distribution` - 供应商分布报表（饼图）
- `GET /contracts` - 合同列表
- `GET /amortization/contract/{contractId}` - 合同摊销台账

## 注意事项
1. 数据为实时统计，反映当前数据库最新状态
2. 生效合同数量会随着时间推移自动变化
3. 本月摊销金额仅统计摊销期间为当前月份的数据
4. 待付款金额包含所有未来期间的待付款项
5. 建议在前端添加数据刷新功能，确保数据时效性
