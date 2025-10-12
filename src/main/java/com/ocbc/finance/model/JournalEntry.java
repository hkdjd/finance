package com.ocbc.finance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "journal_entries")
@Data
@EqualsAndHashCode(callSuper = true)
public class JournalEntry extends BaseAuditEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName; // 会计科目名称，如"应付"、"费用"、"活期存款"等

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO; // 借方金额

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO; // 贷方金额

    @Column(name = "memo", length = 500)
    private String memo; // 备注
    
    @Column(name = "description", length = 500)
    private String description; // 描述

    @Column(name = "entry_order", nullable = false)
    private Integer entryOrder = 0; // 分录顺序，用于排序显示

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType = EntryType.AMORTIZATION; // 分录类型

    /**
     * 会计分录类型枚举
     */
    public enum EntryType {
        AMORTIZATION, // 摊销分录（步骤3）
        PAYMENT       // 付款分录（步骤4）
    }

    public JournalEntry() {
        // 无参构造函数
    }

    public JournalEntry(LocalDate bookingDate, String accountName, BigDecimal debitAmount, BigDecimal creditAmount, String memo) {
        this.bookingDate = bookingDate;
        this.accountName = accountName;
        this.debitAmount = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        this.creditAmount = creditAmount != null ? creditAmount : BigDecimal.ZERO;
        this.memo = memo;
    }
}
