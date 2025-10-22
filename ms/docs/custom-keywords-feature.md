# 自定义关键字提取功能

## 功能概述

本功能允许用户自定义需要从合同中提取的字段，在上传合同时，系统会自动根据用户预设的关键字从PDF文件中提取相应的字段值。

## 需求来源

参考需求文档 `Requirement_new.md` 的第192-202行：

### 1. 自定义文档关键词提取（192-195行）
- 在调用AI模块时增加用户自定义字段参数
- 用户自定义字段通过 `List<String>` 表示
- AI接口根据自定义字段返回 `Map<String, String>`
- 在合同表中增加字段存储自定义字段结果并返回给前端

### 2. 自定义文档关键词接口（197-199行）
- 提供管理自定义关键字的功能
- 每个用户管理自己的关键字
- 创建 `custom_keywords` 表存储关键字数据

### 3. 解析合同的优化（201-202行）
- 上传合同时，通过用户ID获取自定义关键字
- 将关键字作为AI解析接口的请求参数

---

## 功能架构

### 1. 数据库层

#### custom_keywords 表
```sql
CREATE TABLE custom_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE INDEX idx_user_keyword (user_id, keyword)
);
```

### 2. 后端层

#### 实体类
- `CustomKeyword` - 自定义关键字实体

#### Repository
- `CustomKeywordRepository` - 数据访问层

#### Service
- `CustomKeywordService` - 关键字管理服务
- `ContractService` - 集成自定义关键字功能

#### Controller
- `CustomKeywordController` - 关键字管理接口
- `ContractUploadController` - 更新上传接口

#### DTO
- `CustomKeywordRequest` - 请求DTO
- `CustomKeywordResponse` - 响应DTO
- `ContractUploadResponse` - 添加 `customFields` 字段

### 3. AI集成

#### 调用流程
1. 上传合同时接收 `userId` 参数
2. 根据 `userId` 查询用户的自定义关键字
3. 将关键字列表传递给AI模块
4. AI模块返回标准字段 + 自定义字段
5. 保存并返回完整的解析结果

---

## API接口

### 1. 自定义关键字管理

#### 创建关键字
```
POST /api/custom-keywords
Content-Type: application/json

{
  "userId": 1,
  "keyword": "法人"
}
```

#### 查询用户关键字
```
GET /api/custom-keywords/user/{userId}
```

#### 获取关键字列表（仅字符串）
```
GET /api/custom-keywords/user/{userId}/strings
```

#### 更新关键字
```
PUT /api/custom-keywords/{id}
Content-Type: application/json

{
  "userId": 1,
  "keyword": "法定代表人"
}
```

#### 删除关键字
```
DELETE /api/custom-keywords/{id}?userId={userId}
```

#### 批量创建关键字
```
POST /api/custom-keywords/batch?userId={userId}
Content-Type: application/json

["法人", "项目名称", "合同编号", "负责人"]
```

### 2. 合同上传（增强版）

```
POST /contracts/upload
Content-Type: multipart/form-data

file: contract.pdf
userId: 1  (可选)
```

#### 响应示例
```json
{
  "contractId": 4,
  "totalAmount": 6000.00,
  "startDate": "2024-01-01",
  "endDate": "2024-06-30",
  "taxRate": 0.06,
  "vendorName": "测试供应商",
  "attachmentName": "contract.pdf",
  "createdAt": "2025-10-21T21:45:30+08:00",
  "message": "合同上传和解析成功",
  "customFields": {
    "法人": "张三",
    "项目名称": "云服务器租赁项目",
    "合同编号": "HT-2025-001"
  }
}
```

---

## 使用场景

### 场景1：财务人员提取项目信息

**步骤1：** 设置需要提取的字段
```bash
curl -X POST http://localhost:8081/api/custom-keywords \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "keyword": "项目名称"}'

curl -X POST http://localhost:8081/api/custom-keywords \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "keyword": "合同编号"}'
```

**步骤2：** 上传合同
```bash
curl -X POST http://localhost:8081/contracts/upload \
  -F "file=@contract.pdf" \
  -F "userId=1"
```

**结果：** 系统自动提取项目名称和合同编号

### 场景2：批量设置关键字

```bash
curl -X POST http://localhost:8081/api/custom-keywords/batch?userId=1 \
  -H "Content-Type: application/json" \
  -d '["法人", "项目名称", "合同编号", "负责人", "联系电话"]'
```

---

## 技术实现要点

### 1. 权限控制
- 每个用户只能管理自己的关键字
- 删除和更新操作需验证 `userId`
- 防止用户操作其他用户的关键字

### 2. 数据完整性
- 唯一索引确保同一用户不会有重复关键字
- 创建时自动检查关键字是否已存在
- 批量创建时自动过滤已存在的关键字

### 3. AI集成
- 自定义关键字以 `List<String>` 格式传递给AI
- AI返回 `Map<String, String>` 格式的提取结果
- 支持AI模块启用/禁用配置

### 4. 向后兼容
- `userId` 参数为可选参数
- 未提供 `userId` 时，系统正常工作，不提取自定义字段
- `customFields` 为 null 时，前端可正常处理

---

## 测试验证

### 功能测试
✅ 创建自定义关键字  
✅ 查询用户关键字列表  
✅ 更新关键字  
✅ 删除关键字  
✅ 批量创建关键字  
✅ 上传合同时自动获取用户关键字  
✅ AI提取自定义字段  
✅ 响应包含 customFields 数据  

### 集成测试
✅ MS模块启动成功  
✅ AI模块启动成功  
✅ 关键字管理API正常工作  
✅ 合同上传API正常工作  
✅ 自定义字段提取功能正常  

---

## 文档索引

1. **自定义关键字管理**: `/docs/18-custom-keywords-manage自定义关键字管理/README.md`
2. **合同上传（更新版）**: `/docs/03-contracts-upload合同上传/README.md`
3. **需求文档**: `/Requirement_new.md` (第192-202行)

---

## 后续优化建议

1. **关键字智能推荐**
   - 根据历史上传的合同，推荐常用关键字
   - 提供行业标准关键字模板

2. **提取结果验证**
   - 提供界面让用户验证和修正AI提取的结果
   - 收集用户反馈优化AI模型

3. **关键字分组**
   - 支持关键字分组管理（如：基本信息、财务信息、联系信息）
   - 便于批量应用关键字组

4. **提取历史记录**
   - 记录每次提取的结果
   - 支持查看历史提取记录

5. **导入导出**
   - 支持导出关键字配置
   - 支持从Excel导入关键字

---

## 更新日志

### 2025-10-21
- ✅ 创建 `CustomKeyword` 实体和表结构
- ✅ 实现关键字CRUD接口
- ✅ 集成到合同上传流程
- ✅ 更新 `ContractUploadResponse` 添加 `customFields`
- ✅ 完成功能测试
- ✅ 生成完整文档
