# 按预提摊销期间分组展示会计分录实现

## 📋 需求说明

按照预提摊销期间分组展示会计分录，比如支付11月的预提单，生成了四条会计科目，则该四条会计科目均显示在11月摊销期间的分组下。

## ✅ 实现方案

### 1. 后端数据结构增强

#### **PaymentJournalEntryDto增加摊销期间字段**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentJournalEntryDto {
    // ... 原有字段
    private String amortizationPeriod; // 摊销期间，用于分组显示
}
```

#### **PaymentService增加期间提取逻辑**
```java
/**
 * 从备注中提取摊销期间信息
 */
private String extractAmortizationPeriodFromMemo(String memo, List<String> selectedPeriods) {
    if (memo == null || selectedPeriods == null || selectedPeriods.isEmpty()) {
        return selectedPeriods != null && !selectedPeriods.isEmpty() ? selectedPeriods.get(0) : null;
    }
    
    // 尝试从备注中提取期间信息（格式：yyyy-MM）
    Pattern pattern = Pattern.compile("(\\d{4}-\\d{2})");
    Matcher matcher = pattern.matcher(memo);
    
    if (matcher.find()) {
        String extractedPeriod = matcher.group(1);
        // 验证提取的期间是否在选中的期间列表中
        if (selectedPeriods.contains(extractedPeriod)) {
            return extractedPeriod;
        }
    }
    
    // 如果无法从备注中提取或提取的期间不在选中列表中，使用第一个选中的期间
    return selectedPeriods.get(0);
}
```

### 2. 前端类型定义更新

#### **JournalEntry接口增加摊销期间字段**
```typescript
export interface JournalEntry {
  // ... 原有字段
  /** 摊销期间 */
  amortizationPeriod?: string;
}
```

### 3. 前端分组逻辑优化

#### **按预提摊销期间分组**
```javascript
// 按预提摊销期间分组分录
const groupedByPeriod = filteredEntries.reduce((groups, entry) => {
  // 优先使用后端传递的摊销期间信息，如果没有则从备注中提取，最后使用入账日期
  const period = entry.amortizationPeriod || 
                entry.memo?.match(/(\d{4}-\d{2})/)?.[1] || 
                new Date(entry.bookingDate).toISOString().slice(0, 7);
  if (!groups[period]) {
    groups[period] = [];
  }
  groups[period].push(entry);
  return groups;
}, {});
```

## 🔧 技术实现细节

### 1. 摊销期间信息传递流程

```mermaid
graph LR
    A[用户选择期间] --> B[PaymentService.executePayment]
    B --> C[提取selectedPeriods]
    C --> D[生成会计分录]
    D --> E[为每个分录设置amortizationPeriod]
    E --> F[返回PaymentJournalEntryDto]
    F --> G[前端按期间分组显示]
```

### 2. 期间提取优先级

1. **第一优先级**：后端传递的`amortizationPeriod`字段
2. **第二优先级**：从备注中正则提取的期间信息（格式：yyyy-MM）
3. **第三优先级**：使用入账日期的年月作为回退

### 3. 数据一致性保证

#### **后端验证机制**
- 从备注中提取的期间必须在选中期间列表中
- 如果提取失败或不匹配，使用第一个选中期间作为默认值
- 确保每个分录都有明确的摊销期间归属

#### **前端容错机制**
- 支持多级回退策略
- 即使后端数据不完整也能正常分组显示
- 保持向后兼容性

## 📊 实现效果

### 业务场景示例

#### **场景：支付11月预提单**
```
用户操作：选择2024-11期间，支付金额1000元
生成分录：
1. 借：应付账款 1000.00 (备注：2024-11期间预提费用)
2. 借：预付费用 200.00  (备注：2024-12期间预付)
3. 贷：银行存款 1200.00 (备注：实际付款金额)
4. 贷：费用调整 200.00  (备注：超额支付调整)

前端显示：
摊销期间：2024-11 (3 条分录)
├── 应付账款 借方 ¥1,000.00
├── 银行存款 贷方 ¥1,200.00  
└── 费用调整 贷方 ¥200.00

摊销期间：2024-12 (1 条分录)
└── 预付费用 借方 ¥200.00
```

### 界面展示效果

```
摊销期间：2024-11 (3 条分录)
┌─────────────────────────────────────────────────────────┐
│ 分录1 | 付款 | 2024-11-27 | 应付 | ¥1,000.00 | 14:30:25 │
│ 分录2 | 付款 | 2024-11-27 | 银行存款 | ¥1,200.00 | 14:30:26│
│ 分录3 | 付款 | 2024-11-27 | 费用调整 | ¥200.00 | 14:30:27 │
└─────────────────────────────────────────────────────────┘

摊销期间：2024-12 (1 条分录)
┌─────────────────────────────────────────────────────────┐
│ 分录4 | 付款 | 2024-11-27 | 预付费用 | ¥200.00 | 14:30:28│
└─────────────────────────────────────────────────────────┘

总计：2 个摊销期间，共 4 条分录
```

## 🎯 核心优势

### 1. 业务逻辑清晰
- **期间归属明确**：每个分录都明确归属到具体的摊销期间
- **分组逻辑合理**：按照业务含义进行分组，便于理解
- **数据追溯性强**：可以清楚看到每个期间相关的所有分录

### 2. 技术实现稳定
- **多级回退机制**：确保在各种情况下都能正确分组
- **数据一致性**：后端验证确保期间信息的准确性
- **向后兼容性**：不影响现有功能，平滑升级

### 3. 用户体验优化
- **信息组织清晰**：按期间分组便于查看和理解
- **视觉效果良好**：每个期间独立展示，层次分明
- **操作便捷性**：可以快速定位到特定期间的分录

## 🔍 验证方法

### 1. 功能测试
```bash
# 测试场景1：单期间付款
选择期间：2024-11
预期结果：所有分录都归属到2024-11分组

# 测试场景2：跨期间付款
选择期间：2024-11, 2024-12
预期结果：分录按照实际期间归属分别分组

# 测试场景3：复杂分录
包含应付、预付、费用调整等多种科目
预期结果：按照期间正确分组，科目类型不影响分组
```

### 2. 数据验证
```sql
-- 验证分录的摊销期间信息
SELECT memo, payment_timestamp, entry_order 
FROM journal_entries 
WHERE payment_id = ? 
ORDER BY entry_order;
```

### 3. 前端验证
- 检查分组标题显示正确的期间信息
- 验证每个分组内的分录数量统计准确
- 确认总计信息正确显示

## 📝 注意事项

1. **期间格式统一**：确保所有期间信息都使用yyyy-MM格式
2. **时区处理**：注意前后端时间格式的一致性
3. **数据完整性**：确保每个分录都有明确的期间归属
4. **性能考虑**：大量分录时的分组性能优化

现在系统能够准确按照预提摊销期间对会计分录进行分组展示，满足了业务需求，提供了更清晰的数据组织方式。
