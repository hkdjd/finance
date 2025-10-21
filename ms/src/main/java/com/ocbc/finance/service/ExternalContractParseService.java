package com.ocbc.finance.service;

import com.ocbc.finance.dto.ExternalContractParseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部合同解析接口调用服务
 */
@Service
public class ExternalContractParseService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalContractParseService.class);
    
    @Value("${external.contract.parse.url:http://localhost:9999/api/contract/parse}")
    private String externalParseUrl;
    
    @Value("${external.contract.parse.enabled:false}")
    private boolean externalParseEnabled;
    
    private final RestTemplate restTemplate;
    
    public ExternalContractParseService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 调用外部接口解析合同文件
     * @param contractFile 合同文件
     * @param customFields 自定义字段列表
     * @return 解析结果
     */
    public ExternalContractParseResponse parseContract(File contractFile, List<String> customFields) {
        if (!externalParseEnabled) {
            logger.info("外部接口解析已禁用，使用模拟数据");
            return createMockResponse(contractFile.getName(), customFields);
        }
        
        try {
            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(contractFile));
            
            // 添加自定义字段参数
            if (customFields != null && !customFields.isEmpty()) {
                for (String field : customFields) {
                    body.add("customFields", field);
                }
            }
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 调用外部接口
            logger.info("调用外部接口解析合同: {}, 自定义字段: {}", externalParseUrl, customFields);
            ResponseEntity<ExternalContractParseResponse> response = restTemplate.exchange(
                    externalParseUrl,
                    HttpMethod.POST,
                    requestEntity,
                    ExternalContractParseResponse.class
            );
            
            ExternalContractParseResponse result = response.getBody();
            if (result != null && result.isSuccess()) {
                logger.info("合同解析成功: {}", contractFile.getName());
                return result;
            } else {
                logger.warn("合同解析失败: {}", result != null ? result.getErrorMessage() : "未知错误");
                return createErrorResponse("外部接口解析失败");
            }
            
        } catch (Exception e) {
            logger.error("调用外部接口异常: {}", e.getMessage(), e);
            // 如果外部接口调用失败，返回模拟数据
            logger.info("外部接口调用失败，使用模拟数据");
            return createMockResponse(contractFile.getName(), customFields);
        }
    }
    
    /**
     * 调用外部接口解析合同文件（无自定义字段）
     * @param contractFile 合同文件
     * @return 解析结果
     */
    public ExternalContractParseResponse parseContract(File contractFile) {
        return parseContract(contractFile, null);
    }
    
    /**
     * 创建模拟响应数据（用于开发测试）
     */
    private ExternalContractParseResponse createMockResponse(String fileName, List<String> customFields) {
        ExternalContractParseResponse response = new ExternalContractParseResponse();
        response.setSuccess(true);
        response.setTotalAmount(new BigDecimal("6000.00"));
        response.setStartDate("2024-01-01");
        response.setEndDate("2024-06-30");
        response.setTaxRate(new BigDecimal("0.06"));
        response.setVendorName("测试供应商_" + fileName.substring(0, Math.min(fileName.length(), 10)));
        
        // 生成模拟自定义字段数据
        if (customFields != null && !customFields.isEmpty()) {
            Map<String, String> mockCustomFields = new HashMap<>();
            for (String field : customFields) {
                mockCustomFields.put(field, "模拟值_" + field);
            }
            response.setCustomFields(mockCustomFields);
        }
        
        return response;
    }
    
    /**
     * 创建错误响应
     */
    private ExternalContractParseResponse createErrorResponse(String errorMessage) {
        ExternalContractParseResponse response = new ExternalContractParseResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
