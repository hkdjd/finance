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
- **Description**: 根据合同ID和摊销明细生成摊销会计分录
- **返回格式**: JournalEntryListResponse（合同信息 + 会计分录列表）

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
- 记账日期为入账期间的最后一天
- 例如：入账期间为"2024-01"，记账日期为"2024-01-31"

## 使用场景

### 步骤3工作流程
1. 前端通过合同ID请求生成会计分录
2. 后端读取步骤2的摊销明细数据
3. 根据摊销明细自动生成会计分录
4. 保存到数据库并返回给前端
5. 前端展示会计分录列表，支持增删改操作

### 前端操作支持
- **查询**: 获取合同的所有会计分录
- **新增**: 手动添加新的会计分录行
- **修改**: 编辑会计分录的金额、科目等信息
- **删除**: 删除不需要的会计分录行

## 错误处理
- 合同不存在: 404 Not Found
- 摊销明细未生成: 400 Bad Request
- 数据验证失败: 400 Bad Request
- 权限不足: 403 Forbidden
