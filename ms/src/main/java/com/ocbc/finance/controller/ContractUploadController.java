package com.ocbc.finance.controller;

import com.ocbc.finance.constants.UserConstants;
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
     * 解析合同文件（不保存到数据库）
     * 
     * @param file 合同文件
     * @param userId 用户ID（可选，用于获取自定义关键字，默认使用admin用户ID）
     * @return 合同解析响应（不包含contractId）
     */
    @PostMapping("/parse")
    public ResponseEntity<ContractUploadResponse> parseContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        // 如果未传入userId，使用默认admin用户ID
        if (userId == null) {
            userId = UserConstants.DEFAULT_ADMIN_USER_ID;
        }
        ContractUploadResponse response = contractService.parseContractOnly(file, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 上传合同文件
     * 
     * @param file 合同文件
     * @param userId 用户ID（可选，用于获取自定义关键字，默认使用admin用户ID）
     * @return 合同上传响应
     */
    @PostMapping("/upload")
    public ResponseEntity<ContractUploadResponse> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "operatorId", required = false) String operatorId) {
        // 如果未传入userId，使用默认admin用户ID
        if (userId == null) {
            userId = UserConstants.DEFAULT_ADMIN_USER_ID;
        }
        ContractUploadResponse response = contractService.uploadContract(file, userId, operatorId);
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

    /**
     * 访问临时上传的文件（用于PDF预览）
     * @param fileName 文件名
     * @return 文件流
     */
    @GetMapping("/temp/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> getTempFile(
            @PathVariable String fileName) {
        try {
            org.springframework.core.io.Resource resource = contractService.getTempFileResource(fileName);
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
