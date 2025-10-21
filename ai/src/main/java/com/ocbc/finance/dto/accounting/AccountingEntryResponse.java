package com.ocbc.finance.dto.accounting;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会计分录响应DTO - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
public class AccountingEntryResponse {

    /**
     * 主键ID，自增id，不为空
     */
    private Long id;

    /**
     * 合同ID（外键），关联tbl_contract的主键id
     */
    private Long contractId;

    /**
     * 预提/待摊时间表ID（外键），关联tbl_amortization_schedule的主键id
     */
    private Long amortizationScheduleId;

    /**
     * 会计分录编号，索引，varchar32，不为空
     */
    private String accountingNo;

    /**
     * 会计分录日期，timestamp，不为空，生成本合同会计分录日期
     */
    private LocalDateTime accountingDate;

    /**
     * 记账日期，timestamp，不为空
     */
    private LocalDateTime bookingDate;

    /**
     * 总账科目，varchar32，不为空
     */
    private String glAccount;

    /**
     * 总账科目名称（中文）
     */
    private String glAccountName;

    /**
     * 录入借方金额，bigdecimal，不为空
     */
    private BigDecimal enteredDr;

    /**
     * 录入借方币种，varchar8，不为空
     */
    private String enteredDrCurrency;

    /**
     * 录入贷方金额，bigdecimal，不为空
     */
    private BigDecimal enteredCr;

    /**
     * 录入贷方币种，varchar8，不为空
     */
    private String enteredCrCurrency;

    /**
     * 预留字段，jsonb
     */
    private String reservedField;

    /**
     * 创建时间，timestamp，不为空，生成本条会计分录日期
     */
    private LocalDateTime createdDate;

    /**
     * 创建人，varchar32，不为空
     */
    private String createdBy;

    /**
     * 更新时间，timestamp
     */
    private LocalDateTime updatedDate;

    /**
     * 更新人，varchar32
     */
    private String updatedBy;

    /**
     * 版本号，int，默认为1
     */
    private Integer version;
}
