# Mock合同数据使用说明

## 📋 概述

本系统已创建三组Mock合同数据用于测试摊销和付款会计分录生成功能。所有的摊销会计分录和付款会计分录均调用实际接口，非Mock数据。

## 🏢 Mock合同信息

### 合同1：史密斯净水器租赁合同
- **供应商**: 史密斯净水器租赁公司
- **摊销期间**: 2025年9月 至 2026年2月（6个月）
- **每月摊销金额**: ¥800.00
- **合同总金额**: ¥4,800.00
- **税率**: 13%

### 合同2：美的空调租赁协议
- **供应商**: 美的空调租赁有限公司
- **摊销期间**: 2025年10月 至 2026年3月（6个月）
- **每月摊销金额**: ¥300.00
- **合同总金额**: ¥1,800.00
- **税率**: 13%

### 合同3：海尔冰箱设备租赁合同
- **供应商**: 海尔冰箱设备租赁公司
- **摊销期间**: 2025年11月 至 2026年4月（6个月）
- **每月摊销金额**: ¥500.00
- **合同总金额**: ¥3,000.00
- **税率**: 13%

## 🚀 使用方法

### 1. 启动后端服务
```bash
cd /Users/victor/Desktop/ocbc/finance
mvn spring-boot:run
```

打开浏览器访问：`http://localhost:8081/mock-test.html`

### 3. 可用的测试功能

#### 生成摊销分录
- 点击“生成摊销分录”按钮
- 调用实际接口：`POST /api/mock-test/contracts/{contractId}/amortization-entries`
- 使用MockTestService处理业务逻辑，调用JournalEntryService生成摊销会计分录

#### 付第一个月
- 点击“付第一个月”按钮
- 调用实际接口：`POST /api/mock-test/contracts/{contractId}/payment/first-month`
- 使用MockTestService自动设置付款参数，调用PaymentService执行付款

#### 超额付款测试
- 设置超额付款金额（默认1500元）
- 点击“超额付款测试”按钮
- 调用实际接口：`POST /api/mock-test/contracts/{contractId}/payment/overpay`
- 使用MockTestService处理超额付款逻辑，测试非跨期超额付款

#### 跨期付款测试
- 设置跨期付款金额（默认2000元）和月数（默认3个月）
- 点击“跨期付款测试”按钮
- 调用实际接口：`POST /api/mock-test/contracts/{contractId}/payment/cross-period`
- 使用MockTestService处理跨期付款逻辑，测试预付转应付功能

##  API接口说明

### 获取所有Mock合同
```http
{{ ... }}
```
- **处理类**：MockTestService.getAllMockContracts()
- **返回**：所有Mock合同列表

### 生成摊销会计分录
```http
POST /api/mock-test/contracts/{contractId}/amortization-entries
```
- **处理类**：MockTestService.generateAmortizationEntries()
- **调用**：JournalEntryService.generateJournalEntries()
- **返回**：摊销会计分录列表

### 执行第一个月付款
```http
POST /api/mock-test/contracts/{contractId}/payment/first-month
```
- **处理类**：MockTestService.executeFirstMonthPayment()
- **调用**：PaymentService.executePayment()
- **返回**：付款执行结果

### 超额付款测试
```http
POST /api/mock-test/contracts/{contractId}/payment/overpay?amount={amount}
```
- **处理类**：MockTestService.testOverpayment()
- **调用**：PaymentService.preview()
- **返回**：付款预览结果

### 跨期付款测试
```http
POST /api/mock-test/contracts/{contractId}/payment/cross-period?amount={amount}&monthCount={monthCount}
```
- **处理类**：MockTestService.testCrossPeriodPayment()
- **调用**：PaymentService.preview()
- **返回**：跨期付款预览结果

## 📊 测试场景

### 场景1：正常付款
- 选择任一合同，点击"付第一个月"
- 验证会计分录是否正确生成
- 检查借贷平衡

### 场景2：非跨期超额付款
- 设置超额付款金额（如史密斯合同设置1200元）
- 验证超额部分是否直接记入费用科目
- 确认不生成预付科目

### 场景3：跨期付款
- 设置跨期付款金额和月数
- 验证预付转应付分录是否按期间顺序生成
- 检查预付金额不足时的费用补偿逻辑

### 场景4：借贷平衡检查
- 执行任意付款操作
- 查看响应中的balanceCheck字段
- 验证借贷平衡计算是否正确

## 🎯 测试重点

1. **会计分录生成规则**
   - 借方：应付（对应预提费用）、预付（对应未来期间）、费用（差异调整）
   - 贷方：活期存款（实际付款金额）、费用（差异调整）

2. **入账日期变化**
   - 付款日期晚于应付入账日期，则入账日期为付款日期
   - 付款日期早于应付入账日期，则使用原始入账日期

3. **跨期付款处理**
   - 预付转应付按期间顺序处理
   - 借方始终使用预摊金额
   - 预付金额不足时用费用补偿

4. **非跨期超额付款**
   - 超出金额直接作为借方费用
   - 不生成预付科目

5. **借贷平衡检查**
   - 实时计算借贷平衡
   - 自动调整不平衡分录

## 🔍 数据初始化

Mock数据通过`MockDataInitializer`类在应用启动时自动初始化。如果需要重新初始化数据：

1. 停止应用
2. 删除H2数据库文件（如果使用H2）
3. 重新启动应用

## 🔍 架构优化

### 代码结构改进
- **分离关注点**：将业务逻辑从 Controller 移到 MockTestService
- **提高可维护性**：Controller 只负责 HTTP 请求处理，Service 负责业务逻辑
- **增强可测试性**：业务逻辑集中在 Service 中，便于单元测试
- **代码复用**：公共逻辑在 Service 中可被多个 Controller 使用

### MockTestService 功能
- **合同管理**：获取和验证Mock合同信息
- **参数设置**：根据合同类型自动设置付款参数
- **业务调用**：封装对实际业务Service的调用
- **错误处理**：统一的异常处理和错误信息

## 📝 注意事项

1. **真实业务调用**：所有会计分录生成均调用实际业务接口
2. **数据隔离**：Mock数据仅用于测试，不影响生产环境
3. **用户体验**：测试页面提供直观的操作界面和结果展示
4. **数据精度**：所有金额计算保留2位小数，使用HALF_UP舍入模式
5. **自动检查**：系统会自动进行借贷平衡检查和调整
6. **代码质量**：遵循单一职责原则，提高代码可维护性

## 🚨 故障排除

如果遇到问题：

1. 确认后端服务已正常启动（端口8081）
2. 检查控制台是否有错误日志
3. 验证Mock数据是否已正确初始化
4. 确认前端页面能正常访问后端API

---

**Happy Testing! 🎉**
