package com.ocbc.finance.dto.contract;

import com.ocbc.finance.entity.Contract;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 合同信息及关联状态响应DTO
 * 包含合同基本信息和各关联表的状态标志位
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractWithStatusResponse extends Contract {
    
    // ========== 合同详细信息状态标志位 ==========
    
    /**
     * 是否有基础信息
     */
    private Boolean hasBaseInfo = false;
    
    /**
     * 是否有财务信息
     */
    private Boolean hasFinanceInfo = false;
    
    /**
     * 是否有时间信息
     */
    private Boolean hasTimeInfo = false;
    
    /**
     * 是否有结算信息
     */
    private Boolean hasSettlementInfo = false;
    
    /**
     * 是否有费用信息
     */
    private Boolean hasFeeInfo = false;
    
    /**
     * 是否有税务信息
     */
    private Boolean hasTaxInfo = false;
    
    /**
     * 是否有风险信息
     */
    private Boolean hasRiskInfo = false;
    
    // ========== 其他关联表状态标志位 ==========
    
    /**
     * 是否有预提/待摊时间表
     */
    private Boolean hasAmortizationSchedule = false;
    
    /**
     * 是否有支付时间表
     */
    private Boolean hasPaymentSchedule = false;
    
    /**
     * 是否有会计分录
     */
    private Boolean hasAccountingEntry = false;
}
