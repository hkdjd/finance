package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 供应商分布报表响应（饼图数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDistributionResponse {
    
    /**
     * 供应商分布数据列表
     */
    private List<VendorDistributionItem> vendors;
    
    /**
     * 合同总数
     */
    private Integer totalContracts;
    
    /**
     * 数据生成时间
     */
    private String generatedAt;
    
    /**
     * 供应商分布项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorDistributionItem {
        
        /**
         * 供应商名称
         */
        private String vendorName;
        
        /**
         * 合同数量
         */
        private Integer contractCount;
        
        /**
         * 占比百分比（保留2位小数，如：25.50表示25.50%）
         */
        private Double percentage;
    }
}
