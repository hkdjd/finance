package com.ocbc.finance.dto.finance;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 时间表信息更新请求DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class ScheduleInfoRequest {

    /**
     * 预提/待摊时间表必要信息
     */
    private Map<String, Object> amortizationScheduleInfo;
    
    /**
     * 支付时间表必要信息
     */
    private Map<String, Object> paymentScheduleInfo;
    
    /**
     * 会计分录必要信息
     */
    private List<Map<String, Object>> accountInfo;
    
    /**
     * 更新人
     */
    @NotNull(message = "更新人不能为空")
    private String updatedBy;
}
