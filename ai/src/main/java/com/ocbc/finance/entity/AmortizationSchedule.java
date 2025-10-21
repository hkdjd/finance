package com.ocbc.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预提/待摊时间表实体类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tbl_amortization_schedule")
public class AmortizationSchedule extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 合同ID（外键）
     */
    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    /**
     * 多对一关系 - 合同信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", insertable = false, updatable = false)
    private Contract contract;

    /**
     * 时间表编号
     */
    @Column(name = "schedule_no", nullable = false, length = 32)
    private String scheduleNo;

    /**
     * 摊销日期
     */
    @Column(name = "schedule_date", nullable = false)
    private LocalDateTime scheduleDate;

    /**
     * 入账日期
     */
    @Column(name = "post_date", nullable = false)
    private LocalDateTime postDate;

    /**
     * 摊销金额
     */
    @Column(name = "amortization_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amortizationAmount;

    /**
     * 摊销金额币种
     */
    @Column(name = "amortization_amount_currency", nullable = false, length = 8)
    private String amortizationAmountCurrency;

    /**
     * 是否已生成会计分录
     */
    @Column(name = "is_posted", nullable = false)
    private Boolean isPosted = false;

    /**
     * 一对多关系 - 会计分录
     */
    @OneToMany(mappedBy = "amortizationSchedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountingEntry> accountingEntries;
}
