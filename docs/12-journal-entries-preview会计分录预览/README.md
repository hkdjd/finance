# 预览会计分录接口

## 接口信息
- **URL**: `/journal-entries/preview`
- **Method**: POST
- **Description**: 预览会计分录，不保存到数据库，用于前端展示和确认

## 请求参数
```json
{
  "contractId": 1,
  "previewType": "AMORTIZATION"
}
```

## 响应格式
```json
{
  "contract": {
    "id": 1,
    "totalAmount": 3000.00,
    "startDate": "2024-01-01",
    "endDate": "2024-03-31",
    "vendorName": "供应商A"
  },
  "previewEntries": [
    {
      "entryType": "AMORTIZATION",
      "bookingDate": "2024-01-31",
      "accountName": "费用",
      "debitAmount": 1000.00,
      "creditAmount": 0.00,
      "description": "摊销费用预览",
      "memo": "摊销费用预览 - 2024-01",
      "entryOrder": 1
    },
    {
      "entryType": "AMORTIZATION",
      "bookingDate": "2024-01-31",
      "accountName": "应付",
      "debitAmount": 0.00,
      "creditAmount": 1000.00,
      "description": "摊销应付预览",
      "memo": "摊销应付预览 - 2024-01",
      "entryOrder": 2
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

#### 预览会计分录信息（previewEntries）
- **entryType**: 分录类型（AMORTIZATION摊销、PAYMENT付款）
- **bookingDate**: 记账日期（入账期间的最后一天）
- **accountName**: 会计科目名称（如"费用"、"应付"等）
- **debitAmount**: 借方金额
- **creditAmount**: 贷方金额
- **description**: 分录描述
- **memo**: 备注信息
- **entryOrder**: 分录顺序，用于排序显示

### 响应格式优势
- **避免重复**: 合同信息只在根节点显示一次，不在每个分录中重复
- **结构清晰**: 合同信息和分录列表分离，便于前端处理
- **数据精简**: 减少响应数据大小，提高传输效率
- **格式统一**: 与生成接口保持一致的数据结构

## 预览类型
- **AMORTIZATION**: 预览摊销会计分录
- **PAYMENT**: 预览付款会计分录

## 后台逻辑

### 预览会计分录的生成流程
1. **查询现有分录**: 根据合同ID和预览类型查询数据库中是否已存在相应的会计分录
2. **自动生成机制**: 如果查询不到摊销会计分录，后台会自动执行以下步骤：
   - 调用 `/journal-entries/generate/{contractId}` 接口
   - 传入参数：`{"entryType": "AMORTIZATION", "description": "自动生成摊销会计分录"}`
   - 生成并保存摊销会计分录到数据库
3. **返回预览数据**: 基于生成的会计分录数据构造预览响应格式返回给前端

### 自动生成的优势
- **确保预览可用性**: 即使用户未手动生成会计分录，预览功能仍然可用
- **简化用户操作**: 用户无需先生成再预览，可以直接预览
- **数据一致性**: 预览的数据与实际生成的数据完全一致
- **透明化处理**: 前端接口调用方式不变，后台自动处理数据准备

### 注意事项
- 自动生成仅适用于摊销会计分录（AMORTIZATION类型）
- 付款会计分录（PAYMENT类型）仍需通过付款流程生成
- 生成的会计分录会持久化到数据库，不仅仅是预览数据

## 使用场景
- 步骤3：生成会计分录前的预览（支持自动生成）
- 步骤4：付款前的会计分录预览
- 用户确认会计分录内容

## 特点
- **智能数据准备**: 如果查询不到会计分录，会自动生成并保存到数据库
- **实时计算和展示**: 根据当前合同和摊销数据动态生成预览
- **支持多种预览类型**: 支持摊销和付款两种预览模式
- **格式统一**: 与 `/journal-entries/generate` 接口保持相同的响应结构
- **完整合同信息**: 包含合同的详细信息，便于前端展示和确认
- **用户体验优化**: 无需手动生成即可直接预览会计分录

## 后台实现示例

### 预览接口的伪代码逻辑
```java
@PostMapping("/journal-entries/preview")
public ResponseEntity<JournalEntryPreviewResponse> previewJournalEntries(
    @RequestBody JournalEntryPreviewRequest request) {
    
    // 1. 查询现有会计分录
    List<JournalEntry> existingEntries = journalEntryService
        .findByContractIdAndEntryType(request.getContractId(), request.getPreviewType());
    
    // 2. 如果查询不到摊销会计分录，自动生成
    if (existingEntries.isEmpty() && "AMORTIZATION".equals(request.getPreviewType())) {
        // 调用生成接口
        JournalEntryGenerateRequest generateRequest = new JournalEntryGenerateRequest();
        generateRequest.setEntryType("AMORTIZATION");
        generateRequest.setDescription("自动生成摊销会计分录");
        
        // 内部调用生成服务
        JournalEntryListResponse generateResponse = journalEntryService
            .generateJournalEntries(request.getContractId(), generateRequest);
        
        existingEntries = generateResponse.getJournalEntries();
    }
    
    // 3. 构造预览响应
    Contract contract = contractService.findById(request.getContractId());
    JournalEntryPreviewResponse response = new JournalEntryPreviewResponse();
    response.setContract(contract);
    response.setPreviewEntries(convertToPreviewEntries(existingEntries));
    
    return ResponseEntity.ok(response);
}
```

### 关键实现要点
- **条件判断**: 仅在查询不到摊销会计分录时才自动生成
- **内部调用**: 复用现有的会计分录生成服务，避免代码重复
- **数据转换**: 将生成的会计分录转换为预览格式
- **错误处理**: 需要处理生成过程中可能出现的异常
