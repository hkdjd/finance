package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 合同基础信息DTO
 * 用于会计分录预览响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractInfoDto {
    /** 合同ID */
    private Long id;
    
    /** 合同总金额 */
    private BigDecimal totalAmount;
    
    /** 合同开始日期 */
    private String startDate;
    
    /** 合同结束日期 */
    private String endDate;
    
    /** 供应商名称 */
    private String vendorName;
}
