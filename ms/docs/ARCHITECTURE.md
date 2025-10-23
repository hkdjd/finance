# Finance财务应用系统架构文档

## 系统概述

Finance是一个完整的财务应用系统，主要用于合同管理、摊销计算、付款记录和审计日志追踪。系统采用微服务架构，分为三个核心模块：

## 架构图

![Finance系统架构图](../../finance-architecture-corrected.png)

## 核心模块

### 1. MFE前端模块 (Port: 3000)
- **技术栈**: React + TypeScript + Ant Design
- **功能**:
  - 合同管理界面
  - 摊销明细管理
  - Audit Log查看
  - 用户登录和权限管理
- **特点**: 响应式设计，现代化UI组件

### 2. MS后端服务 (Port: 8081)
- **技术栈**: Spring Boot + JPA + PostgreSQL
- **功能**:
  - RESTful API服务
  - 业务逻辑处理
  - Audit Log记录
  - 数据持久化
- **特点**: 微服务架构，支持水平扩展

### 3. AI合同解析 (Port: 8082)
- **技术栈**: Spring Boot + DeepSeek AI
- **功能**:
  - 合同文档解析
  - 智能信息提取
  - PDF文本处理和分析
  - 多AI服务支持(DeepSeek/Gemini)
- **特点**: 高性能异步处理，支持多种AI模型

## 数据存储

### PostgreSQL数据库
- **合同数据**: 存储合同基本信息
- **摊销明细**: 记录摊销计算结果
- **审计日志**: audit_log表记录所有操作历史
- **会计分录**: 财务会计数据

### 文件存储系统
- **合同文档存储**: 上传的合同PDF文件
- **上传文件管理**: 文件版本控制
- **静态资源服务**: 图片、样式等资源

### 外部AI服务
- **DeepSeek AI API**: 主要的文本分析和信息提取服务
- **Google Gemini API**: 备用AI服务，提供冗余保障
- **Gemma3模型**: 支持多种AI模型选择
- **文本分析**: 智能合同内容解析和关键信息提取

## 核心功能模块

### 1. 合同管理
- 合同CRUD操作
- 文件上传和管理
- 合同状态跟踪

### 2. 摊销管理
- 自动摊销计算
- 付款记录管理
- 期间管理

### 3. 审计日志 (最新功能)
- 操作记录追踪
- 历史数据查看
- 操作人信息记录

### 4. 报表分析
- 数据仪表盘
- 统计报表生成
- 可视化图表

## 数据流向

1. **用户操作**: MFE前端 → MS后端服务
2. **合同解析**: MS后端 → AI服务 → 外部AI API → 返回解析结果
3. **数据持久化**: MS后端 ↔ PostgreSQL数据库
4. **文件操作**: MS后端 ↔ 文件存储系统
5. **AI处理**: AI服务 → DeepSeek/Gemini API → 智能分析
6. **审计记录**: 所有操作 → audit_log表

## API接口

根据精简API设计原则，所有PUT、DELETE操作都合并到POST接口中，通过`operate`字段区分操作类型：

### 主要接口
- `GET /health` - 健康检查
- `GET /contracts` - 查询合同列表
- `POST /contracts/operate` - 合同操作（增删改）
- `GET /amortization-entries/contract/{contractId}` - 查询摊销明细
- `POST /amortization-entries/operate` - 摊销明细操作
- `GET /audit-logs/amortization-entry/{id}` - 查询审计日志

## 最新功能更新

### Audit Log审计日志功能
- ✅ **数据库表**: 新增audit_log表记录操作历史
- ✅ **后端实现**: AuditLogService自动记录付款操作
- ✅ **前端界面**: 合同详情页显示audit log链接
- ✅ **操作人记录**: 正确记录登录用户信息(如A5136589)
- ✅ **历史追踪**: 完整的操作时间、金额、状态变更记录

### 技术特点
- **自动化**: 付款操作时自动记录audit log
- **完整性**: 记录操作前后的状态变化
- **可追溯**: 支持按摊销明细ID查询操作历史
- **用户友好**: 只对已付款状态显示audit log链接

## 部署信息

### 开发环境
- **前端**: `npm start` → localhost:3000
- **后端**: `mvn spring-boot:run` → localhost:8081  
- **AI服务**: `python main.py` → localhost:8082
- **数据库**: PostgreSQL本地实例

### 数据库迁移
使用Flyway进行版本控制：
- V1_1 到 V1_6: 基础表结构
- V1_7: audit_log表 (最新)

## 系统优势

1. **模块化设计**: 三个独立模块，职责清晰
2. **技术多样性**: 前端React、后端Spring Boot、AI服务Spring Boot
3. **数据完整性**: 完善的审计日志机制
4. **用户体验**: 现代化UI设计，操作简便
5. **可扩展性**: 微服务架构支持独立部署和扩展

---

*最后更新: 2025-10-23*  
*版本: v1.7 (包含Audit Log功能)*
