# contractConfirmModal 弹窗组件设计文档

## 1. 组件概述

### 1.1 功能描述
contractConfirmModal 是一个用于合同确认的弹窗组件，主要用于展示合同信息和编辑预付时间表。该组件提供直观的界面让用户查看合同详情并管理相关的预付款项时间安排。

### 1.2 业务场景
- 合同管理流程中的确认环节
- 预付款项时间表的编辑和管理
- 合同信息的快速查看和确认

## 2. 组件结构设计

### 2.1 整体布局
```
┌─────────────────────────────────────┐
│            Modal Header             │
├─────────────────────────────────────┤
│                                     │
│        合同信息部分 (80%宽度)         │
│                                     │
├─────────────────────────────────────┤
│                                     │
│       预付时间表部分 (80%宽度)        │
│                                     │
└─────────────────────────────────────┘
│          确认    取消               │
└─────────────────────────────────────┘
```

### 2.2 组件层级结构
```
contractConfirmModal/
├── index.tsx                 # 主组件文件
├── styles.module.css         # 样式文件
├── types.ts                  # TypeScript 类型定义
└── components/               # 子组件目录
    └── EditableTable.tsx     # 可编辑表格组件
```

## 3. 详细设计规范

### 3.1 合同信息部分
- **布局**: 左右居中，宽度为弹窗的80%
- **内容**: 仅展示合同名称
- **样式规范**:
  - 边框: `1px solid black`
  - 边框圆角: `6px`
  - 内边距: `16px`
  - 背景色: `#fafafa`

### 3.2 预付时间表部分
- **组件**: 使用 Antd EditableProTable 组件
- **布局**: 左右居中，宽度为弹窗的80%
- **上边距**: `20px`
- **列配置**:
  1. **预提时间** (prepaymentDate)
     - 类型: DatePicker
     - 可编辑: 是
     - 必填: 是
  2. **入账时间** (accountingDate)
     - 类型: DatePicker
     - 可编辑: 是
     - 必填: 是
  3. **金额** (amount)
     - 类型: InputNumber
     - 可编辑: 是
     - 必填: 是
     - 格式: 货币格式，保留2位小数
- **功能特性**:
  - 所有单元格可编辑
  - 支持新增行功能
  - 支持删除行功能
  - 数据验证
- **样式规范**:
  - 边框: `1px solid black`
  - 边框圆角: `6px`

### 3.3 弹窗配置
- **标题**: "合同确认"
- **宽度**: `800px`
- **可拖拽**: 是
- **遮罩可关闭**: 否
- **Footer按钮**:
  - 确认按钮: 主要按钮样式
  - 取消按钮: 默认按钮样式

## 4. 技术实现规范

### 4.1 依赖包要求
```json
{
  "dependencies": {
    "@ant-design/pro-table": "^3.x.x",
    "antd": "^5.x.x",
    "react": "^18.x.x",
    "dayjs": "^1.x.x"
  }
}
```

### 4.2 TypeScript 类型定义
```typescript
// 预付时间表数据项
interface PrepaymentItem {
  id: string;
  prepaymentDate: string;
  accountingDate: string;
  amount: number;
}

// 合同信息
interface ContractInfo {
  id: string;
  name: string;
}

// 组件 Props
interface ContractConfirmModalProps {
  visible: boolean;
  contractInfo: ContractInfo;
  prepaymentData: PrepaymentItem[];
  onConfirm: (data: PrepaymentItem[]) => void;
  onCancel: () => void;
}
```

### 4.3 样式规范
```css
/* 主容器样式 */
.modalContainer {
  padding: 24px;
}

/* 合同信息部分样式 */
.contractInfo {
  width: 80%;
  margin: 0 auto;
  border: 1px solid black;
  border-radius: 6px;
  padding: 16px;
  background-color: #fafafa;
  text-align: center;
}

/* 预付时间表部分样式 */
.prepaymentTable {
  width: 80%;
  margin: 20px auto 0;
  border: 1px solid black;
  border-radius: 6px;
  overflow: hidden;
}

/* 合同名称样式 */
.contractName {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}
```

## 5. 开发实现步骤

### 5.1 第一步：创建组件基础结构
- 创建组件目录和文件
- 设置基本的 TypeScript 类型定义
- 创建基础的 Modal 组件框架

### 5.2 第二步：实现合同信息部分
- 创建合同信息展示区域
- 应用样式规范（80%宽度、边框、圆角等）
- 实现合同名称的展示逻辑

### 5.3 第三步：集成 EditableProTable 组件
- 安装和配置 @ant-design/pro-table 依赖
- 创建可编辑表格的列配置
- 实现基础的表格展示功能

### 5.4 第四步：实现编辑功能
- 配置各列的编辑器（DatePicker、InputNumber）
- 实现单元格编辑逻辑
- 添加数据验证规则

### 5.5 第五步：实现新增和删除功能
- 添加"新增行"按钮和逻辑
- 实现行删除功能
- 处理数据状态管理

### 5.6 第六步：完善样式和交互
- 应用所有样式规范
- 优化用户交互体验
- 添加加载状态和错误处理

### 5.7 第七步：集成和测试
- 将组件集成到主应用中
- 编写单元测试
- 进行用户体验测试和优化

## 6. 使用示例

### 6.1 基本用法
```tsx
import { ContractConfirmModal } from '@/components/ContractConfirmModal';

function App() {
  const [modalVisible, setModalVisible] = useState(false);
  const [contractInfo] = useState({
    id: '1',
    name: '软件开发服务合同'
  });
  const [prepaymentData, setPrepaymentData] = useState([]);

  const handleConfirm = (data: PrepaymentItem[]) => {
    console.log('确认数据:', data);
    setModalVisible(false);
  };

  const handleCancel = () => {
    setModalVisible(false);
  };

  return (
    <ContractConfirmModal
      visible={modalVisible}
      contractInfo={contractInfo}
      prepaymentData={prepaymentData}
      onConfirm={handleConfirm}
      onCancel={handleCancel}
    />
  );
}
```

## 7. 性能优化建议

### 7.1 组件优化
- 使用 React.memo 包装组件避免不必要的重渲染
- 使用 useMemo 和 useCallback 优化计算和回调函数
- 合理使用 EditableProTable 的虚拟滚动功能

### 7.2 数据处理优化
- 实现防抖处理用户输入
- 优化大量数据的渲染性能
- 使用适当的数据结构管理表格状态

## 8. 测试和验收标准

### 8.1 功能测试
- [ ] 弹窗正常显示和关闭
- [ ] 合同信息正确展示
- [ ] 表格数据正常加载和显示
- [ ] 单元格编辑功能正常
- [ ] 新增行功能正常
- [ ] 删除行功能正常
- [ ] 数据验证正常工作
- [ ] 确认和取消按钮功能正常

### 8.2 样式测试
- [ ] 整体布局符合设计规范
- [ ] 80%宽度布局正确
- [ ] 边框和圆角样式正确
- [ ] 间距和边距符合要求
- [ ] 响应式布局正常

### 8.3 交互测试
- [ ] 用户操作流畅自然
- [ ] 错误提示友好明确
- [ ] 加载状态显示正常
- [ ] 键盘操作支持良好

## 9. 后续扩展计划

### 9.1 功能扩展
- 支持批量导入预付时间表数据
- 添加数据导出功能
- 实现模板保存和应用功能
- 添加历史记录查看功能

### 9.2 用户体验优化
- 添加快捷键支持
- 实现拖拽排序功能
- 优化移动端适配
- 添加操作指引和帮助文档

---

**文档版本**: v1.0  
**创建日期**: 2025-10-08  
**最后更新**: 2025-10-08  
**负责人**: 前端开发团队
