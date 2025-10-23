package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AuditLogResponse;
import com.ocbc.finance.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 根据摊销明细ID查询审计日志
     *
     * @param amortizationEntryId 摊销明细ID
     * @return 审计日志响应
     */
    @GetMapping("/amortization-entry/{amortizationEntryId}")
    public ResponseEntity<AuditLogResponse> getAuditLogsByAmortizationEntryId(
            @PathVariable("amortizationEntryId") Long amortizationEntryId) {
        
        log.info("接收到查询审计日志请求，摊销明细ID: {}", amortizationEntryId);
        
        try {
            AuditLogResponse response = auditLogService.getAuditLogsByAmortizationEntryId(amortizationEntryId);
            log.info("审计日志查询成功，摊销明细ID: {}, 记录数: {}", amortizationEntryId, response.getTotalCount());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询审计日志失败，摊销明细ID: {}, 错误: {}", amortizationEntryId, e.getMessage(), e);
            
            AuditLogResponse errorResponse = AuditLogResponse.builder()
                    .amortizationEntryId(amortizationEntryId)
                    .totalCount(0)
                    .message("查询审计日志失败: " + e.getMessage())
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
