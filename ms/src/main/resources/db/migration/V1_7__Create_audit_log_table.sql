-- 创建audit log表，记录摊销明细的付款操作历史
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amortization_entry_id BIGINT NOT NULL COMMENT '摊销明细ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型：PAYMENT, UPDATE, DELETE',
    operator_id VARCHAR(100) NOT NULL COMMENT '操作人ID',
    operation_time TIMESTAMP NOT NULL COMMENT '操作时间',
    payment_amount DECIMAL(15,2) COMMENT '支付金额',
    payment_date DATE COMMENT '付款时间',
    payment_status VARCHAR(20) COMMENT '付款状态：PENDING, PAID, CANCELLED',
    old_payment_amount DECIMAL(15,2) COMMENT '修改前支付金额',
    old_payment_date DATE COMMENT '修改前付款时间',
    old_payment_status VARCHAR(20) COMMENT '修改前付款状态',
    remark TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    
    INDEX idx_amortization_entry_id (amortization_entry_id),
    INDEX idx_operation_time (operation_time),
    INDEX idx_operator_id (operator_id)
) COMMENT='摊销明细审计日志表';
