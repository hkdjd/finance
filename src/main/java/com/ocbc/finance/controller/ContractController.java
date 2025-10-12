package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.CalculateAmortizationRequest;
import com.ocbc.finance.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    // 创建合同并初始化摊销台账
    @PostMapping
    public ResponseEntity<AmortizationResponse> create(@Valid @RequestBody CalculateAmortizationRequest request) {
        return ResponseEntity.ok(contractService.createContractAndInitialize(request));
    }

    // 查询合同摊销台账（用于前端展示已保存可编辑表格）
    @GetMapping("/{id}/amortization")
    public ResponseEntity<AmortizationResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(contractService.getContractAmortization(id));
    }
}
