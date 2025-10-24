package com.ocbc.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建操作记录请求DTO
 */
public class CreateOperationLogRequest {
    
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    
    @NotBlank(message = "操作类型不能为空")
    private String operationType;
    
    @NotBlank(message = "操作描述不能为空")
    private String description;
    
    @NotBlank(message = "操作人不能为空")
    private String operator;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;
    
    // 构造函数
    public CreateOperationLogRequest() {}
    
    public CreateOperationLogRequest(Long contractId, String operationType, String description, 
                                   String operator, LocalDateTime operationTime) {
        this.contractId = contractId;
        this.operationType = operationType;
        this.description = description;
        this.operator = operator;
        this.operationTime = operationTime;
    }
    
    // Getters and Setters
    public Long getContractId() {
        return contractId;
    }
    
    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public LocalDateTime getOperationTime() {
        return operationTime;
    }
    
    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
