package com.ocbc.finance.controller;

import com.ocbc.finance.dto.common.ApiResponse;
import com.ocbc.finance.dto.common.PageResponse;
import com.ocbc.finance.dto.contract.ContractUploadResponse;
import com.ocbc.finance.dto.contract.ContractWithStatusResponse;
import com.ocbc.finance.dto.contract.OriginalContractPreviewResponse;
import com.ocbc.finance.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 合同管理控制器
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * 上传合同文件
     */
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ContractUploadResponse>> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdBy") String createdBy,
            @RequestParam(value = "customFields", required = false) java.util.List<String> customFields) {
        
        log.info("上传合同文件: {}, 创建者: {}, 自定义字段: {}", file.getOriginalFilename(), createdBy, customFields);
        
        try {
            ContractUploadResponse result = contractService.uploadContract(file, createdBy, customFields);
            
            // 设置响应头，确保正确的内容类型和编码
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cache-Control", "no-cache");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("上传合同文件失败", e);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(ApiResponse.error("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 查询合同列表（支持多条件查询，包含关联状态信息）
     * 支持按contract_no、contract_alias、is_finished、is_deleted进行组合查询
     * 返回合同基本信息及各关联表的状态标志位
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContractWithStatusResponse>>> getContracts(
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String contractAlias,
            @RequestParam(required = false) Boolean isFinished,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("查询合同列表: contractNo={}, contractAlias={}, isFinished={}, isDeleted={}, page={}, size={}", 
                contractNo, contractAlias, isFinished, isDeleted, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ContractWithStatusResponse> contracts;
            
            // 如果没有任何查询条件，使用默认查询（仅查询未删除的）
            if (contractNo == null && contractAlias == null && isFinished == null && isDeleted == null) {
                contracts = contractService.getContractsWithStatus(pageable);
            } else {
                // 如果没有指定isDeleted参数，默认查询未删除的记录
                Boolean finalIsDeleted = (isDeleted != null) ? isDeleted : false;
                contracts = contractService.getContractsByConditionsWithStatus(
                    contractNo, contractAlias, isFinished, finalIsDeleted, pageable);
            }
            
            PageResponse<ContractWithStatusResponse> pageResponse = PageResponse.of(contracts);
            
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("查询合同列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 下载合同原件
     */
    @GetMapping("/{id}/original")
    public ResponseEntity<ApiResponse<String>> downloadOriginal(@PathVariable Long id) {
        log.info("下载合同原件: contractId={}", id);
        
        try {
            String result = contractService.downloadOriginal(id);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("下载合同原件失败", e);
            return ResponseEntity.ok(ApiResponse.error("下载失败: " + e.getMessage()));
        }
    }

    /**
     * 预览原始合同接口
     * 根据file_id查询原始合同文件，返回给前端页面显示
     */
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<byte[]> previewOriginalContract(@PathVariable Long fileId) {
        log.info("预览原始合同: fileId={}", fileId);
        
        try {
            // 获取原始合同文件数据
            OriginalContractPreviewResponse response = contractService.previewOriginalContract(fileId);
            
            // 设置响应头（处理中文文件名编码问题）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(response.getFileType()));
            
            // 对中文文件名进行URL编码处理
            String encodedFileName = java.net.URLEncoder.encode(response.getFileName(), "UTF-8")
                    .replaceAll("\\+", "%20");
            headers.set("Content-Disposition", "inline; filename*=UTF-8''" + encodedFileName);
            headers.setContentLength(response.getFileData().length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response.getFileData());
        } catch (Exception e) {
            log.error("预览原始合同失败", e);
            // 返回错误信息
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String errorMessage = "{\"error\":\"预览失败: " + e.getMessage() + "\"}";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .headers(headers)
                    .body(errorMessage.getBytes());
        }
    }

    /**
     * 删除合同
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteContract(@PathVariable Long id) {
        log.info("删除合同: contractId={}", id);
        
        try {
            contractService.deleteContract(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            log.error("删除合同失败", e);
            return ResponseEntity.ok(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }
}
