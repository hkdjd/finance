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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;
    
    public ExternalContractParseService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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
            body.add("createdBy", "system"); // AI服务需要的创建者参数
            
            // 添加自定义字段参数
            if (customFields != null && !customFields.isEmpty()) {
                for (String field : customFields) {
                    body.add("customFields", field);
                }
            }
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 调用外部接口
            logger.info("调用外部接口解析合同: {}, 自定义字段: {}", externalParseUrl, customFields);
            ResponseEntity<String> response = restTemplate.exchange(
                    externalParseUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            String responseBody = response.getBody();
            if (responseBody != null) {
                logger.info("合同解析成功: {}", contractFile.getName());
                logger.info("AI服务完整响应: {}", responseBody);
                return parseAiServiceResponse(responseBody, contractFile.getName());
            } else {
                logger.warn("合同解析失败: 响应为空");
                return createErrorResponse("外部接口解析失败：响应为空");
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
    
    /**
     * 解析AI服务返回的JSON响应
     */
    private ExternalContractParseResponse parseAiServiceResponse(String responseBody, String fileName) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // 检查响应是否成功
            boolean success = rootNode.path("success").asBoolean(false);
            if (!success) {
                String message = rootNode.path("message").asText("AI服务解析失败");
                return createErrorResponse(message);
            }
            
            // 获取data节点
            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isMissingNode()) {
                return createErrorResponse("AI服务响应缺少data字段");
            }
            
            // 获取extractedInfo节点
            JsonNode extractedInfoNode = dataNode.path("extractedInfo");
            if (extractedInfoNode.isMissingNode()) {
                return createErrorResponse("AI服务响应缺少extractedInfo字段");
            }
            
            // 构建响应对象
            ExternalContractParseResponse response = new ExternalContractParseResponse();
            response.setSuccess(true);
            
            // 提取合同金额
            JsonNode contractAmountNode = extractedInfoNode.path("contractAmount");
            if (!contractAmountNode.isMissingNode()) {
                response.setTotalAmount(new BigDecimal(contractAmountNode.asDouble()));
            }
            
            // 提取开始日期
            JsonNode startDateNode = extractedInfoNode.path("startDate");
            if (!startDateNode.isMissingNode()) {
                response.setStartDate(startDateNode.asText());
            }
            
            // 提取结束日期
            JsonNode endDateNode = extractedInfoNode.path("endDate");
            if (!endDateNode.isMissingNode()) {
                response.setEndDate(endDateNode.asText());
            }
            
            // 提取税率
            JsonNode taxRateNode = extractedInfoNode.path("taxRate");
            if (!taxRateNode.isMissingNode()) {
                response.setTaxRate(new BigDecimal(taxRateNode.asDouble()).divide(new BigDecimal("100")));
            }
            
            // 提取供应商名称（乙方）
            JsonNode partyBNode = extractedInfoNode.path("partyB");
            if (!partyBNode.isMissingNode()) {
                response.setVendorName(partyBNode.asText());
            }
            
            // 提取自定义字段
            JsonNode customFieldsNode = extractedInfoNode.path("customFields");
            logger.info("customFields节点存在: {}, 是对象: {}, 节点类型: {}", 
                    !customFieldsNode.isMissingNode(), 
                    customFieldsNode.isObject(),
                    customFieldsNode.getNodeType());
            logger.info("customFields节点内容: {}", customFieldsNode);
            
            if (!customFieldsNode.isMissingNode() && customFieldsNode.isObject()) {
                Map<String, String> customFieldsMap = new HashMap<>();
                customFieldsNode.fields().forEachRemaining(entry -> {
                    customFieldsMap.put(entry.getKey(), entry.getValue().asText());
                });
                response.setCustomFields(customFieldsMap);
                logger.info("提取到自定义字段: {}", customFieldsMap);
            } else {
                logger.warn("customFields节点缺失或不是对象类型");
            }
            
            logger.info("AI服务响应解析成功: 文件={}, 金额={}, 供应商={}, 自定义字段={}", 
                    fileName, response.getTotalAmount(), response.getVendorName(), response.getCustomFields());
            
            return response;
            
        } catch (Exception e) {
            logger.error("解析AI服务响应失败: {}", e.getMessage(), e);
            return createErrorResponse("解析AI服务响应失败: " + e.getMessage());
        }
    }
}
