package com.ocbc.finance.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付时间表响应DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class PaymentScheduleResponse {

    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 合同ID
     */
    private Long contractId;
    
    /**
     * 时间表编号
     */
    private String scheduleNo;
    
    /**
     * 支付日期
     */
    private LocalDateTime paymentDate;
    
    /**
     * 支付条件
     */
    private String paymentCondition;
    
    /**
     * 里程碑
     */
    private String milestone;
    
    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;
    
    /**
     * 支付金额币种
     */
    private String paymentAmountCurrency;
    
    /**
     * 状态
     * 可选值：PENDING（待支付）、PAID（已支付）、OVERDUE（逾期）、CANCELLED（已取消）
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 更新人
     */
    private String updatedBy;
}
