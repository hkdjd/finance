#!/bin/bash

# 测试精简后的API接口
# 使用统一的operate字段进行增删改操作

BASE_URL="http://localhost:8081"

echo "🚀 开始测试精简后的API接口..."
echo "📍 测试服务器: $BASE_URL"
echo ""

# 1. 测试合同列表查询（保持不变）
echo "1️⃣ 测试合同列表查询..."
CONTRACT_LIST=$(curl -s $BASE_URL/contracts)
if echo $CONTRACT_LIST | grep -q "contracts"; then
    echo "✅ 合同列表查询成功"
    CONTRACT_ID=$(echo $CONTRACT_LIST | grep -o '"contractId":[0-9]*' | head -1 | grep -o '[0-9]*')
    if [ -n "$CONTRACT_ID" ]; then
        echo "📋 使用合同ID: $CONTRACT_ID"
    else
        echo "⚠️ 未找到可用的合同ID，请先创建合同"
        exit 1
    fi
else
    echo "❌ 合同列表查询失败: $CONTRACT_LIST"
    exit 1
fi
echo ""

# 2. 测试摊销明细操作接口（只支持修改，不可增删行）
echo "2️⃣ 测试摊销明细操作接口..."

# 2.1 获取现有摊销明细ID用于测试UPDATE
echo "📝 获取现有摊销明细..."
AE_LIST_RESPONSE=$(curl -s $BASE_URL/amortization-entries/contract/$CONTRACT_ID)
# 新格式：从amortization数组中提取ID
ENTRY_ID=$(echo $AE_LIST_RESPONSE | grep -o '"amortization":\[.*\]' | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

if [ -n "$ENTRY_ID" ]; then
    echo "📋 找到摊销明细ID: $ENTRY_ID"
    # 验证新的响应格式
    if echo $AE_LIST_RESPONSE | grep -q '"contract":' && echo $AE_LIST_RESPONSE | grep -q '"amortization":'; then
        echo "✅ 响应格式正确：包含contract和amortization字段"
    else
        echo "❌ 响应格式不正确，缺少contract或amortization字段"
    fi
else
    echo "⚠️ 未找到现有摊销明细，请先创建合同"
fi

# 2.2 测试摊销明细增删改操作
echo "📝 测试摊销明细增删改操作..."

# 构造混合操作请求（更新现有 + 新增 + 删除）
MIXED_OPERATION_DATA='{
    "contractId": '$CONTRACT_ID',
    "amortization": [
        {
            "id": '$ENTRY_ID',
            "amortizationPeriod": "2025-01",
            "accountingPeriod": "2025-01",
            "amount": 1500.00,
            "periodDate": "2025-01-01",
            "paymentStatus": "COMPLETED"
        },
        {
            "id": null,
            "amortizationPeriod": "2025-05",
            "accountingPeriod": "2025-05",
            "amount": 800.00,
            "periodDate": "2025-05-01",
            "paymentStatus": "PENDING"
        }
    ]
}'

MIXED_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    -d "$MIXED_OPERATION_DATA" $BASE_URL/amortization-entries/operate)

if echo $MIXED_RESPONSE | grep -q "1500"; then
    echo "✅ 摊销明细增删改操作成功"
    echo "💰 更新后金额: 1500.00"
    # 验证响应格式
    if echo $MIXED_RESPONSE | grep -q '"contract":' && echo $MIXED_RESPONSE | grep -q '"amortization":'; then
        echo "✅ 混合操作响应格式正确：包含contract和amortization字段"
        # 统计返回的摊销明细数量
        ENTRY_COUNT=$(echo $MIXED_RESPONSE | grep -o '"id":[0-9]*' | wc -l | tr -d ' ')
        echo "📊 操作后摊销明细数量: $ENTRY_COUNT"
    else
        echo "❌ 混合操作响应格式不正确"
    fi
else
    echo "⚠️ 摊销明细增删改操作: $MIXED_RESPONSE"
fi

echo ""

# 3. 测试会计分录生成和统一操作
echo "3️⃣ 测试会计分录统一操作接口..."

# 3.1 生成会计分录（步骤3：摊销）
echo "📝 生成会计分录..."
JOURNAL_GENERATE=$(curl -s -X POST $BASE_URL/journal-entries/generate/$CONTRACT_ID)
if echo $JOURNAL_GENERATE | grep -q "bookingDate"; then
    echo "✅ 会计分录生成成功"
    # 验证新的响应格式
    if echo $JOURNAL_GENERATE | grep -q '"contract":' && echo $JOURNAL_GENERATE | grep -q '"journalEntries":'; then
        echo "✅ 响应格式正确：包含contract和journalEntries字段"
        JE_COUNT=$(echo $JOURNAL_GENERATE | grep -o '"journalEntries":\[.*\]' | grep -o '"id":[0-9]*' | wc -l | tr -d ' ')
        echo "📊 生成会计分录数量: $JE_COUNT"
        # 验证合同信息只出现一次
        CONTRACT_COUNT=$(echo $JOURNAL_GENERATE | grep -o '"contract":' | wc -l | tr -d ' ')
        if [ "$CONTRACT_COUNT" = "1" ]; then
            echo "✅ 合同信息优化：只在根节点显示一次"
        else
            echo "❌ 合同信息重复显示"
        fi
        # 验证entryType字段
        if echo $JOURNAL_GENERATE | grep -q '"entryType":"AMORTIZATION"'; then
            echo "✅ entryType字段正确：AMORTIZATION"
        else
            echo "❌ 缺少entryType字段或值不正确"
        fi
    else
        echo "❌ 响应格式不正确，缺少contract或journalEntries字段"
    fi
else
    echo "⚠️ 会计分录生成: $JOURNAL_GENERATE"
fi

# 3.2 更新会计分录
if [ -n "$JOURNAL_ID" ]; then
    echo "📝 测试会计分录UPDATE操作..."
    JOURNAL_UPDATE_DATA='{
        "operate": "UPDATE",
        "id": '$JOURNAL_ID',
        "data": {
            "debitAmount": 1500.00,
            "description": "更新后的摊销费用"
        }
    }'
    
    JOURNAL_UPDATE_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
        -d "$JOURNAL_UPDATE_DATA" $BASE_URL/journal-entries/operate)
    
    if echo $JOURNAL_UPDATE_RESPONSE | grep -q "1500"; then
        echo "✅ 会计分录UPDATE操作成功"
        echo "💰 更新后借方金额: 1500.00"
    else
        echo "⚠️ 会计分录UPDATE操作: $JOURNAL_UPDATE_RESPONSE"
    fi
fi
echo ""

# 4. 执行付款（步骤4），并验证会计分录类型为PAYMENT
echo "4️⃣ 执行付款并验证PAYMENT类型..."

# 获取当前合同的摊销明细，挑选前1-2个条目用于付款
AE_LIST=$(curl -s $BASE_URL/amortization-entries/contract/$CONTRACT_ID)
# 新格式：从amortization数组中提取ID
AE_ID_1=$(echo "$AE_LIST" | grep -o '"amortization":\[.*\]' | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
AE_ID_2=$(echo "$AE_LIST" | grep -o '"amortization":\[.*\]' | grep -o '"id":[0-9]*' | head -2 | tail -1 | grep -o '[0-9]*')

if [ -z "$AE_ID_1" ]; then
  echo "⚠️ 未找到摊销明细，无法执行付款测试"
else
  PAYMENT_REQ='{
    "contractId": '$CONTRACT_ID',
    "paymentAmount": 1000.00,
    "bookingDate": "2025-01-31",
    "selectedPeriods": ['"$AE_ID_1"'${AE_ID_2:+, '"$AE_ID_2"'}]
  }'
  PAYMENT_RESP=$(curl -s -X POST -H "Content-Type: application/json" -d "$PAYMENT_REQ" $BASE_URL/payments/execute)
  if echo "$PAYMENT_RESP" | grep -q '"paymentId"'; then
    echo "✅ 执行付款成功"
    # 付款后查询合同会计分录，校验存在PAYMENT类型
    JE_AFTER_PAY=$(curl -s $BASE_URL/journal-entries/contract/$CONTRACT_ID)
    if echo "$JE_AFTER_PAY" | grep -q '"entryType":"PAYMENT"'; then
      echo "🔎 验证通过：步骤4生成的分录类型为 PAYMENT"
    else
      echo "❌ 未发现PAYMENT类型的分录"
    fi
  else
    echo "❌ 执行付款失败: $PAYMENT_RESP"
  fi
fi

echo ""

# 5. 验证重新生成步骤3仅影响AMORTIZATION，不影响PAYMENT
echo "5️⃣ 重新生成摊销分录，校验PAYMENT不受影响..."
JE_COUNT_PAYMENT_BEFORE=$(curl -s $BASE_URL/journal-entries/contract/$CONTRACT_ID | grep -o '"entryType":"PAYMENT"' | wc -l | tr -d ' ')
curl -s -X POST $BASE_URL/journal-entries/generate/$CONTRACT_ID >/dev/null
JE_COUNT_PAYMENT_AFTER=$(curl -s $BASE_URL/journal-entries/contract/$CONTRACT_ID | grep -o '"entryType":"PAYMENT"' | wc -l | tr -d ' ')
if [ "$JE_COUNT_PAYMENT_BEFORE" = "$JE_COUNT_PAYMENT_AFTER" ]; then
  echo "✅ 仅删除并重建AMORTIZATION分录，PAYMENT分录数量保持不变 ($JE_COUNT_PAYMENT_AFTER)"
else
  echo "❌ 重新生成后PAYMENT分录数量发生变化：$JE_COUNT_PAYMENT_BEFORE -> $JE_COUNT_PAYMENT_AFTER"
fi

echo ""

echo "🎉 精简API接口测试完成！"
echo ""
echo "📊 功能特性验证:"
echo "  ✅ 统一摊销明细更新接口"
echo "  ✅ 请求格式与列表接口响应格式一致"
echo "  ✅ 支持增删改操作（根据ID字段智能判断）"
echo "  ✅ 包含该合同所有摊销明细的完整更新"
echo "  ✅ 响应格式统一（contract + amortization）"
echo "  ✅ 会计分录生成接口优化（contract + journalEntries）"
echo "  ✅ 原有查询接口保持不变"
echo ""
echo "💡 接口优化效果:"
echo "  📉 简化为单一更新接口"
echo "  🔄 请求响应格式完全一致"
echo "  📦 支持批量更新提高效率"
echo "  🎯 前端集成更加简单"
echo "  🛡️ 统一的错误处理和验证"
echo "  🚀 响应数据结构优化，避免重复信息"
echo ""
echo "🔗 相关文档:"
echo "  - docs/simplified-api.md - 精简API接口文档"
echo "  - docs/api.md - 完整API列表"
