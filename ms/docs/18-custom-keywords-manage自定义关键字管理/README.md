# 自定义关键字管理接口

## 接口信息
- **Base URL**: `/api/custom-keywords`
- **Description**: 管理用户自定义的合同提取关键字，用于在上传合同时提取特定字段

## 1. 创建自定义关键字

### 接口信息
- **URL**: `/api/custom-keywords`
- **Method**: POST
- **Content-Type**: application/json

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |
| keyword | String | 是 | 关键字名称 |

### 请求示例
```json
{
  "userId": 1,
  "keyword": "法人"
}
```

### 响应示例
参考 `create-response.json` 文件

### 错误处理
- 关键字已存在: 400 Bad Request
- 用户ID为空: 400 Bad Request
- 关键字为空: 400 Bad Request

---

## 2. 获取用户的所有关键字

### 接口信息
- **URL**: `/api/custom-keywords/user/{userId}`
- **Method**: GET

### 路径参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

### 响应示例
参考 `list-response.json` 文件

---

## 3. 获取关键字字符串列表

### 接口信息
- **URL**: `/api/custom-keywords/user/{userId}/strings`
- **Method**: GET
- **Description**: 仅返回关键字字符串列表，用于传递给AI解析接口

### 路径参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

### 响应示例
```json
["法人", "项目名称", "合同编号", "负责人"]
```

---

## 4. 更新自定义关键字

### 接口信息
- **URL**: `/api/custom-keywords/{id}`
- **Method**: PUT
- **Content-Type**: application/json

### 路径参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 关键字ID |

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（用于权限校验） |
| keyword | String | 是 | 新的关键字名称 |

### 请求示例
```json
{
  "userId": 1,
  "keyword": "法定代表人"
}
```

### 响应示例
参考 `update-response.json` 文件

### 错误处理
- 关键字不存在: 400 Bad Request
- 无权限修改: 400 Bad Request
- 新关键字已存在: 400 Bad Request

---

## 5. 删除自定义关键字

### 接口信息
- **URL**: `/api/custom-keywords/{id}?userId={userId}`
- **Method**: DELETE

### 路径参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 关键字ID |

### 查询参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID（用于权限校验） |

### 响应示例
```json
{
  "success": true,
  "message": "关键字删除成功"
}
```

### 错误处理
- 关键字不存在: 400 Bad Request
- 无权限删除: 400 Bad Request

---

## 6. 批量创建自定义关键字

### 接口信息
- **URL**: `/api/custom-keywords/batch?userId={userId}`
- **Method**: POST
- **Content-Type**: application/json

### 查询参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

### 请求体
关键字字符串数组

### 请求示例
```json
["项目名称", "合同编号", "负责人", "联系电话"]
```

### 响应示例
参考 `batch-create-response.json` 文件

### 业务规则
- 自动过滤已存在的关键字
- 仅创建不存在的关键字
- 返回成功创建的关键字列表

---

## 数据库表结构

### custom_keywords 表
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户ID |
| keyword | VARCHAR(255) | 关键字 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| created_by | VARCHAR(255) | 创建人 |
| updated_by | VARCHAR(255) | 修改人 |

### 索引
- 主键: id
- 组合索引: (user_id, keyword) - 确保同一用户不会有重复关键字

---

## 业务流程

### 关键字管理流程
1. **用户创建关键字**: 定义需要从合同中提取的字段名称
2. **系统验证**: 检查关键字是否已存在
3. **保存到数据库**: 记录用户ID和关键字
4. **返回结果**: 返回创建成功的关键字信息

### 与合同上传的集成
1. **合同上传时**: 系统根据userId获取该用户的所有关键字
2. **传递给AI模块**: 将关键字列表作为自定义字段参数
3. **AI提取**: AI模块根据关键字提取相应的字段值
4. **返回结果**: 在customFields字段中返回提取结果

---

## 使用场景

### 场景1：财务人员需要提取项目信息
```bash
# 1. 创建关键字
POST /api/custom-keywords
{
  "userId": 1,
  "keyword": "项目名称"
}

# 2. 上传合同时自动提取
POST /contracts/upload?userId=1
file: contract.pdf

# 3. 响应包含提取的项目名称
{
  "customFields": {
    "项目名称": "云服务器租赁项目"
  }
}
```

### 场景2：批量设置多个关键字
```bash
POST /api/custom-keywords/batch?userId=1
["法人", "项目名称", "合同编号", "负责人", "联系电话"]
```

---

## 注意事项
1. 每个用户的关键字独立管理，互不影响
2. 关键字名称需与PDF中的字段名称匹配，提高提取准确率
3. 建议关键字名称简洁明确
4. 删除关键字不影响已上传合同的提取结果
5. 自定义字段的提取依赖于AI模块的解析能力
