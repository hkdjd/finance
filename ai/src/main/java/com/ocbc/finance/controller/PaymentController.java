package com.ocbc.finance.controller;

import com.ocbc.finance.dto.common.ApiResponse;
import com.ocbc.finance.dto.payment.PaymentGenerateRequest;
import com.ocbc.finance.dto.payment.PaymentScheduleResponse;
import com.ocbc.finance.dto.payment.PaymentScheduleUpdateRequest;
import com.ocbc.finance.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付时间表控制器
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 生成支付时间表
     */
    @PostMapping("/contracts/{id}/schedule")
    public ResponseEntity<ApiResponse<String>> generateSchedule(
            @PathVariable Long id,
            @RequestBody PaymentGenerateRequest request) {
        
        log.info("生成支付时间表: contractId={}", id);
        
        try {
            paymentService.generateSchedule(id, request);
            return ResponseEntity.ok(ApiResponse.success("生成成功"));
        } catch (Exception e) {
            log.error("生成支付时间表失败", e);
            return ResponseEntity.ok(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }

    /**
     * 查询支付时间表
     */
    @GetMapping("/contracts/{id}/schedule")
    public ResponseEntity<ApiResponse<List<PaymentScheduleResponse>>> getSchedule(@PathVariable Long id) {
        log.info("查询支付时间表: contractId={}", id);
        
        try {
            List<PaymentScheduleResponse> schedules = paymentService.getSchedule(id);
            return ResponseEntity.ok(ApiResponse.success(schedules));
        } catch (Exception e) {
            log.error("查询支付时间表失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 更新支付时间表
     */
    @PutMapping("/contracts/{id}/schedule")
    public ResponseEntity<ApiResponse<String>> updateSchedule(
            @PathVariable Long id,
            @RequestBody PaymentScheduleUpdateRequest request) {
        
        log.info("更新支付时间表: contractId={}", id);
        
        try {
            paymentService.updateSchedule(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } catch (Exception e) {
            log.error("更新支付时间表失败", e);
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }
}
