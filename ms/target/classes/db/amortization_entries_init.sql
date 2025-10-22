-- 摊销明细表建表脚本
-- 表名: amortization_entries
-- 描述: 存储合同的摊销明细信息，记录每个期间的摊销金额

CREATE TABLE IF NOT EXISTS amortization_entries (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 外键
    contract_id BIGINT NOT NULL COMMENT '关联的合同ID',
    
    -- 业务字段
    amortization_period VARCHAR(7) NOT NULL COMMENT '摊销期间(yyyy-MM)',
    accounting_period VARCHAR(7) NOT NULL COMMENT '入账期间(yyyy-MM)',
    amount NUMERIC(19,2) NOT NULL COMMENT '摊销金额',
    period_date DATE COMMENT '期间日期(用于排序)',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '付款状态',
    
    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(255) COMMENT '创建人',
    updated_by VARCHAR(255) COMMENT '更新人',
    
    -- 外键约束
    CONSTRAINT fk_amortization_entries_contract_id 
        FOREIGN KEY (contract_id) REFERENCES contracts(id) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_amortization_entries_contract_id ON amortization_entries(contract_id);
CREATE INDEX IF NOT EXISTS idx_amortization_entries_amortization_period ON amortization_entries(amortization_period);
CREATE INDEX IF NOT EXISTS idx_amortization_entries_accounting_period ON amortization_entries(accounting_period);
CREATE INDEX IF NOT EXISTS idx_amortization_entries_period_date ON amortization_entries(period_date);
CREATE INDEX IF NOT EXISTS idx_amortization_entries_payment_status ON amortization_entries(payment_status);
CREATE INDEX IF NOT EXISTS idx_amortization_entries_created_at ON amortization_entries(created_at);

-- 创建复合索引
CREATE INDEX IF NOT EXISTS idx_amortization_entries_contract_period 
    ON amortization_entries(contract_id, amortization_period);

-- 添加表注释
COMMENT ON TABLE amortization_entries IS '摊销明细表';
COMMENT ON COLUMN amortization_entries.id IS '主键ID';
COMMENT ON COLUMN amortization_entries.contract_id IS '关联的合同ID';
COMMENT ON COLUMN amortization_entries.amortization_period IS '摊销期间(yyyy-MM格式)';
COMMENT ON COLUMN amortization_entries.accounting_period IS '入账期间(yyyy-MM格式)';
COMMENT ON COLUMN amortization_entries.amount IS '摊销金额';
COMMENT ON COLUMN amortization_entries.period_date IS '期间日期(用于排序)';
COMMENT ON COLUMN amortization_entries.payment_status IS '付款状态(PENDING:待付款, COMPLETED:已完成)';
COMMENT ON COLUMN amortization_entries.created_at IS '创建时间';
COMMENT ON COLUMN amortization_entries.updated_at IS '更新时间';
COMMENT ON COLUMN amortization_entries.created_by IS '创建人';
COMMENT ON COLUMN amortization_entries.updated_by IS '更新人';
