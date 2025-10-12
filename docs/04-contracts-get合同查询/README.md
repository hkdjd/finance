# 查询合同信息接口

## 接口信息
- **URL**: `/contracts/{contractId}`
- **Method**: GET
- **Description**: 查询指定合同的详细信息

## 路径参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractId | Long | 是 | 合同ID |

## 响应数据
参考 `response.json` 文件

## 错误处理
- 合同不存在: 400 Bad Request
- 参数格式错误: 400 Bad Request

## 使用场景
- 前端页面显示合同详情
- 编辑合同前获取当前信息
- 业务流程中的合同信息确认
