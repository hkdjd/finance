package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPreviewResponse {
    private BigDecimal paymentAmount;
    private Long contractId;
    private Long paymentId;
    private List<JournalEntryDto> entries;

}
