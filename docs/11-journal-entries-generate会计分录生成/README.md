# 会计分录接口文档

## 步骤3：会计分录生成和管理

根据需求文档，步骤3负责根据步骤2的预提摊销表生成相应的会计分录列表，并支持前端的增删改操作。

## 重要说明：统一的会计分录设计

**会计分录是统一的实体和数据表**，不区分"合同会计分录"和"付款会计分录"：

- **步骤3（摊销）**: 通过 `/journal-entries/generate/{contractId}` 生成摊销相关的会计分录
- **步骤4（付款）**: 通过 `/payments/execute` 执行付款时，同样生成会计分录到同一个表
- **查询和操作**: 所有会计分录都通过 `/journal-entries/*` 接口进行统一管理

### 会计分录类型区分
通过JournalEntry实体中的字段来区分不同业务场景：
- `entryType`: 分录类型（AMORTIZATION摊销、PAYMENT付款）
- `description`: 业务描述
- `contract`: 关联的合同
- `payment`: 关联的付款（仅付款分录有值）
- `bookingDate`: 记账日期

## 接口列表

### 1. 生成会计分录（步骤3摊销）
- **URL**: `/journal-entries/generate/{contractId}`
- **Method**: POST
- **Description**: 根据合同ID和请求参数生成指定类型的会计分录
- **返回格式**: JournalEntryListResponse（合同信息 + 会计分录列表）

#### 请求参数
```json
{
  "entryType": "AMORTIZATION",
  "description": "生成摊销会计分录"
}
```

#### 参数说明
- **entryType** (String, 必填): 会计分录类型
  - `AMORTIZATION`: 生成摊销会计分录（步骤3）
  - `PAYMENT`: 生成付款会计分录（通常在步骤4通过付款接口调用）
- **description** (String, 可选): 分录描述，用于自定义分录说明

### 2. 查询合同会计分录列表
- **URL**: `/journal-entries/contract/{contractId}`
- **Method**: GET
- **Description**: 查询指定合同的所有会计分录（包括摊销和付款）
- **返回格式**: List<JournalEntry>

### 3. 查询单个会计分录
- **URL**: `/journal-entries/{entryId}`
- **Method**: GET
- **Description**: 查询指定ID的会计分录

### 4. 统一操作接口（增删改）
- **URL**: `/journal-entries/operate`
- **Method**: POST
- **Description**: 通过operate字段区分操作类型：CREATE、UPDATE、DELETE

### 5. 批量操作接口
- **URL**: `/journal-entries/batch-operate`
- **Method**: POST
- **Description**: 支持批量增删改操作

### 6. 预览会计分录
- **URL**: `/journal-entries/preview`
- **Method**: POST
- **Description**: 预览会计分录（不保存到数据库）

## 数据结构

### 响应格式（JournalEntryListResponse）
```json
{
  "contract": {
    "id": 1,
    "totalAmount": 3000.00,
    "startDate": "2024-01-01",
    "endDate": "2024-03-31",
    "vendorName": "供应商A"
  },
  "journalEntries": [
    {
      "id": 1,
      "bookingDate": "2024-01-31",
      "accountName": "费用",
      "debitAmount": 1000.00,
      "creditAmount": 0.00,
      "description": "合同摊销费用",
      "memo": "摊销费用 - 2024-01",
      "entryOrder": 1,
      "entryType": "AMORTIZATION",
      "createdAt": "2024-01-24T14:30:52.123456",
      "updatedAt": "2024-01-24T14:30:52.123456",
      "createdBy": "system",
      "updatedBy": "system"
    }
  ]
}
```

### 字段说明

#### 合同信息（contract）
- **id**: 合同ID
- **totalAmount**: 合同总金额
- **startDate**: 合同开始日期
- **endDate**: 合同结束日期
- **vendorName**: 供应商名称

#### 会计分录信息（journalEntries）
- **id**: 会计分录ID
- **bookingDate**: 记账日期（入账期间的最后一天）
- **accountName**: 会计科目名称（如"费用"、"应付"等）
- **debitAmount**: 借方金额
- **creditAmount**: 贷方金额
- **description**: 分录描述
- **memo**: 备注信息
- **entryOrder**: 分录顺序，用于排序显示
- **entryType**: 分录类型（AMORTIZATION摊销、PAYMENT付款）
- **createdAt/updatedAt**: 创建/更新时间
- **createdBy/updatedBy**: 创建/更新人

### 响应格式优势
- **避免重复**: 合同信息只在根节点显示一次，不在每个分录中重复
- **结构清晰**: 合同信息和分录列表分离，便于前端处理
- **数据精简**: 减少响应数据大小，提高传输效率

## 业务逻辑

### 会计分录生成规则
根据需求文档的场景1示例：
- **摊销期间**: 2024年1月-2024年3月
- **每月金额**: 1000.00元

生成的会计分录：
```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-01-27      费用       1000.00       -
2024-01-27      应付       -             1000.00
2024-02-27      费用       1000.00       -
2024-02-27      应付       -             1000.00
2024-03-27      费用       1000.00       -
2024-03-27      应付       -             1000.00
```

### 记账日期规则
- **预提摊销分录**：入账日期为入账期间的27号
- **付款分录**：入账日期为用户填写的付款时间（默认当天，可修改）
- 例如：
  - 预提摊销：入账期间为"2024-01"，记账日期为"2024-01-27"
  - 付款分录：用户填写付款时间为"2024-03-20"，记账日期为"2024-03-20"

## 付款阶段（步骤4）会计分录生成

### 付款参数
用户在付款页面需要填写：
- **参数1**: 付款金额
- **参数2**: 付款时间（默认为当天，可修改）
- **选择期间**: 在预提待摊台账中勾选对应的付款期间

### 付款情形分类

#### 情形1：无预提待摊的直接付款
**场景**: 付款不涉及预提待摊
**示例**: 2024年1月20日，付款1000元

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-01-20      费用       1000.00       -
2024-01-20      活期存款    -             1000.00
```

#### 情形2：涉及预提待摊的付款

##### 情形2.1：付款金额等于预提金额
**场景**: 付款金额与选中期间的预提金额完全匹配
**示例**: 付款2000元，选择1-2月（每月1000元）

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-03-20      应付       1000.00       -
2024-03-20      应付       1000.00       -
2024-03-20      活期存款    -             2000.00
```

##### 情形2.2：付款金额大于预提金额（超额支付）
**场景**: 付款金额超出预提金额，差额记入借方费用
**示例**: 付款2001元，选择1-2月（每月1000元），超出1元

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-03-20      应付       1000.00       -
2024-03-20      应付       1000.00       -
2024-03-20      费用       1.00          -
2024-03-20      活期存款    -             2001.00
```

##### 情形2.3：付款金额小于预提金额（不足支付）
**场景**: 付款金额少于预提金额，差额记入贷方费用
**示例**: 付款1999元，选择1-2月（每月1000元），不足1元

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-03-20      应付       1000.00       -
2024-03-20      应付       1000.00       -
2024-03-20      费用       -             1.00
2024-03-20      活期存款    -             1999.00
```

##### 情形2.4：跨期付款（不足支付）
**场景**: 付款涉及未来期间，总金额小于预提总额
**示例**: 付款5999元，选择1-6月（总计6000元），不足1元

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-03-20      应付       1000.00       -    # 1月
2024-03-20      应付       1000.00       -    # 2月
2024-03-20      预付       3999.00       -    # 3-6月预付
2024-03-20      活期存款    -             5999.00

# 后续逐月预付转应付
2024-03-27      应付       1000.00       -
2024-03-27      预付       -             1000.00
2024-04-27      应付       1000.00       -
2024-04-27      预付       -             1000.00
2024-05-27      应付       1000.00       -
2024-05-27      预付       -             1000.00
2024-06-27      应付       1000.00       -
2024-06-27      预付       -             999.00   # 最后一期调整
2024-06-27      费用       -             1.00     # 差额调整
```

##### 情形2.5：跨期付款（超额支付）
**场景**: 付款涉及未来期间，总金额大于预提总额
**示例**: 付款6001元，选择1-6月（总计6000元），超出1元

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-03-20      应付       1000.00       -    # 1月
2024-03-20      应付       1000.00       -    # 2月
2024-03-20      预付       4001.00       -    # 3-6月预付
2024-03-20      活期存款    -             6001.00

# 后续逐月预付转应付
2024-03-27      应付       1000.00       -
2024-03-27      预付       -             1000.00
2024-04-27      应付       1000.00       -
2024-04-27      预付       -             1000.00
2024-05-27      应付       1000.00       -
2024-05-27      预付       -             1000.00
2024-06-27      应付       1000.00       -
2024-06-27      预付       -             1000.00
2024-06-27      费用       1.00          -     # 超额调整
2024-06-27      预付       -             1.00
```

##### 情形2.6：跨期付款（金额匹配）
**场景**: 付款涉及未来期间，总金额等于预提总额
**示例**: 付款6000元，选择1-6月（总计6000元）

```
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-03-20      应付       1000.00       -    # 1月
2024-03-20      应付       1000.00       -    # 2月
2024-03-20      预付       4000.00       -    # 3-6月预付
2024-03-20      活期存款    -             6000.00

# 后续逐月预付转应付
2024-03-27      应付       1000.00       -
2024-03-27      预付       -             1000.00
2024-04-27      应付       1000.00       -
2024-04-27      预付       -             1000.00
2024-05-27      应付       1000.00       -
2024-05-27      预付       -             1000.00
2024-06-27      应付       1000.00       -
2024-06-27      预付       -             1000.00
```

### 付款分录生成规则总结

1. **当期和过去期间**: 直接记入应付科目
2. **未来期间**: 先记入预付科目，后续按月转为应付
3. **差额处理**: 
   - 超额：记入借方费用
   - 不足：记入贷方费用
   - 跨期差额：在最后一期进行调整
4. **记账日期**: 
   - 付款分录：使用用户填写的付款时间
   - 预付转应付：使用每月27号

## 使用场景

### 步骤3工作流程
1. 前端通过合同ID和分录类型请求生成会计分录
2. 后端验证请求参数（entryType必填）
3. 根据分录类型执行相应的生成逻辑：
   - `AMORTIZATION`: 读取步骤2的摊销明细数据生成摊销分录
   - `PAYMENT`: 通常在付款接口中调用，不直接支持
4. 如果提供了自定义描述，更新分录描述
5. 保存到数据库并返回给前端
6. 前端展示会计分录列表，支持增删改操作

### 前端操作支持
- **查询**: 获取合同的所有会计分录
- **新增**: 手动添加新的会计分录行
- **修改**: 编辑会计分录的金额、科目等信息
- **删除**: 删除不需要的会计分录行

## 错误处理

### 常见错误码
- **400 Bad Request**: 请求参数错误
  - `entryType`字段为空或无效
  - 不支持的分录类型
  - 付款分录不支持直接生成
- **404 Not Found**: 资源不存在
  - 合同不存在
  - 摊销明细未生成
- **403 Forbidden**: 权限不足
- **500 Internal Server Error**: 服务器内部错误

### 错误响应示例
```json
{
  "error": "INVALID_ENTRY_TYPE",
  "message": "分录类型不能为空",
  "timestamp": "2024-01-01T10:30:00.123456"
}
```

```json
{
  "error": "PAYMENT_NOT_SUPPORTED",
  "message": "付款分录应通过付款接口生成，不支持直接调用",
  "timestamp": "2024-01-01T10:30:00.123456"
}
```

## 前端使用示例

### JavaScript调用示例
```javascript
// 1. 生成摊销会计分录
const generateAmortizationEntries = async (contractId) => {
  const response = await fetch(`/journal-entries/generate/${contractId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      entryType: 'AMORTIZATION',
      description: '生成摊销会计分录'
    })
  });
  
  if (response.ok) {
    const data = await response.json();
    console.log('合同信息:', data.contract);
    console.log('会计分录:', data.journalEntries);
    return data;
  } else {
    const error = await response.json();
    throw new Error(error.message);
  }
};

// 2. 生成自定义描述的摊销分录
const generateCustomEntries = async (contractId, customDescription) => {
  const response = await fetch(`/journal-entries/generate/${contractId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      entryType: 'AMORTIZATION',
      description: customDescription
    })
  });
  
  return await response.json();
};

// 3. 错误处理示例
const handleGenerateEntries = async (contractId) => {
  try {
    const result = await generateAmortizationEntries(contractId);
    // 处理成功结果
    displayJournalEntries(result);
  } catch (error) {
    // 处理错误
    if (error.message.includes('付款分录')) {
      alert('付款分录请通过付款功能生成');
    } else if (error.message.includes('合同不存在')) {
      alert('合同不存在，请检查合同ID');
    } else {
      alert('生成会计分录失败：' + error.message);
    }
  }
};
```
