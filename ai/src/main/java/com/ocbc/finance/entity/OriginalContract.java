package com.ocbc.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 合同原件实体类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tbl_original_contract")
public class OriginalContract extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名
     */
    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;

    /**
     * 文件类型
     */
    @Column(name = "file_type", nullable = false, length = 32)
    private String fileType;

    /**
     * 文件数据
     */
    @Column(name = "file_data", nullable = false, columnDefinition = "bytea")
    private byte[] fileData;

    /**
     * 一对一关系 - 合同信息
     */
    @OneToOne(mappedBy = "originalContract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Contract contract;
}
