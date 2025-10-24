package com.ocbc.finance.service;

import com.ocbc.finance.dto.CreateOperationLogRequest;
import com.ocbc.finance.dto.OperationLogDto;
import com.ocbc.finance.entity.OperationLog;
import com.ocbc.finance.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作记录服务类
 */
@Service
@Transactional
public class OperationLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);
    
    @Autowired
    private OperationLogRepository operationLogRepository;
    
    /**
     * 创建操作记录
     * @param request 创建操作记录请求
     * @return 操作记录DTO
     */
    public OperationLogDto createOperationLog(CreateOperationLogRequest request) {
        logger.info("创建操作记录: 合同ID={}, 操作类型={}, 描述={}", 
                   request.getContractId(), request.getOperationType(), request.getDescription());
        
        // 如果没有指定操作时间，使用当前时间
        LocalDateTime operationTime = request.getOperationTime();
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
        
        OperationLog operationLog = new OperationLog(
            request.getContractId(),
            request.getOperationType(),
            request.getDescription(),
            request.getOperator(),
            operationTime
        );
        
        OperationLog savedLog = operationLogRepository.save(operationLog);
        
        logger.info("操作记录创建成功: ID={}", savedLog.getId());
        
        return convertToDto(savedLog);
    }
    
    /**
     * 根据合同ID获取操作记录列表
     * @param contractId 合同ID
     * @return 操作记录列表
     */
    @Transactional(readOnly = true)
    public List<OperationLogDto> getOperationLogsByContractId(Long contractId) {
        logger.info("查询合同操作记录: 合同ID={}", contractId);
        
        List<OperationLog> operationLogs = operationLogRepository.findByContractIdOrderByOperationTimeDesc(contractId);
        
        logger.info("查询到 {} 条操作记录", operationLogs.size());
        
        return operationLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据操作类型获取操作记录
     * @param contractId 合同ID
     * @param operationType 操作类型
     * @return 操作记录列表
     */
    @Transactional(readOnly = true)
    public List<OperationLogDto> getOperationLogsByContractIdAndType(Long contractId, String operationType) {
        logger.info("查询指定类型的操作记录: 合同ID={}, 操作类型={}", contractId, operationType);
        
        List<OperationLog> operationLogs = operationLogRepository.findByContractIdAndOperationType(contractId, operationType);
        
        return operationLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 批量创建操作记录
     * @param requests 创建操作记录请求列表
     * @return 操作记录DTO列表
     */
    public List<OperationLogDto> createOperationLogs(List<CreateOperationLogRequest> requests) {
        logger.info("批量创建操作记录: {} 条", requests.size());
        
        List<OperationLog> operationLogs = requests.stream()
                .map(request -> {
                    LocalDateTime operationTime = request.getOperationTime();
                    if (operationTime == null) {
                        operationTime = LocalDateTime.now();
                    }
                    
                    return new OperationLog(
                        request.getContractId(),
                        request.getOperationType(),
                        request.getDescription(),
                        request.getOperator(),
                        operationTime
                    );
                })
                .collect(Collectors.toList());
        
        List<OperationLog> savedLogs = operationLogRepository.saveAll(operationLogs);
        
        logger.info("批量创建操作记录成功: {} 条", savedLogs.size());
        
        return savedLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为DTO
     * @param operationLog 操作记录实体
     * @return 操作记录DTO
     */
    private OperationLogDto convertToDto(OperationLog operationLog) {
        return new OperationLogDto(
            operationLog.getId(),
            operationLog.getContractId(),
            operationLog.getOperationType(),
            operationLog.getDescription(),
            operationLog.getOperator(),
            operationLog.getOperationTime(),
            operationLog.getCreatedAt()
        );
    }
}
