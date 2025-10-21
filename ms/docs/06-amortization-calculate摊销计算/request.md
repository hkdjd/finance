# 摊销计算接口请求示例

## GET请求
```bash
GET /amortization/calculate/1
```

## 路径参数
- `contractId`: 合同ID (Long类型)

## 请求示例
```bash
# 计算合同ID为1的摊销明细
curl http://localhost:8081/amortization/calculate/1

# 计算合同ID为2的摊销明细  
curl http://localhost:8081/amortization/calculate/2
```

## 说明
- 无需请求体，所有合同信息从数据库自动获取
- 使用GET方法，符合查询操作的RESTful设计
- 支持浏览器缓存，提高性能
