// package com.ocbc.finance.service;

// import com.ocbc.finance.dto.*;
// import com.ocbc.finance.model.AmortizationEntry;
// import com.ocbc.finance.model.Contract;
// import com.ocbc.finance.model.JournalEntry;
// import com.ocbc.finance.repository.AmortizationEntryRepository;
// import com.ocbc.finance.repository.ContractRepository;
// import org.springframework.stereotype.Service;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.time.YearMonth;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import java.util.stream.Collectors;

// /**
//  * Mock测试服务类
//  * 处理Mock合同的测试业务逻辑
//  */
// @Service
// public class MockTestService {

//     private final ContractRepository contractRepository;
//     private final AmortizationEntryRepository amortizationEntryRepository;
//     private final JournalEntryService journalEntryService;
//     private final PaymentService paymentService;

//     public MockTestService(ContractRepository contractRepository,
//                           AmortizationEntryRepository amortizationEntryRepository,
//                           JournalEntryService journalEntryService,
//                           PaymentService paymentService) {
//         this.contractRepository = contractRepository;
//         this.amortizationEntryRepository = amortizationEntryRepository;
//         this.journalEntryService = journalEntryService;
//         this.paymentService = paymentService;
//     }

//     /**
//      * 获取所有Mock合同
//      */
//     public List<Contract> getAllMockContracts() {
//         return contractRepository.findAll();
//     }

//     /**
//      * 生成指定合同的摊销会计分录
//      */
//     public List<JournalEntry> generateAmortizationEntries(Long contractId) {
//         return journalEntryService.generateJournalEntries(contractId);
//     }

//     /**
//      * 执行第一个月付款
//      */
//     public PaymentExecutionResponse executeFirstMonthPayment(Long contractId) {
//         Contract contract = getContractById(contractId);
//         List<AmortizationEntry> amortizationEntries = getAmortizationEntries(contractId);

//         PaymentExecutionRequest request = new PaymentExecutionRequest();
//         request.setContractId(contractId);
//         request.setBookingDate(LocalDate.now());

//         // 根据合同类型设置付款参数
//         setFirstMonthPaymentParams(contract, amortizationEntries, request);

//         return paymentService.executePayment(request);
//     }

//     /**
//      * 执行超额付款测试
//      */
//     public PaymentPreviewResponse testOverpayment(Long contractId, BigDecimal amount) {
//         Contract contract = getContractById(contractId);
//         List<AmortizationEntry> amortizationEntries = getAmortizationEntries(contractId);

//         PaymentRequest request = new PaymentRequest();
//         request.setPaymentAmount(amount);
//         request.setBookingDate(LocalDate.now());

//         // 设置第一个期间进行超额付款测试
//         setFirstPeriodForTest(contract, amortizationEntries, request);

//         return paymentService.preview(request);
//     }

//     /**
//      * 执行跨期付款测试
//      */
//     public PaymentPreviewResponse testCrossPeriodPayment(Long contractId, BigDecimal amount, int monthCount) {
//         Contract contract = getContractById(contractId);
//         List<AmortizationEntry> amortizationEntries = getAmortizationEntries(contractId);

//         PaymentRequest request = new PaymentRequest();
//         request.setPaymentAmount(amount);
//         request.setBookingDate(LocalDate.now());

//         // 设置多个期间进行跨期付款测试
//         setCrossPeriodForTest(contract, amortizationEntries, request, monthCount);

//         return paymentService.preview(request);
//     }

//     /**
//      * 清理重复数据并重新初始化
//      */
//     public void resetMockData() {
//         System.out.println("开始清理重复数据...");
        
//         // 清理所有数据
//         amortizationEntryRepository.deleteAll();
//         contractRepository.deleteAll();
        
//         System.out.println("数据清理完成，重新初始化Mock数据...");
        
//         // 重新初始化数据
//         createMockContracts();
        
//         System.out.println("Mock数据重置完成！");
//     }

//     // ==================== 私有辅助方法 ====================

//     private Contract getContractById(Long contractId) {
//         return contractRepository.findById(contractId)
//                 .orElseThrow(() -> new IllegalArgumentException("合同不存在，ID: " + contractId));
//     }

//     private List<AmortizationEntry> getAmortizationEntries(Long contractId) {
//         return amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(contractId);
//     }

//     private void setFirstMonthPaymentParams(Contract contract, List<AmortizationEntry> entries, PaymentExecutionRequest request) {
//         if (contract.getVendorName().contains("史密斯")) {
//             request.setPaymentAmount(new BigDecimal("800.00"));
//             setSelectedPeriodIds(entries, "2025-09", request);
//         } else if (contract.getVendorName().contains("美的")) {
//             request.setPaymentAmount(new BigDecimal("300.00"));
//             setSelectedPeriodIds(entries, "2025-10", request);
//         } else if (contract.getVendorName().contains("海尔")) {
//             request.setPaymentAmount(new BigDecimal("500.00"));
//             setSelectedPeriodIds(entries, "2025-11", request);
//         }
//     }

//     private void setFirstPeriodForTest(Contract contract, List<AmortizationEntry> entries, PaymentRequest request) {
//         if (contract.getVendorName().contains("史密斯")) {
//             setSelectedPeriods(entries, "2025-09", request);
//         } else if (contract.getVendorName().contains("美的")) {
//             setSelectedPeriods(entries, "2025-10", request);
//         } else if (contract.getVendorName().contains("海尔")) {
//             setSelectedPeriods(entries, "2025-11", request);
//         }
//     }

//     private void setCrossPeriodForTest(Contract contract, List<AmortizationEntry> entries, PaymentRequest request, int monthCount) {
//         List<String> targetPeriods;
        
//         if (contract.getVendorName().contains("史密斯")) {
//             targetPeriods = Arrays.asList("2025-09", "2025-10", "2025-11")
//                     .subList(0, Math.min(monthCount, 3));
//         } else if (contract.getVendorName().contains("美的")) {
//             targetPeriods = Arrays.asList("2025-10", "2025-11", "2025-12")
//                     .subList(0, Math.min(monthCount, 3));
//         } else {
//             targetPeriods = Arrays.asList("2025-11", "2025-12", "2026-01")
//                     .subList(0, Math.min(monthCount, 3));
//         }

//         List<String> selectedPeriods = entries.stream()
//                 .filter(entry -> targetPeriods.contains(entry.getAmortizationPeriod()))
//                 .map(AmortizationEntry::getAmortizationPeriod)
//                 .collect(Collectors.toList());

//         request.setSelectedPeriods(selectedPeriods);
//     }

//     private void setSelectedPeriodIds(List<AmortizationEntry> entries, String period, PaymentExecutionRequest request) {
//         List<Long> selectedIds = entries.stream()
//                 .filter(entry -> period.equals(entry.getAmortizationPeriod()))
//                 .map(AmortizationEntry::getId)
//                 .collect(Collectors.toList());
//         request.setSelectedPeriods(selectedIds);
//     }

//     private void setSelectedPeriods(List<AmortizationEntry> entries, String period, PaymentRequest request) {
//         List<String> selectedPeriods = entries.stream()
//                 .filter(entry -> period.equals(entry.getAmortizationPeriod()))
//                 .map(AmortizationEntry::getAmortizationPeriod)
//                 .collect(Collectors.toList());
//         request.setSelectedPeriods(selectedPeriods);
//     }

//     /**
//      * 创建Mock合同数据
//      */
//     private void createMockContracts() {
//         // 创建第一组合同：2025年9月至2026年2月，每月800元
//         createContract1();
        
//         // 创建第二组合同：2025年10月至2026年3月，每月300元
//         createContract2();
        
//         // 创建第三组合同：2025年11月至2026年4月，每月500元
//         createContract3();
//     }

//     private void createContract1() {
//         Contract contract = new Contract();
//         contract.setVendorName("史密斯净水器租赁公司");
//         contract.setOriginalFileName("史密斯净水器租赁合同.pdf");
//         contract.setAttachmentName("contract_20250901_smith_water_purifier.pdf");
//         contract.setFilePath("/uploads/contracts/contract_20250901_smith_water_purifier.pdf");
//         contract.setStartDate(LocalDate.of(2025, 9, 1));
//         contract.setEndDate(LocalDate.of(2026, 2, 28));
//         contract.setTotalAmount(new BigDecimal("4800.00"));
//         contract.setTaxRate(new BigDecimal("0.13"));
//         contract.setCreatedBy("system");
//         contract.setUpdatedBy("system");
        
//         Contract savedContract1 = contractRepository.save(contract);
        
//         // 创建摊销明细：2025年9月至2026年2月
//         List<AmortizationEntry> entries1 = new ArrayList<>();
//         YearMonth startMonth = YearMonth.of(2025, 9);
//         YearMonth endMonth = YearMonth.of(2026, 2);
        
//         YearMonth currentMonth = startMonth;
//         while (!currentMonth.isAfter(endMonth)) {
//             AmortizationEntry entry = new AmortizationEntry();
//             entry.setContract(savedContract1);
//             entry.setAmortizationPeriod(currentMonth.toString());
//             entry.setAccountingPeriod(currentMonth.toString());
//             entry.setAmount(new BigDecimal("800.00"));
//             entry.setPaidAmount(BigDecimal.ZERO);
//             entry.setPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
//             entry.setPeriodDate(currentMonth.atDay(27));
//             entry.setCreatedBy("system");
//             entry.setUpdatedBy("system");
            
//             entries1.add(entry);
//             currentMonth = currentMonth.plusMonths(1);
//         }
        
//         amortizationEntryRepository.saveAll(entries1);
//     }

//     private void createContract2() {
//         Contract contract = new Contract();
//         contract.setVendorName("美的空调租赁有限公司");
//         contract.setOriginalFileName("美的空调租赁协议.pdf");
//         contract.setAttachmentName("contract_20251001_midea_airconditioner.pdf");
//         contract.setFilePath("/uploads/contracts/contract_20251001_midea_airconditioner.pdf");
//         contract.setStartDate(LocalDate.of(2025, 10, 1));
//         contract.setEndDate(LocalDate.of(2026, 3, 31));
//         contract.setTotalAmount(new BigDecimal("1800.00"));
//         contract.setTaxRate(new BigDecimal("0.13"));
//         contract.setCreatedBy("system");
//         contract.setUpdatedBy("system");
        
//         Contract savedContract2 = contractRepository.save(contract);
        
//         // 创建摊销明细：2025年10月至2026年3月
//         List<AmortizationEntry> entries2 = new ArrayList<>();
//         YearMonth startMonth = YearMonth.of(2025, 10);
//         YearMonth endMonth = YearMonth.of(2026, 3);
        
//         YearMonth currentMonth = startMonth;
//         while (!currentMonth.isAfter(endMonth)) {
//             AmortizationEntry entry = new AmortizationEntry();
//             entry.setContract(savedContract2);
//             entry.setAmortizationPeriod(currentMonth.toString());
//             entry.setAccountingPeriod(currentMonth.toString());
//             entry.setAmount(new BigDecimal("300.00"));
//             entry.setPaidAmount(BigDecimal.ZERO);
//             entry.setPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
//             entry.setPeriodDate(currentMonth.atDay(27));
//             entry.setCreatedBy("system");
//             entry.setUpdatedBy("system");
            
//             entries2.add(entry);
//             currentMonth = currentMonth.plusMonths(1);
//         }
        
//         amortizationEntryRepository.saveAll(entries2);
//     }

//     private void createContract3() {
//         Contract contract = new Contract();
//         contract.setVendorName("海尔冰箱设备租赁公司");
//         contract.setOriginalFileName("海尔冰箱设备租赁合同.pdf");
//         contract.setAttachmentName("contract_20251101_haier_refrigerator.pdf");
//         contract.setFilePath("/uploads/contracts/contract_20251101_haier_refrigerator.pdf");
//         contract.setStartDate(LocalDate.of(2025, 11, 1));
//         contract.setEndDate(LocalDate.of(2026, 4, 30));
//         contract.setTotalAmount(new BigDecimal("3000.00"));
//         contract.setTaxRate(new BigDecimal("0.13"));
//         contract.setCreatedBy("system");
//         contract.setUpdatedBy("system");
        
//         Contract savedContract3 = contractRepository.save(contract);
        
//         // 创建摊销明细：2025年11月至2026年4月
//         List<AmortizationEntry> entries3 = new ArrayList<>();
//         YearMonth startMonth = YearMonth.of(2025, 11);
//         YearMonth endMonth = YearMonth.of(2026, 4);
        
//         YearMonth currentMonth = startMonth;
//         while (!currentMonth.isAfter(endMonth)) {
//             AmortizationEntry entry = new AmortizationEntry();
//             entry.setContract(savedContract3);
//             entry.setAmortizationPeriod(currentMonth.toString());
//             entry.setAccountingPeriod(currentMonth.toString());
//             entry.setAmount(new BigDecimal("500.00"));
//             entry.setPaidAmount(BigDecimal.ZERO);
//             entry.setPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
//             entry.setPeriodDate(currentMonth.atDay(27));
//             entry.setCreatedBy("system");
//             entry.setUpdatedBy("system");
            
//             entries3.add(entry);
//             currentMonth = currentMonth.plusMonths(1);
//         }
        
//         amortizationEntryRepository.saveAll(entries3);
//     }
// }
