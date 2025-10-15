package com.ocbc.finance.service.calculation;

import com.ocbc.finance.dto.AmortizationEntryDto;
import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.CalculateAmortizationRequest;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmortizationCalculationService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;
    
    public AmortizationCalculationService(ContractRepository contractRepository,
                                          AmortizationEntryRepository amortizationEntryRepository) {
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
    }

    public AmortizationResponse calculate(CalculateAmortizationRequest req) {
        YearMonth start = parseToYearMonth(req.getStartDate());
        YearMonth end = parseToYearMonth(req.getEndDate());
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("结束时间早于开始时间");
        }

        LocalDate now = LocalDate.now();
        YearMonth current = YearMonth.from(now);

        String scenario;
        List<AmortizationEntryDto> entries;

        if (now.isBefore(start.atDay(1))) {
            scenario = "SCENARIO_1"; // 当前时间小于合同开始时间，平均摊销到每个月，入账期间等于摊销期间
            entries = buildEqualEntries(req.getTotalAmount(), start, end, null);
        } else if (now.isAfter(end.atEndOfMonth())) {
            scenario = "SCENARIO_3"; // 当前时间大于合同结束时间，按合同期间平均摊销到每个月
            // 修复：对于已结束的合同，仍然按月生成摊销明细，而不是只生成一条记录
            entries = buildEqualEntries(req.getTotalAmount(), start, end, null);
        } else {
            scenario = "SCENARIO_2"; // 当前位于合同期间内，未开始期间集中入账到本月，其余按月入账
            entries = buildEqualEntries(req.getTotalAmount(), start, end, current);
        }

        return AmortizationResponse.builder()
                .totalAmount(req.getTotalAmount().setScale(2, RoundingMode.HALF_UP))
                .startDate(start.format(YM))
                .endDate(end.format(YM))
                .scenario(scenario)
                .generatedAt(java.time.OffsetDateTime.now())
                .entries(entries)
                .build();
    }

    /**
     * 根据合同ID计算摊销明细
     * 从数据库获取合同信息，然后调用计算方法
     * 如果数据库中已有摊销明细，则返回真实的ID
     */
    public AmortizationResponse calculateByContractId(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        
        // 构造计算请求
        CalculateAmortizationRequest request = new CalculateAmortizationRequest();
        request.setTotalAmount(contract.getTotalAmount());
        request.setStartDate(contract.getStartDate().toString());
        request.setEndDate(contract.getEndDate().toString());
        request.setTaxRate(contract.getTaxRate() != null ? contract.getTaxRate() : BigDecimal.ZERO);
        request.setVendorName(contract.getVendorName());
        
        AmortizationResponse response = calculate(request);
        
        // 检查数据库中是否已有摊销明细，如果有则使用真实ID
        List<AmortizationEntry> existingEntries = amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(contractId);
        if (!existingEntries.isEmpty()) {
            // 将数据库中的ID映射到计算结果中
            for (int i = 0; i < response.getEntries().size() && i < existingEntries.size(); i++) {
                AmortizationEntryDto entryDto = response.getEntries().get(i);
                AmortizationEntry existingEntry = existingEntries.get(i);
                
                // 检查期间是否匹配，如果匹配则使用真实ID
                if (entryDto.getAmortizationPeriod().equals(existingEntry.getAmortizationPeriod())) {
                    // 创建新的DTO对象，包含真实ID
                    AmortizationEntryDto updatedDto = new AmortizationEntryDto(
                        existingEntry.getId(),
                        entryDto.getAmortizationPeriod(),
                        entryDto.getAccountingPeriod(),
                        entryDto.getAmount(),
                        entryDto.getStatus()
                    );
                    response.getEntries().set(i, updatedDto);
                }
            }
        }
        
        return response;
    }

    private List<AmortizationEntryDto> buildEqualEntries(BigDecimal total, YearMonth start, YearMonth end, YearMonth concentrateTo) {
        List<YearMonth> months = enumerateMonths(start, end);
        int n = months.size();
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        // 调整最后一个月以保证总和精确等于 total
        BigDecimal sumFirstNMinus1 = base.multiply(BigDecimal.valueOf(n - 1)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal last = total.subtract(sumFirstNMinus1).setScale(2, RoundingMode.HALF_UP);

        List<AmortizationEntryDto> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            YearMonth ym = months.get(i);
            BigDecimal amount = (i == n - 1) ? last : base;
            String amortizationPeriod = ym.format(YM);
            String accountingPeriod;
            if (concentrateTo == null) {
                // 场景1：入账期间=摊销期间
                accountingPeriod = amortizationPeriod;
            } else {
                // 场景2：截至当月(含)的月份集中入账到当月，其余按各自月份
                if (!ym.isAfter(concentrateTo)) {
                    accountingPeriod = concentrateTo.format(YM);
                } else {
                    accountingPeriod = amortizationPeriod;
                }
            }
            list.add(new AmortizationEntryDto(null, amortizationPeriod, accountingPeriod, amount));
        }
        return list;
    }

    private List<YearMonth> enumerateMonths(YearMonth start, YearMonth end) {
        List<YearMonth> res = new ArrayList<>();
        YearMonth cur = start;
        while (!cur.isAfter(end)) {
            res.add(cur);
            cur = cur.plusMonths(1);
        }
        return res;
    }

    private YearMonth parseToYearMonth(String s) {
        String v = s.trim();
        if (v.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate ld = LocalDate.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return YearMonth.of(ld.getYear(), ld.getMonth());
        } else if (v.matches("\\d{4}-\\d{2}")) {
            return YearMonth.parse(v, YM);
        } else {
            throw new IllegalArgumentException("日期格式应为 yyyy-MM 或 yyyy-MM-dd: " + s);
        }
    }
}
