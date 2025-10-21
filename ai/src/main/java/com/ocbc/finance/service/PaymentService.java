package com.ocbc.finance.service;

import com.ocbc.finance.dto.payment.PaymentGenerateRequest;
import com.ocbc.finance.dto.payment.PaymentScheduleResponse;
import com.ocbc.finance.dto.payment.PaymentScheduleUpdateRequest;
import com.ocbc.finance.entity.Contract;
import com.ocbc.finance.entity.PaymentSchedule;
import com.ocbc.finance.exception.BusinessException;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.PaymentScheduleRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 支付时间表服务
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class PaymentService {

    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private PaymentScheduleRepository paymentScheduleRepository;
    
    @Autowired
    private PaymentCalculationService paymentCalculationService;

    /**
     * 生成支付时间表
     */
    public void generateSchedule(Long contractId, PaymentGenerateRequest request) {
        log.info("开始生成支付时间表，合同ID: {}", contractId);
        
        try {
            // 1. 验证合同是否存在
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 检查是否已存在支付时间表
            List<PaymentSchedule> existingSchedules = paymentScheduleRepository.findByContractIdAndNotDeleted(contractId);
            if (!existingSchedules.isEmpty()) {
                throw new BusinessException("该合同已存在支付时间表，请先删除后重新生成");
            }
            
            // 3. 验证请求参数
            validateGenerateRequest(request);
            
            // 4. 生成支付时间表
            List<PaymentSchedule> schedules = paymentCalculationService.calculatePaymentSchedule(contractId, request);
            
            // 5. 保存支付时间表
            paymentScheduleRepository.saveAll(schedules);
            
            log.info("支付时间表生成成功，合同ID: {}，生成条数: {}", contractId, schedules.size());
            
        } catch (Exception e) {
            log.error("生成支付时间表失败，合同ID: {}", contractId, e);
            throw new BusinessException("生成支付时间表失败: " + e.getMessage());
        }
    }

    /**
     * 查询支付时间表
     */
    @Transactional(readOnly = true)
    public List<PaymentScheduleResponse> getSchedule(Long contractId) {
        log.info("查询支付时间表，合同ID: {}", contractId);
        
        try {
            // 1. 验证合同是否存在
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 查询支付时间表
            List<PaymentSchedule> schedules = paymentScheduleRepository.findByContractIdAndNotDeleted(contractId);
            
            // 3. 转换为响应DTO
            List<PaymentScheduleResponse> responses = schedules.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            log.info("查询支付时间表成功，合同ID: {}，查询到条数: {}", contractId, responses.size());
            
            return responses;
            
        } catch (Exception e) {
            log.error("查询支付时间表失败，合同ID: {}", contractId, e);
            throw new BusinessException("查询支付时间表失败: " + e.getMessage());
        }
    }

    /**
     * 更新支付时间表
     */
    public void updateSchedule(Long contractId, PaymentScheduleUpdateRequest request) {
        log.info("开始更新支付时间表，合同ID: {}", contractId);
        
        try {
            // 1. 验证合同是否存在
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 验证请求参数
            validateUpdateRequest(request);
            
            // 3. 批量更新支付时间表
            for (PaymentScheduleUpdateRequest.PaymentScheduleUpdateItem item : request.getUpdateItems()) {
                updateSingleScheduleItem(item, request.getUpdatedBy());
            }
            
            log.info("支付时间表更新成功，合同ID: {}，更新条数: {}", contractId, request.getUpdateItems().size());
            
        } catch (Exception e) {
            log.error("更新支付时间表失败，合同ID: {}", contractId, e);
            throw new BusinessException("更新支付时间表失败: " + e.getMessage());
        }
    }

    /**
     * 验证生成请求参数
     */
    private void validateGenerateRequest(PaymentGenerateRequest request) {
        if (request.getContractEndDate().isBefore(request.getContractStartDate())) {
            throw new BusinessException("合同结束日期不能早于开始日期");
        }
        
        if (request.getFirstPaymentDate() != null && 
            request.getFirstPaymentDate().isBefore(request.getContractStartDate())) {
            throw new BusinessException("首次支付日期不能早于合同开始日期");
        }
        
        // 验证支付方式
        String paymentMethod = request.getPaymentMethod();
        if (!isValidPaymentMethod(paymentMethod)) {
            throw new BusinessException("不支持的支付方式: " + paymentMethod);
        }
    }

    /**
     * 验证更新请求参数
     */
    private void validateUpdateRequest(PaymentScheduleUpdateRequest request) {
        if (request.getUpdateItems() == null || request.getUpdateItems().isEmpty()) {
            throw new BusinessException("更新项列表不能为空");
        }
        
        for (PaymentScheduleUpdateRequest.PaymentScheduleUpdateItem item : request.getUpdateItems()) {
            if (item.getId() == null) {
                throw new BusinessException("支付时间表ID不能为空");
            }
        }
    }

    /**
     * 更新单个支付时间表项
     */
    private void updateSingleScheduleItem(PaymentScheduleUpdateRequest.PaymentScheduleUpdateItem item, String updatedBy) {
        PaymentSchedule schedule = paymentScheduleRepository.findById(item.getId())
            .orElseThrow(() -> new BusinessException("支付时间表不存在，ID: " + item.getId()));
        
        // 更新字段
        if (item.getPaymentDate() != null) {
            schedule.setPaymentDate(item.getPaymentDate());
        }
        if (item.getPaymentCondition() != null) {
            schedule.setPaymentCondition(item.getPaymentCondition());
        }
        if (item.getMilestone() != null) {
            schedule.setMilestone(item.getMilestone());
        }
        if (item.getPaymentAmount() != null) {
            schedule.setPaymentAmount(item.getPaymentAmount());
        }
        if (item.getPaymentAmountCurrency() != null) {
            schedule.setPaymentAmountCurrency(item.getPaymentAmountCurrency());
        }
        if (item.getStatus() != null) {
            schedule.setStatus(item.getStatus());
        }
        
        schedule.setUpdatedBy(updatedBy);
        schedule.setUpdatedDate(LocalDateTime.now());
        
        paymentScheduleRepository.save(schedule);
    }

    /**
     * 转换为响应DTO
     */
    private PaymentScheduleResponse convertToResponse(PaymentSchedule schedule) {
        return PaymentScheduleResponse.builder()
            .id(schedule.getId())
            .contractId(schedule.getContractId())
            .scheduleNo(schedule.getScheduleNo())
            .paymentDate(schedule.getPaymentDate())
            .paymentCondition(schedule.getPaymentCondition())
            .milestone(schedule.getMilestone())
            .paymentAmount(schedule.getPaymentAmount())
            .paymentAmountCurrency(schedule.getPaymentAmountCurrency())
            .status(schedule.getStatus())
            .createdAt(schedule.getCreatedDate())
            .updatedAt(schedule.getUpdatedDate())
            .createdBy(schedule.getCreatedBy())
            .updatedBy(schedule.getUpdatedBy())
            .build();
    }

    /**
     * 验证支付方式是否有效
     */
    private boolean isValidPaymentMethod(String paymentMethod) {
        return "EQUAL_INSTALLMENT".equals(paymentMethod) ||
               "MILESTONE_BASED".equals(paymentMethod) ||
               "QUARTERLY".equals(paymentMethod) ||
               "SEMI_ANNUAL".equals(paymentMethod);
    }
}
