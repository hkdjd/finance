package com.ocbc.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
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
 * DeepSeek AI服务类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.deepseek.enabled", havingValue = "true", matchIfMissing = false)
public class DeepSeekAiService extends BaseAiService {

    @Value("${app.deepseek.api-key}")
    private String apiKey;

    @Value("${app.deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${app.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${app.deepseek.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate;

    public DeepSeekAiService() {
        super(); // 调用父类构造函数，初始化ObjectMapper
        this.restTemplate = new RestTemplate();
    }


    /**
     * 检查DeepSeek AI服务是否可用
     */
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && 
               apiUrl != null && !apiUrl.trim().isEmpty();
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekApi(String prompt) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "你是资深的财务总监，请阅读以下合同，并以JSON格式返回合同中的关键财务信息"),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("stream", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("调用DeepSeek API: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("DeepSeek API调用成功");
                return response.getBody();
            } else {
                throw new RuntimeException("DeepSeek API调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用DeepSeek API异常: {}", e.getMessage(), e);
            throw new RuntimeException("调用DeepSeek API失败: " + e.getMessage(), e);
        }
    }
    /**
     * 使用DeepSeek AI解析合同文本
     * 
     * @param contractText 合同文本内容
     * @return 解析结果
     */
    @Override
    public Map<String, Object> parseContractWithAI(String contractText) {
        if (!isAvailable()) {
            throw new RuntimeException("DeepSeek AI服务未配置或不可用");
        }

        long startTime = System.currentTimeMillis();
        log.info("开始调用DeepSeek AI解析合同，文本长度: {} 字符", contractText.length());

        try {
            // 使用基类的标准提示词
            String prompt = buildStandardContractParsePrompt(contractText);
            log.info("DeepSeek AI 请求提示词: {}", prompt);
            
            // 调用DeepSeek API
            String response = callDeepSeekApi(prompt);
            
            // 解析DeepSeek API响应
            Map<String, Object> result = parseDeepSeekApiResponse(response);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("DeepSeekAI解析完成，总耗时: {}ms ({}秒)", duration, duration / 1000.0);
            
            // 添加耗时信息到结果中
            result.put("_aiDuration", duration);
            result.put("_aiDurationSeconds", duration / 1000.0);
            
            return result;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.error("DeepSeekAI解析失败，耗时: {}ms ({}秒)，错误: {}", 
                    duration, duration / 1000.0, e.getMessage());
            throw new RuntimeException("AI解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析DeepSeek API响应
     */
    private Map<String, Object> parseDeepSeekApiResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            
            // 检查是否有错误
            if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").get("message").asText();
                throw new RuntimeException("DeepSeek API返回错误: " + errorMsg);
            }

            // 提取content内容
            JsonNode choicesNode = rootNode.get("choices");
            if (choicesNode == null || choicesNode.isEmpty()) {
                throw new RuntimeException("DeepSeek API响应格式异常：缺少choices字段");
            }

            JsonNode messageNode = choicesNode.get(0).get("message");
            if (messageNode == null) {
                throw new RuntimeException("DeepSeek API响应格式异常：缺少message字段");
            }

            String content = messageNode.get("content").asText();
            log.info("DeepSeek AI返回内容: {}", content);

            // 解析JSON内容
            JsonNode contentJson = objectMapper.readTree(content);
            Map<String, Object> result = new HashMap<>();

            // 标准字段映射
            mapStandardFields(contentJson, result);

            // 添加解析标记
            result.put("_aiParsed", true);
            result.put("_parseMessage", "DeepSeek AI解析成功");

            log.info("DeepSeek AI解析结果: {}", result);
            return result;

        } catch (Exception e) {
            log.error("解析DeepSeek响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析DeepSeek响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 标准字段映射（使用JsonNode）
     */
    private void mapStandardFields(JsonNode contentJson, Map<String, Object> result) {
        // 基础信息字段
        result.put("contractNo", getTextValue(contentJson, "contractNo"));
        result.put("contractName", getTextValue(contentJson, "contractName"));
        result.put("partyA", getTextValue(contentJson, "partyA"));
        result.put("partyAId", getTextValue(contentJson, "partyAId"));
        result.put("partyB", getTextValue(contentJson, "partyB"));
        result.put("partyBId", getTextValue(contentJson, "partyBId"));
        result.put("contractDescription", getTextValue(contentJson, "contractDescription"));
        
        // 金额字段
        result.put("contractAmount", getNumericValue(contentJson, "contractAmount"));
        
        // 税率字段
        result.put("taxRate", getNumericValue(contentJson, "taxRate"));
        
        // 币种字段（默认SGD）
        String currency = getTextValue(contentJson, "currency");
        result.put("currency", currency != null ? currency : "SGD");
        
        // 日期字段
        result.put("startDate", getTextValue(contentJson, "startDate"));
        result.put("endDate", getTextValue(contentJson, "endDate"));
        result.put("signDate", getTextValue(contentJson, "signDate"));
        result.put("effectiveDate", getTextValue(contentJson, "effectiveDate"));
        
        // 付款相关字段
        result.put("paymentMethod", getTextValue(contentJson, "paymentMethod"));
        result.put("paymentTerms", getTextValue(contentJson, "paymentTerms"));
    }

    /**
     * 获取文本值（JsonNode版本）
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return value.equals("null") || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * 获取数值（JsonNode版本）
     */
    private Object getNumericValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        
        if (fieldNode.isNumber()) {
            return fieldNode.doubleValue();
        }
        
        String textValue = fieldNode.asText();
        if (textValue.equals("null") || textValue.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Double.parseDouble(textValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
