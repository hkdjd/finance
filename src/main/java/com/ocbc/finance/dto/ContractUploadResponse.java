package com.ocbc.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 合同上传响应
 */
@Data
public class ContractUploadResponse {
    
    /** 合同ID */
    private Long contractId;
    
    /** 合同总金额 */
    private BigDecimal totalAmount;
    
    /** 合同开始时间 */
    private String startDate;
    
    /** 合同结束时间 */
    private String endDate;
    
    /** 税率 */
    private BigDecimal taxRate;
    
    /** 供应商名称 */
    private String vendorName;
    
    /** 合同附件名称 */
    private String attachmentName;
    
    /** 创建时间 */
    private OffsetDateTime createdAt;
    
    /** 操作消息 */
    private String message;
}
