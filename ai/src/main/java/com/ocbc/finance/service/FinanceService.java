package com.ocbc.finance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocbc.finance.dto.finance.ContractDetailsRequest;
import com.ocbc.finance.dto.finance.ContractDetailsResponse;
import com.ocbc.finance.dto.finance.ScheduleInfoRequest;
import com.ocbc.finance.entity.Contract;
import com.ocbc.finance.entity.ContractDetails;
import com.ocbc.finance.exception.BusinessException;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.ContractDetailsRepository;
import com.ocbc.finance.repository.OriginalContractRepository;
import com.ocbc.finance.repository.AmortizationScheduleRepository;
import com.ocbc.finance.repository.PaymentScheduleRepository;
import com.ocbc.finance.repository.AccountingEntryRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 财务信息管理服务
 * 
{{ ... }}
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class FinanceService {

    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private ContractDetailsRepository contractDetailsRepository;
    
    @Autowired
    private OriginalContractRepository originalContractRepository;
    
    @Autowired
    private AmortizationScheduleRepository amortizationScheduleRepository;
    
    @Autowired
    private PaymentScheduleRepository paymentScheduleRepository;
    
    @Autowired
    private AccountingEntryRepository accountingEntryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存合同详细信息
     * 
     * @param fileId 文件ID
     * @param request 合同详细信息请求
     * @return 合同ID
     */
    public Long saveContractDetails(Long fileId, ContractDetailsRequest request) {
        log.info("开始保存合同详细信息，文件ID: {}", fileId);
        
        try {
            // 1. 查询原始合同是否存在
            originalContractRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException("原始合同不存在或已被删除"));

            // 2. 查询或创建合同主表记录
            Contract contract = contractRepository.findByFileIdAndIsDeletedFalse(fileId)
                .orElseGet(() -> {
                    Contract newContract = new Contract();
                    newContract.setFileId(fileId);
                    
                    // 设置必填的基本信息字段（从request中获取，如果为空则设置默认值）
                    newContract.setContractNo(request.getContractNo() != null ? request.getContractNo() : "TEMP-" + fileId);
                    newContract.setContractAlias(request.getContractAlias() != null ? request.getContractAlias() : "CONTRACT-" + fileId);
                    newContract.setContractName(request.getContractName() != null ? request.getContractName() : "待完善合同名称");
                    newContract.setPartyA(request.getPartyA() != null ? request.getPartyA() : "待完善甲方");
                    newContract.setPartyAId(request.getPartyAId() != null ? request.getPartyAId() : "待完善甲方ID");
                    newContract.setPartyB(request.getPartyB() != null ? request.getPartyB() : "待完善乙方");
                    newContract.setPartyBId(request.getPartyBId() != null ? request.getPartyBId() : "待完善乙方ID");
                    newContract.setContractDescription(request.getContractDescription() != null ? request.getContractDescription() : "待完善合同描述");
                    newContract.setContractAllAmount(request.getContractAllAmount() != null ? request.getContractAllAmount() : BigDecimal.ZERO);
                    newContract.setContractAllCurrency(request.getContractAllCurrency() != null ? request.getContractAllCurrency() : "SGD");
                    newContract.setContractValidStartDate(request.getContractValidStartDate() != null ? request.getContractValidStartDate().atStartOfDay() : LocalDateTime.now());
                    newContract.setContractValidEndDate(request.getContractValidEndDate() != null ? request.getContractValidEndDate().atTime(23, 59, 59) : LocalDateTime.now().plusYears(1));
                    
                    // 设置审计字段
                    newContract.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : request.getUpdatedBy());
                    newContract.setCreatedDate(LocalDateTime.now());
                    newContract.setIsFinished(false);
                    
                    return newContract;
                });

            // 3. 更新合同主表信息
            updateContractBasicInfo(contract, request);
            
            // 4. 保存或更新合同详细信息
            saveOrUpdateContractDetails(contract.getId(), request);
            
            log.info("合同详细信息保存成功，文件ID: {}, 合同ID: {}", fileId, contract.getId());
            
            return contract.getId();
            
        } catch (Exception e) {
            log.error("保存合同详细信息失败，文件ID: {}", fileId, e);
            throw new BusinessException("保存合同详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询指定合同的财务关键信息
     */
    @Transactional(readOnly = true)
    public ContractDetailsResponse getContractDetails(Long contractId) {
        log.info("查询合同详细信息，合同ID: {}", contractId);
        
        try {
            // 1. 查询合同基本信息
            Contract contract = contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 查询合同详细信息
            ContractDetails contractDetails = contractDetailsRepository.findByContractId(contractId)
                .orElse(null);
            
            // 3. 查询状态信息
            boolean hasAmortizationSchedule = amortizationScheduleRepository.existsByContractId(contractId);
            boolean hasPaymentSchedule = paymentScheduleRepository.existsByContractId(contractId);
            boolean hasAccountingEntries = accountingEntryRepository.existsByContractId(contractId);
            
            // 4. 构建响应对象
            ContractDetailsResponse.ContractDetailsResponseBuilder builder = ContractDetailsResponse.builder()
                .contractId(contract.getId())
                .contractNo(contract.getContractNo())
                .contractAlias(contract.getContractAlias())
                .contractName(contract.getContractName())
                .partyA(contract.getPartyA())
                .partyAId(contract.getPartyAId())
                .partyB(contract.getPartyB())
                .partyBId(contract.getPartyBId())
                .contractDescription(contract.getContractDescription())
                .contractAllAmount(contract.getContractAllAmount())
                .contractAllCurrency(contract.getContractAllCurrency())
                .contractValidStartDate(contract.getContractValidStartDate())
                .contractValidEndDate(contract.getContractValidEndDate())
                .isFinished(contract.getIsFinished())
                .hasAmortizationSchedule(hasAmortizationSchedule)
                .hasPaymentSchedule(hasPaymentSchedule)
                .hasAccountingEntries(hasAccountingEntries)
                .createdDate(contract.getCreatedDate())
                .createdBy(contract.getCreatedBy())
                .updatedDate(contract.getUpdatedDate())
                .updatedBy(contract.getUpdatedBy())
                .version(contract.getVersion());
            
            if (contractDetails != null) {
                builder.id(contractDetails.getId())
                    .baseInfo(jsonToMap(contractDetails.getBaseInfo()))
                    .financeInfo(jsonToMap(contractDetails.getFinanceInfo()))
                    .timeInfo(jsonToMap(contractDetails.getTimeInfo()))
                    .settlementInfo(jsonToMap(contractDetails.getSettlementInfo()))
                    .feeInfo(jsonToMap(contractDetails.getFeeInfo()))
                    .taxInfo(jsonToMap(contractDetails.getTaxInfo()))
                    .riskInfo(jsonToMap(contractDetails.getRiskInfo()))
                    .amortizationScheduleInfo(jsonToMap(contractDetails.getAmortizationScheduleInfo()))
                    .paymentScheduleInfo(jsonToMap(contractDetails.getPaymentScheduleInfo()))
                    .accountInfo(jsonToMap(contractDetails.getAccountInfo()));
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("查询合同详细信息失败，合同ID: {}", contractId, e);
            throw new BusinessException("查询合同详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新指定合同的时间表和会计分录必要信息
     */
    public void updateScheduleInfo(Long contractId, ScheduleInfoRequest request) {
        log.info("更新合同时间表信息，合同ID: {}", contractId);
        
        try {
            // 1. 查询合同是否存在
            contractRepository.findByIdAndIsDeletedFalse(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已被删除"));
            
            // 2. 查询或创建合同详细信息
            ContractDetails contractDetails = contractDetailsRepository.findByContractId(contractId)
                .orElseGet(() -> {
                    ContractDetails newDetails = new ContractDetails();
                    newDetails.setContractId(contractId);
                    newDetails.setCreatedBy(request.getUpdatedBy());
                    newDetails.setCreatedDate(LocalDateTime.now());
                    return newDetails;
                });
            
            // 3. 更新时间表信息
            if (request.getAmortizationScheduleInfo() != null) {
                contractDetails.setAmortizationScheduleInfo(mapToJson(request.getAmortizationScheduleInfo()));
            }
            
            if (request.getPaymentScheduleInfo() != null) {
                contractDetails.setPaymentScheduleInfo(mapToJson(request.getPaymentScheduleInfo()));
            }
            
            if (request.getAccountInfo() != null) {
                contractDetails.setAccountInfo(listToJson(request.getAccountInfo()));
            }
            
            // 4. 设置更新信息
            contractDetails.setUpdatedBy(request.getUpdatedBy());
            contractDetails.setUpdatedDate(LocalDateTime.now());
            
            // 5. 保存
            contractDetailsRepository.save(contractDetails);
            
            log.info("合同时间表信息更新成功，合同ID: {}", contractId);
            
        } catch (Exception e) {
            log.error("更新合同时间表信息失败，合同ID: {}", contractId, e);
            throw new BusinessException("更新合同时间表信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新合同基本信息
     */
    private void updateContractBasicInfo(Contract contract, ContractDetailsRequest request) {
        contract.setContractNo(request.getContractNo());
        contract.setContractAlias(request.getContractAlias());
        contract.setContractName(request.getContractName());
        contract.setPartyA(request.getPartyA());
        contract.setPartyAId(request.getPartyAId());
        contract.setPartyB(request.getPartyB());
        contract.setPartyBId(request.getPartyBId());
        contract.setContractDescription(request.getContractDescription());
        contract.setContractAllAmount(request.getContractAllAmount());
        contract.setContractAllCurrency(request.getContractAllCurrency());
        contract.setContractValidStartDate(request.getContractValidStartDate() != null ? request.getContractValidStartDate().atStartOfDay() : null);
        contract.setContractValidEndDate(request.getContractValidEndDate() != null ? request.getContractValidEndDate().atTime(23, 59, 59) : null);
        contract.setUpdatedBy(request.getUpdatedBy());
        contract.setUpdatedDate(LocalDateTime.now());
        
        contractRepository.save(contract);
    }
    
    /**
     * 保存或更新合同详细信息
     */
    private void saveOrUpdateContractDetails(Long contractId, ContractDetailsRequest request) {
        ContractDetails contractDetails = contractDetailsRepository.findByContractId(contractId)
            .orElseGet(() -> {
                ContractDetails newDetails = new ContractDetails();
                newDetails.setContractId(contractId);
                newDetails.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : request.getUpdatedBy());
                newDetails.setCreatedDate(LocalDateTime.now());
                return newDetails;
            });
        
        // 更新详细信息（将Map转换为JSON字符串）
        contractDetails.setBaseInfo(mapToJson(request.getBaseInfo()));
        contractDetails.setFinanceInfo(mapToJson(request.getFinanceInfo()));
        contractDetails.setTimeInfo(mapToJson(request.getTimeInfo()));
        contractDetails.setSettlementInfo(mapToJson(request.getSettlementInfo()));
        contractDetails.setFeeInfo(mapToJson(request.getFeeInfo()));
        contractDetails.setTaxInfo(mapToJson(request.getTaxInfo()));
        contractDetails.setRiskInfo(mapToJson(request.getRiskInfo()));
        contractDetails.setAmortizationScheduleInfo(mapToJson(request.getAmortizationScheduleInfo()));
        contractDetails.setPaymentScheduleInfo(mapToJson(request.getPaymentScheduleInfo()));
        contractDetails.setAccountInfo(mapToJson(request.getAccountInfo()));
        contractDetails.setUpdatedBy(request.getUpdatedBy());
        contractDetails.setUpdatedDate(LocalDateTime.now());
        
        contractDetailsRepository.save(contractDetails);
    }

    /**
     * 将Map转换为JSON字符串
     */
    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Map转JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将JSON字符串转换为Map
     */
    private Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON转Map失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将List转换为JSON字符串
     */
    private String listToJson(Object list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("List转JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
