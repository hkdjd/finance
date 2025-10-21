package com.ocbc.finance.service;

import com.ocbc.finance.dto.AmortizationListResponse;
import com.ocbc.finance.dto.AmortizationUpdateRequest;
import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.AmortizationEntryDto;
import com.ocbc.finance.dto.OperationRequest;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.service.calculation.AmortizationCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 摊销明细服务类
 * 实现步骤2的摊销明细CRUD操作
 */
@Service
@Transactional(readOnly = true)
public class AmortizationEntryService {

    private final AmortizationEntryRepository amortizationEntryRepository;
    private final ContractRepository contractRepository;
    private final AmortizationCalculationService amortizationCalculationService;

    public AmortizationEntryService(AmortizationEntryRepository amortizationEntryRepository,
                                    ContractRepository contractRepository,
                                    AmortizationCalculationService amortizationCalculationService) {
        this.amortizationEntryRepository = amortizationEntryRepository;
        this.contractRepository = contractRepository;
        this.amortizationCalculationService = amortizationCalculationService;
    }

    /**
     * 查询合同的摊销明细列表（返回List<Entity>格式）
     */
    public List<AmortizationEntry> getAmortizationEntriesByContract(Long contractId) {
        return amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(contractId);
    }

    /**
     * 查询合同的摊销明细列表（返回包装格式：合同信息+摊销明细数组）
     * 如果摊销明细列表为空，则自动调用摊销计算并保存到数据库
     */
    @Transactional
    public AmortizationListResponse getAmortizationListByContract(Long contractId) {
        // 查询合同信息
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        
        // 查询摊销明细列表
        List<AmortizationEntry> entries = amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(contractId);
        
        // 如果摊销明细列表为空，自动调用摊销计算并保存到数据库
        if (entries.isEmpty()) {
            // 调用摊销计算服务
            AmortizationResponse calculationResult = amortizationCalculationService.calculateByContractId(contractId);
            
            // 将计算结果保存到数据库
            List<AmortizationEntry> calculatedEntries = new ArrayList<>();
            for (AmortizationEntryDto entryDto : calculationResult.getEntries()) {
                AmortizationEntry entry = new AmortizationEntry();
                entry.setContract(contract);
                entry.setAmortizationPeriod(entryDto.getAmortizationPeriod());
                entry.setAccountingPeriod(entryDto.getAccountingPeriod());
                entry.setAmount(entryDto.getAmount());
                
                // 解析期间日期
                String periodStr = entryDto.getAmortizationPeriod() + "-01";
                entry.setPeriodDate(LocalDate.parse(periodStr));
                
                // 设置默认状态
                // entry.setPaymentStatus("PENDING"); // 根据实际的状态字段调整
                
                AmortizationEntry savedEntry = amortizationEntryRepository.save(entry);
                calculatedEntries.add(savedEntry);
            }
            
            // 使用新生成的摊销明细
            entries = calculatedEntries;
        }
        
        // 转换为DTO格式
        AmortizationListResponse.ContractInfo contractInfo = new AmortizationListResponse.ContractInfo(contract);
        List<AmortizationListResponse.AmortizationEntryInfo> amortizationList = entries.stream()
                .map(AmortizationListResponse.AmortizationEntryInfo::new)
                .collect(Collectors.toList());
        
        return new AmortizationListResponse(contractInfo, amortizationList);
    }

    /**
     * 创建摊销明细
     */
    @Transactional
    public AmortizationEntry createAmortizationEntry(AmortizationEntry entry) {
        // 验证合同是否存在
        if (entry.getContract() != null && entry.getContract().getId() != null) {
            Contract contract = contractRepository.findById(entry.getContract().getId())
                    .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + entry.getContract().getId()));
            entry.setContract(contract);
        }
        
        return amortizationEntryRepository.save(entry);
    }

    /**
     * 更新摊销明细
     */
    @Transactional
    public AmortizationEntry updateAmortizationEntry(Long entryId, AmortizationEntry updatedEntry) {
        AmortizationEntry existingEntry = amortizationEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));

        // 更新字段
        existingEntry.setAmortizationPeriod(updatedEntry.getAmortizationPeriod());
        existingEntry.setAccountingPeriod(updatedEntry.getAccountingPeriod());
        existingEntry.setAmount(updatedEntry.getAmount());
        existingEntry.setStatus(updatedEntry.getStatus());

        return amortizationEntryRepository.save(existingEntry);
    }

    /**
     * 更新摊销明细并返回包装格式响应
     */
    @Transactional
    public AmortizationListResponse updateAmortizationEntryWithResponse(Long entryId, AmortizationEntry updatedEntry) {
        // 更新摊销明细
        AmortizationEntry existingEntry = amortizationEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));

        // 更新字段
        existingEntry.setAmortizationPeriod(updatedEntry.getAmortizationPeriod());
        existingEntry.setAccountingPeriod(updatedEntry.getAccountingPeriod());
        existingEntry.setAmount(updatedEntry.getAmount());
        existingEntry.setStatus(updatedEntry.getStatus());

        AmortizationEntry savedEntry = amortizationEntryRepository.save(existingEntry);
        
        // 获取合同信息
        Contract contract = savedEntry.getContract();
        
        // 构造包装格式响应
        AmortizationListResponse.ContractInfo contractInfo = new AmortizationListResponse.ContractInfo(contract);
        AmortizationListResponse.AmortizationEntryInfo entryInfo = new AmortizationListResponse.AmortizationEntryInfo(savedEntry);
        
        // 返回包含单个摊销明细的响应
        List<AmortizationListResponse.AmortizationEntryInfo> amortizationList = List.of(entryInfo);
        
        return new AmortizationListResponse(contractInfo, amortizationList);
    }

    /**
     * 删除摊销明细
     */
    @Transactional
    public void deleteAmortizationEntry(Long entryId) {
        if (!amortizationEntryRepository.existsById(entryId)) {
            throw new IllegalArgumentException("未找到摊销明细，ID=" + entryId);
        }
        amortizationEntryRepository.deleteById(entryId);
    }

    /**
     * 根据ID查询摊销明细
     */
    public AmortizationEntry getAmortizationEntryById(Long entryId) {
        return amortizationEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));
    }

    /**
     * 批量更新摊销明细
     */
    @Transactional
    public List<AmortizationEntry> batchUpdateAmortizationEntries(List<AmortizationEntry> entries) {
        for (AmortizationEntry entry : entries) {
            if (entry.getId() != null) {
                // 更新现有记录
                AmortizationEntry existingEntry = amortizationEntryRepository.findById(entry.getId())
                        .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entry.getId()));
                
                existingEntry.setAmortizationPeriod(entry.getAmortizationPeriod());
                existingEntry.setAccountingPeriod(entry.getAccountingPeriod());
                existingEntry.setAmount(entry.getAmount());
                existingEntry.setStatus(entry.getStatus());
            }
        }
        
        return amortizationEntryRepository.saveAll(entries);
    }

    /**
     * 批量操作摊销明细
     * 支持批量增删改操作
     */
    @Transactional
    public List<AmortizationEntry> batchOperateAmortizationEntries(List<OperationRequest<AmortizationEntry>> requests) {
        List<AmortizationEntry> results = new ArrayList<>();
        
        for (OperationRequest<AmortizationEntry> request : requests) {
            switch (request.getOperate()) {
                case CREATE:
                    AmortizationEntry createdEntry = createAmortizationEntry(request.getData());
                    results.add(createdEntry);
                    break;
                    
                case UPDATE:
                    AmortizationEntry updatedEntry = updateAmortizationEntry(request.getId(), request.getData());
                    results.add(updatedEntry);
                    break;
                    
                case DELETE:
                    deleteAmortizationEntry(request.getId());
                    // DELETE操作不返回实体
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的操作类型: " + request.getOperate());
            }
        }
        
        return results;
    }

    /**
     * 批量操作摊销明细并返回包装格式响应
     * 只支持批量UPDATE操作
     */
    @Transactional
    public AmortizationListResponse batchOperateAmortizationEntriesWithResponse(List<OperationRequest<AmortizationEntry>> requests) {
        List<AmortizationEntry> updatedEntries = new ArrayList<>();
        Contract contract = null;
        
        for (OperationRequest<AmortizationEntry> request : requests) {
            if (request.getOperate() == OperationRequest.OperationType.UPDATE) {
                AmortizationEntry updatedEntry = updateAmortizationEntry(request.getId(), request.getData());
                updatedEntries.add(updatedEntry);
                
                // 获取合同信息（所有摊销明细应该属于同一个合同）
                if (contract == null) {
                    contract = updatedEntry.getContract();
                }
            }
        }
        
        // 构造包装格式响应
        AmortizationListResponse.ContractInfo contractInfo = new AmortizationListResponse.ContractInfo(contract);
        List<AmortizationListResponse.AmortizationEntryInfo> amortizationList = updatedEntries.stream()
                .map(AmortizationListResponse.AmortizationEntryInfo::new)
                .collect(Collectors.toList());
        
        return new AmortizationListResponse(contractInfo, amortizationList);
    }

    /**
     * 根据请求更新摊销明细（支持增删改操作）
     * 请求格式与列表接口响应格式保持一致
     * 根据amortization列表里子项的id来判定每条数据的操作：
     * - 若request的id为null，则新增
     * - 若request的id与数据库中的id一致，则更新
     * - 若数据库中存在的id在request中不存在，则删除
     */
    @Transactional
    public AmortizationListResponse updateAmortizationEntriesFromRequest(AmortizationUpdateRequest request) {
        // 验证合同是否存在
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + request.getContractId()));
        
        // 获取该合同现有的所有摊销明细
        List<AmortizationEntry> existingEntries = amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(request.getContractId());
        Set<Long> existingIds = existingEntries.stream()
                .map(AmortizationEntry::getId)
                .collect(Collectors.toSet());
        
        // 收集请求中的ID
        Set<Long> requestIds = request.getAmortization().stream()
                .map(AmortizationUpdateRequest.AmortizationEntryData::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 找出需要删除的ID（存在于数据库但不在请求中）
        Set<Long> idsToDelete = existingIds.stream()
                .filter(id -> !requestIds.contains(id))
                .collect(Collectors.toSet());
        
        // 删除不在请求中的摊销明细
        if (!idsToDelete.isEmpty()) {
            amortizationEntryRepository.deleteAllById(idsToDelete);
        }
        
        List<AmortizationEntry> resultEntries = new ArrayList<>();
        
        // 处理请求中的每个摊销明细
        for (AmortizationUpdateRequest.AmortizationEntryData entryData : request.getAmortization()) {
            AmortizationEntry entry;
            
            if (entryData.getId() == null) {
                // 新增操作
                entry = new AmortizationEntry();
                entry.setContract(contract);
                entry.setAmortizationPeriod(entryData.getAmortizationPeriod());
                entry.setAccountingPeriod(entryData.getAccountingPeriod());
                entry.setAmount(BigDecimal.valueOf(entryData.getAmount()));
                entry.setPeriodDate(LocalDate.parse(entryData.getPeriodDate()));
                // 设置默认状态
                // entry.setPaymentStatus(PaymentStatus.PENDING);
            } else {
                // 更新操作
                entry = amortizationEntryRepository.findById(entryData.getId())
                        .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryData.getId()));
                
                // 更新字段
                if (entryData.getAmortizationPeriod() != null) {
                    entry.setAmortizationPeriod(entryData.getAmortizationPeriod());
                }
                if (entryData.getAccountingPeriod() != null) {
                    entry.setAccountingPeriod(entryData.getAccountingPeriod());
                }
                if (entryData.getAmount() != null) {
                    entry.setAmount(BigDecimal.valueOf(entryData.getAmount()));
                }
                if (entryData.getPeriodDate() != null) {
                    entry.setPeriodDate(LocalDate.parse(entryData.getPeriodDate()));
                }
                if (entryData.getPaymentStatus() != null) {
                    // 假设有PaymentStatus枚举，这里需要根据实际情况调整
                    // entry.setPaymentStatus(PaymentStatus.valueOf(entryData.getPaymentStatus()));
                }
            }
            
            AmortizationEntry savedEntry = amortizationEntryRepository.save(entry);
            resultEntries.add(savedEntry);
        }
        
        // 构造响应
        AmortizationListResponse.ContractInfo contractInfo = new AmortizationListResponse.ContractInfo(contract);
        List<AmortizationListResponse.AmortizationEntryInfo> amortizationList = resultEntries.stream()
                .map(AmortizationListResponse.AmortizationEntryInfo::new)
                .collect(Collectors.toList());
        
        return new AmortizationListResponse(contractInfo, amortizationList);
    }
}
