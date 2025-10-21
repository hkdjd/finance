-- 会计分录表建表脚本
-- 表名: journal_entries
-- 描述: 存储会计分录信息，记录每笔付款产生的借贷分录

CREATE TABLE IF NOT EXISTS journal_entries (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 外键
    payment_id BIGINT NOT NULL COMMENT '关联的付款ID',
    
    -- 业务字段
    booking_date DATE NOT NULL COMMENT '记账日期',
    account_name VARCHAR(100) NOT NULL COMMENT '会计科目名称',
    debit_amount NUMERIC(19,2) NOT NULL DEFAULT 0 COMMENT '借方金额',
    credit_amount NUMERIC(19,2) NOT NULL DEFAULT 0 COMMENT '贷方金额',
    memo VARCHAR(500) COMMENT '备注',
    entry_order INTEGER NOT NULL DEFAULT 0 COMMENT '分录顺序',
    
    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(255) COMMENT '创建人',
    updated_by VARCHAR(255) COMMENT '更新人',
    
    -- 外键约束
    CONSTRAINT fk_journal_entries_payment_id 
        FOREIGN KEY (payment_id) REFERENCES payments(id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
        
    -- 检查约束
    CONSTRAINT chk_journal_entries_amounts_non_negative 
        CHECK (debit_amount >= 0 AND credit_amount >= 0),
    CONSTRAINT chk_journal_entries_not_both_zero 
        CHECK (debit_amount > 0 OR credit_amount > 0)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_journal_entries_payment_id ON journal_entries(payment_id);
CREATE INDEX IF NOT EXISTS idx_journal_entries_booking_date ON journal_entries(booking_date);
CREATE INDEX IF NOT EXISTS idx_journal_entries_account_name ON journal_entries(account_name);
CREATE INDEX IF NOT EXISTS idx_journal_entries_created_at ON journal_entries(created_at);

-- 创建复合索引
CREATE INDEX IF NOT EXISTS idx_journal_entries_payment_order 
    ON journal_entries(payment_id, entry_order);
CREATE INDEX IF NOT EXISTS idx_journal_entries_booking_date_account 
    ON journal_entries(booking_date, account_name);

-- 添加表注释
COMMENT ON TABLE journal_entries IS '会计分录表';
COMMENT ON COLUMN journal_entries.id IS '主键ID';
COMMENT ON COLUMN journal_entries.payment_id IS '关联的付款ID';
COMMENT ON COLUMN journal_entries.booking_date IS '记账日期';
COMMENT ON COLUMN journal_entries.account_name IS '会计科目名称(如应付、费用、活期存款等)';
COMMENT ON COLUMN journal_entries.debit_amount IS '借方金额';
COMMENT ON COLUMN journal_entries.credit_amount IS '贷方金额';
COMMENT ON COLUMN journal_entries.memo IS '备注信息';
COMMENT ON COLUMN journal_entries.entry_order IS '分录顺序(用于排序显示)';
COMMENT ON COLUMN journal_entries.created_at IS '创建时间';
COMMENT ON COLUMN journal_entries.updated_at IS '更新时间';
COMMENT ON COLUMN journal_entries.created_by IS '创建人';
COMMENT ON COLUMN journal_entries.updated_by IS '更新人';
