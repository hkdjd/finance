package com.ocbc.finance.service;

import com.ocbc.finance.config.FileUploadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * 文件上传服务
 */
@Service
public class FileUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    
    private final FileUploadConfig fileUploadConfig;
    
    public FileUploadService(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
    }
    
    /**
     * 上传合同文件
     * @param file 上传的文件
     * @return 保存后的文件名
     */
    public String uploadContractFile(MultipartFile file) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 确保上传目录存在
        String uploadPath = fileUploadConfig.getContractPath();
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            logger.info("创建上传目录: {}", uploadPath);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String savedFileName = String.format("contract_%s_%s.%s", timestamp, uuid, extension);
        
        // 保存文件
        Path filePath = uploadDir.resolve(savedFileName);
        Files.copy(file.getInputStream(), filePath);
        
        logger.info("合同文件上传成功: {} -> {}", originalFilename, savedFileName);
        return savedFileName;
    }
    
    /**
     * 获取合同文件
     * @param fileName 文件名
     * @return 文件对象
     */
    public File getContractFile(String fileName) {
        Path filePath = Paths.get(fileUploadConfig.getContractPath(), fileName);
        File file = filePath.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + fileName);
        }
        return file;
    }
    
    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小超过限制: " + fileUploadConfig.getMaxFileSize() + " 字节");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        String[] allowedTypes = fileUploadConfig.getAllowedTypes();
        boolean isAllowed = Arrays.stream(allowedTypes)
                .anyMatch(type -> type.equalsIgnoreCase(extension));
        
        if (!isAllowed) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension + 
                    "，支持的类型: " + Arrays.toString(allowedTypes));
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
