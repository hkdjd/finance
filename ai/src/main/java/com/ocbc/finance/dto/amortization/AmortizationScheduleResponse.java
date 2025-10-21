package com.ocbc.finance.dto.amortization;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 摊销时间表响应DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class AmortizationScheduleResponse {

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
     * 摊销日期
     */
    private LocalDateTime scheduleDate;

    /**
     * 入账日期
     */
    private LocalDateTime postDate;

    /**
     * 摊销金额
     */
    private BigDecimal amortizationAmount;

    /**
     * 摊销金额币种
     */
    private String amortizationAmountCurrency;

    /**
     * 是否已生成会计分录
     */
    private Boolean isPosted;

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
