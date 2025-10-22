package com.ocbc.finance.service;

import com.ocbc.finance.dto.DashboardReportResponse;
import com.ocbc.finance.dto.VendorDistributionResponse;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表服务类
 * 提供仪表盘和数据分析功能
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;

    public ReportService(ContractRepository contractRepository,
                        AmortizationEntryRepository amortizationEntryRepository) {
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
    }

    /**
     * 获取仪表盘报表数据（柱状图）
     * @return 仪表盘报表数据
     */
    public DashboardReportResponse getDashboardReport() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        String currentMonthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 1. 统计生效合同数量（当前日期在合同期间内的合同）
        List<Contract> allContracts = contractRepository.findAll();
        int activeContractCount = (int) allContracts.stream()
                .filter(contract -> !today.isBefore(contract.getStartDate()) 
                        && !today.isAfter(contract.getEndDate()))
                .count();
        
        // 2. 计算本月摊销金额（摊销期间为当前月份的所有摊销明细金额之和）
        List<AmortizationEntry> currentMonthEntries = amortizationEntryRepository
                .findByAmortizationPeriod(currentMonthStr);
        BigDecimal currentMonthAmortization = currentMonthEntries.stream()
                .map(AmortizationEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 3. 计算剩余待付款金额（所有状态为PENDING的摊销明细金额之和）
        List<AmortizationEntry> pendingEntries = amortizationEntryRepository
                .findByPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
        BigDecimal remainingPayableAmount = pendingEntries.stream()
                .map(entry -> entry.getAmount().subtract(entry.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return DashboardReportResponse.builder()
                .activeContractCount(activeContractCount)
                .currentMonthAmortization(currentMonthAmortization.setScale(2, RoundingMode.HALF_UP))
                .remainingPayableAmount(remainingPayableAmount.setScale(2, RoundingMode.HALF_UP))
                .statisticsMonth(currentMonthStr)
                .generatedAt(LocalDate.now().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    /**
     * 获取供应商分布报表数据（饼图）
     * @return 供应商分布数据
     */
    public VendorDistributionResponse getVendorDistribution() {
        // 1. 获取所有合同
        List<Contract> allContracts = contractRepository.findAll();
        int totalContracts = allContracts.size();
        
        // 2. 按供应商名称分组统计合同数量
        Map<String, Long> vendorCountMap = allContracts.stream()
                .collect(Collectors.groupingBy(
                        Contract::getVendorName,
                        Collectors.counting()
                ));
        
        // 3. 构建供应商分布项列表，并计算百分比
        List<VendorDistributionResponse.VendorDistributionItem> vendorItems = vendorCountMap.entrySet().stream()
                .map(entry -> {
                    String vendorName = entry.getKey();
                    int contractCount = entry.getValue().intValue();
                    double percentage = totalContracts > 0 
                            ? (contractCount * 100.0 / totalContracts) 
                            : 0.0;
                    
                    return VendorDistributionResponse.VendorDistributionItem.builder()
                            .vendorName(vendorName)
                            .contractCount(contractCount)
                            .percentage(Math.round(percentage * 100.0) / 100.0) // 保留2位小数
                            .build();
                })
                .sorted((a, b) -> b.getContractCount().compareTo(a.getContractCount())) // 按合同数量降序排序
                .collect(Collectors.toList());
        
        return VendorDistributionResponse.builder()
                .vendors(vendorItems)
                .totalContracts(totalContracts)
                .generatedAt(LocalDate.now().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
