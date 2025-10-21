package com.ocbc.finance.service;

import com.ocbc.finance.dto.amortization.AmortizationGenerateRequest;
import com.ocbc.finance.dto.amortization.AmortizationScheduleResponse;
import com.ocbc.finance.dto.amortization.MarkPostedRequest;
import com.ocbc.finance.entity.Contract;
import com.ocbc.finance.entity.AmortizationSchedule;
import com.ocbc.finance.exception.BusinessException;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.AmortizationScheduleRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 摊销时间表服务
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class AmortizationService {

    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private AmortizationScheduleRepository amortizationScheduleRepository;
    
    @Autowired
    private AmortizationCalculationService amortizationCalculationService;

    /**
     * 生成摊销时间表
     */
    public void generateSchedule(Long contractId, AmortizationGenerateRequest request) {
        log.info("开始生成摊销时间表，合同ID: {}", contractId);
        
        try {
            // 1. 验证合同是否存在
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 检查是否已存在摊销时间表
            List<AmortizationSchedule> existingSchedules = amortizationScheduleRepository
                .findByContractIdAndIsDeletedFalse(contractId);
            if (!existingSchedules.isEmpty()) {
                throw new BusinessException("该合同已存在摊销时间表，请先删除后重新生成");
            }
            
            // 3. 验证请求参数
            validateGenerateRequest(request);
            
            // 4. 使用计算服务生成摊销计划
            List<AmortizationSchedule> schedules = amortizationCalculationService
                .calculateAmortizationSchedule(contractId, request);
            
            // 5. 批量保存摊销时间表
            amortizationScheduleRepository.saveAll(schedules);
            
            log.info("摊销时间表生成成功，合同ID: {}，生成条数: {}", contractId, schedules.size());
            
        } catch (BusinessException e) {
            log.error("生成摊销时间表失败，合同ID: {}，错误: {}", contractId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("生成摊销时间表异常，合同ID: {}", contractId, e);
            throw new BusinessException("生成摊销时间表失败: " + e.getMessage());
        }
    }

    /**
     * 查询摊销时间表
     */
    @Transactional(readOnly = true)
    public List<AmortizationScheduleResponse> getSchedule(Long contractId) {
        log.info("查询摊销时间表，合同ID: {}", contractId);
        
        try {
            // 1. 验证合同是否存在
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 查询摊销时间表
            List<AmortizationSchedule> schedules = amortizationScheduleRepository
                .findByContractIdAndIsDeletedFalseOrderByScheduleDate(contractId);
            
            // 3. 转换为响应DTO
            List<AmortizationScheduleResponse> responses = schedules.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            log.info("查询摊销时间表成功，合同ID: {}，条数: {}", contractId, responses.size());
            return responses;
            
        } catch (BusinessException e) {
            log.error("查询摊销时间表失败，合同ID: {}，错误: {}", contractId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("查询摊销时间表异常，合同ID: {}", contractId, e);
            throw new BusinessException("查询摊销时间表失败: " + e.getMessage());
        }
    }

    /**
     * 标记已生成分录
     */
    public void markPosted(Long contractId, MarkPostedRequest request) {
        log.info("标记已生成分录，合同ID: {}，时间表ID: {}", contractId, request.getScheduleIds());
        
        try {
            // 1. 验证合同是否存在
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 验证请求参数
            if (request.getScheduleIds() == null || request.getScheduleIds().isEmpty()) {
                throw new BusinessException("摊销时间表ID列表不能为空");
            }
            
            // 3. 查询并验证摊销时间表
            List<AmortizationSchedule> schedules = amortizationScheduleRepository
                .findByIdInAndContractIdAndIsDeletedFalse(request.getScheduleIds(), contractId);
            
            if (schedules.size() != request.getScheduleIds().size()) {
                throw new BusinessException("部分摊销时间表不存在或不属于该合同");
            }
            
            // 4. 标记已生成分录
            LocalDateTime now = LocalDateTime.now();
            for (AmortizationSchedule schedule : schedules) {
                if (schedule.getIsPosted()) {
                    log.warn("摊销时间表已标记为已生成分录，跳过，ID: {}", schedule.getId());
                    continue;
                }
                
                schedule.setIsPosted(true);
                schedule.setUpdatedDate(now);
                schedule.setUpdatedBy(request.getUpdatedBy());
            }
            
            // 5. 批量更新
            amortizationScheduleRepository.saveAll(schedules);
            
            log.info("标记已生成分录成功，合同ID: {}，更新条数: {}", contractId, schedules.size());
            
        } catch (BusinessException e) {
            log.error("标记已生成分录失败，合同ID: {}，错误: {}", contractId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("标记已生成分录异常，合同ID: {}", contractId, e);
            throw new BusinessException("标记已生成分录失败: " + e.getMessage());
        }
    }

    /**
     * 验证生成请求参数
     */
    private void validateGenerateRequest(AmortizationGenerateRequest request) {
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("合同总金额必须大于0");
        }
        
        if (request.getContractStartDate() == null || request.getContractEndDate() == null) {
            throw new BusinessException("合同开始日期和结束日期不能为空");
        }
        
        if (request.getContractStartDate().isAfter(request.getContractEndDate())) {
            throw new BusinessException("合同开始日期不能晚于结束日期");
        }
        
        // 摊销期数现在由系统根据开始和结束日期自动计算，不再需要验证传入的期数
        // if (request.getAmortizationPeriods() == null || request.getAmortizationPeriods() <= 0) {
        //     throw new BusinessException("摊销期数必须大于0");
        // }
    }

    /**
     * 转换为响应DTO
     */
    private AmortizationScheduleResponse convertToResponse(AmortizationSchedule schedule) {
        return AmortizationScheduleResponse.builder()
            .id(schedule.getId())
            .contractId(schedule.getContractId())
            .scheduleNo(schedule.getScheduleNo())
            .scheduleDate(schedule.getScheduleDate())
            .postDate(schedule.getPostDate())
            .amortizationAmount(schedule.getAmortizationAmount())
            .amortizationAmountCurrency(schedule.getAmortizationAmountCurrency())
            .isPosted(schedule.getIsPosted())
            .createdAt(schedule.getCreatedDate())
            .updatedAt(schedule.getUpdatedDate())
            .createdBy(schedule.getCreatedBy())
            .updatedBy(schedule.getUpdatedBy())
            .build();
    }
}
