package com.ocbc.finance.service;

import com.ocbc.finance.dto.accounting.AccountingEntryResponse;
import com.ocbc.finance.dto.accounting.AccountingEntryGroupResponse;
import com.ocbc.finance.dto.accounting.AccountingGenerateRequest;
import com.ocbc.finance.entity.AccountingEntry;
import com.ocbc.finance.entity.AmortizationSchedule;
import com.ocbc.finance.entity.Contract;
import com.ocbc.finance.exception.BusinessException;
import com.ocbc.finance.repository.AccountingEntryRepository;
import com.ocbc.finance.repository.AmortizationScheduleRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.util.GlAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会计分录服务类 - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountingService {

    private final AccountingEntryRepository accountingEntryRepository;
    private final AccountingCalculationService accountingCalculationService;
    private final ContractRepository contractRepository;
    private final AmortizationScheduleRepository amortizationScheduleRepository;

    /**
     * 生成会计分录
     * 
     * @param contractId 合同ID
     * @param request 生成请求
     * @return 会计分录列表
     */
    public List<AccountingEntryResponse> generateAccountingEntries(Long contractId, AccountingGenerateRequest request) {
        log.info("开始生成会计分录，合同ID: {}", contractId);
        
        // 验证合同是否存在
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new BusinessException("合同不存在，ID: " + contractId));
        
        // 调用计算服务生成会计分录
        List<AccountingEntry> entries = accountingCalculationService.calculateAccountingEntries(contract, request);
        
        // 保存会计分录
        List<AccountingEntry> savedEntries = accountingEntryRepository.saveAll(entries);
        
        // 更新相关的摊销时间表状态
        updateAmortizationScheduleStatus(contractId, request);
        
        // 检查并更新合同完成状态
        checkAndUpdateContractFinishedStatus(contractId, request);
        
        // 转换为响应DTO
        List<AccountingEntryResponse> responses = new ArrayList<>();
        for (AccountingEntry entry : savedEntries) {
            AccountingEntryResponse response = convertToResponse(entry);
            responses.add(response);
        }
        
        log.info("成功生成会计分录 {} 条", responses.size());
        return responses;
    }

    /**
     * 查询合同的所有会计分录（按accounting_no分组）
     * 
     * @param contractId 合同ID
     * @return 按accounting_no分组的会计分录列表
     */
    @Transactional(readOnly = true)
    public List<AccountingEntryGroupResponse> getAccountingEntries(Long contractId) {
        log.info("查询合同会计分录，合同ID: {}", contractId);
        
        List<AccountingEntry> entries = accountingEntryRepository.findByContractIdOrderByAccountingDateAsc(contractId);
        
        // 按accounting_no分组
        Map<String, List<AccountingEntryResponse>> groupedEntries = new LinkedHashMap<>();
        
        for (AccountingEntry entry : entries) {
            AccountingEntryResponse response = convertToResponse(entry);
            String accountingNo = entry.getAccountingNo();
            
            groupedEntries.computeIfAbsent(accountingNo, k -> new ArrayList<>()).add(response);
        }
        
        // 转换为分组响应对象，按accounting_no ASC排序
        List<AccountingEntryGroupResponse> groupResponses = groupedEntries.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // 按accounting_no ASC排序
            .map(entry -> new AccountingEntryGroupResponse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        log.info("查询到会计分录分组 {} 个，总计 {} 条", groupResponses.size(), entries.size());
        return groupResponses;
    }

    /**
     * 根据会计分录编号查询分录详情
     * 
     * @param contractId 合同ID
     * @param accountingNo 会计分录编号
     * @return 会计分录详情
     */
    @Transactional(readOnly = true)
    public List<AccountingEntryResponse> getAccountingEntriesByNo(Long contractId, String accountingNo) {
        log.info("查询会计分录详情，合同ID: {}, 分录编号: {}", contractId, accountingNo);
        
        List<AccountingEntry> entries = accountingEntryRepository.findByContractIdAndAccountingNoOrderByIdAsc(contractId, accountingNo);
        
        if (entries.isEmpty()) {
            throw new BusinessException("未找到指定的会计分录");
        }
        
        List<AccountingEntryResponse> responses = new ArrayList<>();
        for (AccountingEntry entry : entries) {
            AccountingEntryResponse response = convertToResponse(entry);
            responses.add(response);
        }
        
        log.info("查询到会计分录详情 {} 条", responses.size());
        return responses;
    }

    /**
     * 更新摊销时间表状态
     * 
     * @param contractId 合同ID
     * @param request 生成请求
     */
    private void updateAmortizationScheduleStatus(Long contractId, AccountingGenerateRequest request) {
        // 查找对应时间段的摊销记录
        List<AmortizationSchedule> schedules = amortizationScheduleRepository
            .findByContractIdAndScheduleDateBetween(
                contractId, 
                request.getPayableStartDate(), 
                request.getPayableEndDate()
            );
        
        // 标记为已生成分录
        for (AmortizationSchedule schedule : schedules) {
            schedule.setIsPosted(true);
            schedule.setUpdatedDate(LocalDateTime.now());
            schedule.setUpdatedBy("SYSTEM");
        }
        
        amortizationScheduleRepository.saveAll(schedules);
        log.info("更新摊销时间表状态，共 {} 条记录", schedules.size());
    }

    /**
     * 生成会计分录编号
     * 
     * @param contractId 合同ID
     * @param accountingDate 会计分录日期
     * @return 会计分录编号
     */
    public String generateAccountingNo(Long contractId, LocalDateTime accountingDate) {
        String dateStr = accountingDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ACC" + contractId + "_" + dateStr;
        
        // 查询当天已有的分录数量
        long count = accountingEntryRepository.countByContractIdAndAccountingDateBetween(
            contractId,
            accountingDate.toLocalDate().atStartOfDay(),
            accountingDate.toLocalDate().atTime(23, 59, 59)
        );
        
        return prefix + "_" + String.format("%03d", count + 1);
    }

    /**
     * 将AccountingEntry转换为AccountingEntryResponse，并添加科目名称映射
     * 
     * @param entry 会计分录实体
     * @return 会计分录响应DTO
     */
    private AccountingEntryResponse convertToResponse(AccountingEntry entry) {
        AccountingEntryResponse response = new AccountingEntryResponse();
        BeanUtils.copyProperties(entry, response);
        
        // 添加科目名称映射
        response.setGlAccountName(GlAccountMapper.getGlAccountName(entry.getGlAccount()));
        
        return response;
    }

    /**
     * 检查并更新合同完成状态
     * 逻辑：检查本次生成的会计分录payableEndDate和本合同预提摊销时间表中最大的schedule_date的年月是否一致
     * 如果一致，说明该合同的预提摊销都已经生成了会计分录，合同应该标记为已完成
     * 
     * @param contractId 合同ID
     * @param request 会计分录生成请求
     */
    private void checkAndUpdateContractFinishedStatus(Long contractId, AccountingGenerateRequest request) {
        try {
            log.info("开始检查合同完成状态，合同ID: {}", contractId);
            
            // 1. 获取本次生成的会计分录的payableEndDate
            LocalDateTime payableEndDate = request.getPayableEndDate();
            if (payableEndDate == null) {
                log.warn("payableEndDate为空，跳过合同完成状态检查，合同ID: {}", contractId);
                return;
            }
            
            // 2. 查询该合同的所有预提摊销时间表，找到最大的schedule_date
            List<AmortizationSchedule> allSchedules = amortizationScheduleRepository.findByContractIdAndIsDeletedFalseOrderByScheduleDate(contractId);
            if (allSchedules.isEmpty()) {
                log.warn("该合同没有预提摊销时间表，跳过合同完成状态检查，合同ID: {}", contractId);
                return;
            }
            
            // 获取最大的schedule_date
            LocalDateTime maxScheduleDate = allSchedules.stream()
                .map(AmortizationSchedule::getScheduleDate)
                .max(LocalDateTime::compareTo)
                .orElse(null);
                
            if (maxScheduleDate == null) {
                log.warn("无法获取最大的schedule_date，跳过合同完成状态检查，合同ID: {}", contractId);
                return;
            }
            
            // 3. 比较年月是否一致
            boolean isSameYearMonth = payableEndDate.getYear() == maxScheduleDate.getYear() 
                && payableEndDate.getMonth() == maxScheduleDate.getMonth();
                
            log.info("合同完成状态检查 - 合同ID: {}, payableEndDate: {}, maxScheduleDate: {}, 年月是否一致: {}", 
                contractId, payableEndDate, maxScheduleDate, isSameYearMonth);
            
            // 4. 如果年月一致，更新合同为已完成状态
            if (isSameYearMonth) {
                Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new BusinessException("合同不存在，ID: " + contractId));
                
                // 只有在当前状态为未完成时才更新
                if (!contract.getIsFinished()) {
                    contract.setIsFinished(true);
                    contract.setUpdatedBy(request.getOperatorBy() != null ? request.getOperatorBy() : "SYSTEM");
                    contract.setUpdatedDate(LocalDateTime.now());
                    
                    contractRepository.save(contract);
                    
                    log.info("合同已标记为完成状态，合同ID: {}, 操作人: {}", contractId, contract.getUpdatedBy());
                } else {
                    log.info("合同已经是完成状态，无需更新，合同ID: {}", contractId);
                }
            } else {
                log.info("合同尚未完成所有预提摊销，保持未完成状态，合同ID: {}", contractId);
            }
            
        } catch (Exception e) {
            log.error("检查合同完成状态时发生异常，合同ID: {}", contractId, e);
            // 不抛出异常，避免影响主业务流程
        }
    }
}
