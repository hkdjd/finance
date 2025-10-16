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

        BigDecimal payment = req.getPaymentAmount().setScale(2, RoundingMode.HALF_UP);
        
        List<JournalEntryDto> entries = new ArrayList<>();
        
        // 按时间顺序分配付款金额
        BigDecimal remainingPayment = payment;
        
        for (AmortizationEntryDto entry : selected) {
            // 获取实际的摊销条目以检查已付金额
            AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId()))
                    .orElse(null);
            
            BigDecimal entryAmount = entry.getAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingAmountForEntry = actualEntry != null ? 
                    actualEntry.getRemainingAmount().setScale(2, RoundingMode.HALF_UP) : entryAmount;
            
            LocalDate entryBookingDate = bookingDateForPeriod(entry.getAccountingPeriod(), bookingDate);
            
            if (remainingAmountForEntry.compareTo(BigDecimal.ZERO) <= 0) {
                // 该条目已完全付款，跳过
                continue;
            }
            
            // 检查该摊销条目是否已经有付款会计分录（应付科目）
            // 通过摊销条目ID获取合同ID
            Long contractId = actualEntry != null ? actualEntry.getContract().getId() : null;
            boolean hasExistingPaymentEntry = false;
            if (contractId != null) {
                hasExistingPaymentEntry = journalEntryRepository.existsByContractIdAndAccountNameAndEntryTypeAndMemo(
                        contractId, "应付", JournalEntry.EntryType.PAYMENT, memo(entry));
            }
            
            // 只有在没有现有付款分录时才生成新的应付分录
            if (!hasExistingPaymentEntry) {
                // 对于每条预摊支付记录，仅生成一条应付款项分录（借方显示原始金额）
                entries.add(new JournalEntryDto(entryBookingDate,
                        "应付", entryAmount, BigDecimal.ZERO, memo(entry)));
            }
            
            // 更新剩余付款金额（用于计算多付或不足）
            if (remainingPayment.compareTo(remainingAmountForEntry) >= 0) {
                // 付款金额足够覆盖剩余应付金额
                remainingPayment = remainingPayment.subtract(remainingAmountForEntry);
            } else if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
                // 付款金额不足，但不在此处生成贷方分录
                remainingPayment = BigDecimal.ZERO;
                break; // 付款金额已用完，停止处理后续期间
            }
        }
        
        // 计算总的应付金额和实际付款金额的差额
        BigDecimal totalSelectedAmount = selected.stream()
                .filter(entry -> {
                    AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
                    return actualEntry == null || actualEntry.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
                })
                .map(entry -> {
                    AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
                    return actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal paymentDifference = payment.subtract(totalSelectedAmount);
        
        if (paymentDifference.compareTo(BigDecimal.ZERO) > 0) {
            // 多付情况
            BigDecimal minMonthly = selected.stream().map(AmortizationEntryDto::getAmount)
                    .min(Comparator.naturalOrder()).orElse(BigDecimal.valueOf(1000));
            BigDecimal tiny = minMonthly.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            
            if (paymentDifference.compareTo(tiny) <= 0) {
                // 小额多付：借费用
                entries.add(new JournalEntryDto(bookingDate, "费用", paymentDifference, BigDecimal.ZERO, "多付小额"));
            } else {
                // 大额多付：借预付
                entries.add(new JournalEntryDto(bookingDate, "预付", paymentDifference, BigDecimal.ZERO, "多付预付款"));
            }
        } else if (paymentDifference.compareTo(BigDecimal.ZERO) < 0) {
            // 付款不足情况：在费用科目贷方显示不足金额
            BigDecimal shortage = paymentDifference.abs();
            entries.add(new JournalEntryDto(bookingDate, "费用", BigDecimal.ZERO, shortage, "付款不足"));
        }
        
        // 贷：活期存款（实际付款金额）
        entries.add(new JournalEntryDto(bookingDate, "活期存款", BigDecimal.ZERO, payment, "付款"));
        
        // 验证借贷平衡
        BigDecimal totalDebitAmount = entries.stream()
                .map(JournalEntryDto::getDr)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal totalCreditAmount = entries.stream()
                .map(JournalEntryDto::getCr)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // 如果借贷不平衡，记录日志（调试用）
        if (totalDebitAmount.compareTo(totalCreditAmount) != 0) {
            System.out.println("警告：借贷不平衡 - 借方总额: " + totalDebitAmount + ", 贷方总额: " + totalCreditAmount);
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

        // 更新摊销条目的付款状态 - 基于实际付款分配结果
        // 重新计算付款分配以确定哪些期间完全付款
        BigDecimal remainingPayment = request.getPaymentAmount().setScale(2, RoundingMode.HALF_UP);
        
        // 按时间顺序获取选中的摊销条目
        List<AmortizationEntry> selectedEntries = new ArrayList<>();
        for (Long entryId : request.getSelectedPeriods()) {
            AmortizationEntry entry = amortizationEntryRepository.findById(entryId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));
            selectedEntries.add(entry);
        }
        
        // 按摊销期间排序
        selectedEntries.sort(Comparator.comparing(AmortizationEntry::getAmortizationPeriod));
        
        // 按顺序分配付款并更新状态（基于累积付款逻辑）
        for (AmortizationEntry entry : selectedEntries) {
            BigDecimal remainingAmountForEntry = entry.getRemainingAmount().setScale(2, RoundingMode.HALF_UP);
            
            System.out.println("处理摊销条目: ID=" + entry.getId() + 
                             ", 期间=" + entry.getAmortizationPeriod() + 
                             ", 总金额=" + entry.getAmount() + 
                             ", 已付金额=" + entry.getPaidAmount() + 
                             ", 剩余应付=" + remainingAmountForEntry + 
                             ", 剩余付款=" + remainingPayment);
            
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                // 没有剩余付款金额
                System.out.println("无剩余付款金额，跳过: ID=" + entry.getId());
                break;
            }
            
            if (remainingAmountForEntry.compareTo(BigDecimal.ZERO) <= 0) {
                // 该条目已完全付款，跳过
                System.out.println("条目已完全付款，跳过: ID=" + entry.getId());
                continue;
            }
            
            if (remainingPayment.compareTo(remainingAmountForEntry) >= 0) {
                // 付款金额足够覆盖剩余应付金额
                entry.addPayment(remainingAmountForEntry);
                remainingPayment = remainingPayment.subtract(remainingAmountForEntry);
                System.out.println("完全付款: ID=" + entry.getId() + 
                                 ", 付款金额=" + remainingAmountForEntry + 
                                 ", 状态=" + entry.getPaymentStatus() + 
                                 ", 剩余付款=" + remainingPayment);
            } else {
                // 付款金额不足，部分付款
                entry.addPayment(remainingPayment);
                System.out.println("部分付款: ID=" + entry.getId() + 
                                 ", 付款金额=" + remainingPayment + 
                                 ", 新累积金额=" + entry.getPaidAmount() + 
                                 ", 剩余应付=" + entry.getRemainingAmount() + 
                                 ", 状态=" + entry.getPaymentStatus());
                remainingPayment = BigDecimal.ZERO;
            }
            
            amortizationEntryRepository.save(entry);
        }

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
