# 会计功能管理系统

一个基于 React + TypeScript + Ant Design 的会计功能管理系统，专门用于处理合同相关的会计业务。

## 功能特性

- 🏢 **合同管理** - 合同上传、列表展示、详情查看
- 💰 **付款管理** - 付款时间表生成、付款执行、状态跟踪
- 📊 **会计分录** - 预付款分录生成、付款分录管理、审核流程
- 🎨 **现代UI** - 基于 Ant Design 的企业级界面设计
- 📱 **响应式** - 支持桌面端和移动端访问

## 技术栈

- **前端框架**: React 18
- **类型系统**: TypeScript
- **构建工具**: Vite
- **UI组件库**: Ant Design 5.x
- **路由管理**: React Router v6
- **状态管理**: Zustand
- **代码规范**: ESLint + Prettier

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 8.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

应用将在 http://localhost:3000 启动

### 构建生产版本

```bash
npm run build
```

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 项目结构

```
src/
├── components/          # 通用组件
│   ├── Layout/         # 布局组件
│   │   ├── AppLayout/  # 应用主布局
│   │   └── AppHeader/  # 头部组件
│   ├── Business/       # 业务组件
│   └── Common/         # 通用组件
├── pages/              # 页面组件
│   ├── PageA/          # 页面A
│   └── PageB/          # 页面B
├── stores/             # Zustand状态管理
├── types/              # TypeScript类型定义
├── utils/              # 工具函数
├── hooks/              # 自定义Hooks
├── constants/          # 常量定义
│   └── theme.ts        # 主题配置
└── styles/             # 样式文件
```

## 开发规范

### 命名规范

- **组件**: PascalCase (如: `ContractList`)
- **文件**: kebab-case (如: `contract-list.tsx`)
- **变量/函数**: camelCase (如: `contractData`)
- **常量**: UPPER_SNAKE_CASE (如: `API_BASE_URL`)

### 组件开发

- 优先使用 Ant Design 组件
- 遵循单一职责原则
- 使用 TypeScript 进行类型约束
- 编写清晰的组件文档

## 设计系统

### 主题配置

- **主色调**: #b5120f (OCBC 品牌红)
- **背景色**: #F5F5DC (米色)
- **布局**: Header(15vh) + Body(75vh)

### 响应式断点

- **桌面端**: >= 1200px
- **平板端**: 768px - 1199px  
- **移动端**: < 768px

## 开发计划

项目采用 5 个阶段的渐进式开发：

1. **第一阶段**: 项目基础搭建 ✅
2. **第二阶段**: 合同管理模块
3. **第三阶段**: 付款管理模块
4. **第四阶段**: 会计分录模块
5. **第五阶段**: 系统集成与优化

详细开发计划请参考 [开发计划文档](./.windsurf/docs/development-plan.md)

## 文档

- [产品概述](./.windsurf/docs/01-product-overview.md)
- [技术架构](./.windsurf/docs/02-technical-architecture.md)
- [页面结构设计](./.windsurf/docs/03-page-structure.md)
- [开发计划](./.windsurf/docs/development-plan.md)

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

如有问题或建议，请通过以下方式联系：

- 项目 Issues: [GitHub Issues](https://github.com/your-repo/issues)
- 邮箱: your-email@example.com
