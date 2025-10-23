package com.ocbc.finance.controller;

import com.ocbc.finance.dto.AmortizationResponse;
import com.ocbc.finance.dto.CalculateAmortizationRequest;
import com.ocbc.finance.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contracts")
@Tag(name = "合同管理", description = "合同创建、查询和摊销台账管理相关接口")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @Operation(summary = "创建合同并初始化摊销台账", description = "根据合同信息创建合同记录并自动生成摊销明细")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<AmortizationResponse> create(
            @Parameter(description = "合同创建请求参数", required = true)
            @Valid @RequestBody CalculateAmortizationRequest request) {
        return ResponseEntity.ok(contractService.createContractAndInitialize(request));
    }

    @GetMapping("/{id}/amortization")
    @Operation(summary = "查询合同摊销台账", description = "根据合同ID查询摊销台账信息，用于前端展示可编辑表格")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "合同不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<AmortizationResponse> get(
            @Parameter(description = "合同ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(contractService.getContractAmortization(id));
    }
}
