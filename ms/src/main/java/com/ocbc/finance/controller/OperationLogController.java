package com.ocbc.finance.controller;

import com.ocbc.finance.dto.CreateOperationLogRequest;
import com.ocbc.finance.dto.OperationLogDto;
import com.ocbc.finance.service.OperationLogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作记录控制器
 */
@RestController
@RequestMapping("/api/operation-logs")
@CrossOrigin(origins = "*")
public class OperationLogController {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogController.class);
    
    @Autowired
    private OperationLogService operationLogService;
    
    
    /**
     * 根据合同ID获取操作记录列表
     * @param contractId 合同ID
     * @return 操作记录列表响应
     */
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<Map<String, Object>> getOperationLogsByContractId(@PathVariable Long contractId) {
        logger.info("查询合同操作记录: 合同ID={}", contractId);
        
        try {
            List<OperationLogDto> operationLogs = operationLogService.getOperationLogsByContractId(contractId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", operationLogs);
            response.put("total", operationLogs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询操作记录失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询操作记录失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据合同ID和操作类型获取操作记录
     * @param contractId 合同ID
     * @param operationType 操作类型
     * @return 操作记录列表响应
     */
    @GetMapping("/contract/{contractId}/type/{operationType}")
    public ResponseEntity<Map<String, Object>> getOperationLogsByContractIdAndType(
            @PathVariable Long contractId, 
            @PathVariable String operationType) {
        logger.info("查询指定类型的操作记录: 合同ID={}, 操作类型={}", contractId, operationType);
        
        try {
            List<OperationLogDto> operationLogs = operationLogService.getOperationLogsByContractIdAndType(contractId, operationType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", operationLogs);
            response.put("total", operationLogs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询操作记录失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询操作记录失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
   
}
