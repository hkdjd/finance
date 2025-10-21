package com.ocbc.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 基础实体类，包含公共字段
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * 预留字段，用于存储扩展信息
     */
    @Column(name = "reserved_field", columnDefinition = "text")
    private String reservedField;

    /**
     * 是否已删除（软删除标记）
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 创建时间
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdDate;

    /**
     * 创建人
     */
    @Column(name = "created_by", nullable = false, length = 32, updatable = false)
    private String createdBy;

    /**
     * 更新时间
     */
    @Column(name = "updated_date")
    @UpdateTimestamp
    private LocalDateTime updatedDate;

    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 32)
    private String updatedBy;

    /**
     * 版本号（乐观锁）
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;
}
