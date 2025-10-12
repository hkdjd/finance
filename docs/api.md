### 会计分录统一说明（重要）
- 系统对“步骤3（预提摊销）”与“步骤4（付款）”产生的会计分录统一存放在同一张表 `journal_entries` 中。
- 通过字段 `entryType` 进行区分：`AMORTIZATION` 表示步骤3产生的分录，`PAYMENT` 表示步骤4产生的分录。
- 重新生成步骤3分录时，仅删除并重建 `entryType=AMORTIZATION` 的分录，不影响 `entryType=PAYMENT` 的分录。
# API接口文档

## 基础信息
- 服务端口: 8081
- 基础URL: http://localhost:8081

## 接口列表

| API | Method | Description | URL |
|-----|--------|-------------|-----|
| localhost:8081/health | GET | 健康检查接口 | /health |
| localhost:8081/contracts | GET | 查询合同列表 | /contracts |
| localhost:8081/contracts/list | GET | 分页查询合同列表 | /contracts/list |
| localhost:8081/contracts/upload | POST | 上传合同文件（步骤1） | /contracts/upload |
| localhost:8081/contracts/{contractId} | GET | 查询合同信息 | /contracts/{contractId} |
| localhost:8081/contracts/operate | POST | 合同操作（增删改） | /contracts/operate |
| localhost:8081/amortization/calculate/{contractId} | GET | 根据合同ID计算摊销明细 | /amortization/calculate/{contractId} |
| localhost:8081/amortization-entries/contract/{contractId} | GET | 查询合同摊销明细列表 | /amortization-entries/contract/{contractId} |
| localhost:8081/amortization-entries/operate | POST | 摊销明细操作（支持增删改操作） | /amortization-entries/operate |
| localhost:8081/contracts | POST | 创建合同并初始化摊销台账 | /contracts |
| localhost:8081/contracts/{id}/amortization | GET | 查询合同摊销台账 | /contracts/{id}/amortization |
| localhost:8081/journal-entries/generate/{contractId} | POST | 生成会计分录（步骤3摊销） | /journal-entries/generate/{contractId} |
| localhost:8081/journal-entries/contract/{contractId} | GET | 查询合同会计分录列表 | /journal-entries/contract/{contractId} |
| localhost:8081/journal-entries/operate | POST | 会计分录操作（增删改） | /journal-entries/operate |
| localhost:8081/journal-entries/preview | POST | 预览会计分录 | /journal-entries/preview |
| localhost:8081/payments/execute | POST | 执行付款（步骤4） | /payments/execute |
| localhost:8081/payments/contracts/{contractId} | GET | 查询合同付款记录 | /payments/contracts/{contractId} |
| localhost:8081/payments/{paymentId} | GET | 查询付款详情 | /payments/{paymentId} |
| localhost:8081/payments/operate | POST | 付款操作（增删改、取消） | /payments/operate |

## 接口详细说明

### 1. 健康检查接口
- **URL**: `/health`
- **Method**: GET
- **Description**: 检查服务健康状态
- **Response**: 返回当前时间戳的OK状态

### 2. 摊销计算接口
- **URL**: `/amortization/calculate/{contractId}`
- **Method**: GET
- **Description**: 根据合同ID计算摊销明细
- **Path Parameter**: contractId (Long) - 合同ID
- **Response**: AmortizationResponse

### 3. 摊销明细管理接口

#### 3.1 更新摊销明细金额
- **URL**: `/amortization/entries/{entryId}`
- **Method**: PUT
- **Description**: 更新指定摊销明细的金额
- **Path Parameters**: 
  - entryId: 摊销明细ID
- **Query Parameters**:
  - amount: 新的金额
- **Response**: AmortizationResponse

#### 3.2 添加摊销明细行
- **URL**: `/amortization/contracts/{contractId}/entries`
- **Method**: POST
- **Description**: 为指定合同添加新的摊销明细行
- **Path Parameters**:
  - contractId: 合同ID
- **Query Parameters**:
  - period: 摊销期间
  - amount: 摊销金额
- **Request Body**: AmortizationEntryDto
  - `id`: 主键ID (Long, 可为null)
  - `amortizationPeriod`: 预提/摊销期间，格式 yyyy-MM (String)
  - `accountingPeriod`: 入账期间，格式 yyyy-MM (String)  
  - `amount`: 预提/摊销金额，保留两位小数 (BigDecimal)
  - `status`: 付款状态 (String)
    - `PENDING`: 待付款
    - `COMPLETED`: 已完成
- **Response**: AmortizationResponse

#### 3.3 删除摊销明细行
- **URL**: `/amortization/entries/{entryId}`
- **Method**: DELETE
- **Description**: 删除指定的摊销明细行
- **Path Parameters**:
  - entryId: 摊销明细ID
- **Response**: AmortizationResponse

### 4. 合同管理接口

#### 4.1 创建合同
- **URL**: `/contracts`
- **Method**: POST
- **Description**: 创建合同并初始化摊销台账
- **Request Body**: CalculateAmortizationRequest
- **Response**: AmortizationResponse

#### 4.2 查询合同摊销台账
- **URL**: `/contracts/{id}/amortization`
- **Method**: GET
- **Description**: 查询合同摊销台账，用于前端展示已保存的可编辑表格
- **Path Parameters**:
  - id: 合同ID
- **Response**: AmortizationResponse

### 5. 会计分录预览接口
- **URL**: `/journals/preview`
- **Method**: POST
- **Description**: 基于摊销结果预览会计分录
- **Request Body**: AmortizationResponse
- **Query Parameters**:
  - bookingDate: 记账日期 (可选，格式: YYYY-MM-DD)
- **Response**: List<JournalEntryDto>

### 6. 付款相关接口

#### 6.1 付款预览接口
- **URL**: `/payments/preview`
- **Method**: POST
- **Description**: 预览付款相关的会计分录
- **Request Body**: PaymentRequest
- **Response**: PaymentPreviewResponse

#### 6.2 执行付款接口（步骤4付款阶段）
- **URL**: `/payments/execute`
- **Method**: POST
- **Description**: 执行付款，根据用户填写的付款金额和付款时间生成会计分录并保存到数据库
- **Request Body**: PaymentExecutionRequest
  - contractId: 合同ID（必填）
  - paymentAmount: 付款金额（必填，必须大于0）
  - bookingDate: 付款过账日期（可选，默认当天）
  - selectedPeriods: 勾选的摊销明细ID列表（必填，格式：[1, 2, 3]）
- **Response**: PaymentExecutionResponse

#### 6.3 查询合同付款记录
- **URL**: `/payments/contracts/{contractId}`
- **Method**: GET
- **Description**: 查询指定合同的所有付款记录
- **Path Parameters**:
  - contractId: 合同ID
- **Response**: List<PaymentExecutionResponse>

#### 6.4 查询付款详情
- **URL**: `/payments/{paymentId}`
- **Method**: GET
- **Description**: 查询单个付款的详细信息，包括会计分录
- **Path Parameters**:
  - paymentId: 付款ID
- **Response**: PaymentExecutionResponse

#### 6.5 取消付款
- **URL**: `/payments/{paymentId}/cancel`
- **Method**: PUT
- **Description**: 取消指定的付款记录
- **Path Parameters**:
  - paymentId: 付款ID
- **Response**: PaymentExecutionResponse

## 数据传输对象 (DTO)

### CalculateAmortizationRequest
用于摊销计算和合同创建的请求对象

### AmortizationResponse
摊销相关操作的响应对象

### JournalEntryDto
会计分录数据传输对象

### PaymentRequest
付款请求对象

### PaymentPreviewResponse
付款预览响应对象

### PaymentExecutionRequest
付款执行请求对象
- contractId: 合同ID
- paymentAmount: 付款金额
- bookingDate: 付款过账日期
- selectedPeriods: 选择的摊销明细ID列表 (List<Long>)

### PaymentExecutionResponse
付款执行响应对象
- paymentId: 付款ID
- contractId: 合同ID
- paymentAmount: 付款金额
- bookingDate: 记账日期
- selectedPeriods: 选择的期间
- status: 付款状态（DRAFT/CONFIRMED/CANCELLED）
- journalEntries: 会计分录列表
- message: 操作消息

## 错误处理
所有接口遵循标准HTTP状态码：
- 200: 操作成功
- 400: 请求参数错误
- 404: 资源不存在
- 500: 服务器内部错误
