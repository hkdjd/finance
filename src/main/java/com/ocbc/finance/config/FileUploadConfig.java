package com.ocbc.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {
    
    /** 合同附件上传目录 */
    private String contractPath = "./uploads/contracts/";
    
    /** 允许的文件类型 */
    private String[] allowedTypes = {"pdf", "doc", "docx", "jpg", "jpeg", "png"};
    
    /** 最大文件大小（字节） */
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    
    public String getContractPath() {
        return contractPath;
    }
    
    public void setContractPath(String contractPath) {
        this.contractPath = contractPath;
    }
    
    public String[] getAllowedTypes() {
        return allowedTypes;
    }
    
    public void setAllowedTypes(String[] allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
