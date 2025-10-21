package com.ocbc.finance.service;

import com.ocbc.finance.dto.accounting.AccountingGenerateRequest;
import com.ocbc.finance.entity.AccountingEntry;
import com.ocbc.finance.entity.AmortizationSchedule;
import com.ocbc.finance.entity.Contract;
import com.ocbc.finance.exception.BusinessException;
import com.ocbc.finance.repository.AmortizationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 会计分录计算服务 - 严格按照需求文档实现8种复杂场景
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingCalculationService {

    private static final int BOOKING_DAY = 27; // 公司规定的记账日

    // 会计科目映射 - 严格按照需求文档
    private static final String GL_ACCOUNT_EXPENSE = "6001";      // 费用科目
    private static final String GL_ACCOUNT_PAYABLE = "2202";     // 应付科目  
    private static final String GL_ACCOUNT_CASH = "1002";        // 活期存款科目
    private static final String GL_ACCOUNT_PREPAID = "1122";     // 预付费用科目

    private final AmortizationScheduleRepository amortizationScheduleRepository;

    /**
     * 根据需求文档生成会计分录 - 实现8种复杂场景
     */
    public List<AccountingEntry> calculateAccountingEntries(Contract contract, AccountingGenerateRequest request) {
        log.info("开始计算会计分录，合同ID: {}", contract.getId());
        
        // 获取相关的摊销时间表
        List<AmortizationSchedule> amortizationSchedules = getAmortizationSchedules(contract.getId(), request);
        
        // 验证业务逻辑
        validateBusinessLogic(contract, request, amortizationSchedules);
        
        // 判断场景并生成相应的会计分录
        List<AccountingEntry> entries = determineScenarioAndGenerate(contract, request, amortizationSchedules);
        
        log.info("完成会计分录计算，生成 {} 条分录", entries.size());
        return entries;
    }

    /**
     * 获取相关的摊销时间表
     */
    private List<AmortizationSchedule> getAmortizationSchedules(Long contractId, AccountingGenerateRequest request) {
        return amortizationScheduleRepository.findByContractIdAndScheduleDateBetween(
            contractId, 
            request.getPayableStartDate(), 
            request.getPayableEndDate()
        );
    }

    /**
     * 判断场景并生成相应的会计分录
     */
    private List<AccountingEntry> determineScenarioAndGenerate(Contract contract, AccountingGenerateRequest request, 
                                                              List<AmortizationSchedule> amortizationSchedules) {
        
        // 判断是否有付款信息
        boolean hasPayment = request.getPaymentAmount() != null && request.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0;
        
        if (!hasPayment) {
            // 场景一或场景二：未付款场景
            return generateUnpaidScenarios(contract, request, amortizationSchedules);
        } else {
            // 场景三到八：已付款场景
            return generatePaidScenarios(contract, request, amortizationSchedules);
        }
    }

    /**
     * 生成未付款场景的会计分录（场景一、二）
     */
    private List<AccountingEntry> generateUnpaidScenarios(Contract contract, AccountingGenerateRequest request, 
                                                         List<AmortizationSchedule> amortizationSchedules) {
        List<AccountingEntry> entries = new ArrayList<>();
        // 使用简化的编号策略
        
        for (AmortizationSchedule schedule : amortizationSchedules) {
            // 计算Booking Date
            LocalDateTime bookingDate = calculateBookingDate(schedule, request.getAmortizationApprovalDate());
            
            // 生成借方分录：费用
            AccountingEntry debitEntry = createBaseEntry(contract, request);
            debitEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
            debitEntry.setAmortizationScheduleId(schedule.getId());
            debitEntry.setBookingDate(bookingDate);
            debitEntry.setGlAccount(GL_ACCOUNT_EXPENSE); // 费用科目
            debitEntry.setEnteredDr(schedule.getAmortizationAmount());
            debitEntry.setEnteredDrCurrency(schedule.getAmortizationAmountCurrency());
            debitEntry.setEnteredCr(BigDecimal.ZERO);
            debitEntry.setEnteredCrCurrency(schedule.getAmortizationAmountCurrency());
            entries.add(debitEntry);
            
            // 生成贷方分录：应付
            AccountingEntry creditEntry = createBaseEntry(contract, request);
            creditEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
            creditEntry.setAmortizationScheduleId(schedule.getId());
            creditEntry.setBookingDate(bookingDate);
            creditEntry.setGlAccount(GL_ACCOUNT_PAYABLE); // 应付科目
            creditEntry.setEnteredDr(BigDecimal.ZERO);
            creditEntry.setEnteredDrCurrency(schedule.getAmortizationAmountCurrency());
            creditEntry.setEnteredCr(schedule.getAmortizationAmount());
            creditEntry.setEnteredCrCurrency(schedule.getAmortizationAmountCurrency());
            entries.add(creditEntry);
        }
        
        return entries;
    }

    /**
     * 生成已付款场景的会计分录（场景三到八）
     */
    private List<AccountingEntry> generatePaidScenarios(Contract contract, AccountingGenerateRequest request, 
                                                       List<AmortizationSchedule> amortizationSchedules) {
        
        // 计算总的摊销金额
        BigDecimal totalAmortizationAmount = amortizationSchedules.stream()
            .map(AmortizationSchedule::getAmortizationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal paymentAmount = request.getPaymentAmount();
        BigDecimal difference = paymentAmount.subtract(totalAmortizationAmount);
        
        // 判断是否涉及未来期间（预付场景）
        boolean hasFuturePeriods = checkForFuturePeriods(amortizationSchedules, request);
        
        // 根据付款金额与摊销金额的关系以及是否涉及未来期间判断具体场景
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            if (hasFuturePeriods) {
                // 场景六：金额相等但涉及未来期间（预付场景）
                return generatePrepaymentScenarios(contract, request, amortizationSchedules, BigDecimal.ZERO);
            } else {
                // 场景三：金额相等且无未来期间
                return generateScenario3(contract, request, amortizationSchedules);
            }
        } else if (difference.compareTo(BigDecimal.ZERO) > 0) {
            // 场景四、七：多付场景
            return generateOverpaymentScenarios(contract, request, amortizationSchedules, difference);
        } else {
            // 场景五、八：少付场景
            return generateUnderpaymentScenarios(contract, request, amortizationSchedules, difference.abs());
        }
    }

    /**
     * 场景三：付款金额等于摊销金额
     */
    private List<AccountingEntry> generateScenario3(Contract contract, AccountingGenerateRequest request, 
                                                   List<AmortizationSchedule> amortizationSchedules) {
        List<AccountingEntry> entries = new ArrayList<>();
        // 使用简化的编号策略
        LocalDateTime bookingDate = request.getReviewCompletionDate();
        
        // 为每个摊销期间生成应付分录
        for (AmortizationSchedule schedule : amortizationSchedules) {
            AccountingEntry payableEntry = createBaseEntry(contract, request);
            payableEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
            payableEntry.setAmortizationScheduleId(schedule.getId());
            payableEntry.setBookingDate(bookingDate);
            payableEntry.setGlAccount(GL_ACCOUNT_PAYABLE); // 应付科目
            payableEntry.setEnteredDr(schedule.getAmortizationAmount());
            payableEntry.setEnteredDrCurrency(schedule.getAmortizationAmountCurrency());
            payableEntry.setEnteredCr(BigDecimal.ZERO);
            payableEntry.setEnteredCrCurrency(schedule.getAmortizationAmountCurrency());
            entries.add(payableEntry);
        }
        
        // 生成活期存款贷方分录
        AccountingEntry cashEntry = createBaseEntry(contract, request);
        cashEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
        cashEntry.setBookingDate(bookingDate);
        cashEntry.setGlAccount(GL_ACCOUNT_CASH); // 活期存款科目
        cashEntry.setEnteredDr(BigDecimal.ZERO);
        cashEntry.setEnteredDrCurrency(request.getPaymentCurrency());
        cashEntry.setEnteredCr(request.getPaymentAmount());
        cashEntry.setEnteredCrCurrency(request.getPaymentCurrency());
        entries.add(cashEntry);
        
        return entries;
    }

    /**
     * 处理多付场景（场景四、六、七）
     */
    private List<AccountingEntry> generateOverpaymentScenarios(Contract contract, AccountingGenerateRequest request, 
                                                              List<AmortizationSchedule> amortizationSchedules, 
                                                              BigDecimal overpayment) {
        
        // 判断是否涉及未来期间（预付场景）
        boolean hasFuturePeriods = checkForFuturePeriods(amortizationSchedules, request);
        
        if (hasFuturePeriods) {
            // 场景六、七：预付场景
            return generatePrepaymentScenarios(contract, request, amortizationSchedules, overpayment);
        } else {
            // 场景四：简单多付
            return generateSimpleOverpayment(contract, request, amortizationSchedules, overpayment);
        }
    }

    /**
     * 场景四：简单多付1元
     */
    private List<AccountingEntry> generateSimpleOverpayment(Contract contract, AccountingGenerateRequest request, 
                                                           List<AmortizationSchedule> amortizationSchedules, 
                                                           BigDecimal overpayment) {
        List<AccountingEntry> entries = new ArrayList<>();
        // 使用简化的编号策略
        LocalDateTime bookingDate = request.getReviewCompletionDate();
        
        // 生成基本的应付分录
        entries.addAll(generateScenario3(contract, request, amortizationSchedules));
        
        // 生成多付金额的费用分录
        AccountingEntry expenseEntry = createBaseEntry(contract, request);
        expenseEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
        expenseEntry.setBookingDate(bookingDate);
        expenseEntry.setGlAccount(GL_ACCOUNT_EXPENSE); // 费用科目
        expenseEntry.setEnteredDr(overpayment);
        expenseEntry.setEnteredDrCurrency(request.getPaymentCurrency());
        expenseEntry.setEnteredCr(BigDecimal.ZERO);
        expenseEntry.setEnteredCrCurrency(request.getPaymentCurrency());
        entries.add(expenseEntry);
        
        // 调整活期存款金额
        entries.stream()
            .filter(entry -> GL_ACCOUNT_CASH.equals(entry.getGlAccount()))
            .findFirst()
            .ifPresent(cashEntry -> cashEntry.setEnteredCr(request.getPaymentAmount()));
        
        return entries;
    }

    /**
     * 处理预付场景（场景六、七、八）
     * 根据需求文档：复核审批通过时间之后的日期应该都是预付
     */
    private List<AccountingEntry> generatePrepaymentScenarios(Contract contract, AccountingGenerateRequest request, 
                                                             List<AmortizationSchedule> amortizationSchedules, 
                                                             BigDecimal overpayment) {
        List<AccountingEntry> entries = new ArrayList<>();
        LocalDateTime bookingDate = request.getReviewCompletionDate();
        LocalDateTime reviewCompletionDate = request.getReviewCompletionDate();
        
        // 根据复核时间与会计分录时间（27号）比较区分当期应付和未来预付
        // 如果摊销期间的27号 > 复核审批通过时间，则为预付；否则为应付
        List<AmortizationSchedule> currentPeriodSchedules = amortizationSchedules.stream()
            .filter(schedule -> {
                // 计算该摊销期间对应的会计分录时间（27号）
                LocalDateTime scheduleBookingDate = schedule.getScheduleDate().withDayOfMonth(BOOKING_DAY);
                // 如果会计分录时间 <= 复核时间，则为当期应付
                return !scheduleBookingDate.isAfter(reviewCompletionDate);
            })
            .toList();
            
        List<AmortizationSchedule> futurePeriodSchedules = amortizationSchedules.stream()
            .filter(schedule -> {
                // 计算该摊销期间对应的会计分录时间（27号）
                LocalDateTime scheduleBookingDate = schedule.getScheduleDate().withDayOfMonth(BOOKING_DAY);
                // 如果会计分录时间 > 复核时间，则为未来预付
                return scheduleBookingDate.isAfter(reviewCompletionDate);
            })
            .toList();
        
        // 计算当期应付金额（复核时间之前或当时的期间）
        BigDecimal currentPeriodAmount = currentPeriodSchedules.stream()
            .map(AmortizationSchedule::getAmortizationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        // 计算未来期间金额（复核时间之后的期间）
        BigDecimal futurePeriodAmount = futurePeriodSchedules.stream()
            .map(AmortizationSchedule::getAmortizationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        log.info("预付场景分析 - 复核时间: {}", reviewCompletionDate);
        log.info("当期应付期间数: {}, 金额: {}", currentPeriodSchedules.size(), currentPeriodAmount);
        log.info("未来预付期间数: {}, 金额: {}", futurePeriodSchedules.size(), futurePeriodAmount);
        log.info("付款金额: {}", request.getPaymentAmount());
        
        // 详细输出每个期间的判断结果
        for (AmortizationSchedule schedule : amortizationSchedules) {
            LocalDateTime scheduleBookingDate = schedule.getScheduleDate().withDayOfMonth(BOOKING_DAY);
            boolean isPrePayment = scheduleBookingDate.isAfter(reviewCompletionDate);
            log.info("摊销期间: {}, 会计分录时间: {}, 是否预付: {}", 
                schedule.getScheduleDate().toLocalDate(), 
                scheduleBookingDate.toLocalDate(), 
                isPrePayment ? "是" : "否");
        }
        
        // 生成当期应付分录（复核时间之前或当时的期间）
        for (AmortizationSchedule schedule : currentPeriodSchedules) {
            AccountingEntry payableEntry = createBaseEntry(contract, request);
            payableEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
            payableEntry.setAmortizationScheduleId(schedule.getId());
            payableEntry.setBookingDate(bookingDate);
            payableEntry.setGlAccount(GL_ACCOUNT_PAYABLE); // 应付科目
            payableEntry.setEnteredDr(schedule.getAmortizationAmount());
            payableEntry.setEnteredDrCurrency(schedule.getAmortizationAmountCurrency());
            payableEntry.setEnteredCr(BigDecimal.ZERO);
            payableEntry.setEnteredCrCurrency(schedule.getAmortizationAmountCurrency());
            entries.add(payableEntry);
        }
        
        // 计算预付金额（未来期间金额 + 多付/少付差额）
        BigDecimal totalPaymentForFuture = request.getPaymentAmount().subtract(currentPeriodAmount);
        
        // 生成预付分录（包含未来期间和多付/少付金额）
        if (totalPaymentForFuture.compareTo(BigDecimal.ZERO) > 0) {
            AccountingEntry prepaidEntry = createBaseEntry(contract, request);
            prepaidEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
            prepaidEntry.setBookingDate(bookingDate);
            prepaidEntry.setGlAccount(GL_ACCOUNT_PREPAID); // 预付费用科目
            prepaidEntry.setEnteredDr(totalPaymentForFuture);
            prepaidEntry.setEnteredDrCurrency(request.getPaymentCurrency());
            prepaidEntry.setEnteredCr(BigDecimal.ZERO);
            prepaidEntry.setEnteredCrCurrency(request.getPaymentCurrency());
            entries.add(prepaidEntry);
        }
        
        // 生成活期存款贷方分录
        AccountingEntry cashEntry = createBaseEntry(contract, request);
        cashEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
        cashEntry.setBookingDate(bookingDate);
        cashEntry.setGlAccount(GL_ACCOUNT_CASH); // 活期存款科目
        cashEntry.setEnteredDr(BigDecimal.ZERO);
        cashEntry.setEnteredDrCurrency(request.getPaymentCurrency());
        cashEntry.setEnteredCr(request.getPaymentAmount());
        cashEntry.setEnteredCrCurrency(request.getPaymentCurrency());
        entries.add(cashEntry);
        
        // 生成未来期间的预付转应付分录
        BigDecimal remainingPrepaidBalance = totalPaymentForFuture; // 预付账户余额
        
        for (int i = 0; i < futurePeriodSchedules.size(); i++) {
            AmortizationSchedule futureSchedule = futurePeriodSchedules.get(i);
            LocalDateTime futureBookingDate = futureSchedule.getScheduleDate().withDayOfMonth(BOOKING_DAY);
            boolean isLastPeriod = (i == futurePeriodSchedules.size() - 1);
            
            // 借：应付（未来期间的正常摊销金额）
            AccountingEntry futurePayableEntry = createBaseEntry(contract, request);
            futurePayableEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
            futurePayableEntry.setAmortizationScheduleId(futureSchedule.getId());
            futurePayableEntry.setBookingDate(futureBookingDate);
            futurePayableEntry.setGlAccount(GL_ACCOUNT_PAYABLE); // 应付科目
            futurePayableEntry.setEnteredDr(futureSchedule.getAmortizationAmount());
            futurePayableEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
            futurePayableEntry.setEnteredCr(BigDecimal.ZERO);
            futurePayableEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
            entries.add(futurePayableEntry);
            
            if (isLastPeriod) {
                // 最后一个期间：根据预付余额情况处理
                if (remainingPrepaidBalance.compareTo(futureSchedule.getAmortizationAmount()) >= 0) {
                    // 预付余额充足，正常转出当期应付
                    AccountingEntry futurePrepaidEntry = createBaseEntry(contract, request);
                    futurePrepaidEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
                    futurePrepaidEntry.setAmortizationScheduleId(futureSchedule.getId());
                    futurePrepaidEntry.setBookingDate(futureBookingDate);
                    futurePrepaidEntry.setGlAccount(GL_ACCOUNT_PREPAID); // 预付费用科目
                    futurePrepaidEntry.setEnteredDr(BigDecimal.ZERO);
                    futurePrepaidEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
                    futurePrepaidEntry.setEnteredCr(futureSchedule.getAmortizationAmount());
                    futurePrepaidEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
                    entries.add(futurePrepaidEntry);
                    
                    // 处理剩余的多付金额（如果有）
                    BigDecimal excessAmount = remainingPrepaidBalance.subtract(futureSchedule.getAmortizationAmount());
                    if (excessAmount.compareTo(BigDecimal.ZERO) > 0) {
                        // 场景七：多付金额处理 - 借费用，贷预付
                        // 按照用户要求：先费用入账，再预付出账
                        
                        // 1. 费用入账（借方）
                        AccountingEntry excessExpenseEntry = createBaseEntry(contract, request);
                        excessExpenseEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
                        excessExpenseEntry.setAmortizationScheduleId(futureSchedule.getId());
                        excessExpenseEntry.setBookingDate(futureBookingDate);
                        excessExpenseEntry.setGlAccount(GL_ACCOUNT_EXPENSE); // 费用科目
                        excessExpenseEntry.setEnteredDr(excessAmount);
                        excessExpenseEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        excessExpenseEntry.setEnteredCr(BigDecimal.ZERO);
                        excessExpenseEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        entries.add(excessExpenseEntry);
                        
                        // 2. 预付出账（贷方）
                        AccountingEntry excessPrepaidEntry = createBaseEntry(contract, request);
                        excessPrepaidEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
                        excessPrepaidEntry.setAmortizationScheduleId(futureSchedule.getId());
                        excessPrepaidEntry.setBookingDate(futureBookingDate);
                        excessPrepaidEntry.setGlAccount(GL_ACCOUNT_PREPAID); // 预付费用科目
                        excessPrepaidEntry.setEnteredDr(BigDecimal.ZERO);
                        excessPrepaidEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        excessPrepaidEntry.setEnteredCr(excessAmount);
                        excessPrepaidEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        entries.add(excessPrepaidEntry);
                    }
                } else {
                    // 预付余额不足，分别处理
                    if (remainingPrepaidBalance.compareTo(BigDecimal.ZERO) > 0) {
                        // 转出剩余的预付余额
                        AccountingEntry futurePrepaidEntry = createBaseEntry(contract, request);
                        futurePrepaidEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
                        futurePrepaidEntry.setAmortizationScheduleId(futureSchedule.getId());
                        futurePrepaidEntry.setBookingDate(futureBookingDate);
                        futurePrepaidEntry.setGlAccount(GL_ACCOUNT_PREPAID); // 预付费用科目
                        futurePrepaidEntry.setEnteredDr(BigDecimal.ZERO);
                        futurePrepaidEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        futurePrepaidEntry.setEnteredCr(remainingPrepaidBalance);
                        futurePrepaidEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        entries.add(futurePrepaidEntry);
                    }
                    
                    // 差额从费用账户补充
                    BigDecimal shortfallAmount = futureSchedule.getAmortizationAmount().subtract(remainingPrepaidBalance);
                    if (shortfallAmount.compareTo(BigDecimal.ZERO) > 0) {
                        AccountingEntry shortfallExpenseEntry = createBaseEntry(contract, request);
                        shortfallExpenseEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
                        shortfallExpenseEntry.setAmortizationScheduleId(futureSchedule.getId());
                        shortfallExpenseEntry.setBookingDate(futureBookingDate);
                        shortfallExpenseEntry.setGlAccount(GL_ACCOUNT_EXPENSE); // 费用科目
                        shortfallExpenseEntry.setEnteredDr(BigDecimal.ZERO);
                        shortfallExpenseEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        shortfallExpenseEntry.setEnteredCr(shortfallAmount);
                        shortfallExpenseEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
                        entries.add(shortfallExpenseEntry);
                    }
                }
                remainingPrepaidBalance = BigDecimal.ZERO; // 最后一期，余额清零
            } else {
                // 非最后期间：正常转出预付
                AccountingEntry futurePrepaidEntry = createBaseEntry(contract, request);
                futurePrepaidEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
                futurePrepaidEntry.setAmortizationScheduleId(futureSchedule.getId());
                futurePrepaidEntry.setBookingDate(futureBookingDate);
                futurePrepaidEntry.setGlAccount(GL_ACCOUNT_PREPAID); // 预付费用科目
                futurePrepaidEntry.setEnteredDr(BigDecimal.ZERO);
                futurePrepaidEntry.setEnteredDrCurrency(futureSchedule.getAmortizationAmountCurrency());
                futurePrepaidEntry.setEnteredCr(futureSchedule.getAmortizationAmount());
                futurePrepaidEntry.setEnteredCrCurrency(futureSchedule.getAmortizationAmountCurrency());
                entries.add(futurePrepaidEntry);
                
                remainingPrepaidBalance = remainingPrepaidBalance.subtract(futureSchedule.getAmortizationAmount());
            }
        }
        
        return entries;
    }

    /**
     * 处理少付场景（场景五、八）
     */
    private List<AccountingEntry> generateUnderpaymentScenarios(Contract contract, AccountingGenerateRequest request, 
                                                              List<AmortizationSchedule> amortizationSchedules, 
                                                              BigDecimal underpayment) {
        
        // 判断是否涉及未来期间（预付场景）
        boolean hasFuturePeriods = checkForFuturePeriods(amortizationSchedules, request);
        
        if (hasFuturePeriods) {
            // 场景八：少付+预付场景
            return generateUnderpaymentPrepaymentScenario(contract, request, amortizationSchedules, underpayment);
        } else {
            // 场景五：简单少付场景
            return generateSimpleUnderpayment(contract, request, amortizationSchedules, underpayment);
        }
    }
    
    /**
     * 场景五：简单少付场景
     */
    private List<AccountingEntry> generateSimpleUnderpayment(Contract contract, AccountingGenerateRequest request, 
                                                           List<AmortizationSchedule> amortizationSchedules, 
                                                           BigDecimal underpayment) {
        List<AccountingEntry> entries = new ArrayList<>();
        LocalDateTime bookingDate = request.getReviewCompletionDate();
        
        // 生成基本的应付分录
        entries.addAll(generateScenario3(contract, request, amortizationSchedules));
        
        // 生成少付金额的费用贷方分录
        AccountingEntry expenseEntry = createBaseEntry(contract, request);
        expenseEntry.setAccountingNo(generateEntryNo(contract.getId(), request));
        expenseEntry.setBookingDate(bookingDate);
        expenseEntry.setGlAccount(GL_ACCOUNT_EXPENSE); // 费用科目
        expenseEntry.setEnteredDr(BigDecimal.ZERO);
        expenseEntry.setEnteredDrCurrency(request.getPaymentCurrency());
        expenseEntry.setEnteredCr(underpayment);
        expenseEntry.setEnteredCrCurrency(request.getPaymentCurrency());
        entries.add(expenseEntry);
        
        // 调整活期存款金额
        entries.stream()
            .filter(entry -> GL_ACCOUNT_CASH.equals(entry.getGlAccount()))
            .findFirst()
            .ifPresent(cashEntry -> cashEntry.setEnteredCr(request.getPaymentAmount()));
        
        return entries;
    }
    
    /**
     * 场景八：少付+预付场景
     */
    private List<AccountingEntry> generateUnderpaymentPrepaymentScenario(Contract contract, AccountingGenerateRequest request, 
                                                                        List<AmortizationSchedule> amortizationSchedules, 
                                                                        BigDecimal underpayment) {
        // 使用预付场景的逻辑，传入负的多付金额（即少付金额）
        return generatePrepaymentScenarios(contract, request, amortizationSchedules, underpayment.negate());
    }

    /**
     * 计算Booking Date - 严格按照需求文档实现
     * 
     * 场景一：预提/待摊审批通过时间 <= 摊销开始日期
     * - Booking Date = 摊销期间的27号
     * 
     * 场景二：预提/待摊审批通过时间 > 摊销开始日期
     * - 对于审批时间之前的摊销期间：Booking Date = 审批通过时间的27号
     * - 对于审批时间之后的摊销期间：Booking Date = 摊销期间的27号
     * 
     * 需求文档示例：审批时间2024/2，1月份摊销期间对应Booking Date是2024/2/27
     */
    private LocalDateTime calculateBookingDate(AmortizationSchedule schedule, LocalDateTime approvalDate) {
        // 默认Booking Date为摊销期间的27号
        LocalDateTime scheduleMonth = schedule.getScheduleDate().withDayOfMonth(1);
        LocalDateTime defaultBookingDate = scheduleMonth.withDayOfMonth(BOOKING_DAY);
        
        // 如果没有审批时间，直接使用摊销期间的27号
        if (approvalDate == null) {
            return defaultBookingDate;
        }
        
        LocalDateTime approvalMonth = approvalDate.withDayOfMonth(1);
        
        // 如果摊销期间早于审批时间，使用审批时间的27号作为Booking Date
        // 这对应需求文档场景二：1月份摊销期间 -> 2月27号Booking Date
        if (scheduleMonth.isBefore(approvalMonth)) {
            return approvalMonth.withDayOfMonth(BOOKING_DAY);
        }
        
        // 如果摊销期间等于或晚于审批时间，使用摊销期间的27号
        return defaultBookingDate;
    }

    /**
     * 检查是否涉及未来期间（预付场景）
     * 根据需求文档：如果摊销期间的27号 > 复核审批通过时间，则为预付
     */
    private boolean checkForFuturePeriods(List<AmortizationSchedule> amortizationSchedules, AccountingGenerateRequest request) {
        if (amortizationSchedules.isEmpty() || request.getReviewCompletionDate() == null) {
            return false;
        }
        
        // 检查是否有摊销期间的会计分录时间（27号）在复核审批通过时间之后
        LocalDateTime reviewCompletionDate = request.getReviewCompletionDate();
        
        boolean hasFuturePeriods = amortizationSchedules.stream()
            .anyMatch(schedule -> {
                // 计算该摊销期间对应的会计分录时间（27号）
                LocalDateTime scheduleBookingDate = schedule.getScheduleDate().withDayOfMonth(BOOKING_DAY);
                // 如果会计分录时间 > 复核时间，则为未来预付
                return scheduleBookingDate.isAfter(reviewCompletionDate);
            });
        
        log.info("预付场景检查 - 复核时间: {}, 是否有未来期间: {}", 
            reviewCompletionDate, hasFuturePeriods);
        
        return hasFuturePeriods;
    }

    /**
     * 验证业务逻辑
     */
    private void validateBusinessLogic(Contract contract, AccountingGenerateRequest request, 
                                     List<AmortizationSchedule> amortizationSchedules) {
        // 验证必填字段
        if (request.getPayableStartDate() == null || request.getPayableEndDate() == null) {
            throw new BusinessException("应付款开始时间和结束时间不能为空");
        }
        
        if (request.getAmortizationApprovalDate() == null) {
            throw new BusinessException("预提/待摊审批通过时间不能为空");
        }
        
        // 验证时间逻辑
        if (request.getPayableStartDate().isAfter(request.getPayableEndDate())) {
            throw new BusinessException("应付款开始时间不能晚于结束时间");
        }
        
        // 验证摊销时间表
        if (amortizationSchedules.isEmpty()) {
            throw new BusinessException("指定时间段内没有找到摊销时间表数据");
        }
        
        // 验证付款场景的必填字段
        boolean hasPayment = request.getPaymentAmount() != null && request.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0;
        if (hasPayment) {
            if (request.getPaymentApplicationDate() == null) {
                throw new BusinessException("付款申请日期不能为空");
            }
            if (request.getReviewCompletionDate() == null) {
                throw new BusinessException("完成复核时间不能为空");
            }
            if (request.getPaymentCurrency() == null || request.getPaymentCurrency().trim().isEmpty()) {
                throw new BusinessException("付款币种不能为空");
            }
        }
        
        // 验证是否按顺序生成会计分录
        validateSequentialGeneration(contract.getId(), amortizationSchedules);
    }

    /**
     * 验证是否按预提待摊时间顺序生成会计分录
     */
    private void validateSequentialGeneration(Long contractId, List<AmortizationSchedule> requestedSchedules) {
        // 获取所有已生成会计分录的摊销时间表
        List<AmortizationSchedule> postedSchedules = amortizationScheduleRepository
            .findByContractIdAndIsPostedAndIsDeletedFalse(contractId, true);
        
        if (postedSchedules.isEmpty()) {
            return; // 第一次生成，无需验证
        }
        
        // 检查是否有时间间隔
        LocalDateTime lastPostedDate = postedSchedules.stream()
            .map(AmortizationSchedule::getScheduleDate)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime firstRequestedDate = requestedSchedules.stream()
            .map(AmortizationSchedule::getScheduleDate)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        
        if (lastPostedDate != null && firstRequestedDate != null) {
            // 检查是否连续（允许1天的误差）
            if (firstRequestedDate.isBefore(lastPostedDate.minusDays(1))) {
                throw new BusinessException("请按预提待摊时间顺序选择未生成会计分录的时间段");
            }
        }
    }

    /**
     * 创建基础会计分录
     */
    private AccountingEntry createBaseEntry(Contract contract, AccountingGenerateRequest request) {
        AccountingEntry entry = new AccountingEntry();
        entry.setContractId(contract.getId());
        entry.setAccountingDate(LocalDateTime.now());
        entry.setCreatedBy(request.getOperatorBy() != null ? request.getOperatorBy() : "SYSTEM");
        return entry;
    }

    /**
     * 生成会计分录编号 - 格式：contractId_payableStartDate_payableEndDate
     */
    private String generateEntryNo(Long contractId, AccountingGenerateRequest request) {
        String startDateStr = request.getPayableStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDateStr = request.getPayableEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return contractId + "_" + startDateStr + "_" + endDateStr;
    }
}
