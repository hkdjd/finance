# 编辑合同信息接口

## 接口信息
- **URL**: `/contracts/{contractId}`
- **Method**: PUT
- **Content-Type**: application/json
- **Description**: 编辑合同信息，支持前端修改外部接口解析的结果

## 路径参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractId | Long | 是 | 合同ID |

## 请求体参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| totalAmount | BigDecimal | 是 | 合同总金额，必须大于0 |
| startDate | String | 是 | 合同开始时间，格式：yyyy-MM-dd |
| endDate | String | 是 | 合同结束时间，格式：yyyy-MM-dd |
| taxRate | BigDecimal | 是 | 税率，如0.06表示6% |
| vendorName | String | 是 | 供应商名称，不能为空 |

## 请求示例
参考 `request.json` 文件

## 响应数据
参考 `response.json` 文件

## 验证规则
- totalAmount: 必须大于0
- startDate: 必须是有效的日期格式
- endDate: 必须是有效的日期格式，且不能早于startDate
- taxRate: 必须是有效的数值
- vendorName: 不能为空或空白字符串

## 错误处理
- 合同不存在: 400 Bad Request
- 参数验证失败: 400 Bad Request
- 日期格式错误: 400 Bad Request
- 数据库更新失败: 500 Internal Server Error

## 业务场景
- 外部接口解析结果有误，需要人工修正
- 合同条款发生变更，需要更新信息
- 数据质量检查后的批量修正
