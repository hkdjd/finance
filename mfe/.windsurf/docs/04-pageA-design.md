# PageA 页面设计文档

## 1. 页面概述

PageA 是会计功能管理系统中的一个核心页面，主要用于文件上传和合同管理。页面采用简洁的两区域布局设计，提供直观的用户交互体验。

### 1.1 功能描述
- **文件上传功能**：支持拖拽上传文件
- **合同管理功能**：展示合同列表，提供操作功能

### 1.2 页面路由
- 路径：`/page-a`
- 组件名：`PageA`

## 2. 页面布局设计

### 2.1 整体布局结构
```
┌─────────────────────────────────────────────────────────────┐
│                        Header (15vh)                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    区域A                            │    │
│  │                 (文件上传区域)                       │    │
│  │                  30vh高度                           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│                     40px 间距                               │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    区域B                            │    │
│  │                 (合同列表区域)                       │    │
│  │                  自适应高度                          │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 区域A - 文件上传区域

#### 样式规范
- **宽度**：80% (相对于父容器)
- **高度**：30vh
- **对齐方式**：左右居中
- **边框**：1px solid #000000
- **内边距**：10px
- **背景色**：#ffffff

#### CSS 样式
```css
.upload-area {
  width: 80%;
  height: 30vh;
  margin: 0 auto;
  border: 1px solid #000000;
  padding: 10px;
  background-color: #ffffff;
}
```

#### 组件规范
- 使用 Ant Design 的 `Upload.Dragger` 组件
- 支持拖拽上传功能
- 显示上传提示文本和图标

### 2.3 区域B - 合同列表区域

#### 样式规范
- **宽度**：80% (相对于父容器)
- **高度**：自适应内容
- **对齐方式**：左右居中
- **上边距**：40px
- **背景色**：#ffffff

#### CSS 样式
```css
.contract-table-area {
  width: 80%;
  margin: 40px auto 0;
  background-color: #ffffff;
}
```

#### 表格规范
- 使用 Ant Design 的 `Table` 组件
- **列配置**：
  - 合同名称 (Contract Name)
  - 操作 (Actions)

## 3. 组件设计

### 3.1 PageA 主组件结构

```typescript
interface PageAProps {}

interface ContractData {
  id: string;
  contractName: string;
}

const PageA: React.FC<PageAProps> = () => {
  // 组件逻辑
};
```

### 3.2 Upload 组件配置

```typescript
const uploadProps: UploadProps = {
  name: 'file',
  multiple: true,
  action: '/api/upload', // 上传接口
  onChange: handleUploadChange,
  onDrop: handleDrop,
};
```

### 3.3 Table 组件配置

```typescript
const columns: ColumnsType<ContractData> = [
  {
    title: '合同名称',
    dataIndex: 'contractName',
    key: 'contractName',
    width: '70%',
  },
  {
    title: '操作',
    key: 'actions',
    width: '30%',
    render: (_, record) => (
      <Space>
        <Button type="primary" size="small">
          查看
        </Button>
        <Button size="small">
          编辑
        </Button>
        <Button danger size="small">
          删除
        </Button>
      </Space>
    ),
  },
];
```

## 4. 实现步骤

### 步骤 1：创建基础组件文件
1. 在 `src/components/` 目录下创建 `PageA/` 文件夹
2. 创建以下文件：
   - `index.tsx` - 主组件文件
   - `styles.module.css` - 样式文件
   - `types.ts` - 类型定义文件

### 步骤 2：实现页面布局结构
1. 创建主容器组件
2. 实现区域A的布局容器
3. 实现区域B的布局容器
4. 添加响应式设计支持

### 步骤 3：集成文件上传功能
1. 导入 Ant Design Upload 组件
2. 配置上传参数和事件处理
3. 实现文件上传状态管理
4. 添加上传进度显示

### 步骤 4：实现合同列表功能
1. 导入 Ant Design Table 组件
2. 定义表格列配置
3. 实现数据获取逻辑
4. 添加操作按钮功能

### 步骤 5：添加样式和交互
1. 应用CSS样式
2. 添加hover效果
3. 实现响应式布局
4. 添加加载状态

### 步骤 6：状态管理集成
1. 使用 Zustand 管理页面状态
2. 实现文件上传状态
3. 实现合同列表状态
4. 添加错误处理

### 步骤 7：路由集成
1. 在路由配置中添加 PageA
2. 配置页面导航
3. 添加面包屑导航

## 5. 技术要求

### 5.1 依赖包
- `react` - React 框架
- `antd` - UI 组件库
- `zustand` - 状态管理
- `react-router-dom` - 路由管理

### 5.2 TypeScript 类型
```typescript
// 文件上传相关类型
interface UploadFile {
  uid: string;
  name: string;
  status: 'uploading' | 'done' | 'error';
  url?: string;
}

// 合同数据类型
interface ContractData {
  id: string;
  contractName: string;
  createTime: string;
  status: 'active' | 'inactive';
}

// 页面状态类型
interface PageAState {
  uploadedFiles: UploadFile[];
  contractList: ContractData[];
  loading: boolean;
  error: string | null;
}
```

## 6. 性能优化

### 6.1 组件优化
- 使用 `React.memo` 优化表格渲染
- 实现虚拟滚动（如果数据量大）
- 使用 `useMemo` 缓存计算结果

### 6.2 文件上传优化
- 实现分片上传
- 添加上传进度显示
- 支持断点续传

## 7. 测试要求

### 7.1 单元测试
- 组件渲染测试
- 事件处理测试
- 状态管理测试

### 7.2 集成测试
- 文件上传流程测试
- 表格操作测试
- 路由导航测试

## 8. 验收标准

### 8.1 功能验收
- ✅ 文件上传功能正常
- ✅ 合同列表显示正确
- ✅ 操作按钮功能完整
- ✅ 响应式布局适配

### 8.2 UI验收
- ✅ 布局尺寸符合设计规范
- ✅ 样式效果与设计一致
- ✅ 交互体验流畅
- ✅ 错误状态处理完善

## 9. 后续扩展

### 9.1 功能扩展
- 支持多种文件格式上传
- 添加文件预览功能
- 实现批量操作
- 添加搜索和筛选功能

### 9.2 UI扩展
- 添加动画效果
- 支持主题切换
- 优化移动端体验
- 添加快捷键支持
