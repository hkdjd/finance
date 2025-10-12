package com.ocbc.finance.service;

import com.ocbc.finance.dto.AmortizationEntryDto;
import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.JournalEntryDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class JournalService {

    public List<JournalEntryDto> previewFromAmortization(AmortizationResponse amort, LocalDate bookingDate) {
        List<JournalEntryDto> list = new ArrayList<>();
        if (amort == null || amort.getEntries() == null) return list;
        for (AmortizationEntryDto e : amort.getEntries()) {
            BigDecimal amt = e.getAmount();
            // 借：费用；贷：应付
            LocalDate book = bookingDateFromAccountingPeriod(e.getAccountingPeriod(), bookingDate);
            list.add(new JournalEntryDto(book, "费用", amt, BigDecimal.ZERO, periodMemo(e)));
            list.add(new JournalEntryDto(book, "应付", BigDecimal.ZERO, amt, periodMemo(e)));
        }
        return list;
    }

    private LocalDate bookingDateFromAccountingPeriod(String accountingPeriod, LocalDate fallback) {
        try {
            YearMonth ym = YearMonth.parse(accountingPeriod);
            // 示例中多为27号，默认取当月27日
            int day = Math.min(27, ym.lengthOfMonth());
            return ym.atDay(day);
        } catch (Exception ex) {
            return fallback != null ? fallback : LocalDate.now();
        }
    }

    private String periodMemo(AmortizationEntryDto e) {
        return "amort:" + e.getAmortizationPeriod() + " acct:" + e.getAccountingPeriod();
        
    }
}
