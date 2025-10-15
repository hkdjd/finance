package com.ocbc.finance.dto;

import java.time.LocalDateTime;

/**
 * 合同附件响应DTO
 */
public class ContractAttachmentResponse {
    
    private Long contractId;
    private AttachmentInfo attachment;
    
    public ContractAttachmentResponse() {}
    
    public ContractAttachmentResponse(Long contractId, AttachmentInfo attachment) {
        this.contractId = contractId;
        this.attachment = attachment;
    }
    
    // Getters and Setters
    public Long getContractId() {
        return contractId;
    }
    
    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }
    
    public AttachmentInfo getAttachment() {
        return attachment;
    }
    
    public void setAttachment(AttachmentInfo attachment) {
        this.attachment = attachment;
    }
    
    /**
     * 附件信息内嵌类
     */
    public static class AttachmentInfo {
        private Long id;
        private String fileName;
        private String originalFileName;
        private Long fileSize;
        private String contentType;
        private LocalDateTime uploadTime;
        private String uploadBy;
        private String filePath;
        private String downloadUrl;
        
        public AttachmentInfo() {}
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getOriginalFileName() {
            return originalFileName;
        }
        
        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
        
        public LocalDateTime getUploadTime() {
            return uploadTime;
        }
        
        public void setUploadTime(LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
        }
        
        public String getUploadBy() {
            return uploadBy;
        }
        
        public void setUploadBy(String uploadBy) {
            this.uploadBy = uploadBy;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getDownloadUrl() {
            return downloadUrl;
        }
        
        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }
}
