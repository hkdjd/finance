package com.ocbc.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 合同详细信息实体类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tbl_contract_details")
public class ContractDetails extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 合同ID（外键，唯一键）
     */
    @Column(name = "contract_id", nullable = false, unique = true)
    private Long contractId;

    /**
     * 一对一关系 - 合同信息
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", insertable = false, updatable = false)
    private Contract contract;

    /**
     * 基本信息
     */
    @Column(name = "base_info", columnDefinition = "text")
    private String baseInfo;

    /**
     * 财务信息
     */
    @Column(name = "finance_info", columnDefinition = "text")
    private String financeInfo;

    /**
     * 时间信息
     */
    @Column(name = "time_info", columnDefinition = "text")
    private String timeInfo;

    /**
     * 结算信息
     */
    @Column(name = "settlement_info", columnDefinition = "text")
    private String settlementInfo;

    /**
     * 费用信息
     */
    @Column(name = "fee_info", columnDefinition = "text")
    private String feeInfo;

    /**
     * 税务信息
     */
    @Column(name = "tax_info", columnDefinition = "text")
    private String taxInfo;

    /**
     * 风险信息
     */
    @Column(name = "risk_info", columnDefinition = "text")
    private String riskInfo;

    /**
     * 预提/待摊时间表生成必要信息
     */
    @Column(name = "amortization_schedule_info", columnDefinition = "text")
    private String amortizationScheduleInfo;

    /**
     * 支付时间表生成必要信息
     */
    @Column(name = "payment_schedule_info", columnDefinition = "text")
    private String paymentScheduleInfo;

    /**
     * 会计分录生成必要信息（列表，支持多次生成）
     */
    @Column(name = "account_info", columnDefinition = "text")
    private String accountInfo;
}
