package com.ocbc.finance.dto;

import lombok.Data;
import lombok.ToString;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@ToString
public class PaymentRequest {
    private AmortizationResponse amortization; // 来自步骤2已确认/调整的摊销表
    private BigDecimal paymentAmount;         // 付款金额
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate bookingDate;            // 付款过账日期（可为空，默认当天）
    private List<String> selectedPeriods;     // 勾选的付款对应期间（yyyy-MM）
    private Long contractId;
    private Long paymentId;
}
