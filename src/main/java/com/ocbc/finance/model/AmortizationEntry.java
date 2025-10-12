package com.ocbc.finance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "amortization_entries")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmortizationEntry extends BaseAuditEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "amortization_period", nullable = false, length = 7)
    private String amortizationPeriod; // yyyy-MM

    @Column(name = "accounting_period", nullable = false, length = 7)
    private String accountingPeriod; // yyyy-MM

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "period_date")
    private LocalDate periodDate; // 对应月份第一天，便于排序

    @Column(name = "payment_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    public enum PaymentStatus {
        PENDING,    // 待付款
        COMPLETED   // 已完成
    }
    
    // 为了兼容性，添加status方法
    public String getStatus() {
        return paymentStatus != null ? paymentStatus.name() : PaymentStatus.PENDING.name();
    }
    
    public void setStatus(String status) {
        if (status != null) {
            this.paymentStatus = PaymentStatus.valueOf(status);
        }
    }
}
