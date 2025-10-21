package com.ocbc.finance.dto.amortization;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 摊销时间表生成请求DTO - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
public class AmortizationGenerateRequest {

    /**
     * 合同总金额
     */
    @NotNull(message = "合同总金额不能为空")
    @DecimalMin(value = "0.01", message = "合同总金额必须大于0")
    private BigDecimal totalAmount;
    
    /**
     * 合同总金额币种
     */
    @NotBlank(message = "合同总金额币种不能为空")
    private String totalAmountCurrency;
    
    /**
     * 摊销开始日期（必填）
     */
    @NotNull(message = "摊销开始日期不能为空")
    private LocalDateTime amortizationStartDate;
    
    /**
     * 摊销结束日期（必填）
     */
    @NotNull(message = "摊销结束日期不能为空")
    private LocalDateTime amortizationEndDate;
    
    /**
     * 预提/待摊审批通过时间（影响入账时间）
     */
    private LocalDateTime approvalDate;
    
    /**
     * 合同开始日期
     */
    private LocalDateTime contractStartDate;
    
    /**
     * 合同结束日期
     */
    private LocalDateTime contractEndDate;
    
    /**
     * 摊销期数（由系统根据开始结束日期自动计算，可选）
     */
    private Integer amortizationPeriods;
    
    /**
     * 摊销方式（默认按月）
     */
    private String amortizationMethod = "MONTHLY";
    
    /**
     * 首次摊销日期
     */
    private LocalDateTime firstAmortizationDate;
    
    /**
     * 创建人
     */
    @NotBlank(message = "创建人不能为空")
    private String createdBy;
    
    /**
     * 备注
     */
    private String remarks;
}
