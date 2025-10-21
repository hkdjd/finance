package com.ocbc.finance.service;

import com.ocbc.finance.dto.amortization.AmortizationGenerateRequest;
import com.ocbc.finance.entity.AmortizationSchedule;
import com.ocbc.finance.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 摊销计算服务
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class AmortizationCalculationService {

    private static final DateTimeFormatter SCHEDULE_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 计算摊销时间表 - 严格按照需求文档实现
     * 使用总金额，摊销开始日期，摊销结束日期，预提/待摊审批通过时间来生成
     */
    public List<AmortizationSchedule> calculateAmortizationSchedule(Long contractId, AmortizationGenerateRequest request) {
        log.info("开始计算摊销时间表，合同ID: {}", contractId);
        
        List<AmortizationSchedule> schedules = new ArrayList<>();
        
        try {
            // 1. 确定摊销开始和结束日期
            LocalDateTime startDate = request.getAmortizationStartDate();
            LocalDateTime endDate = request.getAmortizationEndDate();
            
            if (startDate == null || endDate == null) {
                throw new BusinessException("摊销开始日期和结束日期不能为空");
            }
            
            // 2. 计算摊销期数（按月计算）
            int periods = calculatePeriods(startDate, endDate);
            log.info("计算得出摊销期数: {}", periods);
            
            // 3. 计算每期摊销金额
            BigDecimal totalAmount = request.getTotalAmount();
            BigDecimal averageAmount = totalAmount.divide(BigDecimal.valueOf(periods), 2, RoundingMode.DOWN);
            BigDecimal lastAmount = totalAmount.subtract(averageAmount.multiply(BigDecimal.valueOf(periods - 1)));
            
            // 4. 生成每期摊销记录
            LocalDateTime currentDate = startDate;
            
            for (int i = 1; i <= periods; i++) {
                AmortizationSchedule schedule = new AmortizationSchedule();
                
                // 基本信息
                schedule.setContractId(contractId);
                schedule.setScheduleNo(generateScheduleNo(contractId, currentDate, i));
                
                // 摊销日期（每月1号）
                schedule.setScheduleDate(currentDate.withDayOfMonth(1));
                
                // 入账日期（考虑审批时间影响）
                LocalDateTime postDate = calculatePostDate(currentDate, request.getApprovalDate());
                schedule.setPostDate(postDate);
                
                // 摊销金额（最后一期处理除不尽情况）
                BigDecimal amortizationAmount = (i == periods) ? lastAmount : averageAmount;
                schedule.setAmortizationAmount(amortizationAmount);
                schedule.setAmortizationAmountCurrency(request.getTotalAmountCurrency());
                
                // 状态信息
                schedule.setIsPosted(false);
                
                // 审计信息
                LocalDateTime now = LocalDateTime.now();
                schedule.setCreatedDate(now);
                schedule.setUpdatedDate(now);
                schedule.setCreatedBy(request.getCreatedBy());
                schedule.setUpdatedBy(request.getCreatedBy());
                schedule.setIsDeleted(false);
                
                schedules.add(schedule);
                
                // 下一期日期（按月递增）
                currentDate = currentDate.plusMonths(1);
            }
            
            log.info("摊销时间表计算完成，合同ID: {}, 生成条数: {}", contractId, schedules.size());
            return schedules;
            
        } catch (Exception e) {
            log.error("计算摊销时间表异常，合同ID: {}", contractId, e);
            throw new BusinessException("计算摊销时间表失败: " + e.getMessage());
        }
    }

    /**
     * 计算摊销期数（按月计算）- 严格按照需求文档
     * 需求文档场景一：摊销开始日期为2024/1，摊销结束日期为2024/3 -> 3个月
     * 生成：2024/1/1, 2024/2/1, 2024/3/1 共3条记录
     */
    private int calculatePeriods(LocalDateTime startDate, LocalDateTime endDate) {
        // 计算月份差 - 包含开始月份，包含结束月份
        int startYear = startDate.getYear();
        int startMonth = startDate.getMonthValue();
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonthValue();
        
        // 计算逻辑：2024/1 到 2024/3 = 3个月
        int periods = (endYear - startYear) * 12 + (endMonth - startMonth) + 1;
        
        if (periods <= 0) {
            throw new BusinessException("摊销结束日期必须晚于开始日期");
        }
        
        return periods;
    }


    /**
     * 计算入账日期 - 严格按照需求文档实现
     * 
     * 场景一：预提/待摊审批通过时间 <= 摊销开始日期
     * - 入账期间 = 摊销期间（每月1号）
     * 
     * 场景二：预提/待摊审批通过时间 > 摊销开始日期  
     * - 对于审批时间之前的摊销期间：入账期间 = 审批通过时间的月份1号
     * - 对于审批时间之后的摊销期间：入账期间 = 摊销期间（每月1号）
     * 
     * 需求文档示例：审批时间2024/2，1月份摊销期间对应入账期间是2024/2/1
     */
    private LocalDateTime calculatePostDate(LocalDateTime scheduleDate, LocalDateTime approvalDate) {
        // 默认入账日期为摊销日期（每月1号）
        LocalDateTime scheduleMonth = scheduleDate.withDayOfMonth(1);
        
        // 如果没有审批时间，直接使用摊销期间
        if (approvalDate == null) {
            return scheduleMonth;
        }
        
        LocalDateTime approvalMonth = approvalDate.withDayOfMonth(1);
        
        // 如果摊销期间早于审批时间，使用审批时间作为入账期间
        // 这对应需求文档场景二：1月份摊销期间 -> 2月份入账期间
        if (scheduleMonth.isBefore(approvalMonth)) {
            return approvalMonth;
        }
        
        // 如果摊销期间等于或晚于审批时间，使用摊销期间本身
        return scheduleMonth;
    }


    /**
     * 生成时间表编号
     */
    private String generateScheduleNo(Long contractId, LocalDateTime scheduleDate, int period) {
        String dateStr = scheduleDate.format(SCHEDULE_NO_FORMATTER);
        return String.format("AMT%s%06d%03d", dateStr, contractId, period);
    }
}
