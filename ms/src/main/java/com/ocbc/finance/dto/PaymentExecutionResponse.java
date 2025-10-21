package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentExecutionResponse {
    
    private Long paymentId;
    private Long contractId;
    private BigDecimal paymentAmount;
    private LocalDate bookingDate;
    private List<String> selectedPeriods;
    private String status;
    private List<PaymentJournalEntryDto> journalEntries;
    private String message;
}
