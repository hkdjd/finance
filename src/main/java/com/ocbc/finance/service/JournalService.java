package com.ocbc.finance.service;

import com.ocbc.finance.dto.AmortizationEntryDto;
import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.JournalEntryDto;
import com.ocbc.finance.dto.JournalEntriesPreviewResponse;
import com.ocbc.finance.dto.ContractInfoDto;
import com.ocbc.finance.dto.PreviewEntryDto;
import com.ocbc.finance.model.Contract;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * 根据合同和预览类型生成符合文档规范的会计分录预览响应
     */
    public JournalEntriesPreviewResponse generatePreviewResponse(Contract contract, String previewType, LocalDate bookingDate) {
        // 构建合同信息
        ContractInfoDto contractInfo = new ContractInfoDto(
            contract.getId(),
            contract.getTotalAmount(),
            contract.getStartDate().toString(),
            contract.getEndDate().toString(),
            contract.getVendorName()
        );

        // 根据预览类型生成不同的分录
        List<PreviewEntryDto> previewEntries = new ArrayList<>();
        
        if ("AMORTIZATION".equals(previewType)) {
            previewEntries = generateAmortizationEntries(contract, bookingDate);
        } else if ("PAYMENT".equals(previewType)) {
            previewEntries = generatePaymentEntries(contract, bookingDate);
        }

        return new JournalEntriesPreviewResponse(contractInfo, previewEntries);
    }

    /**
     * 生成摊销类型的会计分录
     */
    private List<PreviewEntryDto> generateAmortizationEntries(Contract contract, LocalDate bookingDate) {
        List<PreviewEntryDto> previewEntries = new ArrayList<>();
        
        // 获取摊销数据
        // 这里需要注入AmortizationCalculationService，暂时先模拟
        YearMonth start = YearMonth.from(contract.getStartDate());
        YearMonth end = YearMonth.from(contract.getEndDate());
        
        List<YearMonth> months = enumerateMonths(start, end);
        int n = months.size();
        BigDecimal monthlyAmount = contract.getTotalAmount().divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        
        int entryOrder = 1;
        for (YearMonth month : months) {
            LocalDate book = bookingDateFromAccountingPeriod(month.toString(), bookingDate);
            String period = month.toString();
            
            // 借方：费用
            previewEntries.add(new PreviewEntryDto(
                "AMORTIZATION",
                book.toString(),
                "费用",
                monthlyAmount,
                BigDecimal.ZERO,
                "摊销费用预览",
                "摊销费用预览 - " + period,
                entryOrder++
            ));
            
            // 贷方：应付
            previewEntries.add(new PreviewEntryDto(
                "AMORTIZATION",
                book.toString(),
                "应付",
                BigDecimal.ZERO,
                monthlyAmount,
                "摊销应付预览",
                "摊销应付预览 - " + period,
                entryOrder++
            ));
        }
        
        return previewEntries;
    }

    /**
     * 生成付款类型的会计分录
     */
    private List<PreviewEntryDto> generatePaymentEntries(Contract contract, LocalDate bookingDate) {
        List<PreviewEntryDto> previewEntries = new ArrayList<>();
        
        // 付款分录：借方记应付，贷方记银行存款
        LocalDate paymentDate = bookingDate != null ? bookingDate : LocalDate.now();
        
        previewEntries.add(new PreviewEntryDto(
            "PAYMENT",
            paymentDate.toString(),
            "应付",
            contract.getTotalAmount(),
            BigDecimal.ZERO,
            "付款预览",
            "付款预览 - " + contract.getVendorName(),
            1
        ));
        
        previewEntries.add(new PreviewEntryDto(
            "PAYMENT",
            paymentDate.toString(),
            "银行存款",
            BigDecimal.ZERO,
            contract.getTotalAmount(),
            "付款预览",
            "付款预览 - " + contract.getVendorName(),
            2
        ));
        
        return previewEntries;
    }

    /**
     * 枚举月份范围
     */
    private List<YearMonth> enumerateMonths(YearMonth start, YearMonth end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = start;
        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }
        return months;
    }

    /**
     * 根据合同和摊销数据生成符合文档规范的会计分录预览响应（保留向后兼容）
     */
    public JournalEntriesPreviewResponse generatePreviewResponse(Contract contract, AmortizationResponse amortizationResponse, LocalDate bookingDate) {
        // 构建合同信息
        ContractInfoDto contractInfo = new ContractInfoDto(
            contract.getId(),
            contract.getTotalAmount(),
            contract.getStartDate().toString(),
            contract.getEndDate().toString(),
            contract.getVendorName()
        );

        // 构建预览分录列表
        List<PreviewEntryDto> previewEntries = new ArrayList<>();
        
        if (amortizationResponse != null && amortizationResponse.getEntries() != null) {
            int entryOrder = 1;
            
            for (AmortizationEntryDto e : amortizationResponse.getEntries()) {
                BigDecimal amount = e.getAmount();
                LocalDate book = bookingDateFromAccountingPeriod(e.getAccountingPeriod(), bookingDate);
                String period = e.getAmortizationPeriod();
                
                // 借方：费用
                previewEntries.add(new PreviewEntryDto(
                    "AMORTIZATION",
                    book.toString(),
                    "费用",
                    amount,
                    BigDecimal.ZERO,
                    "摊销费用预览",
                    "摊销费用预览 - " + period,
                    entryOrder++
                ));
                
                // 贷方：应付
                previewEntries.add(new PreviewEntryDto(
                    "AMORTIZATION",
                    book.toString(),
                    "应付",
                    BigDecimal.ZERO,
                    amount,
                    "摊销应付预览",
                    "摊销应付预览 - " + period,
                    entryOrder++
                ));
            }
        }

        return new JournalEntriesPreviewResponse(contractInfo, previewEntries);
    }
}
