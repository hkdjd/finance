package com.ocbc.finance.controller;

import com.ocbc.finance.dto.amortization.AmortizationGenerateRequest;
import com.ocbc.finance.dto.amortization.AmortizationScheduleResponse;
import com.ocbc.finance.dto.amortization.MarkPostedRequest;
import com.ocbc.finance.dto.common.ApiResponse;
import com.ocbc.finance.service.AmortizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预提/待摊时间表控制器
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/amortization")
@RequiredArgsConstructor
public class AmortizationController {

    private final AmortizationService amortizationService;

    /**
     * 生成摊销时间表
     */
    @PostMapping("/contracts/{id}/schedule")
    public ResponseEntity<ApiResponse<String>> generateSchedule(
            @PathVariable Long id,
            @RequestBody AmortizationGenerateRequest request) {
        
        log.info("生成摊销时间表: contractId={}", id);
        
        try {
            amortizationService.generateSchedule(id, request);
            return ResponseEntity.ok(ApiResponse.success("生成成功"));
        } catch (Exception e) {
            log.error("生成摊销时间表失败", e);
            return ResponseEntity.ok(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }

    /**
     * 查询摊销时间表
     */
    @GetMapping("/contracts/{id}/schedule")
    public ResponseEntity<ApiResponse<List<AmortizationScheduleResponse>>> getSchedule(@PathVariable Long id) {
        log.info("查询摊销时间表: contractId={}", id);
        
        try {
            List<AmortizationScheduleResponse> schedules = amortizationService.getSchedule(id);
            return ResponseEntity.ok(ApiResponse.success(schedules));
        } catch (Exception e) {
            log.error("查询摊销时间表失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 标记已生成分录
     */
    @PutMapping("/contracts/{id}/schedule/mark-posted")
    public ResponseEntity<ApiResponse<String>> markPosted(
            @PathVariable Long id,
            @RequestBody MarkPostedRequest request) {
        
        log.info("标记已生成分录: contractId={}", id);
        
        try {
            amortizationService.markPosted(id, request);
            return ResponseEntity.ok(ApiResponse.success("标记成功"));
        } catch (Exception e) {
            log.error("标记已生成分录失败", e);
            return ResponseEntity.ok(ApiResponse.error("标记失败: " + e.getMessage()));
        }
    }
}
