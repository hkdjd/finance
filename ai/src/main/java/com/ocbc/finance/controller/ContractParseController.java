package com.ocbc.finance.controller;

import com.ocbc.finance.dto.contract.ContractParseResponse;
import com.ocbc.finance.service.ContractParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 合同解析控制器
 * 提供合同文件上传和信息提取接口
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/contract-parse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContractParseController {

    private final ContractParseService contractParseService;

    /**
     * 解析合同文件并提取关键信息
     * 
     * @param file 合同文件（PDF格式）
     * @return 解析结果，包含合同总金额、开始时间、结束时间、税率、乙方公司名称
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractParseResponse> parseContract(
            @RequestParam("file") MultipartFile file) {
        
        log.info("接收到合同解析请求，文件名: {}, 文件大小: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        try {
            // 验证文件
            validateFile(file);
            
            // 解析合同
            ContractParseResponse response = contractParseService.parseContractFile(file);
            
            log.info("合同解析完成，文件: {}, AI解析: {}, 状态: {}", 
                    file.getOriginalFilename(), response.getAiParsed(), response.getParseStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("合同解析失败，文件: {}, 错误: {}", file.getOriginalFilename(), e.getMessage(), e);
            
            ContractParseResponse errorResponse = ContractParseResponse.builder()
                    .parseStatus("ERROR")
                    .parseMessage("合同解析失败: " + e.getMessage())
                    .aiParsed(false)
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Contract Parse Service is running");
    }

    /**
     * 获取支持的文件格式信息
     */
    @GetMapping("/supported-formats")
    public ResponseEntity<String> getSupportedFormats() {
        return ResponseEntity.ok("支持的文件格式: PDF (最大50MB)");
    }

    /**
     * 测试接口：提取PDF文本内容（用于调试）
     */
    @PostMapping(value = "/extract-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> extractText(@RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            String textContent = contractParseService.extractTextFromPdf(file.getBytes());
            return ResponseEntity.ok(textContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("文本提取失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("只支持PDF格式文件");
        }
        
        // 检查文件大小（50MB限制）
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过50MB");
        }
        
        log.debug("文件验证通过: {}", originalFilename);
    }
}
