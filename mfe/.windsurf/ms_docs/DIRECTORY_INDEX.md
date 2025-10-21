# 文档目录索引

本文档按照API接口的顺序重新整理了docs目录结构，每个目录前面都加上了序号，便于查找和维护。

## 目录结构总览

### 健康检查接口
- `01-health健康检查/` - 健康检查接口
  - **URL**: `GET /health`
  - **功能**: 检查服务健康状态

### 合同管理接口（步骤1）
- `02-contracts-list合同列表/` - 查询合同列表
  - **URL**: `GET /contracts`
  - **功能**: 查询合同列表，支持分页

- `03-contracts-upload合同上传/` - 上传合同文件（步骤1）
  - **URL**: `POST /contracts/upload`
  - **功能**: 上传合同文件

- `04-contracts-get合同查询/` - 查询合同信息
  - **URL**: `GET /contracts/{contractId}`
  - **功能**: 查询指定合同的详细信息

- `05-contracts-operate合同操作/` - 合同操作（增删改）
  - **URL**: `POST /contracts/operate`
  - **功能**: 合同操作接口，支持增删改操作

- `09-contracts-create合同创建/` - 创建合同并初始化摊销台账
  - **URL**: `POST /contracts`
  - **功能**: 创建合同记录并生成摊销台账

- `10-contracts-get-amortization合同摊销台账/` - 查询合同摊销台账
  - **URL**: `GET /contracts/{id}/amortization`
  - **功能**: 查询合同的摊销台账信息

### 摊销计算接口（步骤2）
- `06-amortization-calculate摊销计算/` - 计算摊销明细
  - **URL**: `GET /amortization/calculate/{contractId}`
  - **功能**: 根据合同信息计算摊销明细

- `07-amortization-entries-list摊销明细列表/` - 查询合同摊销明细列表
  - **URL**: `GET /amortization-entries/contract/{contractId}`
  - **功能**: 查询指定合同的摊销明细列表

- `08-amortization-entries-operate摊销明细操作/` - 摊销明细操作（支持增删改操作）
  - **URL**: `POST /amortization-entries/operate`
  - **功能**: 摊销明细操作接口，支持增删改操作

### 会计分录接口（步骤3）
- `11-journal-entries-generate会计分录生成/` - 生成会计分录（步骤3摊销）
  - **URL**: `POST /journal-entries/generate/{contractId}`
  - **功能**: 根据摊销明细生成会计分录（entryType=AMORTIZATION）

- `12-journal-entries-preview会计分录预览/` - 预览会计分录
  - **URL**: `POST /journal-entries/preview`
  - **功能**: 预览会计分录，不保存到数据库

### 付款处理接口（步骤4）
- `13-payments-execute付款执行/` - 执行付款（步骤4）
  - **URL**: `POST /payments/execute`
  - **功能**: 执行付款操作，生成付款记录和会计分录（entryType=PAYMENT）

- `14-payments-get-by-contract付款记录查询/` - 查询合同付款记录
  - **URL**: `GET /payments/contracts/{contractId}`
  - **功能**: 查询指定合同的付款记录列表

- `15-payments-get-detail付款详情查询/` - 查询付款详情
  - **URL**: `GET /payments/{paymentId}`
  - **功能**: 查询指定付款的详细信息

- `16-payments-operate付款操作/` - 付款操作（增删改、取消）
  - **URL**: `POST /payments/operate`
  - **功能**: 付款操作接口，支持增删改和取消操作接口

## 业务流程对应

### 步骤1：合同上传和解析
- `03-contracts-upload合同上传/` - 上传合同文件
- `06-amortization-calculate摊销计算/` - 计算摊销明细

### 步骤2：摊销台账管理
- `09-contracts-create合同创建/` - 创建合同并初始化摊销台账
- `07-amortization-entries-list摊销明细列表/` - 查看摊销明细
- `08-amortization-entries-operate摊销明细操作/` - 调整摊销明细

### 步骤3：会计分录生成
- `11-journal-entries-generate会计分录生成/` - 生成摊销会计分录
- `12-journal-entries-preview会计分录预览/` - 预览会计分录

### 步骤4：付款执行
- `13-payments-execute付款执行/` - 执行付款
- `14-payments-get-by-contract付款记录查询/` - 查看付款记录
- `15-payments-get-detail付款详情查询/` - 查看付款详情

## 📚 其他文档文件

### 主要文档
- `README.md` - 文档总览和使用说明
- `api.md` - 完整API接口列表
- `simplified-api.md` - 精简API设计文档
- `CHANGELOG.md` - 更新日志

### UML图表
- `UML_README.md` - UML图表说明
- `data_flow_sequence.puml` - 数据流程序列图
- `payment_sequence_diagram.puml` - 付款流程序列图
- `sequence_diagram.puml` - 系统序列图
- `system_architecture_sequence.puml` - 系统架构序列图

## 🎯 重要说明

### 会计分录统一设计
- 步骤3和步骤4的会计分录存储在同一张表 `journal_entries` 中
- 通过 `entryType` 字段区分：
  - `AMORTIZATION` - 步骤3生成的摊销分录
  - `PAYMENT` - 步骤4生成的付款分录
- 重新生成步骤3分录时，只删除 `AMORTIZATION` 类型，不影响 `PAYMENT` 类型

### 统一操作接口设计
- 所有增删改操作都使用 `POST` 方法
- 通过 `operate` 字段区分操作类型：`CREATE`、`UPDATE`、`DELETE`
- 支持批量操作接口

## 📖 使用建议

1. **查找接口文档**：根据序号快速定位对应的接口文档
2. **业务流程**：按照步骤1-4的顺序查看相关接口
3. **统一操作**：优先使用 `operate` 接口进行增删改操作
4. **会计分录**：注意区分摊销分录和付款分录的不同类型
