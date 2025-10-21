package com.ocbc.finance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 自定义关键字实体类
 * 用于存储用户自定义的合同提取关键字
 */
@Entity
@Table(name = "custom_keywords")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CustomKeyword extends BaseAuditEntity {
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 关键字
     */
    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;
}
