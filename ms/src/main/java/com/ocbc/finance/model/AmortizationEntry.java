package com.ocbc.finance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "amortization_entries")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmortizationEntry extends BaseAuditEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "amortization_period", nullable = false)
    private String amortizationPeriod; // yyyy-MM (支持任意长度)

    @Column(name = "accounting_period", nullable = false)
    private String accountingPeriod; // yyyy-MM (支持任意长度)

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO; // 累积已付金额

    @Column(name = "period_date")
    private LocalDate periodDate; // 对应月份第一天，便于排序

    @Column(name = "payment_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // 支付时间，仅在已支付时有值

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
            // 任何付款都将状态设置为已完成（包括不足付款）
            this.paymentStatus = PaymentStatus.COMPLETED;
            // 注意：支付时间需要在调用此方法前单独设置
        }
    }
    
    // 添加付款金额并设置支付时间
    public void addPayment(BigDecimal paymentAmount, LocalDateTime paymentDateTime) {
        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paidAmount = (this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO).add(paymentAmount);
            // 任何付款都将状态设置为已完成（包括不足付款）
            this.paymentStatus = PaymentStatus.COMPLETED;
            // 设置支付时间
            if (this.paymentDate == null && paymentDateTime != null) {
                this.paymentDate = paymentDateTime;
            }
        }
    }
    
}
