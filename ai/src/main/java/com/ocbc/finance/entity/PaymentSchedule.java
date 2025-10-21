package com.ocbc.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付时间表实体类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tbl_payment_schedule")
public class PaymentSchedule extends BaseEntity {

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
     * 支付日期
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    /**
     * 支付条件
     */
    @Column(name = "payment_condition", nullable = false, length = 32)
    private String paymentCondition;

    /**
     * 里程碑
     */
    @Column(name = "milestone", nullable = false, length = 128)
    private String milestone;

    /**
     * 支付金额
     */
    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    /**
     * 支付金额币种
     */
    @Column(name = "payment_amount_currency", nullable = false, length = 8)
    private String paymentAmountCurrency;

    /**
     * 状态
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
