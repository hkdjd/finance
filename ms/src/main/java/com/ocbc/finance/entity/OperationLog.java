package com.ocbc.finance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 操作记录实体类
 */
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 合同ID
     */
    @Column(name = "contract_id", nullable = false)
    private Long contractId;
    
    /**
     * 操作类型（生成、付款、导出、完成等）
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;
    
    /**
     * 操作描述
     */
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    
    /**
     * 操作人
     */
    @Column(name = "operator", nullable = false, length = 100)
    private String operator;
    
    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 构造函数
    public OperationLog() {}
    
    public OperationLog(Long contractId, String operationType, String description, String operator, LocalDateTime operationTime) {
        this.contractId = contractId;
        this.operationType = operationType;
        this.description = description;
        this.operator = operator;
        this.operationTime = operationTime;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
