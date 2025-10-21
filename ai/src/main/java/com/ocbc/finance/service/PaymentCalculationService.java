package com.ocbc.finance.service;

import com.ocbc.finance.dto.payment.PaymentGenerateRequest;
import com.ocbc.finance.entity.PaymentSchedule;
import com.ocbc.finance.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付计算服务
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class PaymentCalculationService {

    /**
     * 计算支付时间表
     */
    public List<PaymentSchedule> calculatePaymentSchedule(Long contractId, PaymentGenerateRequest request) {
        log.info("开始计算支付时间表，合同ID: {}, 支付方式: {}", contractId, request.getPaymentMethod());
        
        List<PaymentSchedule> schedules = new ArrayList<>();
        
        switch (request.getPaymentMethod()) {
            case "EQUAL_INSTALLMENT":
                schedules = calculateEqualInstallmentSchedule(contractId, request);
                break;
            case "MILESTONE_BASED":
                schedules = calculateMilestoneBasedSchedule(contractId, request);
                break;
            case "QUARTERLY":
                schedules = calculateQuarterlySchedule(contractId, request);
                break;
            case "SEMI_ANNUAL":
                schedules = calculateSemiAnnualSchedule(contractId, request);
                break;
            default:
                throw new BusinessException("不支持的支付方式: " + request.getPaymentMethod());
        }
        
        log.info("支付时间表计算完成，合同ID: {}, 生成条数: {}", contractId, schedules.size());
        return schedules;
    }

    /**
     * 计算等额分期支付时间表
     */
    private List<PaymentSchedule> calculateEqualInstallmentSchedule(Long contractId, PaymentGenerateRequest request) {
        List<PaymentSchedule> schedules = new ArrayList<>();
        
        BigDecimal totalAmount = request.getTotalAmount();
        Integer periods = request.getPaymentPeriods();
        
        // 计算每期支付金额
        BigDecimal periodAmount = totalAmount.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
        
        // 处理除不尽的情况，最后一期调整
        BigDecimal lastPeriodAmount = totalAmount.subtract(periodAmount.multiply(BigDecimal.valueOf(periods - 1)));
        
        LocalDateTime paymentDate = request.getFirstPaymentDate() != null ? 
            request.getFirstPaymentDate() : request.getContractStartDate();
        
        for (int i = 1; i <= periods; i++) {
            PaymentSchedule schedule = new PaymentSchedule();
            schedule.setContractId(contractId);
            schedule.setScheduleNo(generateScheduleNo(contractId, i));
            schedule.setPaymentDate(paymentDate);
            schedule.setPaymentCondition("第" + i + "期等额分期付款");
            schedule.setMilestone("分期付款第" + i + "期");
            
            // 最后一期使用调整后的金额
            BigDecimal amount = (i == periods) ? lastPeriodAmount : periodAmount;
            schedule.setPaymentAmount(amount);
            schedule.setPaymentAmountCurrency(request.getTotalAmountCurrency());
            schedule.setStatus("PENDING");
            
            setCommonFields(schedule, request.getCreatedBy());
            schedules.add(schedule);
            
            // 下一期支付日期（按月递增）
            paymentDate = paymentDate.plusMonths(1);
        }
        
        return schedules;
    }

    /**
     * 计算基于里程碑的支付时间表
     */
    private List<PaymentSchedule> calculateMilestoneBasedSchedule(Long contractId, PaymentGenerateRequest request) {
        List<PaymentSchedule> schedules = new ArrayList<>();
        
        BigDecimal totalAmount = request.getTotalAmount();
        Integer periods = request.getPaymentPeriods();
        
        // 里程碑支付：通常分为预付款、进度款、尾款
        String[] milestones = {"合同签署", "项目启动", "阶段验收", "项目完成", "质保期结束"};
        BigDecimal[] percentages = {
            new BigDecimal("0.20"), // 20% 预付款
            new BigDecimal("0.30"), // 30% 启动款
            new BigDecimal("0.30"), // 30% 进度款
            new BigDecimal("0.15"), // 15% 完成款
            new BigDecimal("0.05")  // 5% 质保金
        };
        
        // 根据期数调整里程碑数量
        int actualPeriods = Math.min(periods, milestones.length);
        
        LocalDateTime paymentDate = request.getFirstPaymentDate() != null ? 
            request.getFirstPaymentDate() : request.getContractStartDate();
        
        BigDecimal accumulatedAmount = BigDecimal.ZERO;
        
        for (int i = 0; i < actualPeriods; i++) {
            PaymentSchedule schedule = new PaymentSchedule();
            schedule.setContractId(contractId);
            schedule.setScheduleNo(generateScheduleNo(contractId, i + 1));
            schedule.setPaymentDate(paymentDate);
            schedule.setPaymentCondition("里程碑付款");
            schedule.setMilestone(milestones[i]);
            
            BigDecimal amount;
            if (i == actualPeriods - 1) {
                // 最后一期使用剩余金额
                amount = totalAmount.subtract(accumulatedAmount);
            } else {
                amount = totalAmount.multiply(percentages[i]).setScale(2, RoundingMode.HALF_UP);
                accumulatedAmount = accumulatedAmount.add(amount);
            }
            
            schedule.setPaymentAmount(amount);
            schedule.setPaymentAmountCurrency(request.getTotalAmountCurrency());
            schedule.setStatus("PENDING");
            
            setCommonFields(schedule, request.getCreatedBy());
            schedules.add(schedule);
            
            // 里程碑间隔时间根据合同期间计算
            long contractDays = ChronoUnit.DAYS.between(request.getContractStartDate(), request.getContractEndDate());
            long intervalDays = contractDays / actualPeriods;
            paymentDate = paymentDate.plusDays(intervalDays);
        }
        
        return schedules;
    }

    /**
     * 计算按季度支付时间表
     */
    private List<PaymentSchedule> calculateQuarterlySchedule(Long contractId, PaymentGenerateRequest request) {
        List<PaymentSchedule> schedules = new ArrayList<>();
        
        BigDecimal totalAmount = request.getTotalAmount();
        
        // 计算合同期间的季度数
        long contractMonths = ChronoUnit.MONTHS.between(request.getContractStartDate(), request.getContractEndDate());
        int quarters = (int) Math.ceil(contractMonths / 3.0);
        
        BigDecimal quarterAmount = totalAmount.divide(BigDecimal.valueOf(quarters), 2, RoundingMode.HALF_UP);
        BigDecimal lastQuarterAmount = totalAmount.subtract(quarterAmount.multiply(BigDecimal.valueOf(quarters - 1)));
        
        LocalDateTime paymentDate = request.getFirstPaymentDate() != null ? 
            request.getFirstPaymentDate() : request.getContractStartDate();
        
        for (int i = 1; i <= quarters; i++) {
            PaymentSchedule schedule = new PaymentSchedule();
            schedule.setContractId(contractId);
            schedule.setScheduleNo(generateScheduleNo(contractId, i));
            schedule.setPaymentDate(paymentDate);
            schedule.setPaymentCondition("第" + i + "季度付款");
            schedule.setMilestone("季度付款第" + i + "期");
            
            BigDecimal amount = (i == quarters) ? lastQuarterAmount : quarterAmount;
            schedule.setPaymentAmount(amount);
            schedule.setPaymentAmountCurrency(request.getTotalAmountCurrency());
            schedule.setStatus("PENDING");
            
            setCommonFields(schedule, request.getCreatedBy());
            schedules.add(schedule);
            
            // 下一季度支付日期
            paymentDate = paymentDate.plusMonths(3);
        }
        
        return schedules;
    }

    /**
     * 计算半年度支付时间表
     */
    private List<PaymentSchedule> calculateSemiAnnualSchedule(Long contractId, PaymentGenerateRequest request) {
        List<PaymentSchedule> schedules = new ArrayList<>();
        
        BigDecimal totalAmount = request.getTotalAmount();
        
        // 计算合同期间的半年数
        long contractMonths = ChronoUnit.MONTHS.between(request.getContractStartDate(), request.getContractEndDate());
        int semiAnnuals = (int) Math.ceil(contractMonths / 6.0);
        
        BigDecimal semiAnnualAmount = totalAmount.divide(BigDecimal.valueOf(semiAnnuals), 2, RoundingMode.HALF_UP);
        BigDecimal lastSemiAnnualAmount = totalAmount.subtract(semiAnnualAmount.multiply(BigDecimal.valueOf(semiAnnuals - 1)));
        
        LocalDateTime paymentDate = request.getFirstPaymentDate() != null ? 
            request.getFirstPaymentDate() : request.getContractStartDate();
        
        for (int i = 1; i <= semiAnnuals; i++) {
            PaymentSchedule schedule = new PaymentSchedule();
            schedule.setContractId(contractId);
            schedule.setScheduleNo(generateScheduleNo(contractId, i));
            schedule.setPaymentDate(paymentDate);
            schedule.setPaymentCondition("第" + i + "半年度付款");
            schedule.setMilestone("半年度付款第" + i + "期");
            
            BigDecimal amount = (i == semiAnnuals) ? lastSemiAnnualAmount : semiAnnualAmount;
            schedule.setPaymentAmount(amount);
            schedule.setPaymentAmountCurrency(request.getTotalAmountCurrency());
            schedule.setStatus("PENDING");
            
            setCommonFields(schedule, request.getCreatedBy());
            schedules.add(schedule);
            
            // 下一半年度支付日期
            paymentDate = paymentDate.plusMonths(6);
        }
        
        return schedules;
    }

    /**
     * 生成时间表编号
     */
    private String generateScheduleNo(Long contractId, int sequence) {
        return String.format("PAY-%d-%03d", contractId, sequence);
    }

    /**
     * 设置公共字段
     */
    private void setCommonFields(PaymentSchedule schedule, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        schedule.setCreatedBy(createdBy);
        schedule.setCreatedDate(now);
        schedule.setUpdatedBy(createdBy);
        schedule.setUpdatedDate(now);
        schedule.setIsDeleted(false);
        schedule.setVersion(1);
    }
}
