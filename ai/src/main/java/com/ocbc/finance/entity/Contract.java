package com.ocbc.finance.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同主表实体类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tbl_contract")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "originalContract", "contractDetails", "amortizationSchedules", "paymentSchedules", "accountingEntries"})
public class Contract extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件ID（外键）
     */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    /**
     * 一对一关系 - 合同原件
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", insertable = false, updatable = false)
    private OriginalContract originalContract;

    /**
     * 合同编号
     */
    @Column(name = "contract_no", nullable = false, length = 128)
    private String contractNo;

    /**
     * 合同别名（唯一键）
     */
    @Column(name = "contract_alias", nullable = false, length = 128, unique = true)
    private String contractAlias;

    /**
     * 合同名称
     */
    @Column(name = "contract_name", nullable = false, length = 128)
    private String contractName;

    /**
     * 甲方
     */
    @Column(name = "party_a", nullable = false, length = 128)
    private String partyA;

    /**
     * 甲方ID
     */
    @Column(name = "party_a_id", nullable = false, length = 128)
    private String partyAId;

    /**
     * 乙方
     */
    @Column(name = "party_b", nullable = false, length = 128)
    private String partyB;

    /**
     * 乙方ID
     */
    @Column(name = "party_b_id", nullable = false, length = 128)
    private String partyBId;

    /**
     * 合同描述
     */
    @Column(name = "contract_description", nullable = false, length = 256)
    private String contractDescription;

    /**
     * 合同总金额
     */
    @Column(name = "contract_all_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal contractAllAmount;

    /**
     * 合同总金额币种
     */
    @Column(name = "contract_all_currency", nullable = false, length = 8)
    private String contractAllCurrency;

    /**
     * 合同生效开始日期
     */
    @Column(name = "contract_valid_start_date", nullable = false)
    private LocalDateTime contractValidStartDate;

    /**
     * 合同生效结束日期
     */
    @Column(name = "contract_valid_end_date", nullable = false)
    private LocalDateTime contractValidEndDate;

    /**
     * 是否已完成
     */
    @Column(name = "is_finished", nullable = false)
    private Boolean isFinished = false;

    /**
     * 一对一关系 - 合同详细信息
     */
    @OneToOne(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ContractDetails contractDetails;

    /**
     * 一对多关系 - 预提/待摊时间表
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AmortizationSchedule> amortizationSchedules;

    /**
     * 一对多关系 - 支付时间表
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentSchedule> paymentSchedules;

    /**
     * 一对多关系 - 会计分录
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountingEntry> accountingEntries;
}
