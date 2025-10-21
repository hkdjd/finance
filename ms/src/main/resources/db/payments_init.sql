-- 付款表建表脚本
-- 表名: payments
-- 描述: 存储付款信息，记录付款金额、时间、状态等

CREATE TABLE IF NOT EXISTS payments (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 外键
    contract_id BIGINT NOT NULL COMMENT '关联的合同ID',
    
    -- 业务字段
    payment_amount NUMERIC(19,2) NOT NULL COMMENT '付款金额',
    booking_date DATE NOT NULL COMMENT '记账日期',
    selected_periods VARCHAR(1000) COMMENT '选择的付款期间(逗号分隔)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '付款状态',
    
    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(255) COMMENT '创建人',
    updated_by VARCHAR(255) COMMENT '更新人',
    
    -- 外键约束
    CONSTRAINT fk_payments_contract_id 
        FOREIGN KEY (contract_id) REFERENCES contracts(id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
        
    -- 检查约束
    CONSTRAINT chk_payments_status 
        CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_payments_amount_positive 
        CHECK (payment_amount > 0)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_payments_contract_id ON payments(contract_id);
CREATE INDEX IF NOT EXISTS idx_payments_booking_date ON payments(booking_date);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

-- 创建复合索引
CREATE INDEX IF NOT EXISTS idx_payments_contract_status 
    ON payments(contract_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_contract_booking_date 
    ON payments(contract_id, booking_date DESC);

-- 添加表注释
COMMENT ON TABLE payments IS '付款信息表';
COMMENT ON COLUMN payments.id IS '主键ID';
COMMENT ON COLUMN payments.contract_id IS '关联的合同ID';
COMMENT ON COLUMN payments.payment_amount IS '付款金额';
COMMENT ON COLUMN payments.booking_date IS '记账日期';
COMMENT ON COLUMN payments.selected_periods IS '选择的付款期间(逗号分隔，如2024-01,2024-02)';
COMMENT ON COLUMN payments.status IS '付款状态(DRAFT:草稿, CONFIRMED:已确认, CANCELLED:已取消)';
COMMENT ON COLUMN payments.created_at IS '创建时间';
COMMENT ON COLUMN payments.updated_at IS '更新时间';
COMMENT ON COLUMN payments.created_by IS '创建人';
COMMENT ON COLUMN payments.updated_by IS '更新人';
