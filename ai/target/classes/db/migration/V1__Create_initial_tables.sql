-- OCBC合同订单财务管理系统 - 数据库表结构
-- Version: 1.0.0 - 严格按照需求文档设计
-- Author: OCBC Finance Team
-- 说明: 完全按照需求文档第14-135行设计所有表结构

-- 创建schema（如果不存在）
CREATE SCHEMA IF NOT EXISTS ocbc_finance_contract;

-- 设置当前schema
SET search_path TO ocbc_finance_contract;

-- 合同原件表 (需求文档第42-54行)
CREATE TABLE tbl_original_contract (
    id BIGSERIAL PRIMARY KEY,                    -- 主键，自增id，不为空
    file_name VARCHAR(128) NOT NULL,             -- 文件名，varchar128，不为空
    file_type VARCHAR(32) NOT NULL,              -- 文件类型，varchar32，不为空
    file_data BYTEA NOT NULL,                    -- 文件数据，bytea，不为空
    reserved_field JSONB,                        -- 预留字段，jsonb
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,   -- 是否删除，boolean，默认false，不为空
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，timestamp，不为空
    created_by VARCHAR(32) NOT NULL,             -- 创建人，varchar32，不为空
    updated_date TIMESTAMP,                      -- 更新时间，timestamp
    updated_by VARCHAR(32),                      -- 更新人，varchar32
    version INTEGER NOT NULL DEFAULT 1           -- 版本，int，默认为1
);

-- 合同主表 (需求文档第15-38行)
CREATE TABLE tbl_contract (
    id BIGSERIAL PRIMARY KEY,                    -- 主键，自增id，不为空
    file_id BIGINT NOT NULL REFERENCES tbl_original_contract(id), -- 外键，关联tbl_original_contract的主键id
    contract_no VARCHAR(128) NOT NULL,           -- 合同编号，varchar128，不为空
    contract_alias VARCHAR(128) NOT NULL, -- 合同别名，varchar128，不为空
    contract_name VARCHAR(128) NOT NULL,         -- 合同名称，varchar128，不为空
    party_a VARCHAR(128) NOT NULL,               -- 甲方，varchar128，不为空
    party_a_id VARCHAR(128) NOT NULL,            -- 甲方ID，varchar128，不为空
    party_b VARCHAR(128) NOT NULL,               -- 乙方，varchar128，不为空
    party_b_id VARCHAR(128) NOT NULL,            -- 乙方ID，varchar128，不为空
    contract_description VARCHAR(256) NOT NULL,  -- 合同描述，varchar256，不为空
    contract_all_amount DECIMAL(15,2) NOT NULL,  -- 合同总金额，decimal，不为空
    contract_all_currency VARCHAR(8) NOT NULL,   -- 合同总金额币种，varchar8，不为空
    contract_valid_start_date TIMESTAMP NOT NULL, -- 合同生效开始日期，timestamp，不为空
    contract_valid_end_date TIMESTAMP NOT NULL,  -- 合同生效结束日期，timestamp，不为空
    reserved_field JSONB,                        -- 预留字段，jsonb
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,   -- 是否删除，boolean，默认false，不为空
    is_finished BOOLEAN NOT NULL DEFAULT FALSE,  -- 是否完成，boolean，默认false，不为空
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，timestamp，不为空
    created_by VARCHAR(32) NOT NULL,             -- 创建人，varchar32，不为空
    updated_date TIMESTAMP,                      -- 更新时间，timestamp
    updated_by VARCHAR(32),                      -- 更新人，varchar32
    version INTEGER NOT NULL DEFAULT 1           -- 版本，int，默认为1
);

-- 合同详细信息表 (需求文档第58-76行)
CREATE TABLE tbl_contract_details (
    id BIGSERIAL PRIMARY KEY,                    -- 主键，自增id，不为空
    contract_id BIGINT NOT NULL UNIQUE REFERENCES tbl_contract(id), -- 外键，唯一键，关联tbl_contract的主键id
    base_info JSONB,                             -- 基础信息，jsonb
    finance_info JSONB,                          -- 财务信息，jsonb
    time_info JSONB,                             -- 时间信息，jsonb
    settlement_info JSONB,                       -- 结算信息，jsonb
    fee_info JSONB,                              -- 费用信息，jsonb
    tax_info JSONB,                              -- 税务信息，jsonb
    risk_info JSONB,                             -- 风险信息，jsonb
    amortization_schedule_info JSONB,            -- 预提/待摊时间表生成必要信息，jsonb
    payment_schedule_info JSONB,                 -- 支付时间表生成必要信息，jsonb
    account_info JSONB,                          -- 会计分录生成必要信息，jsonb（list格式）
    reserved_field JSONB,                        -- 预留字段，jsonb
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，timestamp，不为空
    created_by VARCHAR(32) NOT NULL,             -- 创建人，varchar32，不为空
    updated_date TIMESTAMP,                      -- 更新时间，timestamp
    updated_by VARCHAR(32),                      -- 更新人，varchar32
    version INTEGER NOT NULL DEFAULT 1           -- 版本，int，默认为1
);

-- 预提/待摊时间表 (需求文档第79-94行)
CREATE TABLE tbl_amortization_schedule (
    id BIGSERIAL PRIMARY KEY,                    -- 主键，自增id，不为空
    contract_id BIGINT NOT NULL REFERENCES tbl_contract(id), -- 外键，关联tbl_contract的主键id
    schedule_no VARCHAR(32) NOT NULL,            -- 时间表编号，索引，varchar32，不为空
    schedule_date TIMESTAMP NOT NULL,            -- 摊销日期，timestamp，不为空
    post_date TIMESTAMP NOT NULL,                -- 入账日期，timestamp，不为空
    amortization_amount DECIMAL(15,2) NOT NULL,  -- 摊销金额，bigdecimal，不为空
    amortization_amount_currency VARCHAR(8) NOT NULL, -- 摊销金额币种，varchar8，不为空
    is_posted BOOLEAN NOT NULL DEFAULT FALSE,    -- 是否已生成分录，boolean，默认false，不为空
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,   -- 是否删除，boolean，默认false，不为空
    reserved_field JSONB,                        -- 预留字段，jsonb
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，timestamp，不为空
    created_by VARCHAR(32) NOT NULL,             -- 创建人，varchar32，不为空
    updated_date TIMESTAMP,                      -- 更新时间，timestamp
    updated_by VARCHAR(32),                      -- 更新人，varchar32
    version INTEGER NOT NULL DEFAULT 1           -- 版本，int，默认为1
);

-- 支付时间表 (需求文档第97-113行)
CREATE TABLE tbl_payment_schedule (
    id BIGSERIAL PRIMARY KEY,                    -- 主键，自增id，不为空
    contract_id BIGINT NOT NULL REFERENCES tbl_contract(id), -- 外键，关联tbl_contract的主键id
    schedule_no VARCHAR(32) NOT NULL,            -- 时间表编号，索引，varchar32，不为空
    payment_date TIMESTAMP NOT NULL,             -- 支付日期，timestamp，不为空
    payment_condition VARCHAR(32) NOT NULL,      -- 支付条件，varchar32，不为空
    milestone VARCHAR(128) NOT NULL,             -- 里程碑，varchar128，不为空
    payment_amount DECIMAL(15,2) NOT NULL,       -- 支付金额，bigdecimal，不为空
    payment_amount_currency VARCHAR(8) NOT NULL, -- 支付金额币种，varchar8，不为空
    status VARCHAR(32) NOT NULL,                 -- 状态，varchar32，不为空
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,   -- 是否删除，boolean，默认false，不为空
    reserved_field JSONB,                        -- 预留字段，jsonb
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，timestamp，不为空
    created_by VARCHAR(32) NOT NULL,             -- 创建人，varchar32，不为空
    updated_date TIMESTAMP,                      -- 更新时间，timestamp
    updated_by VARCHAR(32),                      -- 更新人，varchar32
    version INTEGER NOT NULL DEFAULT 1           -- 版本，int，默认为1
);

-- 会计分录表 (需求文档第117-135行) - 严格按照需求文档设计
CREATE TABLE tbl_accounting_entry (
    id BIGSERIAL PRIMARY KEY,                    -- 主键，自增id，不为空
    contract_id BIGINT NOT NULL REFERENCES tbl_contract(id), -- 外键，关联tbl_contract的主键id
    amortization_schedule_id BIGINT REFERENCES tbl_amortization_schedule(id), -- 外键，关联tbl_amortization_schedule的主键id
    accounting_no VARCHAR(32) NOT NULL,          -- 会计分录编号，索引，varchar32，不为空
    accounting_date TIMESTAMP NOT NULL,          -- 会计分录日期，timestamp，不为空，生成本合同会计分录日期
    booking_date TIMESTAMP NOT NULL,             -- 记账日期，timestamp，不为空
    gl_account VARCHAR(32) NOT NULL,             -- 总账科目，varchar32，不为空
    entered_dr DECIMAL(15,2) NOT NULL,           -- 录入借方金额，bigdecimal，不为空
    entered_dr_currency VARCHAR(8) NOT NULL,     -- 录入借方币种，varchar8，不为空
    entered_cr DECIMAL(15,2) NOT NULL,           -- 录入贷方金额，bigdecimal，不为空
    entered_cr_currency VARCHAR(8) NOT NULL,     -- 录入贷方币种，varchar8，不为空
    reserved_field JSONB,                        -- 预留字段，jsonb
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间，timestamp，不为空，生成本条会计分录日期
    created_by VARCHAR(32) NOT NULL,             -- 创建人，varchar32，不为空
    updated_date TIMESTAMP,                      -- 更新时间，timestamp
    updated_by VARCHAR(32),                      -- 更新人，varchar32
    version INTEGER NOT NULL DEFAULT 1           -- 版本号，int，默认为1
);

-- 创建索引
CREATE INDEX idx_contract_file_id ON tbl_contract(file_id);
CREATE INDEX idx_contract_no ON tbl_contract(contract_no);
CREATE INDEX idx_contract_alias ON tbl_contract(contract_alias);
CREATE INDEX idx_contract_is_deleted ON tbl_contract(is_deleted);
CREATE INDEX idx_contract_is_finished ON tbl_contract(is_finished);

CREATE INDEX idx_contract_details_contract_id ON tbl_contract_details(contract_id);

CREATE INDEX idx_amortization_schedule_contract_id ON tbl_amortization_schedule(contract_id);
CREATE INDEX idx_amortization_schedule_no ON tbl_amortization_schedule(schedule_no);
CREATE INDEX idx_amortization_schedule_is_posted ON tbl_amortization_schedule(is_posted);
CREATE INDEX idx_amortization_schedule_is_deleted ON tbl_amortization_schedule(is_deleted);

CREATE INDEX idx_payment_schedule_contract_id ON tbl_payment_schedule(contract_id);
CREATE INDEX idx_payment_schedule_no ON tbl_payment_schedule(schedule_no);
CREATE INDEX idx_payment_schedule_status ON tbl_payment_schedule(status);
CREATE INDEX idx_payment_schedule_is_deleted ON tbl_payment_schedule(is_deleted);

CREATE INDEX idx_accounting_entry_contract_id ON tbl_accounting_entry(contract_id);
CREATE INDEX idx_accounting_entry_amortization_schedule_id ON tbl_accounting_entry(amortization_schedule_id);
CREATE INDEX idx_accounting_entry_accounting_no ON tbl_accounting_entry(accounting_no);
CREATE INDEX idx_accounting_entry_gl_account ON tbl_accounting_entry(gl_account);

-- 添加表注释 - 严格按照需求文档
COMMENT ON TABLE tbl_original_contract IS '合同原件表';
COMMENT ON TABLE tbl_contract IS '合同主表';
COMMENT ON TABLE tbl_contract_details IS '合同详细信息表';
COMMENT ON TABLE tbl_amortization_schedule IS '预提/待摊时间表';
COMMENT ON TABLE tbl_payment_schedule IS '支付时间表';
COMMENT ON TABLE tbl_accounting_entry IS '会计分录表';

-- 添加字段注释 - 会计分录表（严格按照需求文档第117-135行）
COMMENT ON COLUMN tbl_accounting_entry.id IS '主键，自增id，不为空';
COMMENT ON COLUMN tbl_accounting_entry.contract_id IS '外键，关联tbl_contract的主键id';
COMMENT ON COLUMN tbl_accounting_entry.amortization_schedule_id IS '外键，关联tbl_amortization_schedule的主键id';
COMMENT ON COLUMN tbl_accounting_entry.accounting_no IS '会计分录编号，索引，varchar32，不为空';
COMMENT ON COLUMN tbl_accounting_entry.accounting_date IS '会计分录日期，timestamp，不为空，生成本合同会计分录日期';
COMMENT ON COLUMN tbl_accounting_entry.booking_date IS '记账日期，timestamp，不为空';
COMMENT ON COLUMN tbl_accounting_entry.gl_account IS '总账科目，varchar32，不为空';
COMMENT ON COLUMN tbl_accounting_entry.entered_dr IS '录入借方金额，bigdecimal，不为空';
COMMENT ON COLUMN tbl_accounting_entry.entered_dr_currency IS '录入借方币种，varchar8，不为空';
COMMENT ON COLUMN tbl_accounting_entry.entered_cr IS '录入贷方金额，bigdecimal，不为空';
COMMENT ON COLUMN tbl_accounting_entry.entered_cr_currency IS '录入贷方币种，varchar8，不为空';
COMMENT ON COLUMN tbl_accounting_entry.reserved_field IS '预留字段，jsonb';
COMMENT ON COLUMN tbl_accounting_entry.created_date IS '创建时间，timestamp，不为空，生成本条会计分录日期';
COMMENT ON COLUMN tbl_accounting_entry.created_by IS '创建人，varchar32，不为空';
COMMENT ON COLUMN tbl_accounting_entry.updated_date IS '更新时间，timestamp';
COMMENT ON COLUMN tbl_accounting_entry.updated_by IS '更新人，varchar32';
COMMENT ON COLUMN tbl_accounting_entry.version IS '版本号，int，默认为1';
