package com.ocbc.finance.service.calculation;

import com.ocbc.finance.dto.AmortizationEntryDto;
import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.CalculateAmortizationRequest;
import com.ocbc.finance.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class AmortizationCalculationServiceTest {

    private AmortizationCalculationService service;
    
    @Mock
    private ContractRepository contractRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AmortizationCalculationService(contractRepository);
    }

    @Test
    void testScenario1_CurrentBeforeStart() {
        // 当前时间小于合同开始时间
        YearMonth futureStart = YearMonth.now().plusMonths(6);
        YearMonth futureEnd = futureStart.plusMonths(2);
        
        CalculateAmortizationRequest req = new CalculateAmortizationRequest();
        req.setTotalAmount(new BigDecimal("3000"));
        req.setStartDate(futureStart.toString());
        req.setEndDate(futureEnd.toString());
        req.setTaxRate(BigDecimal.ZERO);
        req.setVendorName("测试供应商");

        AmortizationResponse resp = service.calculate(req);

        assertEquals("SCENARIO_1", resp.getScenario());
        assertEquals(3, resp.getEntries().size());
        
        // 验证金额分配：3000 / 3 = 1000
        for (AmortizationEntryDto entry : resp.getEntries()) {
            assertEquals(new BigDecimal("1000.00"), entry.getAmount());
            assertEquals(entry.getAmortizationPeriod(), entry.getAccountingPeriod());
        }
        
        // 验证总额
        BigDecimal total = resp.getEntries().stream()
                .map(AmortizationEntryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("3000.00"), total);
    }

    @Test
    void testScenario3_CurrentAfterEnd() {
        // 当前时间大于合同结束时间
        YearMonth pastStart = YearMonth.now().minusMonths(6);
        YearMonth pastEnd = YearMonth.now().minusMonths(3);
        
        CalculateAmortizationRequest req = new CalculateAmortizationRequest();
        req.setTotalAmount(new BigDecimal("5000"));
        req.setStartDate(pastStart.toString());
        req.setEndDate(pastEnd.toString());
        req.setTaxRate(BigDecimal.ZERO);
        req.setVendorName("测试供应商");

        AmortizationResponse resp = service.calculate(req);

        assertEquals("SCENARIO_3", resp.getScenario());
        assertEquals(1, resp.getEntries().size());
        
        AmortizationEntryDto entry = resp.getEntries().get(0);
        assertEquals(new BigDecimal("5000.00"), entry.getAmount());
        
        // 当前月份
        YearMonth current = YearMonth.now();
        assertEquals(current.toString(), entry.getAmortizationPeriod());
        assertEquals(current.toString(), entry.getAccountingPeriod());
    }

    @Test
    void testAmountPrecision_LastMonthAdjustment() {
        // 测试金额精度和最后一月调整
        YearMonth futureStart = YearMonth.now().plusMonths(6);
        YearMonth futureEnd = futureStart.plusMonths(2);
        
        CalculateAmortizationRequest req = new CalculateAmortizationRequest();
        req.setTotalAmount(new BigDecimal("1000")); // 1000 / 3 = 333.33...
        req.setStartDate(futureStart.toString());
        req.setEndDate(futureEnd.toString());
        req.setTaxRate(BigDecimal.ZERO);
        req.setVendorName("测试供应商");

        AmortizationResponse resp = service.calculate(req);

        assertEquals(3, resp.getEntries().size());
        
        // 前两个月应该是 333.33
        assertEquals(new BigDecimal("333.33"), resp.getEntries().get(0).getAmount());
        assertEquals(new BigDecimal("333.33"), resp.getEntries().get(1).getAmount());
        
        // 最后一个月应该是 333.34 (调整差额)
        assertEquals(new BigDecimal("333.34"), resp.getEntries().get(2).getAmount());
        
        // 验证总额精确等于 1000.00
        BigDecimal total = resp.getEntries().stream()
                .map(AmortizationEntryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1000.00"), total);
    }

    @Test
    void testDateFormatSupport() {
        // 测试日期格式支持
        YearMonth futureStart = YearMonth.now().plusMonths(6);
        YearMonth futureEnd = futureStart.plusMonths(1);
        
        CalculateAmortizationRequest req1 = new CalculateAmortizationRequest();
        req1.setTotalAmount(new BigDecimal("1000"));
        req1.setStartDate(futureStart.atDay(15).toString()); // yyyy-MM-dd 格式
        req1.setEndDate(futureEnd.atEndOfMonth().toString());
        req1.setTaxRate(BigDecimal.ZERO);
        req1.setVendorName("测试供应商");

        AmortizationResponse resp1 = service.calculate(req1);
        assertEquals(2, resp1.getEntries().size());
        assertEquals(futureStart.toString(), resp1.getStartDate());
        assertEquals(futureEnd.toString(), resp1.getEndDate());

        CalculateAmortizationRequest req2 = new CalculateAmortizationRequest();
        req2.setTotalAmount(new BigDecimal("1000"));
        req2.setStartDate(futureStart.toString()); // yyyy-MM 格式
        req2.setEndDate(futureEnd.toString());
        req2.setTaxRate(BigDecimal.ZERO);
        req2.setVendorName("测试供应商");

        AmortizationResponse resp2 = service.calculate(req2);
        assertEquals(2, resp2.getEntries().size());
        assertEquals(futureStart.toString(), resp2.getStartDate());
        assertEquals(futureEnd.toString(), resp2.getEndDate());
    }

    @Test
    void testInvalidDateFormat() {
        CalculateAmortizationRequest req = new CalculateAmortizationRequest();
        req.setTotalAmount(new BigDecimal("1000"));
        req.setStartDate("invalid-date");
        req.setEndDate(YearMonth.now().plusMonths(1).toString());
        req.setTaxRate(BigDecimal.ZERO);
        req.setVendorName("测试供应商");

        assertThrows(IllegalArgumentException.class, () -> service.calculate(req));
    }

    @Test
    void testEndBeforeStart() {
        YearMonth start = YearMonth.now().plusMonths(3);
        YearMonth end = YearMonth.now().plusMonths(1);
        
        CalculateAmortizationRequest req = new CalculateAmortizationRequest();
        req.setTotalAmount(new BigDecimal("1000"));
        req.setStartDate(start.toString());
        req.setEndDate(end.toString()); // 结束时间早于开始时间
        req.setTaxRate(BigDecimal.ZERO);
        req.setVendorName("测试供应商");

        assertThrows(IllegalArgumentException.class, () -> service.calculate(req));
    }
}
