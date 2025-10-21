package com.ocbc.finance.dto.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 合同上传响应DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractUploadResponse {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 提取的信息（限制大小，避免响应过大）
     */
    private Map<String, Object> extractedInfo;

    /**
     * 是否使用AI解析
     */
    private Boolean aiParsed;

    /**
     * 解析消息（限制长度）
     */
    private String parseMessage;
    
    /**
     * 处理状态
     */
    private String status;
}
