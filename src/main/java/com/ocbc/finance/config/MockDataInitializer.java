package com.ocbc.finance.config;

import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock数据初始化器
 * 创建三组合同信息用于测试
 */
@Component
public class MockDataInitializer implements CommandLineRunner {

    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public MockDataInitializer(ContractRepository contractRepository,
                              AmortizationEntryRepository amortizationEntryRepository) {
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 检查是否已经初始化过数据，避免重复初始化
        long existingContractCount = contractRepository.count();
        if (existingContractCount > 0) {
            System.out.println("Mock数据已存在（" + existingContractCount + "个合同），跳过初始化");
            return;
        }
        
        System.out.println("开始初始化Mock合同数据...");

        // 创建第一组合同：2025年9月至2026年2月，每月800元
        createContract1();
        
        // 创建第二组合同：随机期间，每月300元
        createContract2();
        
        // 创建第三组合同：随机期间，每月500元
        createContract3();

        System.out.println("Mock合同数据初始化完成！");
    }


    /**
     * 创建第一组合同：2025年9月至2026年2月，每月800元
     */
    private void createContract1() {
        Contract contract = new Contract();
        contract.setVendorName("史密斯净水器租赁公司");
        contract.setOriginalFileName("史密斯净水器租赁合同.pdf");
        contract.setAttachmentName("contract_20250901_smith_water_purifier.pdf");
        contract.setFilePath("/uploads/contracts/contract_20250901_smith_water_purifier.pdf");
        contract.setStartDate(LocalDate.of(2025, 9, 1));
        contract.setEndDate(LocalDate.of(2026, 2, 28));
        contract.setTotalAmount(new BigDecimal("4800.00")); // 6个月 * 800元
        contract.setTaxRate(new BigDecimal("0.13"));
        contract.setCreatedBy("system");
        contract.setUpdatedBy("system");
        
        Contract savedContract1 = contractRepository.save(contract);
        System.out.println("创建合同1，ID: " + savedContract1.getId());
        
        // 创建摊销明细：2025年9月至2026年2月
        List<AmortizationEntry> entries1 = new ArrayList<>();
        YearMonth startMonth = YearMonth.of(2025, 9);
        YearMonth endMonth = YearMonth.of(2026, 2);
        
        YearMonth currentMonth = startMonth;
        while (!currentMonth.isAfter(endMonth)) {
            AmortizationEntry entry = new AmortizationEntry();
            entry.setContract(savedContract1);
            entry.setAmortizationPeriod(currentMonth.toString());
            entry.setAccountingPeriod(currentMonth.toString());
            entry.setAmount(new BigDecimal("800.00"));
            entry.setPaidAmount(BigDecimal.ZERO);
            entry.setPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
            entry.setPeriodDate(currentMonth.atDay(27)); // 每月27日
            entry.setCreatedBy("system");
            entry.setUpdatedBy("system");
            
            entries1.add(entry);
            currentMonth = currentMonth.plusMonths(1);
        }
        
        amortizationEntryRepository.saveAll(entries1);
        System.out.println("创建合同1：史密斯净水器租赁合同，摊销期间2025-09至2026-02，每月800元");
    }

    /**
     * 创建第二组合同：2025年10月至2026年3月，每月300元
     */
    private void createContract2() {
        Contract contract = new Contract();
        contract.setVendorName("美的空调租赁有限公司");
        contract.setOriginalFileName("美的空调租赁协议.pdf");
        contract.setAttachmentName("contract_20251001_midea_airconditioner.pdf");
        contract.setFilePath("/uploads/contracts/contract_20251001_midea_airconditioner.pdf");
        contract.setStartDate(LocalDate.of(2025, 10, 1));
        contract.setEndDate(LocalDate.of(2026, 3, 31));
        contract.setTotalAmount(new BigDecimal("1800.00")); // 6个月 * 300元
        contract.setTaxRate(new BigDecimal("0.13"));
        contract.setCreatedBy("system");
        contract.setUpdatedBy("system");
        
        Contract savedContract2 = contractRepository.save(contract);
        System.out.println("创建合同2，ID: " + savedContract2.getId());
        
        // 创建摊销明细：2025年10月至2026年3月
        List<AmortizationEntry> entries2 = new ArrayList<>();
        YearMonth startMonth = YearMonth.of(2025, 10);
        YearMonth endMonth = YearMonth.of(2026, 3);
        
        YearMonth currentMonth = startMonth;
        while (!currentMonth.isAfter(endMonth)) {
            AmortizationEntry entry = new AmortizationEntry();
            entry.setContract(savedContract2);
            entry.setAmortizationPeriod(currentMonth.toString());
            entry.setAccountingPeriod(currentMonth.toString());
            entry.setAmount(new BigDecimal("300.00"));
            entry.setPaidAmount(BigDecimal.ZERO);
            entry.setPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
            entry.setPeriodDate(currentMonth.atDay(27)); // 每月27日
            entry.setCreatedBy("system");
            entry.setUpdatedBy("system");
            
            entries2.add(entry);
            currentMonth = currentMonth.plusMonths(1);
        }
        
        amortizationEntryRepository.saveAll(entries2);
        System.out.println("创建合同2：美的空调租赁协议，摊销期间2025-10至2026-03，每月300元");
    }

    /**
     * 创建第三组合同：2025年11月至2026年4月，每月500元
     */
    private void createContract3() {
        Contract contract = new Contract();
        contract.setVendorName("海尔冰箱设备租赁公司");
        contract.setOriginalFileName("海尔冰箱设备租赁合同.pdf");
        contract.setAttachmentName("contract_20251101_haier_refrigerator.pdf");
        contract.setFilePath("/uploads/contracts/contract_20251101_haier_refrigerator.pdf");
        contract.setStartDate(LocalDate.of(2025, 11, 1));
        contract.setEndDate(LocalDate.of(2026, 4, 30));
        contract.setTotalAmount(new BigDecimal("3000.00")); // 6个月 * 500元
        contract.setTaxRate(new BigDecimal("0.13"));
        contract.setCreatedBy("system");
        contract.setUpdatedBy("system");
        
        Contract savedContract3 = contractRepository.save(contract);
        System.out.println("创建合同3，ID: " + savedContract3.getId());
        
        // 创建摊销明细：2025年11月至2026年4月
        List<AmortizationEntry> entries3 = new ArrayList<>();
        YearMonth startMonth = YearMonth.of(2025, 11);
        YearMonth endMonth = YearMonth.of(2026, 4);
        
        YearMonth currentMonth = startMonth;
        while (!currentMonth.isAfter(endMonth)) {
            AmortizationEntry entry = new AmortizationEntry();
            entry.setContract(savedContract3);
            entry.setAmortizationPeriod(currentMonth.toString());
            entry.setAccountingPeriod(currentMonth.toString());
            entry.setAmount(new BigDecimal("500.00"));
            entry.setPaidAmount(BigDecimal.ZERO);
            entry.setPaymentStatus(AmortizationEntry.PaymentStatus.PENDING);
            entry.setPeriodDate(currentMonth.atDay(27)); // 每月27日
            entry.setCreatedBy("system");
            entry.setUpdatedBy("system");
            
            entries3.add(entry);
            currentMonth = currentMonth.plusMonths(1);
        }
        
        amortizationEntryRepository.saveAll(entries3);
        System.out.println("创建合同3：海尔冰箱设备租赁合同，摊销期间2025-11至2026-04，每月500元");
    }
}
