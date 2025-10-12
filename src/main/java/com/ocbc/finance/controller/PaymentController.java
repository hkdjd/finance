package com.ocbc.finance.controller;

import com.ocbc.finance.dto.PaymentExecutionRequest;
import com.ocbc.finance.dto.PaymentExecutionResponse;
import com.ocbc.finance.dto.PaymentPreviewResponse;
import com.ocbc.finance.dto.PaymentRequest;
import com.ocbc.finance.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/preview")
    public ResponseEntity<PaymentPreviewResponse> preview(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.preview(request));
    }

    /**
     * 执行付款 - 步骤4付款阶段
     */
    @PostMapping("/execute")
    public ResponseEntity<PaymentExecutionResponse> executePayment(@Valid @RequestBody PaymentExecutionRequest request) {
        PaymentExecutionResponse response = paymentService.executePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询合同的付款记录
     */
    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<List<PaymentExecutionResponse>> getPaymentsByContract(@PathVariable Long contractId) {
        List<PaymentExecutionResponse> payments = paymentService.getPaymentsByContract(contractId);
        return ResponseEntity.ok(payments);
    }

    /**
     * 查询单个付款详情
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentExecutionResponse> getPaymentDetail(@PathVariable Long paymentId) {
        PaymentExecutionResponse payment = paymentService.getPaymentDetail(paymentId);
        return ResponseEntity.ok(payment);
    }

    /**
     * 取消付款
     */
    @PutMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentExecutionResponse> cancelPayment(@PathVariable Long paymentId) {
        PaymentExecutionResponse response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
