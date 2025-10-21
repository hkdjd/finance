package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmortizationResponse {
    private BigDecimal totalAmount;
    private String startDate;
    private String endDate;
    /** 计算场景：SCENARIO_1 / SCENARIO_2 / SCENARIO_3 */
    private String scenario;
    private OffsetDateTime generatedAt;

    private List<AmortizationEntryDto> entries;
}
