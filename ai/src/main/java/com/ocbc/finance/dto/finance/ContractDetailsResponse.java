package com.ocbc.finance.dto.finance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 合同详细信息查询响应DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class ContractDetailsResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 合同ID
     */
    private Long contractId;

    // ========== 合同基本信息字段 ==========
    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同别名
     */
    private String contractAlias;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 甲方
     */
    private String partyA;

    /**
     * 甲方ID
     */
    private String partyAId;

    /**
     * 乙方
     */
    private String partyB;

    /**
     * 乙方ID
     */
    private String partyBId;

    /**
     * 合同描述
     */
    private String contractDescription;

    /**
     * 合同总金额
     */
    private BigDecimal contractAllAmount;

    /**
     * 合同币种
     */
    private String contractAllCurrency;

    /**
     * 合同开始日期
     */
    private LocalDateTime contractValidStartDate;

    /**
     * 合同结束日期
     */
    private LocalDateTime contractValidEndDate;

    /**
     * 是否已完成
     */
    private Boolean isFinished;

    // ========== 详细信息字段 ==========
    /**
     * 基本信息
     */
    private Map<String, Object> baseInfo;

    /**
     * 财务信息
     */
    private Map<String, Object> financeInfo;

    /**
     * 时间信息
     */
    private Map<String, Object> timeInfo;

    /**
     * 结算信息
     */
    private Map<String, Object> settlementInfo;

    /**
     * 费用信息
     */
    private Map<String, Object> feeInfo;

    /**
     * 税务信息
     */
    private Map<String, Object> taxInfo;

    /**
     * 风险信息
     */
    private Map<String, Object> riskInfo;

    /**
     * 预提/待摊时间表生成必要信息
     */
    private Map<String, Object> amortizationScheduleInfo;

    /**
     * 支付时间表生成必要信息
     */
    private Map<String, Object> paymentScheduleInfo;

    /**
     * 会计分录生成必要信息
     */
    private Map<String, Object> accountInfo;

    // ========== 状态信息 ==========
    /**
     * 是否已生成摊销时间表
     */
    private Boolean hasAmortizationSchedule;

    /**
     * 是否已生成支付时间表
     */
    private Boolean hasPaymentSchedule;

    /**
     * 是否已生成会计分录
     */
    private Boolean hasAccountingEntries;

    // ========== 元数据 ==========
    /**
     * 创建时间
     */
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    private LocalDateTime updatedDate;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 版本号（乐观锁）
     */
    private Integer version;
}
