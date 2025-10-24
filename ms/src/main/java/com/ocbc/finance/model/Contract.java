package com.ocbc.finance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contracts")
public class Contract extends BaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate startDate; // 存储为第一天，如 2024-01-01

    @Column(nullable = false)
    private LocalDate endDate;   // 存储为当月最后一天，如 2024-06-30

    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "file_path", length = 1000)
    private String filePath; // 文件存储路径
    
    @Column(name = "original_file_name", length = 500)
    private String originalFileName; // 原始文件名
    
    /**
     * 自定义字段（以JSON格式存储）
     * 用于存储AI提取的用户自定义字段，格式：{"法人":"张三","项目名称":"云服务器租赁"}
     */
    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFieldsJson;
}
