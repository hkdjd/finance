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

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO; // 累积已付金额

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
    
    // 获取剩余应付金额
    public BigDecimal getRemainingAmount() {
        return amount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
    
    // 检查是否已完全付款
    public boolean isFullyPaid() {
        return getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0;
    }
    
    // 添加付款金额
    public void addPayment(BigDecimal paymentAmount) {
        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paidAmount = (this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO).add(paymentAmount);
            // 如果已完全付款，更新状态
            if (isFullyPaid()) {
                this.paymentStatus = PaymentStatus.COMPLETED;
            }
        }
    }
}
