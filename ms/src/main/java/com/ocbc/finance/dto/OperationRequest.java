package com.ocbc.finance.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 统一操作请求DTO
 * 用于所有增删改操作，通过operate字段区分操作类型
 */
@Data
public class OperationRequest<T> {
    
    /**
     * 操作类型
     * CREATE - 创建
     * UPDATE - 更新
     * DELETE - 删除
     */
    @NotNull(message = "操作类型不能为空")
    private OperationType operate;
    
    /**
     * 操作数据
     * 对于CREATE和UPDATE操作，包含完整的实体数据
     * 对于DELETE操作，只需要包含ID
     */
    private T data;
    
    /**
     * 实体ID（用于UPDATE和DELETE操作）
     */
    private Long id;
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        CREATE,
        UPDATE,
        DELETE
    }
}
