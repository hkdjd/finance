package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AuditLogResponse;
import com.ocbc.finance.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "审计日志", description = "审计日志查询相关接口，用于追踪付款操作历史")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/amortization-entry/{amortizationEntryId}")
    @Operation(summary = "查询摊销明细审计日志", description = "根据摊销明细ID查询该条目的所有操作历史记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "摊销明细不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<AuditLogResponse> getAuditLogsByAmortizationEntryId(
            @Parameter(description = "摊销明细ID", required = true, example = "1")
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
