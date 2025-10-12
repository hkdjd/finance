# 接口文档

本目录包含所有API接口的详细文档。

## 文档格式说明
- 所有接口文档使用Markdown格式（`.md`）
- 每个接口目录包含`README.md`主文档
- 统一的文档结构：接口信息、请求参数、响应格式、使用场景、错误处理

## 目录结构
每个接口都有独立的子目录，包含请求/响应示例。

## 文档结构

每个接口目录包含以下文件：
- `README.md` - 接口详细说明文档
- `request.json` - 请求示例（GET请求为参数说明）
- `response.json` - 响应示例

## 接口列表（按API顺序排列）

### 基础接口
- `01-health/` - 健康检查接口

### 合同管理接口
- `02-contracts-list/` - 查询合同列表
- `03-contracts-upload/` - 上传合同文件（步骤1）
- `04-contracts-get/` - 查询合同信息
- `05-contracts-operate/` - 合同操作（增删改）
- `09-contracts-create/` - 创建合同并初始化摊销台账
- `10-contracts-get-amortization/` - 查询合同摊销台账

### 摊销计算接口（步骤2）
- `06-amortization-calculate/` - 计算摊销明细
- `07-amortization-entries-list/` - 查询合同摊销明细列表
- `08-amortization-entries-operate/` - 摊销明细操作（增删改）

### 会计分录接口（步骤3）
- `11-journal-entries-generate/` - 生成会计分录（步骤3摊销）
- `12-journal-entries-preview/` - 预览会计分录

### 付款接口（步骤4）
- `13-payments-execute/` - 执行付款（步骤4）
- `14-payments-get-by-contract/` - 查询合同付款记录
- `15-payments-get-detail/` - 查询付款详情
- `16-payments-operate/` - 付款操作（增删改、取消）

## 业务流程

1. **步骤1**: 使用 `03-contracts-upload` 上传合同文件，然后使用 `06-amortization-calculate` 计算摊销明细
2. **步骤2**: 使用 `09-contracts-create` 创建合同并保存摊销台账，可通过 `08-amortization-entries-operate` 调整摊销明细
3. **步骤3**: 使用 `11-journal-entries-generate` 生成会计分录，可通过 `12-journal-entries-preview` 预览
4. **步骤4**: 使用 `13-payments-execute` 执行付款，根据付款金额和时间生成会计分录

## 核心业务逻辑

### 付款执行逻辑（步骤4）
根据需求文档，支持以下付款情形：

1. **情形1**: 付款不涉及预提待摊
   - 直接借记费用，贷记活期存款

2. **情形2**: 付款涉及预提待摊
   - **平账**: 付款金额 = 选择期间摊销金额总和
   - **多付**: 付款金额 > 选择期间摊销金额总和
     - 小额差异：记入费用科目
     - 大额差异：记入预付科目
   - **少付**: 付款金额 < 选择期间摊销金额总和
     - 小额差异：贷记费用科目
     - 大额差异：贷记预付科目

### 数据库设计

系统包含以下主要实体：
- `Contract` - 合同信息
- `AmortizationEntry` - 摊销明细
- `Payment` - 付款记录
- `JournalEntry` - 会计分录

所有实体都继承 `BaseAuditable`，包含创建时间、修改时间、创建人、修改人等审计字段。

## 使用说明

1. 所有接口都基于REST风格设计
2. 服务运行在端口8081
3. 请求和响应都使用JSON格式
4. 所有金额字段保留两位小数
5. 日期格式统一使用ISO标准格式

## 错误处理

所有接口遵循标准HTTP状态码：
- 200: 操作成功
- 400: 请求参数错误
- 404: 资源不存在
- 500: 服务器内部错误
