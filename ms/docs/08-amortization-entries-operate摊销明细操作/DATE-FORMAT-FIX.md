# 日期格式修复说明

**修复日期**: 2025-10-22  
**问题**: `periodDate` 字段不支持 `yyyy-MM` 格式，导致解析错误

---

## 问题描述

### 错误信息
```json
{
    "error": "Internal Server Error",
    "message": "服务器内部错误: Text '2024-01' could not be parsed at index 7",
    "timestamp": "2025-10-22T20:29:46.108217+08:00",
    "status": 500
}
```

### 原因
请求中的 `periodDate` 使用了 `yyyy-MM` 格式（如 `"2024-01"`），但代码期望 `yyyy-MM-dd` 格式。

---

## 解决方案

### 修改位置
**文件**: `src/main/java/com/ocbc/finance/dto/AmortizationUpdateRequest.java`

**修改内容**: 在 `AmortizationEntryData` 内部类的 `setPeriodDate` 方法中添加智能格式处理

```java
public void setPeriodDate(String periodDate) {
    // 智能处理日期格式：支持 yyyy-MM 和 yyyy-MM-dd
    if (periodDate != null && periodDate.length() == 7) {
        // yyyy-MM 格式，转换为该月的第一天
        this.periodDate = periodDate + "-01";
    } else {
        this.periodDate = periodDate;
    }
}
```

---

## 支持的日期格式

### 1. yyyy-MM 格式（自动转换）
```json
{
  "periodDate": "2024-01"
}
```
**自动转换为**: `"2024-01-01"`（该月第一天）

### 2. yyyy-MM-dd 格式（直接使用）
```json
{
  "periodDate": "2024-01-15"
}
```
**保持不变**: `"2024-01-15"`

---

## 测试用例

### 测试请求
```bash
POST http://localhost:8081/amortization-entries/operate
Content-Type: application/json

{
  "contractId": 4,
  "amortization": [
    {
      "id": 0,
      "amortizationPeriod": "2024-01",
      "accountingPeriod": "2024-01",
      "amount": 8000,
      "periodDate": "2024-01",
      "paymentStatus": "PENDING"
    },
    {
      "id": null,
      "amortizationPeriod": "2025-10-22",
      "accountingPeriod": "2025-10-22",
      "amount": 10,
      "periodDate": "2025-10-22",
      "paymentStatus": "PENDING"
    }
  ]
}
```

### 预期结果
- ✅ `"periodDate": "2024-01"` 自动转换为 `"2024-01-01"`
- ✅ `"periodDate": "2025-10-22"` 保持为 `"2025-10-22"`
- ✅ 请求成功，返回200状态码

---

## 优势

1. **向后兼容**: 同时支持两种日期格式
2. **前端友好**: 前端可以直接使用 `yyyy-MM` 格式，无需手动添加 `-01`
3. **自动转换**: 在DTO层面自动处理，业务逻辑无需修改
4. **透明处理**: 对Service层完全透明，不影响现有代码

---

## 注意事项

- `yyyy-MM` 格式会自动转换为该月的**第一天**
- 如果需要指定具体日期，请使用完整的 `yyyy-MM-dd` 格式
- 日期格式判断基于字符串长度（7位为 `yyyy-MM`，其他为 `yyyy-MM-dd`）
