package com.ocbc.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会计分录实体类 - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tbl_accounting_entry")
public class AccountingEntry extends BaseEntity {

    /**
     * 主键ID，自增id，不为空
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 合同ID（外键），关联tbl_contract的主键id
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
     * 预提/待摊时间表ID（外键），关联tbl_amortization_schedule的主键id
     */
    @Column(name = "amortization_schedule_id")
    private Long amortizationScheduleId;

    /**
     * 多对一关系 - 预提/待摊时间表
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amortization_schedule_id", insertable = false, updatable = false)
    private AmortizationSchedule amortizationSchedule;

    /**
     * 会计分录编号，索引，varchar32，不为空
     */
    @Column(name = "accounting_no", nullable = false, length = 32)
    private String accountingNo;

    /**
     * 会计分录日期，timestamp，不为空，生成本合同会计分录日期
     */
    @Column(name = "accounting_date", nullable = false)
    private LocalDateTime accountingDate;

    /**
     * 记账日期，timestamp，不为空
     */
    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    /**
     * 总账科目，varchar32，不为空
     */
    @Column(name = "gl_account", nullable = false, length = 32)
    private String glAccount;

    /**
     * 录入借方金额，bigdecimal，不为空
     */
    @Column(name = "entered_dr", nullable = false, precision = 15, scale = 2)
    private BigDecimal enteredDr;

    /**
     * 录入借方币种，varchar8，不为空
     */
    @Column(name = "entered_dr_currency", nullable = false, length = 8)
    private String enteredDrCurrency;

    /**
     * 录入贷方金额，bigdecimal，不为空
     */
    @Column(name = "entered_cr", nullable = false, precision = 15, scale = 2)
    private BigDecimal enteredCr;

    /**
     * 录入贷方币种，varchar8，不为空
     */
    @Column(name = "entered_cr_currency", nullable = false, length = 8)
    private String enteredCrCurrency;

    /**
     * 预留字段，jsonb
     */
    @Column(name = "reserved_field", columnDefinition = "jsonb")
    private String reservedField;
}
