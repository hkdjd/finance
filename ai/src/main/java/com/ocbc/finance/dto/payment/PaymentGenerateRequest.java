package com.ocbc.finance.dto.payment;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付时间表生成请求DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class PaymentGenerateRequest {

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
     * 合同开始日期
     */
    @NotNull(message = "合同开始日期不能为空")
    private LocalDateTime contractStartDate;
    
    /**
     * 合同结束日期
     */
    @NotNull(message = "合同结束日期不能为空")
    private LocalDateTime contractEndDate;
    
    /**
     * 支付期数
     */
    @NotNull(message = "支付期数不能为空")
    @Min(value = 1, message = "支付期数必须大于0")
    private Integer paymentPeriods;
    
    /**
     * 支付方式
     * 可选值：EQUAL_INSTALLMENT（等额分期）、MILESTONE_BASED（基于里程碑）、QUARTERLY（按季度）、SEMI_ANNUAL（半年度）
     */
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;
    
    /**
     * 首次支付日期
     */
    private LocalDateTime firstPaymentDate;
    
    /**
     * 支付条件描述
     */
    private String paymentConditionDesc;
    
    /**
     * 创建人
     */
    @NotBlank(message = "创建人不能为空")
    private String createdBy;
}
