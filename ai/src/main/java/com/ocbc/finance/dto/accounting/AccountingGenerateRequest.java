package com.ocbc.finance.dto.accounting;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会计分录生成请求DTO - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
public class AccountingGenerateRequest {

    /**
     * 付款申请日期（场景三-八需要）
     */
    private LocalDateTime paymentApplicationDate;

    /**
     * 付款金额（场景三-八需要）
     */
    private BigDecimal paymentAmount;

    /**
     * 付款币种（场景三-八需要）
     */
    private String paymentCurrency;

    /**
     * 应付款开始时间（必填）
     */
    private LocalDateTime payableStartDate;

    /**
     * 应付款结束时间（必填）
     */
    private LocalDateTime payableEndDate;

    /**
     * 完成复核时间（场景三-八需要）
     */
    private LocalDateTime reviewCompletionDate;

    /**
     * 预提/待摊审批通过时间（必填）
     */
    private LocalDateTime amortizationApprovalDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作人
     */
    private String operatorBy;
}
