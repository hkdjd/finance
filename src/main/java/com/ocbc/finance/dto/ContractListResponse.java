package com.ocbc.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 合同列表响应
 */
@Data
public class ContractListResponse {
    
    /** 合同列表 */
    private List<ContractSummary> contracts;
    
    /** 总数量 */
    private long totalCount;
    
    /** 操作消息 */
    private String message;
    
    /**
     * 合同摘要信息
     */
    @Data
    public static class ContractSummary {
        
        /** 合同ID */
        private Long contractId;
        
        /** 合同总金额 */
        private BigDecimal totalAmount;
        
        /** 合同开始时间 */
        private String startDate;
        
        /** 合同结束时间 */
        private String endDate;
        
        /** 供应商名称 */
        private String vendorName;
        
        /** 合同附件名称 */
        private String attachmentName;
        
        /** 创建时间 */
        private OffsetDateTime createdAt;
        
        /** 合同状态（可扩展） */
        private String status = "ACTIVE";
    }
}
