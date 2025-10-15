package com.ocbc.finance.service;

import com.ocbc.finance.dto.ContractAttachmentResponse;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.repository.ContractRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 合同附件服务类
 * 处理合同附件的查看和下载
 */
@Service
@Transactional(readOnly = true)
public class ContractAttachmentService {

    private final ContractRepository contractRepository;
    
    // 文件存储根目录，可以通过配置文件设置
    private final String fileStorageRoot = "/uploads/contracts";

    public ContractAttachmentService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    /**
     * 获取合同附件信息
     */
    public ContractAttachmentResponse getAttachmentInfo(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("合同不存在，ID=" + contractId));
        
        // 检查合同是否有附件
        if (contract.getFilePath() == null || contract.getFilePath().isEmpty()) {
            throw new IllegalArgumentException("合同没有附件文件，ID=" + contractId);
        }
        
        // 构造附件信息
        ContractAttachmentResponse.AttachmentInfo attachmentInfo = new ContractAttachmentResponse.AttachmentInfo();
        attachmentInfo.setId(contract.getId());
        attachmentInfo.setFileName(generateFileName(contract));
        attachmentInfo.setOriginalFileName(contract.getOriginalFileName());
        attachmentInfo.setFilePath(contract.getFilePath());
        attachmentInfo.setUploadTime(contract.getCreatedAt());
        attachmentInfo.setUploadBy(contract.getCreatedBy());
        attachmentInfo.setDownloadUrl("/contracts/" + contractId + "/attachment?download=true");
        
        // 获取文件信息
        try {
            Path filePath = Paths.get(contract.getFilePath());
            if (Files.exists(filePath)) {
                attachmentInfo.setFileSize(Files.size(filePath));
                attachmentInfo.setContentType(Files.probeContentType(filePath));
            }
        } catch (IOException e) {
            // 文件不存在或无法访问，设置默认值
            attachmentInfo.setFileSize(0L);
            attachmentInfo.setContentType("application/octet-stream");
        }
        
        return new ContractAttachmentResponse(contractId, attachmentInfo);
    }

    /**
     * 获取附件文件用于下载
     */
    public AttachmentFile getAttachmentFile(Long contractId) throws IOException {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("合同不存在，ID=" + contractId));
        
        if (contract.getFilePath() == null || contract.getFilePath().isEmpty()) {
            throw new IllegalArgumentException("合同没有附件文件，ID=" + contractId);
        }
        
        Path filePath = Paths.get(contract.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("附件文件不存在，路径=" + contract.getFilePath());
        }
        
        Resource resource = new FileSystemResource(filePath.toFile());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return new AttachmentFile(
                resource,
                generateFileName(contract),
                contentType,
                Files.size(filePath)
        );
    }
    
    /**
     * 生成显示用的文件名
     */
    private String generateFileName(Contract contract) {
        String originalName = contract.getOriginalFileName();
        if (originalName != null && !originalName.isEmpty()) {
            return originalName;
        }
        
        // 如果没有原始文件名，生成一个默认名称
        String extension = getFileExtension(contract.getFilePath());
        return String.format("合同_%s_%s%s", 
                contract.getVendorName() != null ? contract.getVendorName() : "未知供应商",
                contract.getCreatedAt().toLocalDate().toString().replace("-", ""),
                extension);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastDotIndex = filePath.lastIndexOf('.');
        return lastDotIndex > 0 ? filePath.substring(lastDotIndex) : "";
    }
    
    /**
     * 附件文件包装类
     */
    public static class AttachmentFile {
        private final Resource resource;
        private final String fileName;
        private final String contentType;
        private final Long fileSize;
        
        public AttachmentFile(Resource resource, String fileName, String contentType, Long fileSize) {
            this.resource = resource;
            this.fileName = fileName;
            this.contentType = contentType;
            this.fileSize = fileSize;
        }
        
        public Resource getResource() {
            return resource;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
    }
}
