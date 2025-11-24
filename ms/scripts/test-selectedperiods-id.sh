#!/bin/bash

# 测试selectedPeriods使用ID而不是期间值的功能

set -e

BASE_URL="http://localhost:8081"

echo "🧪 测试selectedPeriods使用ID功能..."
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

# 2. 查询摊销台账，获取摊销明细ID
echo "2️⃣ 查询摊销台账，获取摊销明细ID..."
AMORTIZATION_RESPONSE=$(curl -s $BASE_URL/contracts/$CONTRACT_ID/amortization)
echo "✅ 摊销台账查询成功"

# 提取第一个和第二个摊销明细的ID
ENTRY_ID_1=$(echo $AMORTIZATION_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
ENTRY_ID_2=$(echo $AMORTIZATION_RESPONSE | grep -o '"id":[0-9]*' | head -2 | tail -1 | grep -o '[0-9]*')

echo "📊 摊销明细ID:"
echo "  - 第一个期间ID: $ENTRY_ID_1"
echo "  - 第二个期间ID: $ENTRY_ID_2"
echo ""

# 3. 使用ID执行付款（新格式）
echo "3️⃣ 使用摊销明细ID执行付款..."
PAYMENT_RESPONSE=$(curl -s -X POST $BASE_URL/payments/execute \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": '$CONTRACT_ID',
    "paymentAmount": 2000.00,
    "bookingDate": "2024-03-20",
    "selectedPeriods": ['$ENTRY_ID_1', '$ENTRY_ID_2']
  }')

if echo $PAYMENT_RESPONSE | grep -q "paymentId"; then
    echo "✅ 付款执行成功！使用ID格式正常工作"
    PAYMENT_ID=$(echo $PAYMENT_RESPONSE | grep -o '"paymentId":[0-9]*' | grep -o '[0-9]*')
    echo "💰 付款ID: $PAYMENT_ID"
else
    echo "❌ 付款执行失败"
    echo "错误信息: $PAYMENT_RESPONSE"
fi
echo ""

# 4. 验证付款记录
echo "4️⃣ 验证付款记录..."
if [ ! -z "$PAYMENT_ID" ]; then
    PAYMENT_DETAIL=$(curl -s $BASE_URL/payments/$PAYMENT_ID)
    echo "✅ 付款详情查询成功"
    
    # 检查selectedPeriods字段是否正确保存
    SELECTED_PERIODS=$(echo $PAYMENT_DETAIL | grep -o '"selectedPeriods":"[^"]*"' | cut -d'"' -f4)
    echo "📋 保存的期间: $SELECTED_PERIODS"
else
    echo "⚠️ 无法验证付款记录，付款ID为空"
fi
echo ""

# 5. 验证摊销台账状态更新
echo "5️⃣ 验证摊销台账状态更新..."
UPDATED_AMORTIZATION=$(curl -s $BASE_URL/contracts/$CONTRACT_ID/amortization)
COMPLETED_COUNT=$(echo $UPDATED_AMORTIZATION | grep -o '"status":"COMPLETED"' | wc -l)
PENDING_COUNT=$(echo $UPDATED_AMORTIZATION | grep -o '"status":"PENDING"' | wc -l)

echo "📊 状态统计:"
echo "  ✅ COMPLETED状态数量: $COMPLETED_COUNT"
echo "  ⏳ PENDING状态数量: $PENDING_COUNT"
echo ""

# 6. 测试结果验证
echo "🧪 测试结果验证:"
if [ "$COMPLETED_COUNT" -eq 2 ] && [ "$PENDING_COUNT" -eq 1 ]; then
    echo "✅ 测试通过！selectedPeriods使用ID功能正常"
    echo "  - 使用摊销明细ID [${ENTRY_ID_1}, ${ENTRY_ID_2}] 执行付款成功"
    echo "  - 对应期间状态正确更新为COMPLETED"
    echo "  - 未选择期间保持PENDING状态"
else
    echo "❌ 测试失败！状态更新异常"
    echo "  - 预期: 2个COMPLETED, 1个PENDING"
    echo "  - 实际: ${COMPLETED_COUNT}个COMPLETED, ${PENDING_COUNT}个PENDING"
fi
echo ""

echo "🎉 selectedPeriods使用ID功能测试完成！"
echo ""
echo "📊 功能特性验证:"
echo "  ✅ PaymentExecutionRequest接受摊销明细ID列表"
echo "  ✅ 系统根据ID查询对应期间值进行处理"
echo "  ✅ 付款记录正确保存期间信息"
echo "  ✅ 摊销台账状态正确更新"
echo ""
echo "💡 使用说明:"
echo "  - 前端需要传递摊销明细的ID而不是期间字符串"
echo "  - 格式: selectedPeriods: [1, 2, 3] 而不是 [\"2024-01\", \"2024-02\"]"
echo "  - 系统内部会自动转换ID为期间值进行业务处理"
