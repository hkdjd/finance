# 合同附件查看接口文档

## 接口信息
- **URL**: `/contracts/{contractId}/attachment`
- **Method**: GET
- **Description**: 根据合同ID查看和下载合同附件文件
- **序号**: 17

## 请求参数

### 路径参数
- **contractId** (Long): 合同ID，必填

### 查询参数
- **download** (Boolean): 是否下载文件，可选，默认false
  - `true`: 直接下载文件
  - `false`: 返回附件信息

## 响应格式

### 1. 查看附件信息 (download=false 或不传)
```json
{
  "contractId": 1,
  "attachment": {
    "id": 1,
    "fileName": "合同_供应商A_20240101.pdf",
    "originalFileName": "contract_vendor_a.pdf",
    "fileSize": 2048576,
    "contentType": "application/pdf",
    "uploadTime": "2024-01-01T10:30:00.123456",
    "uploadBy": "admin",
    "filePath": "/uploads/contracts/2024/01/contract_1_20240101.pdf",
    "downloadUrl": "/contracts/1/attachment?download=true"
  }
}
```

### 2. 查看/预览文件 (download=true)
- **Content-Type**: 根据文件类型设置 (如 application/pdf, image/jpeg 等)
- **Content-Disposition**: inline; filename="合同_供应商A_20240101.pdf"
- **响应体**: 文件二进制流
- **说明**: 使用 `inline` 模式，浏览器会直接在新标签页中打开PDF文件，而不是下载

## 字段说明

### 附件信息字段
- **id**: 附件ID
- **fileName**: 存储文件名（系统生成）
- **originalFileName**: 原始文件名（用户上传时的文件名）
- **fileSize**: 文件大小（字节）
- **contentType**: 文件MIME类型
- **uploadTime**: 上传时间
- **uploadBy**: 上传人
- **filePath**: 服务器存储路径
- **downloadUrl**: 下载链接

## 使用场景

### 业务流程
1. **合同管理**: 在合同详情页面查看附件信息
2. **文件预览**: 用户点击链接在浏览器新标签页中直接打开PDF文件
3. **审核流程**: 审核人员在线查看原始合同文件
4. **存档管理**: 财务人员可通过浏览器的保存功能下载合同进行存档

### 前端集成
```javascript
// 1. 获取附件信息
const getAttachmentInfo = async (contractId) => {
  const response = await fetch(`/contracts/${contractId}/attachment`);
  return await response.json();
};

// 2. 在新标签页中打开PDF文件（推荐方式）
const openAttachment = (contractId) => {
  window.open(`/contracts/${contractId}/attachment?download=true`, '_blank');
};

// 3. 如果需要强制下载，可以通过浏览器的保存功能
// 或者前端可以先获取文件流再触发下载
const forceDownloadAttachment = async (contractId, fileName) => {
  const response = await fetch(`/contracts/${contractId}/attachment?download=true`);
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
};
```

## 错误处理

### 常见错误码
- **404 Not Found**: 合同不存在或合同没有附件
- **403 Forbidden**: 没有权限访问该合同附件
- **500 Internal Server Error**: 文件读取失败或服务器错误

### 错误响应格式
```json
{
  "error": "CONTRACT_NOT_FOUND",
  "message": "合同不存在，ID=999",
  "timestamp": "2024-01-01T10:30:00.123456"
}
```

## 安全考虑

### 访问控制
- 验证用户是否有权限访问该合同
- 检查合同状态是否允许下载附件
- 记录文件下载日志

### 文件安全
- 验证文件路径，防止路径遍历攻击
- 检查文件是否存在且可读
- 限制文件下载频率，防止恶意下载

## 性能优化

### 文件传输
- 支持断点续传（Range请求）
- 设置适当的缓存头
- 对大文件进行流式传输

### 缓存策略
- 附件信息可以缓存一定时间
- 设置ETag支持条件请求
- 使用CDN加速文件下载

## 相关接口
- `POST /contracts/upload` - 上传合同文件
- `GET /contracts/{contractId}` - 查询合同信息
- `POST /contracts/operate` - 合同操作（可能包括附件更新）
