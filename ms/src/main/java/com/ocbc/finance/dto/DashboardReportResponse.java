package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 仪表盘报表响应（柱状图数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportResponse {
    
    /**
     * 生效合同数量
     */
    private Integer activeContractCount;
    
    /**
     * 本月摊销金额
     */
    private BigDecimal currentMonthAmortization;
    
    /**
     * 剩余待付款金额
     */
    private BigDecimal remainingPayableAmount;
    
    /**
     * 数据生成时间
     */
    private String generatedAt;
    
    /**
     * 统计月份（格式：yyyy-MM）
     */
    private String statisticsMonth;
}
