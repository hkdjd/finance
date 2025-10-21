-- 为摊销条目表添加付款备注字段
-- 用于标记挂账等特殊付款情况

ALTER TABLE amortization_entries 
ADD COLUMN payment_remark VARCHAR(500) NULL 
COMMENT '付款备注，用于标记挂账等特殊情况';

-- 为现有数据创建索引以提高查询性能
CREATE INDEX idx_amortization_entries_payment_remark ON amortization_entries(payment_remark);
CREATE INDEX idx_amortization_entries_status_remark ON amortization_entries(payment_status, payment_remark);
