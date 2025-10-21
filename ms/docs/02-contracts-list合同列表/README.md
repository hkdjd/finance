# 查询合同列表接口

## 接口信息
- **URL**: `/contracts`
- **Method**: GET
- **Description**: 查询所有合同列表，显示合同摘要信息

## 请求参数
无需参数

## 响应数据
参考 `response.json` 文件

## 分页查询接口
- **URL**: `/contracts/list`
- **Method**: GET
- **Query Parameters**:
  - page: 页码，从0开始，默认0
  - size: 每页大小，默认10

## 响应字段说明
- `contracts`: 合同列表数组
  - `contractId`: 合同ID
  - `totalAmount`: 合同总金额
  - `startDate`: 合同开始时间
  - `endDate`: 合同结束时间
  - `vendorName`: 供应商名称
  - `attachmentName`: 合同附件名称
  - `createdAt`: 创建时间
  - `status`: 合同状态（ACTIVE等）
- `totalCount`: 总数量
- `message`: 操作消息

## 使用场景
- 合同列表页面展示
- 合同选择下拉框数据源
- 合同管理和查询功能

## 排序规则
- 按创建时间倒序排列（最新的在前）

## 错误处理
- 数据库查询异常: 500 Internal Server Error
