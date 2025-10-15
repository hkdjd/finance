package com.ocbc.finance.controller;

import com.ocbc.finance.dto.ContractAttachmentResponse;
import com.ocbc.finance.service.ContractAttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 合同附件控制器
 * 提供合同附件查看和下载功能
 */
@RestController
@RequestMapping("/contracts")
public class ContractAttachmentController {

    private final ContractAttachmentService contractAttachmentService;

    public ContractAttachmentController(ContractAttachmentService contractAttachmentService) {
        this.contractAttachmentService = contractAttachmentService;
    }

    /**
     * 合同附件查看和下载接口
     * @param contractId 合同ID
     * @param download 是否下载文件，默认false（返回附件信息）
     * @return 附件信息或文件流
     */
    @GetMapping("/{contractId}/attachment")
    public ResponseEntity<?> getContractAttachment(
            @PathVariable Long contractId,
            @RequestParam(value = "download", defaultValue = "false") Boolean download) throws IOException {
        
        if (download) {
            // 下载文件
            return downloadAttachmentFile(contractId);
        } else {
            // 返回附件信息
            ContractAttachmentResponse attachmentInfo = contractAttachmentService.getAttachmentInfo(contractId);
            return ResponseEntity.ok(attachmentInfo);
        }
    }

    /**
     * 下载附件文件
     */
    private ResponseEntity<Resource> downloadAttachmentFile(Long contractId) throws IOException {
        ContractAttachmentService.AttachmentFile attachmentFile = contractAttachmentService.getAttachmentFile(contractId);
        
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachmentFile.getContentType()));
        
        // 设置文件名，支持中文
        String encodedFileName = URLEncoder.encode(attachmentFile.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        headers.setContentDispositionFormData("attachment", encodedFileName);
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(attachmentFile.getFileSize())
                .body(attachmentFile.getResource());
    }
}
