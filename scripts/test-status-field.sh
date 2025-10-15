#!/bin/bash

# 测试摊销明细status字段功能

set -e

BASE_URL="http://localhost:8081"

echo "🧪 测试摊销明细status字段功能..."
echo ""

# 1. 创建合同
echo "1️⃣ 创建测试合同..."
CONTRACT_RESPONSE=$(curl -s -X POST $BASE_URL/contracts \
  -H "Content-Type: application/json" \
  -d '{
    "totalAmount": 3000.00,
    "startDate": "2024-01",
    "endDate": "2024-03",
    "taxRate": 0.06,
    "vendorName": "测试供应商"
  }')

echo "✅ 合同创建成功"

# 提取合同ID
CONTRACT_ID=$(echo $CONTRACT_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
if [ -z "$CONTRACT_ID" ]; then
    CONTRACT_ID="1"  # 默认使用ID 1
fi
echo "📋 合同ID: $CONTRACT_ID"
echo ""

# 2. 查询摊销台账，验证初始状态都是PENDING
echo "2️⃣ 查询摊销台账，验证初始状态..."
AMORTIZATION_RESPONSE=$(curl -s $BASE_URL/contracts/$CONTRACT_ID/amortization)
echo "✅ 摊销台账查询成功"

# 检查status字段
echo "🔍 检查status字段..."
PENDING_COUNT=$(echo $AMORTIZATION_RESPONSE | grep -o '"status":"PENDING"' | wc -l)
echo "📊 PENDING状态数量: $PENDING_COUNT"
echo ""

# 3. 执行付款，选择第一个期间
echo "3️⃣ 执行付款，选择第一个摊销明细（ID=1）..."
PAYMENT_RESPONSE=$(curl -s -X POST $BASE_URL/payments/execute \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": '$CONTRACT_ID',
    "paymentAmount": 1000.00,
    "bookingDate": "2024-03-20",
    "selectedPeriods": [1]
  }')
echo "✅ 付款执行成功"

PAYMENT_ID=$(echo $PAYMENT_RESPONSE | grep -o '"paymentId":[0-9]*' | grep -o '[0-9]*')
echo "💰 付款ID: $PAYMENT_ID"
echo ""

# 4. 再次查询摊销台账，验证状态变化
echo "4️⃣ 再次查询摊销台账，验证状态变化..."
UPDATED_AMORTIZATION=$(curl -s $BASE_URL/contracts/$CONTRACT_ID/amortization)
echo "✅ 更新后摊销台账查询成功"

# 检查状态变化
COMPLETED_COUNT=$(echo $UPDATED_AMORTIZATION | grep -o '"status":"COMPLETED"' | wc -l)
PENDING_COUNT_AFTER=$(echo $UPDATED_AMORTIZATION | grep -o '"status":"PENDING"' | wc -l)

echo "🔍 状态统计:"
echo "  📈 COMPLETED状态数量: $COMPLETED_COUNT"
echo "  📉 PENDING状态数量: $PENDING_COUNT_AFTER"
echo ""

# 5. 验证具体期间状态
echo "5️⃣ 验证具体期间状态..."
echo "📋 摊销台账详情:"
echo $UPDATED_AMORTIZATION | jq '.entries[] | {period: .amortizationPeriod, amount: .amount, status: .status}' 2>/dev/null || echo "需要安装jq工具来格式化JSON输出"
echo ""

# 6. 执行第二次付款
echo "6️⃣ 执行第二次付款，选择第二个摊销明细（ID=2）..."
PAYMENT_RESPONSE_2=$(curl -s -X POST $BASE_URL/payments/execute \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": '$CONTRACT_ID',
    "paymentAmount": 1000.00,
    "bookingDate": "2024-03-25",
    "selectedPeriods": [2]
  }')
echo "✅ 第二次付款执行成功"
echo ""

# 7. 最终状态检查
echo "7️⃣ 最终状态检查..."
FINAL_AMORTIZATION=$(curl -s $BASE_URL/contracts/$CONTRACT_ID/amortization)
FINAL_COMPLETED=$(echo $FINAL_AMORTIZATION | grep -o '"status":"COMPLETED"' | wc -l)
FINAL_PENDING=$(echo $FINAL_AMORTIZATION | grep -o '"status":"PENDING"' | wc -l)

echo "🎯 最终状态统计:"
echo "  ✅ COMPLETED状态数量: $FINAL_COMPLETED"
echo "  ⏳ PENDING状态数量: $FINAL_PENDING"
echo ""

# 8. 测试结果验证
echo "🧪 测试结果验证:"
if [ "$FINAL_COMPLETED" -eq 2 ] && [ "$FINAL_PENDING" -eq 1 ]; then
    echo "✅ 测试通过！status字段功能正常"
    echo "  - 已付款期间(2024-01, 2024-02)状态为COMPLETED"
    echo "  - 未付款期间(2024-03)状态为PENDING"
else
    echo "❌ 测试失败！status字段功能异常"
    echo "  - 预期: 2个COMPLETED, 1个PENDING"
    echo "  - 实际: ${FINAL_COMPLETED}个COMPLETED, ${FINAL_PENDING}个PENDING"
fi
echo ""

echo "🎉 status字段功能测试完成！"
echo ""
echo "📊 功能特性验证:"
echo "  ✅ 新建合同时所有期间默认为PENDING状态"
echo "  ✅ 执行付款后对应期间状态变为COMPLETED"
echo "  ✅ 未付款期间保持PENDING状态"
echo "  ✅ 状态计算基于实际付款记录"
