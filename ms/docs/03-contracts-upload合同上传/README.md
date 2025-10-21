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

## 支持的文件类型
- PDF: .pdf
- Word文档: .doc, .docx  
- 图片: .jpg, .jpeg, .png

## 文件大小限制
- 最大文件大小: 10MB

## 业务流程
1. **文件上传**: 将合同附件保存到配置的指定目录
2. **外部接口调用**: 调用外部系统解析合同内容
3. **数据保存**: 将解析结果保存到合同信息表
4. **返回响应**: 返回合同信息给前端

## 响应数据
参考 `response.json` 文件

## 错误处理
- 文件为空: 400 Bad Request
- 文件类型不支持: 400 Bad Request  
- 文件大小超限: 400 Bad Request
- 外部接口调用失败: 500 Internal Server Error
- 数据库保存失败: 500 Internal Server Error

## 注意事项
- 外部接口可通过配置启用/禁用
- 禁用时使用模拟数据进行开发测试
- 文件保存路径可通过配置文件修改
