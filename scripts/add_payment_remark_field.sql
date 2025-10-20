-- 手动添加 payment_remark 字段的脚本
-- 如果字段不存在则添加，如果已存在则跳过

-- 检查字段是否存在，如果不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'amortization_entries' 
        AND column_name = 'payment_remark'
    ) THEN
        ALTER TABLE amortization_entries 
        ADD COLUMN payment_remark VARCHAR(500) NULL;
        
        -- 添加注释
        COMMENT ON COLUMN amortization_entries.payment_remark IS '付款备注，用于标记挂账等特殊情况';
        
        -- 创建索引
        CREATE INDEX IF NOT EXISTS idx_amortization_entries_payment_remark 
        ON amortization_entries(payment_remark);
        
        CREATE INDEX IF NOT EXISTS idx_amortization_entries_status_remark 
        ON amortization_entries(payment_status, payment_remark);
        
        RAISE NOTICE 'payment_remark 字段已成功添加到 amortization_entries 表';
    ELSE
        RAISE NOTICE 'payment_remark 字段已存在于 amortization_entries 表中';
    END IF;
END $$;
