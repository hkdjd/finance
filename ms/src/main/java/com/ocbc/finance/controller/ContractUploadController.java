package com.ocbc.finance.controller;

import com.ocbc.finance.dto.ContractEditRequest;
import com.ocbc.finance.dto.ContractListResponse;
import com.ocbc.finance.dto.ContractUploadResponse;
import com.ocbc.finance.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/**
 * 合同上传和管理控制器
 */
@RestController
@RequestMapping("/contracts")
public class ContractUploadController {

    private final ContractService contractService;

    public ContractUploadController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * 上传合同文件
     * 
     * @param file 合同文件
     * @param userId 用户ID（可选，用于获取自定义关键字）
     * @return 合同上传响应
     */
    @PostMapping("/upload")
    public ResponseEntity<ContractUploadResponse> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        ContractUploadResponse response = contractService.uploadContract(file, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 编辑合同信息
     */
    @PutMapping("/{contractId}")
    public ResponseEntity<ContractUploadResponse> editContract(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractEditRequest request) {
        ContractUploadResponse response = contractService.editContract(contractId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询合同信息
     */
    @GetMapping("/{contractId}")
    public ResponseEntity<ContractUploadResponse> getContract(@PathVariable Long contractId) {
        ContractUploadResponse response = contractService.getContract(contractId);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询所有合同列表
     */
    @GetMapping
    public ResponseEntity<ContractListResponse> getAllContracts() {
        ContractListResponse response = contractService.getAllContracts();
        return ResponseEntity.ok(response);
    }

    /**
     * 分页查询合同列表
     */
    @GetMapping("/list")
    public ResponseEntity<ContractListResponse> getContractsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ContractListResponse response = contractService.getContractsPaged(page, size);
        return ResponseEntity.ok(response);
    }
}
