-- 为会计分录表添加支付操作时间戳字段
-- 用于区分不同批次的分录生成时间

ALTER TABLE journal_entries 
ADD COLUMN payment_timestamp TIMESTAMP NULL 
COMMENT '支付操作时间戳，用于区分不同批次的分录';

-- 为现有的付款类型分录设置时间戳（基于创建时间）
UPDATE journal_entries 
SET payment_timestamp = created_at 
WHERE entry_type = 'PAYMENT' AND payment_timestamp IS NULL;

-- 为现有数据创建索引以提高查询性能
CREATE INDEX idx_journal_entries_payment_timestamp ON journal_entries(payment_timestamp);
CREATE INDEX idx_journal_entries_entry_type_payment_timestamp ON journal_entries(entry_type, payment_timestamp);
