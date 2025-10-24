-- 创建操作记录表
CREATE TABLE operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    contract_id BIGINT NOT NULL COMMENT '合同ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型（生成、付款、导出、完成等）',
    description VARCHAR(500) NOT NULL COMMENT '操作描述',
    operator VARCHAR(100) NOT NULL COMMENT '操作人',
    operation_time DATETIME NOT NULL COMMENT '操作时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_contract_id (contract_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_operator (operator),
    INDEX idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作记录表';
