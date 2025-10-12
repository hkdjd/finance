package com.ocbc.finance.service;

import com.ocbc.finance.dto.*;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.JournalEntry;
import com.ocbc.finance.model.Payment;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.JournalEntryRepository;
import com.ocbc.finance.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;
    private final ContractService contractService;

    public PaymentService(PaymentRepository paymentRepository,
                         JournalEntryRepository journalEntryRepository,
                         ContractRepository contractRepository,
                         AmortizationEntryRepository amortizationEntryRepository,
                         ContractService contractService) {
        this.paymentRepository = paymentRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
        this.contractService = contractService;
    }

    public PaymentPreviewResponse preview(PaymentRequest req) {
        AmortizationResponse amort = Objects.requireNonNull(req.getAmortization(), "amortization为空");
        LocalDate bookingDate = req.getBookingDate() != null ? req.getBookingDate() : LocalDate.now();

        // 选中期间与摊销条目映射
        Map<String, AmortizationEntryDto> byPeriod = amort.getEntries().stream()
                .collect(Collectors.toMap(AmortizationEntryDto::getAmortizationPeriod, e -> e, (a, b) -> a));
        List<AmortizationEntryDto> selected = req.getSelectedPeriods() == null ? new ArrayList<>() :
                req.getSelectedPeriods().stream()
                        .map(byPeriod::get)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(AmortizationEntryDto::getAmortizationPeriod))
                        .collect(Collectors.toList());

        BigDecimal selectedSum = selected.stream()
                .map(AmortizationEntryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal payment = req.getPaymentAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal delta = payment.subtract(selectedSum).setScale(2, RoundingMode.HALF_UP);

        // 阈值：判断“微小差额”走费用还是预付/应付
        BigDecimal minMonthly = selected.stream().map(AmortizationEntryDto::getAmount)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.valueOf(1000));
        BigDecimal tiny = minMonthly.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP); // 10% 作为阈值

        List<JournalEntryDto> entries = new ArrayList<>();

        // 借：应付（逐期间）
        for (AmortizationEntryDto e : selected) {
            entries.add(new JournalEntryDto(bookingDateForPeriod(e.getAccountingPeriod(), bookingDate),
                    "应付", e.getAmount(), BigDecimal.ZERO, memo(e)));
        }

        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            // 平账
            entries.add(new JournalEntryDto(bookingDate, "活期存款", BigDecimal.ZERO, payment, "payment"));
        } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
            // 多付
            BigDecimal over = delta;
            if (over.compareTo(tiny) <= 0) {
                // 小额：借费用
                entries.add(new JournalEntryDto(bookingDate, "费用", over, BigDecimal.ZERO, "over small"));
            } else {
                // 大额：借预付
                entries.add(new JournalEntryDto(bookingDate, "预付", over, BigDecimal.ZERO, "over prepayment"));
            }
            entries.add(new JournalEntryDto(bookingDate, "活期存款", BigDecimal.ZERO, payment, "payment"));
        } else {
            // 少付
            BigDecimal shortage = delta.abs();
            if (shortage.compareTo(tiny) <= 0) {
                // 小额：贷费用
                entries.add(new JournalEntryDto(bookingDate, "费用", BigDecimal.ZERO, shortage, "short small"));
            } else {
                // 大额：贷预付（代表未来冲减）
                entries.add(new JournalEntryDto(bookingDate, "预付", BigDecimal.ZERO, shortage, "short prepayment"));
            }
            entries.add(new JournalEntryDto(bookingDate, "活期存款", BigDecimal.ZERO, payment, "payment"));
        }

        return PaymentPreviewResponse.builder()
                .paymentAmount(payment)
                .entries(entries)
                .build();
    }

    private LocalDate bookingDateForPeriod(String accountingPeriod, LocalDate fallback) {
        try {
            YearMonth ym = YearMonth.parse(accountingPeriod);
            int day = Math.min(27, ym.lengthOfMonth());
            return ym.atDay(day);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String memo(AmortizationEntryDto e) {
        return "period:" + e.getAmortizationPeriod();
    }

    /**
     * 执行付款 - 步骤4付款阶段
     */
    @Transactional
    public PaymentExecutionResponse executePayment(PaymentExecutionRequest request) {
        // 验证合同存在
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + request.getContractId()));

        // 获取合同的摊销数据
        AmortizationResponse amortization = contractService.getContractAmortization(request.getContractId());

        // 构建PaymentRequest用于预览计算
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmortization(amortization);
        paymentRequest.setPaymentAmount(request.getPaymentAmount());
        paymentRequest.setBookingDate(request.getBookingDate() != null ? request.getBookingDate() : LocalDate.now());
        
        // 根据摊销明细ID查询对应的期间值
        List<String> periodStrings = new ArrayList<>();
        for (Long entryId : request.getSelectedPeriods()) {
            AmortizationEntry entry = amortizationEntryRepository.findById(entryId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));
            periodStrings.add(entry.getAmortizationPeriod());
        }
        paymentRequest.setSelectedPeriods(periodStrings);

        // 计算会计分录
        PaymentPreviewResponse previewResponse = preview(paymentRequest);

        // 创建付款记录
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPaymentAmount(request.getPaymentAmount());
        payment.setBookingDate(paymentRequest.getBookingDate());
        payment.setSelectedPeriods(String.join(",", periodStrings));
        payment.setStatus(Payment.PaymentStatus.CONFIRMED);

        payment = paymentRepository.save(payment);

        // 创建会计分录记录（步骤4：付款阶段），标记为PAYMENT类型，并关联合同与付款
        List<JournalEntry> journalEntries = new ArrayList<>();
        int order = 1;
        for (JournalEntryDto dto : previewResponse.getEntries()) {
            JournalEntry entry = new JournalEntry();
            entry.setContract(contract);
            entry.setPayment(payment);
            entry.setBookingDate(dto.getBookingDate());
            entry.setAccountName(dto.getAccount());
            entry.setDebitAmount(dto.getDr());
            entry.setCreditAmount(dto.getCr());
            entry.setMemo(dto.getMemo());
            entry.setEntryType(JournalEntry.EntryType.PAYMENT);
            entry.setEntryOrder(order++);
            journalEntries.add(entry);
        }
        journalEntryRepository.saveAll(journalEntries);

        // 构建响应
        return PaymentExecutionResponse.builder()
                .paymentId(payment.getId())
                .contractId(contract.getId())
                .paymentAmount(payment.getPaymentAmount())
                .bookingDate(payment.getBookingDate())
                .selectedPeriods(List.of(payment.getSelectedPeriods().split(",")))
                .status(payment.getStatus().name())
                .journalEntries(previewResponse.getEntries())
                .message("付款执行成功")
                .build();
    }

    /**
     * 查询合同的付款记录
     */
    public List<PaymentExecutionResponse> getPaymentsByContract(Long contractId) {
        List<Payment> payments = paymentRepository.findByContractIdOrderByBookingDateDesc(contractId);
        return payments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询单个付款详情
     */
    public PaymentExecutionResponse getPaymentDetail(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到付款记录，ID=" + paymentId));
        return convertToResponse(payment);
    }

    /**
     * 取消付款
     */
    @Transactional
    public PaymentExecutionResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到付款记录，ID=" + paymentId));

        if (payment.getStatus() == Payment.PaymentStatus.CANCELLED) {
            throw new IllegalStateException("付款已经被取消");
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);

        return PaymentExecutionResponse.builder()
                .paymentId(payment.getId())
                .contractId(payment.getContract().getId())
                .paymentAmount(payment.getPaymentAmount())
                .bookingDate(payment.getBookingDate())
                .selectedPeriods(List.of(payment.getSelectedPeriods().split(",")))
                .status(payment.getStatus().name())
                .message("付款已取消")
                .build();
    }

    /**
     * 转换Payment实体为响应DTO
     */
    private PaymentExecutionResponse convertToResponse(Payment payment) {
        List<JournalEntry> entries = journalEntryRepository.findByPaymentIdOrderByEntryOrderAsc(payment.getId());
        List<JournalEntryDto> entryDtos = entries.stream()
                .map(e -> new JournalEntryDto(
                        e.getBookingDate(),
                        e.getAccountName(),  // account字段
                        e.getDebitAmount(),  // dr字段
                        e.getCreditAmount(), // cr字段
                        e.getMemo()
                ))
                .collect(Collectors.toList());

        return PaymentExecutionResponse.builder()
                .paymentId(payment.getId())
                .contractId(payment.getContract().getId())
                .paymentAmount(payment.getPaymentAmount())
                .bookingDate(payment.getBookingDate())
                .selectedPeriods(List.of(payment.getSelectedPeriods().split(",")))
                .status(payment.getStatus().name())
                .journalEntries(entryDtos)
                .build();
    }
}
