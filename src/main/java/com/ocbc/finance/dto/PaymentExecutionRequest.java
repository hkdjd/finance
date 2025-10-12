package com.ocbc.finance.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PaymentExecutionRequest {
    
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    
    @NotNull(message = "付款金额不能为空")
    @Positive(message = "付款金额必须大于0")
    private BigDecimal paymentAmount;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate bookingDate; // 付款过账日期（可为空，默认当天）
    
    @NotNull(message = "选择的摊销明细ID不能为空")
    private List<Long> selectedPeriods; // 勾选的摊销明细ID列表
}
