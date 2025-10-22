# 财务报表页面使用说明

## 📊 功能概述

新增的报表页面提供了两个核心报表功能：
1. **仪表盘报表（柱状图）** - 展示生效合同数量、本月摊销金额、剩余待付款金额
2. **供应商分布报表（饼图）** - 展示各供应商的合同数量及占比

## 🚀 快速开始

### 1. 安装依赖

```bash
cd /Users/victor/Develop/code/ocbc/finance/mfe
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

### 3. 访问报表页面

在浏览器中访问：
```
http://localhost:5173/reports
```

## 📁 文件结构

```
mfe/
├── src/
│   ├── api/
│   │   └── reports/
│   │       ├── index.ts          # 报表 API 接口
│   │       └── types.ts          # 报表类型定义
│   ├── pages/
│   │   └── Reports/
│   │       ├── index.tsx         # 报表页面组件
│   │       └── styles.module.css # 报表页面样式
│   └── router/
│       └── index.tsx             # 路由配置（已添加 /reports 路由）
└── REPORTS_README.md             # 本文档
```

## 🎨 页面功能

### 数据卡片
- 生效合同数量
- 本月摊销金额
- 剩余待付款金额

### 柱状图
- 展示三个关键财务指标
- 支持鼠标悬停查看详细数值
- 自动格式化金额显示

### 饼图
- 展示供应商合同分布
- 环形图设计，更加美观
- 显示百分比和具体数量

### 供应商详细列表
- 表格形式展示所有供应商数据
- 包含排名、名称、数量、占比
- 支持响应式布局

## 🔌 API 接口

### 1. 获取仪表盘报表

```typescript
import { getDashboardReport } from '@/api';

const data = await getDashboardReport();
// 返回: DashboardReportResponse
```

### 2. 获取供应商分布

```typescript
import { getVendorDistribution } from '@/api';

const data = await getVendorDistribution();
// 返回: VendorDistributionResponse
```

## 🎯 数据类型

### DashboardReportResponse

```typescript
interface DashboardReportResponse {
  activeContractCount: number;        // 生效合同数量
  currentMonthAmortization: number;   // 本月摊销金额
  remainingPayableAmount: number;     // 剩余待付款金额
  statisticsMonth: string;            // 统计月份 (yyyy-MM)
  generatedAt: string;                // 生成时间
}
```

### VendorDistributionResponse

```typescript
interface VendorDistributionResponse {
  vendors: VendorDistributionItem[];  // 供应商列表
  totalContracts: number;             // 合同总数
  generatedAt: string;                // 生成时间
}

interface VendorDistributionItem {
  vendorName: string;                 // 供应商名称
  contractCount: number;              // 合同数量
  percentage: number;                 // 占比百分比
}
```

## 🛠️ 技术栈

- **React 18** - UI 框架
- **TypeScript** - 类型安全
- **Ant Design** - UI 组件库
- **ECharts** - 图表库
- **echarts-for-react** - React ECharts 封装
- **Axios** - HTTP 客户端
- **React Router** - 路由管理
- **CSS Modules** - 样式隔离

## 📱 响应式设计

页面支持多种屏幕尺寸：
- **桌面端** (>768px): 双列布局，图表并排显示
- **移动端** (≤768px): 单列布局，图表垂直堆叠

## 🎨 样式特性

- 渐变色标题栏
- 卡片阴影和悬停效果
- 图表交互动画
- 表格斑马纹
- 响应式字体大小

## 🔧 自定义配置

### 修改图表颜色

编辑 `src/pages/Reports/index.tsx`：

```typescript
// 柱状图颜色
const colors = ['#5470c6', '#91cc75', '#fac858'];

// 饼图会自动使用 ECharts 默认配色
```

### 修改 API 地址

编辑 `src/api/client.ts`：

```typescript
const baseURL = 'http://localhost:8081';  // 修改为你的后端地址
```

## 🐛 常见问题

### 1. 图表不显示

**原因**: 后端服务未启动或 API 地址错误

**解决**: 
- 确保后端服务运行在 `http://localhost:8081`
- 检查浏览器控制台的网络请求

### 2. 数据加载失败

**原因**: 后端接口返回错误

**解决**:
- 检查后端日志
- 确认数据库中有数据
- 查看浏览器 Network 面板的响应

### 3. 样式错乱

**原因**: CSS Modules 未正确加载

**解决**:
- 重启开发服务器
- 清除浏览器缓存

## 📝 开发建议

### 添加刷新按钮

```typescript
<Button onClick={loadReportData} icon={<ReloadOutlined />}>
  刷新数据
</Button>
```

### 添加日期筛选

```typescript
<DatePicker.RangePicker onChange={handleDateChange} />
```

### 添加导出功能

```typescript
import * as XLSX from 'xlsx';

const exportToExcel = () => {
  const worksheet = XLSX.utils.json_to_sheet(vendorData.vendors);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, "供应商分布");
  XLSX.writeFile(workbook, "供应商分布报表.xlsx");
};
```

## 🚀 部署

### 构建生产版本

```bash
npm run build
```

构建产物将生成在 `dist/` 目录。

### 预览生产版本

```bash
npm run preview
```

## 📞 技术支持

如有问题，请联系开发团队或查看：
- 后端接口文档: `ms/docs/19-reports-dashboard仪表盘报表/`
- 后端接口文档: `ms/docs/20-reports-vendor-distribution供应商分布报表/`

## 🎉 更新日志

### v1.0.0 (2024-10-22)
- ✨ 新增仪表盘报表页面
- ✨ 新增供应商分布报表
- ✨ 支持响应式布局
- ✨ 添加数据卡片展示
- ✨ 集成 ECharts 图表库
