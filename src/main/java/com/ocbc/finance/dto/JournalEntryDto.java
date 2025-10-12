package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryDto {
    private LocalDate bookingDate;
    private String account; // 会计科目：费用/应付/预付/活期存款
    private BigDecimal dr;  // 借方
    private BigDecimal cr;  // 贷方
    private String memo;
}
