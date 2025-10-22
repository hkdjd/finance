# 供应商分布报表接口文档

## 接口信息
- **URL**: `/reports/vendor-distribution`
- **Method**: GET
- **Description**: 获取供应商分布报表数据，用于展示饼图，按供应商统计合同数量及占比
- **序号**: 20

## 请求参数
无需参数，系统自动统计所有供应商的合同分布

## 响应格式

### 成功响应
```json
{
  "vendors": [
    {
      "vendorName": "供应商A",
      "contractCount": 10,
      "percentage": 33.33
    },
    {
      "vendorName": "供应商B",
      "contractCount": 8,
      "percentage": 26.67
    },
    {
      "vendorName": "供应商C",
      "contractCount": 7,
      "percentage": 23.33
    },
    {
      "vendorName": "供应商D",
      "contractCount": 5,
      "percentage": 16.67
    }
  ],
  "totalContracts": 30,
  "generatedAt": "2024-10-22T00:00:00"
}
```

## 字段说明

### 响应字段
| 字段名 | 类型 | 说明 |
|--------|------|------|
| vendors | Array | 供应商分布数据列表，按合同数量降序排序 |
| totalContracts | Integer | 合同总数 |
| generatedAt | String | 数据生成时间（ISO 8601格式） |

### 供应商分布项字段
| 字段名 | 类型 | 说明 |
|--------|------|------|
| vendorName | String | 供应商名称 |
| contractCount | Integer | 该供应商的合同数量 |
| percentage | Double | 占比百分比（保留2位小数，如：25.50表示25.50%） |

## 数据计算规则

### 1. 合同数量统计
- **定义**: 按供应商名称分组统计合同数量
- **计算公式**: `GROUP BY vendor_name, COUNT(*)`
- **包含范围**: 所有状态的合同（包括生效中、已过期、未生效）

### 2. 占比计算
- **定义**: 每个供应商的合同数量占总合同数量的百分比
- **计算公式**: `(供应商合同数 / 总合同数) × 100`
- **精度**: 保留2位小数
- **示例**:
  - 供应商A: 10个合同，总数30 → 10/30×100 = 33.33%
  - 供应商B: 8个合同，总数30 → 8/30×100 = 26.67%

### 3. 排序规则
- 按合同数量降序排序
- 合同数量多的供应商排在前面
- 便于识别主要供应商

## 使用场景

### 业务场景
1. **供应商管理**: 了解各供应商的业务占比
2. **采购决策**: 识别主要合作供应商
3. **风险分析**: 评估供应商集中度风险
4. **战略规划**: 优化供应商结构

### 前端集成示例

#### 1. 获取报表数据
```javascript
const getVendorDistribution = async () => {
  const response = await fetch('/reports/vendor-distribution');
  const data = await response.json();
  return data;
};
```

#### 2. 饼图展示（使用 ECharts）
```javascript
const renderPieChart = async () => {
  const data = await getVendorDistribution();
  
  const option = {
    title: {
      text: '供应商分布',
      subtext: `合同总数: ${data.totalContracts}`,
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: data.vendors.map(v => v.vendorName)
    },
    series: [
      {
        name: '合同数量',
        type: 'pie',
        radius: '50%',
        data: data.vendors.map(v => ({
          value: v.contractCount,
          name: v.vendorName
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };
  
  myChart.setOption(option);
};
```

#### 3. 环形图展示
```javascript
const renderDonutChart = async () => {
  const data = await getVendorDistribution();
  
  const option = {
    title: {
      text: '供应商分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    series: [
      {
        name: '供应商',
        type: 'pie',
        radius: ['40%', '70%'],  // 环形图
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        data: data.vendors.map(v => ({
          value: v.contractCount,
          name: v.vendorName
        }))
      }
    ]
  };
  
  myChart.setOption(option);
};
```

#### 4. 表格展示
```javascript
const renderVendorTable = async () => {
  const data = await getVendorDistribution();
  
  return (
    <table className="vendor-table">
      <thead>
        <tr>
          <th>排名</th>
          <th>供应商名称</th>
          <th>合同数量</th>
          <th>占比</th>
        </tr>
      </thead>
      <tbody>
        {data.vendors.map((vendor, index) => (
          <tr key={vendor.vendorName}>
            <td>{index + 1}</td>
            <td>{vendor.vendorName}</td>
            <td>{vendor.contractCount}</td>
            <td>{vendor.percentage}%</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <td colSpan="2">合计</td>
          <td>{data.totalContracts}</td>
          <td>100%</td>
        </tr>
      </tfoot>
    </table>
  );
};
```

## 数据分析建议

### 1. 供应商集中度分析
```javascript
const analyzeConcentration = (data) => {
  // 计算前3大供应商的占比
  const top3Percentage = data.vendors
    .slice(0, 3)
    .reduce((sum, v) => sum + v.percentage, 0);
  
  if (top3Percentage > 70) {
    return '供应商集中度较高，建议分散风险';
  } else if (top3Percentage > 50) {
    return '供应商集中度适中';
  } else {
    return '供应商分布较为分散';
  }
};
```

### 2. 长尾供应商识别
```javascript
const identifyLongTail = (data) => {
  // 识别合同数量少于平均值的供应商
  const avgCount = data.totalContracts / data.vendors.length;
  const longTailVendors = data.vendors.filter(v => v.contractCount < avgCount);
  
  return {
    count: longTailVendors.length,
    percentage: (longTailVendors.length / data.vendors.length * 100).toFixed(2)
  };
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
- 建议缓存时间: 10-30分钟
- 缓存键: `vendor_distribution_{date}`
- 适合场景: 供应商数据变化不频繁

### 数据库优化
```sql
-- 建议添加的索引
CREATE INDEX idx_contracts_vendor_name ON contracts(vendor_name);
```

### 大数据量优化
如果供应商数量过多（>100），建议：
1. 只展示Top N供应商
2. 将其余供应商合并为"其他"类别
3. 提供详细列表的分页查询接口

```javascript
// 前端处理示例：只显示Top 10
const processTopVendors = (data, topN = 10) => {
  if (data.vendors.length <= topN) {
    return data;
  }
  
  const topVendors = data.vendors.slice(0, topN);
  const others = data.vendors.slice(topN);
  
  const othersCount = others.reduce((sum, v) => sum + v.contractCount, 0);
  const othersPercentage = others.reduce((sum, v) => sum + v.percentage, 0);
  
  return {
    ...data,
    vendors: [
      ...topVendors,
      {
        vendorName: '其他',
        contractCount: othersCount,
        percentage: Math.round(othersPercentage * 100) / 100
      }
    ]
  };
};
```

## 相关接口
- `GET /reports/dashboard` - 仪表盘报表（柱状图）
- `GET /contracts` - 合同列表
- `GET /contracts?vendorName={name}` - 按供应商筛选合同

## 扩展功能建议

### 1. 按金额统计
除了按合同数量，还可以按合同金额统计供应商分布

### 2. 时间范围筛选
支持查询特定时间范围内的供应商分布

### 3. 状态筛选
支持只统计生效中的合同，或按状态分类统计

### 4. 导出功能
支持导出为Excel或PDF格式

## 注意事项
1. 数据为实时统计，反映当前数据库最新状态
2. 包含所有状态的合同（生效中、已过期、未生效）
3. 供应商名称必须规范，避免因名称不一致导致统计错误
4. 百分比总和应为100%（可能因四舍五入存在微小误差）
5. 建议定期检查供应商集中度，防范业务风险
