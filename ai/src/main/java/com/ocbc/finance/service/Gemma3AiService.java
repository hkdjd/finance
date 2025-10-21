package com.ocbc.finance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemma3 AI服务类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.gemma3.enabled", havingValue = "true", matchIfMissing = false)
public class Gemma3AiService extends BaseAiService {

    @Value("${app.gemma3.api-key:}")
    private String apiKey;

    @Value("${app.gemma3.api-url:https://generativelanguage.googleapis.com/v1beta/models/gemma-2-27b-it:generateContent}")
    private String apiUrl;

    @Value("${app.gemma3.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate;

    public Gemma3AiService() {
        super(); // 调用父类构造函数，初始化ObjectMapper
        this.restTemplate = new RestTemplate();
    }

    /**
     * 检查AI服务是否可用
     */
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * 使用Gemma3 AI解析合同文本
     * 
     * @param contractText 合同文本内容
     * @return 解析结果
     */
    @Override
    public Map<String, Object> parseContractWithAI(String contractText) {
        if (!isAvailable()) {
            throw new RuntimeException("Gemma3 AI服务未配置或不可用");
        }

        try {
            // 使用基类的标准提示词
            String prompt = buildStandardContractParsePrompt(contractText);
            log.info("Gemma3 AI 请求提示词: {}", prompt);
            
            // 调用Gemma3 API
            String response = callGemma3Api(prompt);
            
            // 使用基类的标准解析方法
            return parseStandardAiResponse(response, "Gemma3");
            
        } catch (Exception e) {
            log.error("Gemma3 AI解析合同失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用Gemma3 API
     */
    private String callGemma3Api(String prompt) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return extractTextFromGemma3Response(response.getBody());
            } else {
                throw new RuntimeException("Gemma3 API调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用Gemma3 API失败: {}", e.getMessage(), e);
            throw new RuntimeException("API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从Gemma3响应中提取文本内容
     */
    private String extractTextFromGemma3Response(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                
                if (content != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            
            throw new RuntimeException("无法从Gemma3响应中提取文本内容");
            
        } catch (Exception e) {
            log.error("解析Gemma3响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("响应解析失败: " + e.getMessage(), e);
        }
    }

}
