package com.ocbc.finance.dto.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 原始合同预览响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OriginalContractPreviewResponse {
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件数据
     */
    private byte[] fileData;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 创建时间
     */
    private String createdDate;
    
    /**
     * 创建者
     */
    private String createdBy;
}
