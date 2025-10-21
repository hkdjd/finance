package com.ocbc.finance.controller;

import com.ocbc.finance.dto.common.ApiResponse;
import com.ocbc.finance.dto.finance.ContractDetailsRequest;
import com.ocbc.finance.dto.finance.ContractDetailsResponse;
import com.ocbc.finance.dto.finance.ScheduleInfoRequest;
import com.ocbc.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 财务信息管理控制器
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /**
     * 保存合同详细信息
     */
    @PostMapping("/contracts/{id}/details")
    public ResponseEntity<ApiResponse<Long>> saveContractDetails(
            @PathVariable Long id,
            @RequestBody ContractDetailsRequest request) {
        
        log.info("保存合同详细信息: fileId={}", id);
        
        try {
            Long contractId = financeService.saveContractDetails(id, request);
            return ResponseEntity.ok(ApiResponse.success(contractId));
        } catch (Exception e) {
            log.error("保存合同详细信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("保存失败: " + e.getMessage()));
        }
    }

    /**
     * 查询合同详细信息
     */
    @GetMapping("/contracts/{id}/details")
    public ResponseEntity<ApiResponse<ContractDetailsResponse>> getContractDetails(@PathVariable Long id) {
        log.info("查询合同详细信息: contractId={}", id);
        
        try {
            ContractDetailsResponse response = financeService.getContractDetails(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("查询合同详细信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 更新时间表信息
     */
    @PutMapping("/contracts/{id}/schedule-info")
    public ResponseEntity<ApiResponse<String>> updateScheduleInfo(
            @PathVariable Long id,
            @RequestBody ScheduleInfoRequest request) {
        
        log.info("更新时间表信息: contractId={}", id);
        
        try {
            financeService.updateScheduleInfo(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } catch (Exception e) {
            log.error("更新时间表信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }
}
