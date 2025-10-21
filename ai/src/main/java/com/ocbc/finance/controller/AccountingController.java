package com.ocbc.finance.controller;

import com.ocbc.finance.dto.accounting.AccountingEntryResponse;
import com.ocbc.finance.dto.accounting.AccountingEntryGroupResponse;
import com.ocbc.finance.dto.accounting.AccountingGenerateRequest;
import com.ocbc.finance.dto.common.ApiResponse;
import com.ocbc.finance.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计分录控制器 - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;

    /**
     * 生成会计分录
     */
    @PostMapping("/contracts/{id}/entries")
    public ResponseEntity<ApiResponse<List<AccountingEntryResponse>>> generateEntries(
            @PathVariable Long id,
            @RequestBody AccountingGenerateRequest request) {
        
        log.info("生成会计分录: contractId={}", id);
        
        try {
            List<AccountingEntryResponse> entries = accountingService.generateAccountingEntries(id, request);
            return ResponseEntity.ok(ApiResponse.success(entries));
        } catch (Exception e) {
            log.error("生成会计分录失败", e);
            return ResponseEntity.ok(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }

    /**
     * 查询所有会计分录（按accounting_no分组）
     */
    @GetMapping("/contracts/{id}/entries")
    public ResponseEntity<ApiResponse<List<AccountingEntryGroupResponse>>> getEntries(@PathVariable Long id) {
        log.info("查询所有会计分录: contractId={}", id);
        
        try {
            List<AccountingEntryGroupResponse> entries = accountingService.getAccountingEntries(id);
            return ResponseEntity.ok(ApiResponse.success(entries));
        } catch (Exception e) {
            log.error("查询所有会计分录失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 查询分录详情
     */
    @GetMapping("/contracts/{id}/entries/{no}")
    public ResponseEntity<ApiResponse<List<AccountingEntryResponse>>> getEntryDetail(
            @PathVariable Long id,
            @PathVariable String no) {
        
        log.info("查询分录详情: contractId={}, accountingNo={}", id, no);
        
        try {
            List<AccountingEntryResponse> entries = accountingService.getAccountingEntriesByNo(id, no);
            return ResponseEntity.ok(ApiResponse.success(entries));
        } catch (Exception e) {
            log.error("查询分录详情失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
}
