package com.ocbc.finance.service;

import com.ocbc.finance.dto.AmortizationEntryDto;
import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.JournalEntryDto;
import com.ocbc.finance.dto.PaymentPreviewResponse;
import com.ocbc.finance.dto.PaymentRequest;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.JournalEntryRepository;
import com.ocbc.finance.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private JournalEntryRepository journalEntryRepository;
    
    @Mock
    private ContractRepository contractRepository;
    
    @Mock
    private AmortizationEntryRepository amortizationEntryRepository;
    
    @Mock
    private ContractService contractService;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, journalEntryRepository, contractRepository, amortizationEntryRepository, contractService);
    }

    private AmortizationResponse createMockAmortization() {
        List<AmortizationEntryDto> entries = Arrays.asList(
                new AmortizationEntryDto(1L, "2024-01", "2024-01", new BigDecimal("1000.00")),
                new AmortizationEntryDto(2L, "2024-02", "2024-02", new BigDecimal("1000.00")),
                new AmortizationEntryDto(3L, "2024-03", "2024-03", new BigDecimal("1000.00")),
                new AmortizationEntryDto(4L, "2024-04", "2024-04", new BigDecimal("1000.00")),
                new AmortizationEntryDto(5L, "2024-05", "2024-05", new BigDecimal("1000.00")),
                new AmortizationEntryDto(6L, "2024-06", "2024-06", new BigDecimal("1000.00"))
        );
        return AmortizationResponse.builder()
                .totalAmount(new BigDecimal("6000.00"))
                .entries(entries)
                .build();
    }

    @Test
    void testPaymentExactMatch() {
        // 情形：付款金额正好等于勾选期间总额
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(createMockAmortization());
        req.setPaymentAmount(new BigDecimal("2000")); // 1000 + 1000
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList("2024-01", "2024-02"));

        PaymentPreviewResponse resp = service.preview(req);

        assertEquals(new BigDecimal("2000.00"), resp.getPaymentAmount());
        assertEquals(3, resp.getEntries().size());

        // 验证应付分录
        List<JournalEntryDto> payableEntries = resp.getEntries().stream()
                .filter(e -> "应付".equals(e.getAccount()) && e.getDr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(2, payableEntries.size());
        assertEquals(new BigDecimal("1000.00"), payableEntries.get(0).getDr());
        assertEquals(new BigDecimal("1000.00"), payableEntries.get(1).getDr());

        // 验证活期存款分录
        List<JournalEntryDto> cashEntries = resp.getEntries().stream()
                .filter(e -> "活期存款".equals(e.getAccount()))
                .toList();
        assertEquals(1, cashEntries.size());
        assertEquals(new BigDecimal("2000.00"), cashEntries.get(0).getCr());
    }

    @Test
    void testPaymentOverpaySmall() {
        // 情形2.2：多付小额（1元）
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(createMockAmortization());
        req.setPaymentAmount(new BigDecimal("2001")); // 多付1元
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList("2024-01", "2024-02"));

        PaymentPreviewResponse resp = service.preview(req);

        assertEquals(new BigDecimal("2001.00"), resp.getPaymentAmount());

        // 验证费用分录（小额差异）
        List<JournalEntryDto> expenseEntries = resp.getEntries().stream()
                .filter(e -> "费用".equals(e.getAccount()) && e.getDr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(1, expenseEntries.size());
        assertEquals(new BigDecimal("1.00"), expenseEntries.get(0).getDr());
    }

    @Test
    void testPaymentUnderpaySmall() {
        // 情形2.3：少付小额（1元）
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(createMockAmortization());
        req.setPaymentAmount(new BigDecimal("1999")); // 少付1元
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList("2024-01", "2024-02"));

        PaymentPreviewResponse resp = service.preview(req);

        assertEquals(new BigDecimal("1999.00"), resp.getPaymentAmount());

        // 验证费用分录（小额差异，贷方）
        List<JournalEntryDto> expenseEntries = resp.getEntries().stream()
                .filter(e -> "费用".equals(e.getAccount()) && e.getCr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(1, expenseEntries.size());
        assertEquals(new BigDecimal("1.00"), expenseEntries.get(0).getCr());
    }

    @Test
    void testPaymentOverpayLarge() {
        // 情形2.4/2.5：多付大额，走预付
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(createMockAmortization());
        req.setPaymentAmount(new BigDecimal("6001")); // 多付1元，但选择全部期间
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList("2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06"));

        PaymentPreviewResponse resp = service.preview(req);

        assertEquals(new BigDecimal("6001.00"), resp.getPaymentAmount());

        // 验证费用分录（1元差异应该走费用，因为阈值是100）
        List<JournalEntryDto> expenseEntries = resp.getEntries().stream()
                .filter(e -> "费用".equals(e.getAccount()) && e.getDr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(1, expenseEntries.size());
        assertEquals(new BigDecimal("1.00"), expenseEntries.get(0).getDr());

        // 验证应付分录数量
        List<JournalEntryDto> payableEntries = resp.getEntries().stream()
                .filter(e -> "应付".equals(e.getAccount()) && e.getDr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(6, payableEntries.size()); // 6个月
    }

    @Test
    void testPaymentPartialPeriods() {
        // 测试部分期间付款
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(createMockAmortization());
        req.setPaymentAmount(new BigDecimal("3000")); // 选择3个月
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList("2024-01", "2024-03", "2024-05")); // 非连续期间

        PaymentPreviewResponse resp = service.preview(req);

        assertEquals(new BigDecimal("3000.00"), resp.getPaymentAmount());

        // 验证应付分录
        List<JournalEntryDto> payableEntries = resp.getEntries().stream()
                .filter(e -> "应付".equals(e.getAccount()) && e.getDr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(3, payableEntries.size());
    }

    @Test
    void testPaymentNullAmortization() {
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(null);
        req.setPaymentAmount(new BigDecimal("1000"));
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList("2024-01"));

        assertThrows(NullPointerException.class, () -> service.preview(req));
    }

    @Test
    void testPaymentEmptySelectedPeriods() {
        PaymentRequest req = new PaymentRequest();
        req.setAmortization(createMockAmortization());
        req.setPaymentAmount(new BigDecimal("1000"));
        req.setBookingDate(LocalDate.of(2024, 3, 20));
        req.setSelectedPeriods(Arrays.asList()); // 空列表

        PaymentPreviewResponse resp = service.preview(req);

        // 没有选择期间，应该只有活期存款和费用/预付分录
        List<JournalEntryDto> payableEntries = resp.getEntries().stream()
                .filter(e -> "应付".equals(e.getAccount()))
                .toList();
        assertEquals(0, payableEntries.size());

        // 全额走预付（大额差异）
        List<JournalEntryDto> prepaidEntries = resp.getEntries().stream()
                .filter(e -> "预付".equals(e.getAccount()) && e.getDr().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(1, prepaidEntries.size());
        assertEquals(new BigDecimal("1000.00"), prepaidEntries.get(0).getDr());
    }
}
