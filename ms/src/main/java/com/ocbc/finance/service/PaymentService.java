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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;
    private final ContractService contractService;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentRepository paymentRepository,
                         JournalEntryRepository journalEntryRepository,
                         ContractRepository contractRepository,
                         AmortizationEntryRepository amortizationEntryRepository,
                         ContractService contractService,
                         AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
        this.contractService = contractService;
        this.auditLogService = auditLogService;
    }

    public PaymentPreviewResponse preview(PaymentRequest req) {
        log.info("开始执行付款预览 - req: {}", req);
        
        try {
            AmortizationResponse amort = Objects.requireNonNull(req.getAmortization(), "amortization为空");
            LocalDate paymentDate = req.getBookingDate() != null ? req.getBookingDate() : LocalDate.now();

            // 选中期间与摊销条目映射
            Map<String, AmortizationEntryDto> byPeriod = amort.getEntries().stream()
                    .collect(Collectors.toMap(AmortizationEntryDto::getAmortizationPeriod, e -> e, (a, b) -> a));
            List<AmortizationEntryDto> selected = req.getSelectedPeriods() == null ? new ArrayList<>() :
                    req.getSelectedPeriods().stream()
                            .map(byPeriod::get)
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparing(AmortizationEntryDto::getAmortizationPeriod))
                            .collect(Collectors.toList());

            BigDecimal paymentAmount = req.getPaymentAmount().setScale(2, RoundingMode.HALF_UP);
            
            PaymentPreviewResponse response = generatePaymentJournalEntries(selected, paymentAmount, paymentDate, req.getContractId(), req.getPaymentId());
            
            log.info("付款预览执行完成 - 生成分录数量: {}", response.getEntries().size());
            return response;
        } catch (Exception e) {
            log.error("付款预览执行失败", e);
            throw e;
        }
    }
    
    /**
     * 根据新的付款会计分录生成规则生成会计分录
     * 新需求第184行：根据付款时间判断是否为过去或未来付款
     * 过去付款：借方应付+贷方活期存款，差异费用调整
     * 未来付款：借方应付+贷方预付，按摊销期间顺序逐月转预付
     */
    private PaymentPreviewResponse generatePaymentJournalEntries(List<AmortizationEntryDto> selected, 
                                                               BigDecimal paymentAmount, 
                                                               LocalDate paymentDate, Long contractId, Long paymentId) {
        log.debug("开始生成付款会计分录 - 选中期间数量: {}, 付款金额: {}, 付款日期: {}", 
                selected.size(), paymentAmount, paymentDate);
        
        List<JournalEntryDto> entries = new ArrayList<>();
       
        if (selected.isEmpty()) {
            // 无预提摊销的情况：直接记费用 + 活期存款
            entries.add(new JournalEntryDto(paymentDate, "活期存款", BigDecimal.ZERO, paymentAmount, "付款",LocalDateTime.now()));
            entries.add(new JournalEntryDto(paymentDate, "费用", paymentAmount, BigDecimal.ZERO, "付款费用",LocalDateTime.now()));
            return PaymentPreviewResponse.builder().paymentAmount(paymentAmount).entries(entries).build();
        }
        
        // 计算总的预提摊销金额
        BigDecimal totalPreAccruedAmount = selected.stream()
                .map(entry -> {
                    AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
                    return actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // 根据付款时间判断是否为过去或未来付款
        YearMonth paymentYearMonth = YearMonth.from(paymentDate);
        
        // 分离过去期间和未来期间
        List<AmortizationEntryDto> pastPeriods = new ArrayList<>();
        List<AmortizationEntryDto> futurePeriods = new ArrayList<>();
        
        for (AmortizationEntryDto entry : selected) {
            YearMonth entryYearMonth = YearMonth.parse(entry.getAccountingPeriod());
            if (entryYearMonth.isAfter(paymentYearMonth)) {
                futurePeriods.add(entry);
            } else {
                pastPeriods.add(entry);
            }
        }
        
        // 计算过去期间的预提金额
        BigDecimal pastPeriodsAmount = pastPeriods.stream()
                .map(entry -> {
                    AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
                    return actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // 计算未来期间的预提金额
        BigDecimal futurePeriodsAmount = futurePeriods.stream()
                .map(entry -> {
                    AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
                    return actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // 处理过去付款：借方应付 + 贷方活期存款，差异费用调整
        if (!pastPeriods.isEmpty()) {
            generatePastPaymentEntries(entries, pastPeriods, paymentAmount, paymentDate, pastPeriodsAmount);
        }
        
        // 处理未来付款：借方应付 + 贷方预付，按摊销期间顺序逐月转预付
        if (!futurePeriods.isEmpty()) {
            generateFuturePaymentEntries(entries, futurePeriods, paymentAmount, paymentDate, futurePeriodsAmount, pastPeriodsAmount);
        }
        
        // 按新排序规则：生成时间 > 入账时间 > 会计科目优先级
        // 为每个分录添加生成顺序索引
        for (int i = 0; i < entries.size(); i++) {
            final int generationOrder = i;
            JournalEntryDto entry = entries.get(i);
            // 使用memo字段暂存生成顺序（在原有memo后面添加）
            String originalMemo = entry.getMemo();
            entry.setMemo(originalMemo + "#ORDER:" + generationOrder);
        }
        
        // 添加借贷平衡检查（第190行要求）
        BalanceCheckResult balanceCheck = checkDebitCreditBalance(entries);
        log.info("借贷平衡检查结果: {}", balanceCheck.getMessage());
        
        entries.sort(Comparator.<JournalEntryDto>comparingInt(entry -> entry.getBookingDate().getMonthValue())
                .thenComparing(entry -> entry.getCreatedAt())
                .thenComparingInt(entry -> {
                    // 第三优先级：同一入账日期内按会计科目优先级排序：应付 > 预付 > 费用 > 活期存款
                    String accountName = entry.getAccount();
                    switch (accountName) {
                        case "应付": return 1;
                        case "预付": return 2;
                        case "费用": return 3;
                        case "活期存款": return 4;
                        default: return 5;
                    }
                }));
        
        // 清理memo中的临时顺序标记
        for (JournalEntryDto entry : entries) {
            String memo = entry.getMemo();
            if (memo != null && memo.contains("#ORDER:")) {
                entry.setMemo(memo.substring(0, memo.lastIndexOf("#ORDER:")));
            }
        }
        
        PaymentPreviewResponse response = PaymentPreviewResponse.builder()
                .paymentAmount(paymentAmount)
                .contractId(contractId)
                .paymentId(paymentId)
                .entries(entries)
                .build();
        
        log.debug("付款会计分录生成完成 - 生成分录数量: {}, 借贷平衡: {}", entries.size(), balanceCheck.isBalanced());
        return response;
    }
    
    /**
     * 处理过去付款：借方应付 + 贷方活期存款，差异费用调整
     * 根据新需求第184行：根据付款时间与摊销入账时间判断过去付款的处理逻辑
     * 入账日期规则：付款日期晚于摊销入账时间，则入账日期为付款日期
     */
    private void generatePastPaymentEntries(List<JournalEntryDto> entries, 
                                          List<AmortizationEntryDto> pastPeriods,
                                          BigDecimal paymentAmount,
                                          LocalDate paymentDate,
                                          BigDecimal pastPeriodsAmount) {
        log.debug("开始处理过去付款 - 过去期间数量: {}, 过去期间金额: {}, 付款金额: {}", 
                pastPeriods.size(), pastPeriodsAmount, paymentAmount);
        
        // 贷方：活期存款（实际付款金额）
        entries.add(new JournalEntryDto(paymentDate, "活期存款", BigDecimal.ZERO, paymentAmount, 
                "付款 - 过去期间", LocalDateTime.now()));
        
        // 借方：应付（对应预提费用）
        for (AmortizationEntryDto entry : pastPeriods) {
            AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
            BigDecimal entryAmount = actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
            entryAmount = entryAmount.setScale(2, RoundingMode.HALF_UP);
            
            LocalDate amortizationBookingDate = bookingDateForPeriod(entry.getAccountingPeriod(), paymentDate);
            // 入账日期规则：付款日期晚于摊销入账时间，则入账日期为付款日期
            LocalDate actualBookingDate = paymentDate.isAfter(amortizationBookingDate) ? paymentDate : amortizationBookingDate;
            
            entries.add(new JournalEntryDto(actualBookingDate, "应付", entryAmount, BigDecimal.ZERO,
                    "应付款项 - " + entry.getAmortizationPeriod(), LocalDateTime.now()));
        }
        
        // 差异调整：不足或超额以费用调整
        BigDecimal difference = paymentAmount.subtract(pastPeriodsAmount);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // 超额支付：借方费用
                entries.add(new JournalEntryDto(paymentDate, "费用", difference, BigDecimal.ZERO,
                        "超额付款调整", LocalDateTime.now()));
            } else {
                // 不足支付：贷方费用
                entries.add(new JournalEntryDto(paymentDate, "费用", BigDecimal.ZERO, difference.abs(),
                        "不足付款调整", LocalDateTime.now()));
            }
        }
        
        log.debug("过去付款处理完成 - 差异金额: {}", difference);
    }
    
    /**
     * 处理未来付款：借方应付 + 贷方预付，按摊销期间顺序逐月转预付
     * 根据新需求第184行：根据付款时间与摊销入账时间判断未来付款的处理逻辑
     * 超额支付增加贷方预付科目以及借方费用科目以平衡借贷，不足金额以贷方费用科目调整
     * 入账日期规则：付款日期早于摊销入账时间，则分录的入账日期均为该笔摊销的初始入账日期
     */
    private void generateFuturePaymentEntries(List<JournalEntryDto> entries,
                                            List<AmortizationEntryDto> futurePeriods,
                                            BigDecimal paymentAmount,
                                            LocalDate paymentDate,
                                            BigDecimal futurePeriodsAmount,
                                            BigDecimal pastPeriodsAmount) {
        log.debug("开始处理未来付款 - 未来期间数量: {}, 未来期间金额: {}", 
                futurePeriods.size(), futurePeriodsAmount);
        
        // 按期间排序
        futurePeriods.sort(Comparator.comparing(AmortizationEntryDto::getAmortizationPeriod));
        
        // 计算可用于未来期间的付款金额
        BigDecimal availableForFuture = paymentAmount.subtract(pastPeriodsAmount);
        
        // 借方：应付（对应预提费用）
        for (AmortizationEntryDto entry : futurePeriods) {
            AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
            BigDecimal entryAmount = actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
            entryAmount = entryAmount.setScale(2, RoundingMode.HALF_UP);
            
            LocalDate amortizationBookingDate = bookingDateForPeriod(entry.getAccountingPeriod(), paymentDate);
            // 入账日期规则：付款日期早于摊销入账时间，则分录的入账日期均为该笔摊销的初始入账日期
            LocalDate actualBookingDate = paymentDate.isBefore(amortizationBookingDate) ? paymentDate : amortizationBookingDate;
            
            entries.add(new JournalEntryDto(actualBookingDate, "应付", entryAmount, BigDecimal.ZERO,
                    "应付款项 - " + entry.getAmortizationPeriod(), LocalDateTime.now()));
        }
        
        // 贷方：预付科目金额按照应付科目金额按摊销期间顺序逐月转预付
        BigDecimal remainingPrePaid = availableForFuture;
        
        for (int i = 0; i < futurePeriods.size(); i++) {
            AmortizationEntryDto entry = futurePeriods.get(i);
            AmortizationEntry actualEntry = amortizationEntryRepository.findById(Long.valueOf(entry.getId())).orElse(null);
            BigDecimal entryAmount = actualEntry != null ? actualEntry.getRemainingAmount() : entry.getAmount();
            entryAmount = entryAmount.setScale(2, RoundingMode.HALF_UP);
            
            LocalDate amortizationBookingDate = bookingDateForPeriod(entry.getAccountingPeriod(), paymentDate);
            LocalDate actualBookingDate = paymentDate.isBefore(amortizationBookingDate) ? paymentDate : amortizationBookingDate;
            
            if (remainingPrePaid.compareTo(entryAmount) >= 0) {
                // 预付金额足够抵扣整个摊销金额
                entries.add(new JournalEntryDto(actualBookingDate, "预付", BigDecimal.ZERO, entryAmount,
                        "预付转应付 - " + entry.getAmortizationPeriod(), LocalDateTime.now()));
                remainingPrePaid = remainingPrePaid.subtract(entryAmount);
            } else if (remainingPrePaid.compareTo(BigDecimal.ZERO) > 0) {
                // 预付金额不足，部分抵扣
                entries.add(new JournalEntryDto(actualBookingDate, "预付", BigDecimal.ZERO, remainingPrePaid,
                        "预付转应付（部分） - " + entry.getAmortizationPeriod(), LocalDateTime.now()));
                
                // 不足金额，计入贷方费用科目
                BigDecimal shortfall = entryAmount.subtract(remainingPrePaid);
                entries.add(new JournalEntryDto(actualBookingDate, "费用", BigDecimal.ZERO, shortfall,
                        "预付不足补偿 - " + entry.getAmortizationPeriod(), LocalDateTime.now()));
                
                remainingPrePaid = BigDecimal.ZERO;
            } else {
                // 无预付金额，全部计入贷方费用
                entries.add(new JournalEntryDto(actualBookingDate, "费用", BigDecimal.ZERO, entryAmount,
                        "无预付补偿 - " + entry.getAmortizationPeriod(), LocalDateTime.now()));
            }
        }
        
        // 超额支付增加贷方预付科目以及借方费用科目以平衡借贷
        if (remainingPrePaid.compareTo(BigDecimal.ZERO) > 0) {
            String lastPeriod = futurePeriods.get(futurePeriods.size() - 1).getAmortizationPeriod();
            LocalDate amortizationBookingDate = bookingDateForPeriod(lastPeriod, paymentDate);
            LocalDate actualBookingDate = paymentDate.isBefore(amortizationBookingDate) ? paymentDate : amortizationBookingDate;
            
            // 贷方预付科目（记录超额的预付款）
            entries.add(new JournalEntryDto(actualBookingDate, "预付", BigDecimal.ZERO, remainingPrePaid,
                    "超额预付 - " + lastPeriod, LocalDateTime.now()));
            
            // 借方费用科目（用于平衡借贷）
            entries.add(new JournalEntryDto(actualBookingDate, "费用", remainingPrePaid, BigDecimal.ZERO,
                    "超额预付平衡调整 - " + lastPeriod, LocalDateTime.now()));
        }
        
        log.debug("未来付款处理完成 - 剩余预付: {}", remainingPrePaid);
    }
    
    /**
     * 检查会计分录的借贷平衡（第190行要求）
     */
    private BalanceCheckResult checkDebitCreditBalance(List<JournalEntryDto> entries) {
        BigDecimal totalDebitAmount = entries.stream()
                .map(JournalEntryDto::getDr)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal totalCreditAmount = entries.stream()
                .map(JournalEntryDto::getCr)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal difference = totalDebitAmount.subtract(totalCreditAmount);
        boolean balanced = difference.compareTo(BigDecimal.ZERO) == 0;
        
        String message = balanced ? 
                "借贷平衡：借方" + totalDebitAmount + "，贷方" + totalCreditAmount :
                "借贷不平衡：借方" + totalDebitAmount + "，贷方" + totalCreditAmount + "，差异" + difference;
        
        return BalanceCheckResult.builder()
                .totalDebitAmount(totalDebitAmount)
                .totalCreditAmount(totalCreditAmount)
                .difference(difference)
                .balanced(balanced)
                .message(message)
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


    /**
     * 执行付款 - 步骤4付款阶段
     */
    @Transactional
    public PaymentExecutionResponse executePayment(PaymentExecutionRequest request) {
        log.info("开始执行付款 - contractId: {}, paymentAmount: {}, selectedPeriods: {}", 
                request.getContractId(), request.getPaymentAmount(), request.getSelectedPeriods());
        
        try {
            // 验证合同存在
            Contract contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + request.getContractId()));

        // 获取合同的摊销数据
        AmortizationResponse amortization = contractService.getContractAmortization(request.getContractId());

        // 构建PaymentRequest用于预览计算
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmortization(amortization);
        paymentRequest.setPaymentAmount(request.getPaymentAmount());
        paymentRequest.setBookingDate(request.getPaymentDate() != null ? request.getPaymentDate().toLocalDate() : LocalDate.now());
        
        // 根据摊销明细ID查询对应的期间值
        List<String> periodStrings = new ArrayList<>();
        for (Long entryId : request.getSelectedPeriods()) {
            AmortizationEntry entry = amortizationEntryRepository.findById(entryId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));
            periodStrings.add(entry.getAmortizationPeriod());
        }
        paymentRequest.setSelectedPeriods(periodStrings);
        paymentRequest.setContractId(request.getContractId());

        // 计算会计分录
        PaymentPreviewResponse previewResponse = preview(paymentRequest);

        // 创建付款记录
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPaymentAmount(request.getPaymentAmount());
        payment.setBookingDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now());
        payment.setSelectedPeriods(String.join(",", periodStrings));
        payment.setStatus(Payment.PaymentStatus.CONFIRMED);

        payment = paymentRepository.save(payment);
        paymentRequest.setPaymentId(payment.getId());

        // 创建会计分录记录（步骤4：付款阶段），标记为PAYMENT类型，并关联合同与付款
        List<JournalEntry> journalEntries = new ArrayList<>();
        int order = 1;
        
        // 为每个分录设置不同的时间戳，确保时间唯一性
        LocalDateTime baseTimestamp = LocalDateTime.now();
        
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
            entry.setEntryOrder(order);
            entry.setPaymentTimestamp(dto.getCreatedAt()); // 每个分录相差1毫秒
        
            journalEntries.add(entry);
            order++;
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
            
            // 记录修改前的状态用于audit log
            BigDecimal oldPaidAmount = entry.getPaidAmount();
            AmortizationEntry.PaymentStatus oldPaymentStatus = entry.getPaymentStatus();
            LocalDate oldPaymentDate = entry.getPaymentDate();
            
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
            
            BigDecimal paymentAmountForThisEntry = BigDecimal.ZERO;
            
            if (remainingPayment.compareTo(remainingAmountForEntry) >= 0) {
                // 付款金额足够覆盖剩余应付金额
                paymentAmountForThisEntry = remainingAmountForEntry;
                entry.addPayment(remainingAmountForEntry, request.getPaymentDate());
                remainingPayment = remainingPayment.subtract(remainingAmountForEntry);
                System.out.println("完全付款: ID=" + entry.getId() + 
                                 ", 付款金额=" + remainingAmountForEntry + 
                                 ", 状态=" + entry.getPaymentStatus() + 
                                 ", 剩余付款=" + remainingPayment);
            } else {
                // 付款金额不足，部分付款
                paymentAmountForThisEntry = remainingPayment;
                entry.addPayment(remainingPayment, request.getPaymentDate());
                System.out.println("部分付款: ID=" + entry.getId() + 
                                 ", 付款金额=" + remainingPayment + 
                                 ", 新累积金额=" + entry.getPaidAmount() + 
                                 ", 剩余应付=" + entry.getRemainingAmount() + 
                                 ", 状态=" + entry.getPaymentStatus());
                remainingPayment = BigDecimal.ZERO;
            }
            
            // 保存摊销明细
            amortizationEntryRepository.save(entry);
            
            // 记录audit log - 只有当付款状态发生变化时才记录
            if (!oldPaymentStatus.equals(entry.getPaymentStatus()) || 
                paymentAmountForThisEntry.compareTo(BigDecimal.ZERO) > 0) {
                
                String operatorId = request.getOperatorId() != null ? request.getOperatorId() : "system";
                String remark = String.format("付款操作：期间%s，付款金额%.2f", 
                                            entry.getAmortizationPeriod(), paymentAmountForThisEntry);
                
                auditLogService.recordPaymentAuditLog(
                    entry.getId(),
                    operatorId,
                    paymentAmountForThisEntry,
                    request.getPaymentDate() != null ? request.getPaymentDate().toLocalDate() : LocalDate.now(),
                    entry.getPaymentStatus(),
                    remark
                );
                
                log.info("已记录audit log - 摊销明细ID: {}, 操作人: {}, 付款金额: {}", 
                        entry.getId(), operatorId, paymentAmountForThisEntry);
            }
        }

        // 构建响应
        // 转换JournalEntryDto为PaymentJournalEntryDto，包含时间戳和摊销期间信息
        List<PaymentJournalEntryDto> paymentEntryDtos = new ArrayList<>();
        for (int i = 0; i < previewResponse.getEntries().size(); i++) {
            JournalEntryDto dto = previewResponse.getEntries().get(i);
            
            // 根据分录类型和备注内容分配摊销期间
            String amortizationPeriod = assignAmortizationPeriod(dto, periodStrings);
            
            PaymentJournalEntryDto paymentDto = new PaymentJournalEntryDto(
                    dto.getBookingDate(), // parent: bookingDate
                    dto.getAccount(), // parent: account
                    dto.getDr(), // parent: dr
                    dto.getCr(), // parent: cr
                    dto.getMemo(), // parent: memo
                    baseTimestamp.plusNanos((i + 1) * 1000000L), // parent: createdAt
                    baseTimestamp.plusNanos((i + 1) * 1000000L), // child: paymentTimestamp
                    Integer.valueOf(i + 1), // child: entryOrder
                    "PAYMENT", // child: entryType
                    dto.getMemo(), // child: description
                    baseTimestamp.plusNanos((i + 1) * 1000000L), // child: updatedAt
                    amortizationPeriod, // child: amortizationPeriod
                    payment.getId() // child: paymentId
            );
            paymentEntryDtos.add(paymentDto);
        }

            PaymentExecutionResponse response = PaymentExecutionResponse.builder()
                    .paymentId(payment.getId())
                    .contractId(contract.getId())
                    .paymentAmount(payment.getPaymentAmount())
                    .paymentDate(payment.getBookingDate())
                    .selectedPeriods(List.of(payment.getSelectedPeriods().split(",")))
                    .status(payment.getStatus().name())
                    .journalEntries(paymentEntryDtos)
                    .message("付款执行成功")
                    .build();
            
            log.info("付款执行完成 - paymentId: {}, 生成分录数量: {}", payment.getId(), paymentEntryDtos.size());
            return response;
        } catch (Exception e) {
            log.error("付款执行失败 - contractId: {}", request.getContractId(), e);
            throw e;
        }
    }

    /**
     * 查询合同的付款记录
     */
    public List<PaymentExecutionResponse> getPaymentsByContract(Long contractId) {
        log.info("开始查询合同付款记录 - contractId: {}", contractId);
        
        try {
            List<Payment> payments = paymentRepository.findByContractIdOrderByBookingDateDesc(contractId);
            List<PaymentExecutionResponse> responses = payments.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            log.info("合同付款记录查询完成 - contractId: {}, 付款记录数量: {}", contractId, responses.size());
            return responses;
        } catch (Exception e) {
            log.error("查询合同付款记录失败 - contractId: {}", contractId, e);
            throw e;
        }
    }

    /**
     * 查询单个付款详情
     */
    public PaymentExecutionResponse getPaymentDetail(Long paymentId) {
        log.info("开始查询付款详情 - paymentId: {}", paymentId);
        
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到付款记录，ID=" + paymentId));
            PaymentExecutionResponse response = convertToResponse(payment);
            
            log.info("付款详情查询完成 - paymentId: {}", paymentId);
            return response;
        } catch (Exception e) {
            log.error("查询付款详情失败 - paymentId: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * 取消付款
     */
    @Transactional
    public PaymentExecutionResponse cancelPayment(Long paymentId) {
        log.info("开始取消付款 - paymentId: {}", paymentId);
        
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到付款记录，ID=" + paymentId));

            if (payment.getStatus() == Payment.PaymentStatus.CANCELLED) {
                throw new IllegalStateException("付款已经被取消");
            }

            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            payment = paymentRepository.save(payment);

            PaymentExecutionResponse response = PaymentExecutionResponse.builder()
                    .paymentId(payment.getId())
                    .contractId(payment.getContract().getId())
                    .paymentAmount(payment.getPaymentAmount())
                    .paymentDate(payment.getBookingDate())
                    .selectedPeriods(List.of(payment.getSelectedPeriods().split(",")))
                    .status(payment.getStatus().name())
                    .message("付款已取消")
                    .build();
            
            log.info("付款取消完成 - paymentId: {}", paymentId);
            return response;
        } catch (Exception e) {
            log.error("取消付款失败 - paymentId: {}", paymentId, e);
            throw e;
        }
    }

    /**
     * 根据分录类型和内容分配摊销期间
     */
    private String assignAmortizationPeriod(JournalEntryDto dto, List<String> selectedPeriods) {
        log.debug("开始分配摊销期间 - 会计科目: {}, 选中期间数量: {}", dto.getAccount(), selectedPeriods != null ? selectedPeriods.size() : 0);
        
        if (selectedPeriods == null || selectedPeriods.isEmpty()) {
            log.debug("无选中期间，返回null");
            return null;
        }
        
        // 优先从备注中提取期间信息（格式：yyyy-MM）
        if (dto.getMemo() != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{4}-\\d{2})");
            java.util.regex.Matcher matcher = pattern.matcher(dto.getMemo());
            
            if (matcher.find()) {
                String extractedPeriod = matcher.group(1);
                // 验证提取的期间是否在选中的期间列表中
                if (selectedPeriods.contains(extractedPeriod)) {
                    return extractedPeriod;
                }
            }
        }
        
        // 根据会计科目类型分配期间
        String account = dto.getAccount();
        if (account != null) {
            // 应付科目：使用当期（第一个选中期间）
            if (account.contains("应付") || account.contains("Payable")) {
                return selectedPeriods.get(0);
            }
            // 预付科目：如果是跨期付款，使用未来期间
            else if (account.contains("预付") || account.contains("Prepaid")) {
                // 如果有多个期间，预付通常对应未来期间
                return selectedPeriods.size() > 1 ? selectedPeriods.get(selectedPeriods.size() - 1) : selectedPeriods.get(0);
            }
            // 费用调整：使用最后一个期间
            else if (account.contains("费用") || account.contains("Expense")) {
                return selectedPeriods.get(selectedPeriods.size() - 1);
            }
            // 银行存款等：使用当期
            else {
                return selectedPeriods.get(0);
            }
        }
        
        // 默认使用第一个选中的期间
        String result = selectedPeriods.get(0);
        log.debug("摊销期间分配完成 - 分配结果: {}", result);
        return result;
    }

    /**
     * 转换Payment实体为响应DTO
     */
    private PaymentExecutionResponse convertToResponse(Payment payment) {
        log.debug("开始转换Payment实体为响应DTO - paymentId: {}", payment.getId());
        
        List<JournalEntry> entries = journalEntryRepository.findByPaymentIdOrderByEntryOrderAsc(payment.getId());
        
        // 获取付款的选中期间列表
        List<String> selectedPeriods = List.of(payment.getSelectedPeriods().split(","));
        
        List<PaymentJournalEntryDto> entryDtos = entries.stream()
                .map(e -> {
                    // 创建临时JournalEntryDto用于期间分配
                    JournalEntryDto tempDto = new JournalEntryDto(
                            e.getBookingDate(), e.getAccountName(), e.getDebitAmount(), e.getCreditAmount(), e.getMemo(),e.getPaymentTimestamp()
                    );
                    String amortizationPeriod = assignAmortizationPeriod(tempDto, selectedPeriods);
                    
                    return new PaymentJournalEntryDto(
                            e.getBookingDate(), // parent: bookingDate
                            e.getAccountName(), // parent: account
                            e.getDebitAmount(), // parent: dr
                            e.getCreditAmount(), // parent: cr
                            e.getMemo(), // parent: memo
                            e.getCreatedAt(), // parent: createdAt
                            e.getPaymentTimestamp(), // child: paymentTimestamp
                            e.getEntryOrder(), // child: entryOrder
                            e.getEntryType().name(), // child: entryType
                            e.getDescription(), // child: description
                            e.getUpdatedAt(), // child: updatedAt
                            amortizationPeriod, // child: amortizationPeriod
                            payment.getId() // child: paymentId
                    );
                })
                .collect(Collectors.toList());

        PaymentExecutionResponse response = PaymentExecutionResponse.builder()
                .paymentId(payment.getId())
                .contractId(payment.getContract().getId())
                .paymentAmount(payment.getPaymentAmount())
                .paymentDate(payment.getBookingDate())
                .selectedPeriods(List.of(payment.getSelectedPeriods().split(",")))
                .status(payment.getStatus().name())
                .journalEntries(entryDtos)
                .build();
        
        log.debug("Payment实体转换完成 - paymentId: {}, 分录数量: {}", payment.getId(), entryDtos.size());
        return response;
    }
}
