package com.ocbc.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculateAmortizationRequest {

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal totalAmount;

    /**
     * 合同开始时间，支持格式：yyyy-MM 或 yyyy-MM-dd
     */
    @NotBlank
    private String startDate;

    /**
     * 合同结束时间，支持格式：yyyy-MM 或 yyyy-MM-dd
     */
    @NotBlank
    private String endDate;

    /**
     * 税率，预留字段（当前计算不使用）
     */
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal taxRate;

    /**
     * 乙方公司名称（甲方默认 OCBC）
     */
    @NotBlank
    private String vendorName;

    /**
     * 操作人ID，用于操作日志记录
     */
    private String operatorId;
}
