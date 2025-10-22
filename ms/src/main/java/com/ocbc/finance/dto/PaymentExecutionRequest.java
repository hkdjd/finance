package com.ocbc.finance.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentExecutionRequest {
    
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    
    @NotNull(message = "付款金额不能为空")
    @Positive(message = "付款金额必须大于0")
    private BigDecimal paymentAmount;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate; // 支付时间（可为空，默认当前时间）
    
    @NotNull(message = "选择的摊销明细ID不能为空")
    private List<Long> selectedPeriods; // 勾选的摊销明细ID列表
}
