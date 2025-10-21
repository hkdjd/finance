package com.ocbc.finance.dto.payment;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付时间表更新请求DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class PaymentScheduleUpdateRequest {

    /**
     * 支付时间表更新项列表
     */
    @NotNull(message = "更新项列表不能为空")
    private List<PaymentScheduleUpdateItem> updateItems;
    
    /**
     * 更新人
     */
    @NotBlank(message = "更新人不能为空")
    private String updatedBy;
    
    /**
     * 支付时间表更新项
     */
    @Data
    @Builder
    public static class PaymentScheduleUpdateItem {
        
        /**
         * 支付时间表ID
         */
        @NotNull(message = "支付时间表ID不能为空")
        private Long id;
        
        /**
         * 支付日期
         */
        private LocalDateTime paymentDate;
        
        /**
         * 支付条件
         */
        private String paymentCondition;
        
        /**
         * 里程碑
         */
        private String milestone;
        
        /**
         * 支付金额
         */
        @DecimalMin(value = "0.01", message = "支付金额必须大于0")
        private BigDecimal paymentAmount;
        
        /**
         * 支付金额币种
         */
        private String paymentAmountCurrency;
        
        /**
         * 状态
         * 可选值：PENDING（待支付）、PAID（已支付）、OVERDUE（逾期）、CANCELLED（已取消）
         */
        private String status;
    }
}
