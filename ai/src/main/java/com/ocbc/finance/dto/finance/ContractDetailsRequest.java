package com.ocbc.finance.dto.finance;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 合同详细信息保存请求DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class ContractDetailsRequest {

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
    private LocalDate contractValidStartDate;

    /**
     * 合同结束日期
     */
    private LocalDate contractValidEndDate;

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

    /**
     * 创建人
     */
    @NotNull(message = "创建人不能为空")
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;
}
