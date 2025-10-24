package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 预提会计分录条目DTO
 * 符合文档规范的响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviewEntryDto {
    /** 业务类型，如 "AMORTIZATION" */
    private String entryType;
    
    /** 入账日期，格式：YYYY-MM-DD */
    private String bookingDate;
    
    /** 会计科目名称 */
    private String accountName;
    
    /** 借方金额 */
    private BigDecimal debitAmount;
    
    /** 贷方金额 */
    private BigDecimal creditAmount;
    
    /** 分录描述 */
    private String description;
    
    /** 备注 */
    private String memo;
    
    /** 分录顺序 */
    private Integer entryOrder;
    
    /** 摊销期间，格式：YYYY-MM */
    private String amortizationPeriod;
}
