package com.ocbc.finance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AmortizationEntryDto {
    /** 主键ID */
    private Long id;
    /** 预提/摊销期间，格式 yyyy-MM */
    private String amortizationPeriod;
    /** 入账期间，格式 yyyy-MM */
    private String accountingPeriod;
    /** 预提/摊销金额，保留两位小数 */
    private BigDecimal amount;
    /** 付款状态：PENDING-待付款, COMPLETED-已完成 */
    private String status;

    // 兼容性构造函数，默认状态为PENDING
    public AmortizationEntryDto(Long id, String amortizationPeriod, String accountingPeriod, BigDecimal amount) {
        this.id = id;
        this.amortizationPeriod = amortizationPeriod;
        this.accountingPeriod = accountingPeriod;
        this.amount = amount;
        this.status = "PENDING";
    }
    
    // 包含状态的构造函数（用于向后兼容）
    public AmortizationEntryDto(Long id, String amortizationPeriod, String accountingPeriod, BigDecimal amount, String status) {
        this.id = id;
        this.amortizationPeriod = amortizationPeriod;
        this.accountingPeriod = accountingPeriod;
        this.amount = amount;
        this.status = status;
    }
    
}
