-- 合同表建表脚本
-- 表名: contracts
-- 描述: 存储合同基本信息，包含合同金额、期间、供应商等信息

CREATE TABLE IF NOT EXISTS contracts (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 业务字段
    total_amount NUMERIC(19,2) NOT NULL COMMENT '合同总金额',
    start_date DATE NOT NULL COMMENT '合同开始日期',
    end_date DATE NOT NULL COMMENT '合同结束日期', 
    vendor_name VARCHAR(255) NOT NULL COMMENT '供应商名称',
    tax_rate NUMERIC(5,4) NOT NULL COMMENT '税率',
    attachment_name VARCHAR(500) COMMENT '合同附件名称',
    
    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(255) COMMENT '创建人',
    updated_by VARCHAR(255) COMMENT '更新人'
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_contracts_vendor_name ON contracts(vendor_name);
CREATE INDEX IF NOT EXISTS idx_contracts_start_date ON contracts(start_date);
CREATE INDEX IF NOT EXISTS idx_contracts_end_date ON contracts(end_date);
CREATE INDEX IF NOT EXISTS idx_contracts_created_at ON contracts(created_at);

-- 添加表注释
COMMENT ON TABLE contracts IS '合同信息表';
COMMENT ON COLUMN contracts.id IS '主键ID';
COMMENT ON COLUMN contracts.total_amount IS '合同总金额';
COMMENT ON COLUMN contracts.start_date IS '合同开始日期';
COMMENT ON COLUMN contracts.end_date IS '合同结束日期';
COMMENT ON COLUMN contracts.vendor_name IS '供应商名称';
COMMENT ON COLUMN contracts.tax_rate IS '税率';
COMMENT ON COLUMN contracts.attachment_name IS '合同附件名称';
COMMENT ON COLUMN contracts.created_at IS '创建时间';
COMMENT ON COLUMN contracts.updated_at IS '更新时间';
COMMENT ON COLUMN contracts.created_by IS '创建人';
COMMENT ON COLUMN contracts.updated_by IS '更新人';
