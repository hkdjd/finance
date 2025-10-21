# 更新日志

## 2025-09-30 - 摊销明细接口响应格式统一

### 📋 第十一次更新 - 统一摊销明细相关接口响应格式

根据用户需求，统一调整摊销明细相关接口的返回JSON格式，包括列表查询和操作接口，将合同信息和摊销明细分开，避免数据重复：

#### 🎯 优化目标
- **减少数据冗余**: 合同信息不在每个摊销明细中重复
- **提高传输效率**: 减少网络传输数据量
- **改善前端处理**: 便于前端分别处理合同信息和摊销明细

#### 🔧 代码修改

##### 1. 新增响应DTO
**AmortizationListResponse.java**:
- 包装类包含 `contract` 和 `amortization` 两个字段
- `ContractInfo` 内嵌类：合同基本信息
- `AmortizationEntryInfo` 内嵌类：摊销明细信息（不包含合同）
- 支持BigDecimal到Double的类型转换

##### 2. 服务层增强
**AmortizationEntryService.java**:
- 新增 `getAmortizationListByContract()` 方法（列表查询）
- 新增 `updateAmortizationEntryWithResponse()` 方法（单个操作）
- 新增 `batchOperateAmortizationEntriesWithResponse()` 方法（批量操作）
- 查询合同信息和摊销明细列表
- 转换为新的DTO格式返回
- 保留原有方法确保兼容性

##### 3. 控制器层更新
**AmortizationEntryController.java**:
- 修改 `/amortization-entries/contract/{contractId}` 列表接口
- 修改 `/amortization-entries/operate` 操作接口
- 修改 `/amortization-entries/batch-operate` 批量操作接口
- 返回类型统一改为 `AmortizationListResponse`
- 调用新的服务方法

#### 📚 文档更新

##### 响应格式变更
**原格式** (数组，合同信息重复):
```json
[
  {
    "id": 1,
    "contract": { "id": 1, "totalAmount": 4000.00, ... },
    "amortizationPeriod": "2025-01",
    ...
  },
  {
    "id": 2,
    "contract": { "id": 1, "totalAmount": 4000.00, ... },
    "amortizationPeriod": "2025-02",
    ...
  }
]
```

**新格式** (包装对象，合同信息独立):
```json
{
  "contract": {
    "id": 1,
    "totalAmount": 4000.00,
    "startDate": "2025-01-01",
    "endDate": "2025-04-30",
    "vendorName": "供应商A"
  },
  "amortization": [
    {
      "id": 1,
      "amortizationPeriod": "2025-01",
      "amount": 1000.00,
      ...
    },
    {
      "id": 2,
      "amortizationPeriod": "2025-02",
      "amount": 1000.00,
      ...
    }
  ]
}
```

##### 文档更新
- **`docs/07-amortization-entries-list/README.md`** - 更新列表接口响应格式说明
- **`docs/07-amortization-entries-list/response.json`** - 更新列表接口响应示例
- **`docs/08-amortization-entries-operate/README.md`** - 更新操作接口响应格式说明
- **`docs/08-amortization-entries-operate/response.json`** - 更新操作接口响应示例

#### 🧪 测试更新

##### 测试脚本适配
**test-simplified-api.sh**:
- 适配新的响应格式解析摊销明细ID
- 验证列表接口响应包含 `contract` 和 `amortization` 字段
- 验证操作接口响应包含 `contract` 和 `amortization` 字段
- 验证批量操作接口响应包含 `contract` 和 `amortization` 字段
- 更新付款测试中的ID提取逻辑

##### 测试验证点
- ✅ 响应格式包含独立的合同信息
- ✅ 摊销明细数组不包含重复的合同信息
- ✅ 数据完整性保持不变
- ✅ 兼容现有业务逻辑

#### 💡 设计优势

##### 数据传输优化
- **减少冗余**: 合同信息只传输一次
- **提高效率**: 减少网络传输数据量
- **节省带宽**: 特别是摊销明细较多时效果明显

##### 前端处理便利
- **分离关注点**: 合同信息和摊销明细可分别处理
- **缓存友好**: 合同信息可单独缓存
- **组件化**: 便于前端组件化开发

##### 向后兼容
- **保留原方法**: 原有服务方法仍然存在
- **渐进升级**: 可逐步迁移到新格式
- **API版本**: 为将来API版本管理做准备

#### 📋 影响范围
- **前端**: 需要适配新的响应格式解析
- **后端**: 新增DTO类和服务方法
- **测试**: 更新测试脚本的数据解析逻辑
- **文档**: 更新接口文档和示例

---

## 2025-09-29 - 摊销明细操作限制

### 📋 第十次更新 - 限制摊销明细操作为仅修改

根据需求文档步骤2的明确要求，摊销明细表格"不支持增删行，只支持修改"，对摊销明细操作接口进行了限制：

#### 🎯 业务需求
根据需求文档步骤2：
- 摊销明细表格**不支持增删行**
- 只支持**修改现有行的内容**
- 摊销明细的行数由合同期间自动确定

#### 🔧 代码修改

##### 1. 控制器层限制
**AmortizationEntryController.java**:
- `/amortization-entries/operate` 接口只允许 `UPDATE` 操作
- `CREATE` 操作返回：`"摊销明细不支持新增操作，请通过合同创建时自动生成"`
- `DELETE` 操作返回：`"摊销明细不支持删除操作，如需调整请修改合同信息重新生成"`

##### 2. 批量操作限制
**批量操作接口**:
- `/amortization-entries/batch-operate` 只允许批量 `UPDATE`
- 检查请求中是否包含 `CREATE` 或 `DELETE` 操作
- 如包含不支持的操作，直接返回错误信息

#### 📚 文档更新

##### API文档
- **`docs/api.md`** - 更新描述为"只支持修改内容，不可增删行"
- **`docs/simplified-api.md`** - 同步更新接口描述
- **`docs/DIRECTORY_INDEX.md`** - 更新目录索引说明

##### 接口文档
- **`docs/08-amortization-entries-operate/README.md`** - 详细说明：
  - 支持的操作类型：仅 `UPDATE`
  - 不支持的操作类型：`CREATE`、`DELETE`
  - 业务限制说明
  - 错误处理信息

#### 🧪 测试更新

##### 测试脚本增强
**test-simplified-api.sh**:
- 测试 `CREATE` 操作被正确拒绝
- 测试 `UPDATE` 操作正常工作
- 测试 `DELETE` 操作被正确拒绝
- 测试批量 `CREATE` 操作被拒绝
- 测试批量 `UPDATE` 操作正常工作

##### 测试验证点
- ✅ UPDATE操作支持（唯一支持的操作）
- ❌ CREATE操作（正确被拒绝）
- ❌ DELETE操作（正确被拒绝）
- ✅ 批量UPDATE操作支持
- ❌ 批量CREATE/DELETE操作（正确被拒绝）

#### 💡 设计理念

##### 业务一致性
- **需求驱动**: 严格按照需求文档的业务规则实现
- **数据完整性**: 摊销明细由系统自动生成，确保数据一致性
- **用户体验**: 明确的错误提示，指导用户正确的操作方式

##### 接口设计
- **操作限制**: 通过代码层面限制不支持的操作
- **错误友好**: 提供清晰的错误信息和操作建议
- **批量支持**: 支持批量修改提高操作效率

##### 系统稳定性
- **防误操作**: 防止用户误删或误增摊销明细
- **数据一致**: 确保摊销明细与合同期间保持一致
- **业务逻辑**: 强制执行业务规则，避免数据不一致

#### 📋 影响范围
- **前端**: 需要隐藏或禁用摊销明细的新增/删除按钮
- **后端**: 接口层面强制限制操作类型
- **测试**: 验证操作限制的正确性
- **文档**: 更新所有相关文档说明

---

## 2025-09-26 - 文档格式统一

### 📋 第九次更新 - 统一文档格式为Markdown

根据用户要求，统一所有接口文档格式，将txt文件改为md格式：

#### 🎯 统一原则
- **格式统一**: 所有接口文档使用Markdown格式（`.md`）
- **结构标准**: 统一的文档结构和内容组织
- **风格一致**: 保持新旧文档的风格一致性

#### 🔧 转换内容

##### 文档格式转换
**转换的文件**:
- `health/note.txt` → `health/README.md`
- `amortization-calculate/note.txt` → `amortization-calculate/README.md`
- `contracts-create/note.txt` → `contracts-create/README.md`
- `contracts-get-amortization/note.txt` → `contracts-get-amortization/README.md`
- `amortization-entries-update/note.txt` → `amortization-entries-update/README.md`
- `journals-preview/note.txt` → `journals-preview/README.md`
- `payments-execute/note.txt` → `payments-execute/README.md`
- `payments-preview/note.txt` → `payments-preview/README.md`
- `payments-get-by-contract/note.txt` → `payments-get-by-contract/README.md`
- `payments-get-detail/note.txt` → `payments-get-detail/README.md`
- `payments-cancel/note.txt` → `payments-cancel/README.md`

##### 文档结构标准化
每个接口文档包含：
```markdown
# 接口名称

## 接口信息
- URL、Method、Description

## 请求参数
- JSON格式示例

## 响应格式  
- JSON格式示例

## 使用场景
- 业务场景说明

## 错误处理
- 错误情况和状态码
```

#### 📚 文档增强

##### 内容丰富化
- **详细说明**: 从简单的txt描述扩展为完整的接口文档
- **示例代码**: 添加完整的请求/响应JSON示例
- **业务逻辑**: 说明接口的业务场景和使用方法
- **错误处理**: 列出常见错误情况和处理方式

##### 统一风格
- **标题层级**: 统一使用H1-H4标题层级
- **代码块**: 统一使用```json格式化JSON
- **列表格式**: 统一使用-符号的无序列表
- **字段说明**: 统一的参数和响应字段说明格式

#### 💡 文档优势

##### 可读性提升
- **Markdown渲染**: 支持更好的格式化和渲染
- **代码高亮**: JSON代码块支持语法高亮
- **结构清晰**: 标题层级和列表让文档结构更清晰

##### 维护性改善
- **格式统一**: 所有文档使用相同的格式和结构
- **内容完整**: 从简单说明扩展为完整的接口文档
- **易于更新**: Markdown格式便于版本控制和协作

##### 开发友好
- **IDE支持**: 现代IDE对Markdown有更好的支持
- **预览功能**: 可以直接预览渲染效果
- **搜索友好**: 更好的文本搜索和索引支持

#### 📚 文档更新
- **`docs/README.md`** - 更新文档格式说明
- **所有接口目录** - 统一使用`README.md`格式
- 移除所有旧的`note.txt`文件

---

## 2025-09-25 - 需求变更更新

### 📋 第八次更新 - 统一会计分录设计

根据用户反馈，统一会计分录接口设计，移除重复的付款会计分录接口：

#### 🎯 设计原则
- **统一实体**: 会计分录是同一个表和实体，不区分合同会计分录和付款会计分录
- **类型区分**: 通过`entryType`字段区分AMORTIZATION（摊销）和PAYMENT（付款）
- **统一管理**: 所有会计分录都通过`/journal-entries/*`接口进行管理

#### 🔧 核心修改

##### 1. JournalEntry实体增强
- 添加`entryType`枚举字段区分分录类型
- 保留`contract`和`payment`关联字段
- 统一的审计字段和业务字段

##### 2. 接口统一
**移除重复接口**:
- ~~`/payments/preview` - 预览付款会计分录~~
- ~~`/journals/preview` - 预览会计分录~~

**统一为**:
- `/journal-entries/preview` - 预览会计分录（统一）
- `/journal-entries/operate` - 会计分录操作（统一）

##### 3. 业务流程统一
- **步骤3（摊销）**: 生成`entryType=AMORTIZATION`的会计分录
- **步骤4（付款）**: 生成`entryType=PAYMENT`的会计分录
- **查询管理**: 通过统一接口查询和操作所有类型的会计分录

#### 📚 接口更新

##### 精简后的会计分录接口
```
POST   /journal-entries/generate/{contractId}       # 生成摊销会计分录
GET    /journal-entries/contract/{contractId}       # 查询合同所有会计分录
POST   /journal-entries/operate                     # 统一增删改操作
POST   /journal-entries/preview                     # 预览会计分录
```

#### 🎯 数据结构统一

##### JournalEntry实体字段
```json
{
  "id": 1,
  "contract": { "id": 1, "vendorName": "供应商A" },
  "payment": null,
  "entryType": "AMORTIZATION",
  "bookingDate": "2024-01-31",
  "accountName": "费用",
  "debitAmount": 1000.00,
  "creditAmount": 0.00,
  "description": "合同摊销费用"
}
```

#### 💡 设计优势

##### 数据一致性
- **单一数据源**: 所有会计分录存储在同一个表中
- **统一查询**: 可以方便地查询合同的所有会计分录
- **类型区分**: 通过entryType字段清晰区分业务类型

##### 接口简化
- **减少重复**: 移除了重复的付款会计分录接口
- **统一操作**: 所有会计分录使用相同的CRUD接口
- **易于维护**: 统一的业务逻辑和数据处理

##### 业务清晰
- **步骤明确**: 每个步骤生成对应类型的会计分录
- **关联清晰**: 通过contract和payment字段明确业务关联
- **扩展性强**: 可以轻松添加新的分录类型

#### 📚 文档更新
- **`docs/journal-entries/README.md`** - 更新统一设计说明
- **`docs/api.md`** - 移除重复接口
- **`docs/simplified-api.md`** - 更新接口列表和说明

---

### 📋 第七次更新 - API接口精简设计

根据用户要求，精简所有API接口，将PUT、DELETE操作合并到POST接口中：

#### 🎯 精简原则
- **统一操作方式**: 所有增删改操作使用POST方法
- **operate字段区分**: 通过`operate`字段区分操作类型
- **支持的操作**: `CREATE`、`UPDATE`、`DELETE`
- **保持查询接口**: GET查询接口保持不变

#### 🔧 核心实现

##### 1. 统一操作请求DTO
- **`OperationRequest<T>`** - 通用操作请求类
- 包含：`operate`(操作类型)、`id`(实体ID)、`data`(实体数据)
- 支持泛型，适用于所有实体类型

##### 2. 精简后的接口设计
```
原来: PUT /amortization-entries/{id}    DELETE /amortization-entries/{id}
现在: POST /amortization-entries/operate

原来: PUT /journal-entries/{id}         DELETE /journal-entries/{id}  
现在: POST /journal-entries/operate
```

##### 3. 批量操作支持
- **单个操作**: `/operate` 接口
- **批量操作**: `/batch-operate` 接口
- 支持在一个请求中混合增删改操作

#### 📚 接口对比

##### 精简前（原设计）
```
GET    /amortization-entries/contract/{contractId}  # 查询列表
POST   /amortization-entries                        # 创建
PUT    /amortization-entries/{entryId}              # 更新
DELETE /amortization-entries/{entryId}              # 删除
PUT    /amortization-entries/batch                  # 批量更新
```

##### 精简后（新设计）
```
GET    /amortization-entries/contract/{contractId}  # 查询列表
GET    /amortization-entries/{entryId}              # 查询单个
POST   /amortization-entries/operate                # 统一增删改
POST   /amortization-entries/batch-operate          # 批量操作
```

#### 🎯 请求格式标准化

##### 单个操作请求
```json
{
  "operate": "CREATE|UPDATE|DELETE",
  "id": 1,
  "data": {
    // 实体数据
  }
}
```

##### 批量操作请求
```json
[
  {
    "operate": "CREATE",
    "data": { /* 新建数据 */ }
  },
  {
    "operate": "UPDATE", 
    "id": 2,
    "data": { /* 更新数据 */ }
  },
  {
    "operate": "DELETE",
    "id": 3
  }
]
```

#### 🔧 代码更新

##### Controller层更新
- **`AmortizationEntryController`** - 合并PUT/DELETE到operate接口
- **`JournalEntryController`** - 合并PUT/DELETE到operate接口
- 统一的错误处理和参数验证

##### Service层增强
- **`AmortizationEntryService.batchOperateAmortizationEntries()`**
- **`JournalEntryService.batchOperateJournalEntries()`**
- 支持批量混合操作处理

#### 🧪 测试验证

##### 新增测试脚本
- **`test-simplified-api.sh`** - 专门测试精简接口
- 验证CREATE、UPDATE、DELETE操作
- 测试批量操作功能
- 验证错误处理机制

##### 测试覆盖
- 统一操作接口的所有操作类型
- 批量操作的混合场景
- 无效操作类型的错误处理
- 参数验证和边界条件

#### 📚 文档完善

##### 新增专门文档
- **`docs/simplified-api.md`** - 精简API设计文档
- 完整的接口对比和使用示例
- 请求格式和响应说明

##### 文档更新
- **`docs/api.md`** - 更新为精简后的接口列表
- **`docs/amortization-entries/README.md`** - 更新操作示例
- **`docs/journal-entries/README.md`** - 更新接口说明

#### 💡 设计优势

##### 接口简化
- **减少接口数量**: 从5个接口减少到4个接口
- **统一操作方式**: 所有增删改使用相同的调用方式
- **降低学习成本**: 前端开发者只需掌握一种操作模式

##### 扩展性强
- **新增操作类型**: 只需扩展OperationType枚举
- **统一错误处理**: 集中的参数验证和错误响应
- **批量操作天然支持**: 无需额外开发批量接口

##### 前端友好
- **统一的请求格式**: 减少前端代码复杂度
- **批量操作支持**: 提高用户体验和性能
- **清晰的操作语义**: operate字段明确表达操作意图

#### 🔄 兼容性说明
- **查询接口保持不变**: 所有GET接口无变化
- **专用接口保持不变**: 上传、生成等特殊POST接口保持不变
- **向后兼容**: 可以同时支持新旧接口（如需要）

---

### 📋 第六次更新 - 实现List<Entity>格式和CRUD操作

根据用户对需求文档的重要更新，实现了以下核心功能：

#### 🎯 需求变更要点
1. **步骤2和3**：要求输出形式为`List<Entity>`格式交给前端
2. **CRUD支持**：后端支持对数据进行调整（修改、加行或删除行）
3. **数据库设计**：步骤1-3分别有独立数据库表，包含审计字段
4. **步骤3增强**：会计分录生成、保存和前端增删改操作
5. **步骤4完善**：付款后保存会计分录，支持修改

#### 🔧 核心技术实现

##### 1. 基础审计实体
- **`BaseAuditEntity.java`** - 统一的审计基类
- 包含：`id`(自增)、`createdAt`、`updatedAt`、`createdBy`、`updatedBy`
- 所有实体继承此基类，确保审计字段一致性

##### 2. 实体模型更新
- **`Contract`** - 继承审计基类，移除重复id字段
- **`AmortizationEntry`** - 继承审计基类，添加status兼容方法
- **`JournalEntry`** - 继承审计基类，添加description字段和无参构造函数

##### 3. 摊销明细CRUD服务
- **`AmortizationEntryService.java`** - 完整的CRUD操作
- **`AmortizationEntryController.java`** - RESTful接口
- 支持单个和批量操作，返回`List<Entity>`格式

##### 4. 会计分录生成和管理
- **`JournalEntryService.java`** - 步骤3核心业务逻辑
- **`JournalEntryController.java`** - 会计分录CRUD接口
- 自动根据摊销明细生成会计分录
- 支持前端增删改操作

#### 📚 新增接口列表

##### 摊销明细接口（步骤2）
```
GET    /amortization-entries/contract/{contractId}  # 查询列表
POST   /amortization-entries                        # 创建
PUT    /amortization-entries/{entryId}              # 更新
DELETE /amortization-entries/{entryId}              # 删除
PUT    /amortization-entries/batch                  # 批量更新
```

##### 会计分录接口（步骤3）
```
POST   /journal-entries/generate/{contractId}       # 生成分录
GET    /journal-entries/contract/{contractId}       # 查询列表
POST   /journal-entries                             # 创建
PUT    /journal-entries/{entryId}                   # 更新
DELETE /journal-entries/{entryId}                   # 删除
```

#### 🎯 业务流程实现

##### 步骤3：会计分录生成
根据需求文档场景1示例：
```
摊销期间: 2024年1月-2024年3月，总额3000元

生成会计分录：
Booking Date    会计科目    ENTERED DR    ENTERED CR
2024-01-31      费用       1000.00       -
2024-01-31      应付       -             1000.00
2024-02-29      费用       1000.00       -
2024-02-29      应付       -             1000.00
2024-03-31      费用       1000.00       -
2024-03-31      应付       -             1000.00
```

##### 记账日期规则
- 记账日期为入账期间的最后一天
- 自动处理月末日期（如2月28/29日）

#### 📊 数据格式标准化

##### List<Entity>响应格式
所有步骤2和3的接口均返回完整的实体列表：
```json
[
  {
    "id": 1,
    "contract": { ... },
    "amortizationPeriod": "2024-01",
    "accountingPeriod": "2024-01",
    "amount": 1000.00,
    "createdAt": "2024-01-24T14:30:52",
    "updatedAt": "2024-01-24T14:30:52",
    "createdBy": "system",
    "updatedBy": "system"
  }
]
```

#### 🧪 测试验证

##### 新增测试脚本
- **`test-new-features.sh`** - 专门测试新增功能
- 验证List<Entity>格式输出
- 测试CRUD操作完整性
- 验证审计字段正确性

##### 测试覆盖范围
- 摊销明细的增删改查
- 会计分录的自动生成
- 会计分录的增删改查
- 数据库审计字段验证
- 业务流程完整性

#### 📚 文档完善

##### 新增专门文档
- **`docs/amortization-entries/`** - 摊销明细接口文档
- **`docs/journal-entries/`** - 会计分录接口文档
- 包含完整的API说明、数据结构和业务规则

##### 文档更新
- **`docs/api.md`** - 更新完整接口列表
- **`docs/README.md`** - 重新组织接口分类
- 按步骤清晰分类所有接口

#### 💡 架构优势

##### 统一审计设计
- 所有实体统一继承`BaseAuditEntity`
- 自动记录创建时间、修改时间、操作人
- 便于数据追溯和审计

##### RESTful设计
- 标准的REST接口设计
- 统一的错误处理和响应格式
- 支持批量操作提高效率

##### 业务分离
- 每个步骤独立的Service和Controller
- 清晰的业务边界和职责分工
- 便于维护和扩展

---

### 📋 第五次更新 - 新增合同列表查询接口

根据用户要求，新增查询合同列表的接口，显示所有合同信息：

#### 新增接口
1. **GET** `/contracts` - 查询所有合同列表
2. **GET** `/contracts/list` - 分页查询合同列表

#### 🔧 代码更新

1. **新增DTO**:
   - `ContractListResponse.java` - 合同列表响应DTO
   - `ContractSummary` - 合同摘要信息内嵌类

2. **Repository更新**:
   - `ContractRepository.java` - 添加 `findAllByOrderByCreatedAtDesc()` 方法

3. **Service更新**:
   - `ContractService.java` - 添加合同列表查询方法
   - 支持全量查询和分页查询
   - 按创建时间倒序排列

4. **Controller更新**:
   - `ContractUploadController.java` - 添加列表查询接口
   - 支持分页参数（page, size）

#### 📚 响应数据结构

```json
{
  "contracts": [
    {
      "contractId": 1,
      "totalAmount": 6000.00,
      "startDate": "2024-01-01",
      "endDate": "2024-06-30",
      "vendorName": "供应商名称",
      "attachmentName": "contract_xxx.pdf",
      "createdAt": "2024-01-24T14:30:52+08:00",
      "status": "ACTIVE"
    }
  ],
  "totalCount": 1,
  "message": "查询成功"
}
```

#### 🎯 功能特性
- **全量查询**: 返回所有合同的摘要信息
- **分页支持**: 支持分页查询（预留功能）
- **排序规则**: 按创建时间倒序排列
- **摘要信息**: 只返回关键字段，提高查询效率
- **状态字段**: 预留合同状态字段，便于后续扩展

#### 🧪 测试更新
- 更新 `test-contract-upload.sh` - 添加列表查询测试
- 更新 `test-apis.sh` - 集成到主测试流程
- 验证全量查询和分页查询功能

#### 📚 文档更新
- 新增 `docs/contracts-list/` - 合同列表接口文档
- 完整的API说明和响应示例
- 更新主API文档和README

#### 💡 使用场景
- **合同列表页面**: 前端展示所有合同
- **合同选择**: 下拉框或选择器数据源
- **合同管理**: 管理员查看所有合同概览
- **数据统计**: 合同数量和基础统计信息

---

## 2025-09-24 - 需求变更更新

### 📋 第四次更新 - 重构步骤1为合同上传和外部接口解析

根据用户对需求文档步骤1的重大修改，从简单的前端输入参数改为完整的合同上传和外部接口解析流程：

#### 需求变更
- **变更前**: 通过前端页面输入合同参数（totalAmount、startDate等）
- **变更后**: 上传合同文件 → 调用外部接口解析 → 保存到数据库 → 支持编辑

#### 🔧 代码更新

1. **实体类更新**:
   - `Contract.java` - 添加 `attachmentName` 字段存储合同附件名称

2. **新增服务类**:
   - `FileUploadService.java` - 文件上传处理服务
   - `ExternalContractParseService.java` - 外部接口调用服务

3. **新增DTO类**:
   - `ExternalContractParseResponse.java` - 外部接口响应
   - `ContractUploadResponse.java` - 合同上传响应
   - `ContractEditRequest.java` - 合同编辑请求

4. **新增Controller**:
   - `ContractUploadController.java` - 合同上传和管理接口

5. **配置更新**:
   - `FileUploadConfig.java` - 文件上传配置类
   - `application.properties` - 添加文件上传和外部接口配置

#### 📚 新增接口

1. **POST** `/contracts/upload` - 上传合同文件
2. **GET** `/contracts/{contractId}` - 查询合同信息  
3. **PUT** `/contracts/{contractId}` - 编辑合同信息

#### 🎯 业务流程

新的步骤1流程：
1. **文件上传**: 前端上传合同附件到指定目录
2. **外部解析**: 调用外部接口解析合同内容获取：
   - 合同总金额 (totalAmount)
   - 合同开始时间 (startDate)  
   - 合同结束时间 (endDate)
   - 税率 (taxRate)
   - 供应商名称 (vendorName)
3. **数据保存**: 将解析结果保存到合同信息表
4. **支持编辑**: 前端可以编辑解析结果并保存

#### 📊 配置说明

```properties
# 文件上传配置
file.upload.contract-path=./uploads/contracts/
file.upload.allowed-types=pdf,doc,docx,jpg,jpeg,png
file.upload.max-file-size=10485760

# 外部接口配置
external.contract.parse.url=http://localhost:9999/api/contract/parse
external.contract.parse.enabled=false  # 开发环境使用模拟数据
```

#### 🧪 测试更新
- 新增 `test-contract-upload.sh` - 专门测试合同上传功能
- 更新 `test-apis.sh` - 集成合同上传到主测试流程
- 完整的错误处理测试（空文件、不支持类型等）

#### 📚 文档更新
- 新增合同管理接口文档目录
- 完整的API说明和示例
- 错误处理和配置说明

---

## 2025-09-23 - 需求变更更新

### 📋 第三次更新 - selectedPeriods使用ID替代期间值

根据用户要求，将付款请求中的 `selectedPeriods` 字段从期间字符串改为摊销明细ID：

#### 字段变更
- **字段名**: `selectedPeriods`
- **变更前**: `List<String>` - 期间值列表，如 `["2024-01", "2024-02"]`
- **变更后**: `List<Long>` - 摊销明细ID列表，如 `[1, 2, 3]`

#### 🔧 代码更新
1. **DTO更新**:
   - `PaymentExecutionRequest.java` - 将 `selectedPeriods` 类型从 `List<String>` 改为 `List<Long>`

2. **业务逻辑更新**:
   - `PaymentService.java` - 添加 `AmortizationEntryRepository` 依赖
   - 根据摊销明细ID查询对应的期间值进行后续处理

#### 📚 文档更新
- `docs/payments-execute/request.json` - 示例使用ID格式
- `docs/api.md` - 更新字段说明
- 测试脚本更新为使用ID格式

#### 🎯 业务逻辑
系统接收摊销明细ID后的处理流程：
1. 根据ID查询摊销明细实体
2. 提取期间值用于内部计算
3. 保持原有业务逻辑不变

---

### 📋 第二次更新 - 添加付款状态字段

根据用户要求，为摊销明细添加付款状态字段：

#### 新增字段
- **字段名**: `status`
- **类型**: String
- **可选值**: 
  - `PENDING`: 待付款
  - `COMPLETED`: 已完成
- **默认值**: `PENDING`

#### 🔧 代码更新
1. **实体类更新**:
   - `AmortizationEntry.java` - 添加 `paymentStatus` 字段和枚举
   - `AmortizationEntryDto.java` - 添加 `status` 字段和兼容性构造函数

2. **业务逻辑更新**:
   - `ContractService.java` - 根据付款记录计算每个期间的状态
   - 添加 `PaymentRepository` 依赖来查询付款状态

3. **数据库更新**:
   - `amortization_entries_init.sql` - 添加 `payment_status` 字段
   - 添加相关索引和约束

#### 📚 文档更新
- 所有响应示例文件都已添加 `status` 字段
- API文档更新了字段说明
- 数据库建表脚本包含新字段定义

#### 🎯 业务逻辑
系统会根据付款记录自动计算每个摊销期间的状态：
- 如果该期间已在确认的付款记录中，状态为 `COMPLETED`
- 否则状态为 `PENDING`

---

## 2025-09-23 - 需求变更更新

### 📋 需求变更内容

根据 `Requirement_new.md` 的修改，进行了以下系统更新：

#### 1. 摊销明细操作限制
- **变更前**: 支持摊销明细的增删改操作
- **变更后**: 只支持修改操作，不支持增加和删除行

#### 2. 主键ID规范确认
- **要求**: 所有表的主键ID使用自增ID
- **状态**: ✅ 已确认所有实体类都使用 `@GeneratedValue(strategy = GenerationType.IDENTITY)`

### 🔧 代码更新

#### 移除的接口
1. **POST** `/amortization/contracts/{contractId}/entries` - 添加摊销明细行
2. **DELETE** `/amortization/entries/{entryId}` - 删除摊销明细行

#### 保留的接口
1. **PUT** `/amortization/entries/{entryId}` - 更新摊销明细金额

#### 更新的文件
- `AmortizationPersistenceController.java` - 移除增删方法
- `ContractService.java` - 移除 `addAmortizationEntry()` 和 `deleteAmortizationEntry()` 方法

### 📚 文档更新

#### API文档
- `docs/api.md` - 移除增删接口说明
- `docs/README.md` - 更新接口列表
- 删除相关接口示例目录：
  - `docs/amortization-entries-add/`
  - `docs/amortization-entries-delete/`

#### UML时序图
- `docs/sequence_diagram.puml` - 移除增删操作流程
- `docs/UML_README.md` - 更新说明文档

#### 部署文档
- `DEPLOYMENT.md` - 更新功能说明
- `test-apis.sh` - 添加注意事项说明

### 🎯 业务影响

#### 用户操作流程
```
步骤1: 输入合同信息 → 计算摊销明细
步骤2: 保存合同和摊销台账 → 只能修改金额，不能增删行
步骤3: 预览会计分录
步骤4: 执行付款
```

#### 数据完整性
- 摊销明细的行数在合同创建时确定
- 用户只能调整每行的金额，不能改变期间结构
- 保证了摊销期间的完整性和一致性

### ✅ 验证检查

#### 主键ID检查
所有实体类都使用自增主键：
- `Contract.java` - ✅ IDENTITY
- `AmortizationEntry.java` - ✅ IDENTITY  
- `Payment.java` - ✅ IDENTITY
- `JournalEntry.java` - ✅ IDENTITY

#### 接口功能检查
- ✅ 摊销计算接口正常
- ✅ 合同创建接口正常
- ✅ 摊销明细修改接口正常
- ❌ 摊销明细增删接口已移除
- ✅ 付款执行接口正常

### 🔄 兼容性说明

#### 前端适配
前端需要相应调整：
1. 移除"添加行"和"删除行"按钮
2. 保留"修改金额"功能
3. 在摊销台账页面显示只读的期间信息

#### API调用
- 现有的修改接口保持不变
- 移除对增删接口的调用
- 错误处理保持一致

### 📝 注意事项

1. **数据迁移**: 现有数据不受影响，只是功能限制
2. **测试覆盖**: 需要更新相关测试用例
3. **文档同步**: 所有相关文档已同步更新
4. **向后兼容**: 保持现有数据结构不变

---

此次更新严格按照需求文档的变更进行，确保系统功能与业务需求保持一致。
