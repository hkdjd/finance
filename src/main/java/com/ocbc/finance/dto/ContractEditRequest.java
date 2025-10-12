package com.ocbc.finance.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 合同编辑请求
 */
@Data
public class ContractEditRequest {
    
    @NotNull(message = "合同总金额不能为空")
    @Positive(message = "合同总金额必须大于0")
    private BigDecimal totalAmount;
    
    @NotBlank(message = "合同开始时间不能为空")
    private String startDate; // yyyy-MM-dd 格式
    
    @NotBlank(message = "合同结束时间不能为空")
    private String endDate; // yyyy-MM-dd 格式
    
    @NotNull(message = "税率不能为空")
    private BigDecimal taxRate;
    
    @NotBlank(message = "供应商名称不能为空")
    private String vendorName;
}
