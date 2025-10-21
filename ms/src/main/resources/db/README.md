# 数据库建表脚本

本目录包含财务系统的PostgreSQL数据库建表脚本。

## 📋 脚本列表

### 单表建表脚本
- `contracts_init.sql` - 合同表
- `amortization_entries_init.sql` - 摊销明细表
- `payments_init.sql` - 付款表
- `journal_entries_init.sql` - 会计分录表

### 批量执行脚本
- `init_all_tables.sql` - 按依赖顺序执行所有建表脚本

## 🏗️ 表结构设计

### 1. contracts (合同表)
**主要字段:**
- `id` - 主键ID
- `total_amount` - 合同总金额
- `start_date` - 合同开始日期
- `end_date` - 合同结束日期
- `vendor_name` - 供应商名称
- `tax_rate` - 税率
- 审计字段 (created_at, updated_at, created_by, updated_by)

### 2. amortization_entries (摊销明细表)
**主要字段:**
- `id` - 主键ID
- `contract_id` - 关联合同ID (外键)
- `amortization_period` - 摊销期间 (yyyy-MM)
- `accounting_period` - 入账期间 (yyyy-MM)
- `amount` - 摊销金额
- `period_date` - 期间日期 (用于排序)
- 审计字段

### 3. payments (付款表)
**主要字段:**
- `id` - 主键ID
- `contract_id` - 关联合同ID (外键)
- `payment_amount` - 付款金额
- `booking_date` - 记账日期
- `selected_periods` - 选择的付款期间 (逗号分隔)
- `status` - 付款状态 (DRAFT/CONFIRMED/CANCELLED)
- 审计字段

### 4. journal_entries (会计分录表)
**主要字段:**
- `id` - 主键ID
- `payment_id` - 关联付款ID (外键)
- `booking_date` - 记账日期
- `account_name` - 会计科目名称
- `debit_amount` - 借方金额
- `credit_amount` - 贷方金额
- `memo` - 备注
- `entry_order` - 分录顺序
- 审计字段

## 🔗 表关系

```
contracts (1) -----> (*) amortization_entries
    |
    |
    v
payments (1) -----> (*) journal_entries
```

## 🚀 使用方法

### 方法1: 执行单个表脚本
```sql
-- 连接到PostgreSQL数据库
psql -U username -d database_name

-- 执行单个表脚本
\i contracts_init.sql
\i amortization_entries_init.sql
\i payments_init.sql
\i journal_entries_init.sql
```

### 方法2: 批量执行所有脚本
```sql
-- 执行主脚本 (推荐)
\i init_all_tables.sql
```

### 方法3: 命令行执行
```bash
# 执行主脚本
psql -U username -d database_name -f init_all_tables.sql

# 或者执行单个脚本
psql -U username -d database_name -f contracts_init.sql
```

## 📊 索引设计

每个表都包含以下类型的索引：
- **主键索引** - 自动创建
- **外键索引** - 提高关联查询性能
- **业务字段索引** - 常用查询字段
- **复合索引** - 多字段组合查询
- **时间索引** - 审计字段和日期字段

## ✅ 约束设计

### 外键约束
- `amortization_entries.contract_id` → `contracts.id`
- `payments.contract_id` → `contracts.id`
- `journal_entries.payment_id` → `payments.id`

### 检查约束
- `payments.status` - 只能是 DRAFT/CONFIRMED/CANCELLED
- `payments.payment_amount` - 必须大于0
- `journal_entries.debit_amount, credit_amount` - 必须非负
- `journal_entries` - 借方或贷方至少有一个大于0

## 🔧 数据库配置建议

### 连接配置
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finance2
spring.datasource.username=finance2_user
spring.datasource.password=finance2_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 性能优化
- 定期执行 `ANALYZE` 更新统计信息
- 根据查询模式调整索引
- 监控慢查询日志
- 适当设置连接池大小

## 📝 注意事项

1. **执行顺序** - 必须按照表依赖关系执行脚本
2. **权限要求** - 需要CREATE TABLE权限
3. **编码设置** - 建议使用UTF8编码
4. **时区设置** - 建议设置为Asia/Shanghai
5. **备份策略** - 生产环境执行前请备份数据库

## 🔍 验证脚本

执行完建表脚本后，可以使用以下SQL验证：

```sql
-- 检查表是否创建成功
SELECT tablename FROM pg_tables 
WHERE schemaname = 'public' 
    AND tablename IN ('contracts', 'amortization_entries', 'payments', 'journal_entries');

-- 检查外键约束
SELECT conname, conrelid::regclass, confrelid::regclass 
FROM pg_constraint 
WHERE contype = 'f' 
    AND conrelid::regclass::text IN ('amortization_entries', 'payments', 'journal_entries');

-- 检查索引
SELECT indexname, tablename 
FROM pg_indexes 
WHERE schemaname = 'public' 
    AND tablename IN ('contracts', 'amortization_entries', 'payments', 'journal_entries')
ORDER BY tablename, indexname;
```
