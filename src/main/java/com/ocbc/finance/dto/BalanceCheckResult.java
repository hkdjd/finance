package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 借贷平衡检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceCheckResult {
    
    /**
     * 借方总金额
     */
    private BigDecimal totalDebitAmount;
    
    /**
     * 贷方总金额
     */
    private BigDecimal totalCreditAmount;
    
    /**
     * 差异金额（借方 - 贷方）
     */
    private BigDecimal difference;
    
    /**
     * 是否平衡
     */
    private boolean balanced;
    
    /**
     * 检查消息
     */
    private String message;
    
    /**
     * 获取差异金额
     */
    public BigDecimal getDifference() {
        return difference != null ? difference : BigDecimal.ZERO;
    }
    
    /**
     * 是否平衡
     */
    public boolean isBalanced() {
        return balanced;
    }
}
