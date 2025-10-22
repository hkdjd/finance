# 合同上传接口

## 接口信息
- **URL**: `/contracts/upload`
- **Method**: POST
- **Content-Type**: multipart/form-data
- **Description**: 上传合同文件，调用外部接口解析获得合同内容（步骤1）

## 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 合同文件 |
| userId | Long | 否 | 用户ID，用于获取该用户的自定义关键字（未传入时默认使用admin用户ID=1） |

## 支持的文件类型
- PDF: .pdf
- Word文档: .doc, .docx  
- 图片: .jpg, .jpeg, .png

## 文件大小限制
- 最大文件大小: 10MB

## 业务流程
1. **文件上传**: 将合同附件保存到配置的指定目录
2. **获取自定义关键字**: 如果提供了userId，则获取该用户的自定义关键字列表
3. **外部接口调用**: 调用外部AI系统解析合同内容，传递自定义关键字
4. **标准字段解析**: AI提取合同的标准字段（总金额、开始日期、结束日期、税率、供应商名称）
5. **自定义字段提取**: AI根据自定义关键字提取对应的字段值
6. **数据保存**: 将解析结果保存到合同信息表
7. **返回响应**: 返回合同信息（包含标准字段和自定义字段）给前端

## 响应数据
参考 `response.json` 文件

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| contractId | Long | 合同ID |
| totalAmount | BigDecimal | 合同总金额 |
| startDate | String | 合同开始日期 (格式: yyyy-MM-dd) |
| endDate | String | 合同结束日期 (格式: yyyy-MM-dd) |
| taxRate | BigDecimal | 税率 |
| vendorName | String | 供应商名称 |
| attachmentName | String | 合同附件名称 |
| attachmentPath | String | 合同附件存放路径（服务器文件系统路径） |
| createdAt | String | 创建时间 (ISO 8601格式) |
| message | String | 操作消息 |
| customFields | Map<String, String> | 自定义字段提取结果（可选） |

## 错误处理
- 文件为空: 400 Bad Request
- 文件类型不支持: 400 Bad Request  
- 文件大小超限: 400 Bad Request
- 外部接口调用失败: 500 Internal Server Error
- 数据库保存失败: 500 Internal Server Error

## 自定义字段提取功能

### 功能说明
- 用户可以通过自定义关键字管理接口（`/api/custom-keywords`）预先设置需要提取的字段
- 上传合同时，系统会自动获取用户的自定义关键字并传递给AI模块
- AI模块根据关键字从合同中提取对应的字段值
- 提取结果在响应的 `customFields` 字段中返回

### 使用示例

#### 1. 创建自定义关键字
```bash
POST /api/custom-keywords
{
  "userId": 1,
  "keyword": "法人"
}
```

#### 2. 上传合同（带userId）
```bash
POST /contracts/upload
file: contract.pdf
userId: 1
```

#### 3. 响应包含自定义字段
```json
{
  "contractId": 4,
  "totalAmount": 6000.00,
  "customFields": {
    "法人": "张三"
  }
}
```

### 字段说明
- **customFields**: Map<String, String>类型
  - Key: 自定义关键字名称
  - Value: AI从合同中提取的对应字段值
- 如果AI无法提取某个字段，该字段可能不会出现在customFields中
- 如果未提供userId或用户没有自定义关键字，customFields为null

## 注意事项
- **默认用户ID**: 未传入userId时，默认使用admin用户ID（值为1）
- 外部接口可通过配置启用/禁用
- 禁用时使用模拟数据进行开发测试
- 文件保存路径可通过配置文件修改
- 自定义字段的提取准确率取决于AI模块的解析能力和关键字的准确性
- 建议关键字名称与PDF中的字段名称保持一致
