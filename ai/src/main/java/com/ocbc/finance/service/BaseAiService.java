package com.ocbc.finance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * AI服务基础抽象类
 * 提供公共的字段映射和数据处理方法
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
public abstract class BaseAiService {

    protected final ObjectMapper objectMapper;

    protected BaseAiService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 构建标准的合同解析提示词
     */
    protected String buildStandardContractParsePrompt(String contractText) {
        return String.format("""
            请深刻理解以下合同内容，并以JSON格式返回合同中的关键财务信息。
            
            合同内容：
            %s
            
            请提取以下信息并以JSON格式返回（如果某些信息在合同中找不到，请返回null）：
            {
                "contractNo": "合同编号",
                "contractName": "合同名称",
                "partyA": "甲方名称",
                "partyAId": "甲方统一社会信用代码",
                "partyB": "乙方名称",
                "partyBId": "乙方统一社会信用代码",
                "contractDescription": "合同描述/服务内容",
                "contractAmount": "合同总金额（数字）",
                "currency": "币种",
                "startDate": "合同开始日期",
                "endDate": "合同结束日期",
                "signDate": "签订日期",
                "effectiveDate": "生效日期",
                "taxRate": "税率（百分比数字）",
                "paymentTerms": "付款条款",
                "paymentMethod": "付款方式"
            }
            
            注意：
            1. 只返回JSON格式的数据，不要包含任何其他文字说明
            2. 日期格式统一为YYYY-MM-DD
            3. 金额只返回数字，不包含货币符号
            4. 如果信息不明确或找不到，返回null
            """, contractText);
    }

    /**
     * 标准的AI响应解析和字段映射
     * 
     * @param responseText AI返回的原始文本
     * @param aiServiceName AI服务名称（用于日志和标记）
     * @return 标准化的解析结果
     */
    protected Map<String, Object> parseStandardAiResponse(String responseText, String aiServiceName) {
        try {
            // 清理响应文本，移除可能的markdown格式
            String cleanedText = cleanResponseText(responseText);
            
            log.info("{} AI返回内容: {}", aiServiceName, cleanedText);

            // 解析JSON内容
            Map<String, Object> contentJson = parseJsonContent(cleanedText);
            Map<String, Object> result = new HashMap<>();

            // 标准字段映射
            mapStandardFields(contentJson, result);

            // 添加解析标记
            result.put("_aiParsed", true);
            result.put("_parseMessage", aiServiceName + " AI解析成功");

            log.info("{} AI解析结果: {}", aiServiceName, result);
            return result;
            
        } catch (Exception e) {
            log.error("解析{} AI响应失败: {}, 原始响应: {}", aiServiceName, e.getMessage(), responseText);
            throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理响应文本，移除markdown格式
     */
    protected String cleanResponseText(String responseText) {
        String cleanedText = responseText.trim();
        if (cleanedText.startsWith("```json")) {
            cleanedText = cleanedText.substring(7);
        }
        if (cleanedText.endsWith("```")) {
            cleanedText = cleanedText.substring(0, cleanedText.length() - 3);
        }
        return cleanedText.trim();
    }

    /**
     * 解析JSON内容
     */
    protected Map<String, Object> parseJsonContent(String jsonText) throws Exception {
        return objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 标准字段映射
     */
    protected void mapStandardFields(Map<String, Object> contentJson, Map<String, Object> result) {
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
     * 获取文本值（通用方法）
     */
    protected String getTextValue(Map<String, Object> json, String fieldName) {
        Object value = json.get(fieldName);
        if (value == null) {
            return null;
        }
        String textValue = value.toString().trim();
        return textValue.equals("null") || textValue.isEmpty() ? null : textValue;
    }

    /**
     * 获取数值（通用方法）
     */
    protected Object getNumericValue(Map<String, Object> json, String fieldName) {
        Object value = json.get(fieldName);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        String textValue = value.toString().trim();
        if (textValue.equals("null") || textValue.isEmpty()) {
            return null;
        }
        
        try {
            return Double.parseDouble(textValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查AI服务是否可用（抽象方法，由子类实现）
     */
    public abstract boolean isAvailable();

    /**
     * 使用AI解析合同文本（抽象方法，由子类实现）
     */
    public abstract Map<String, Object> parseContractWithAI(String contractText);

    /**
     * 带耗时统计的AI解析方法
     */
    public Map<String, Object> parseContractWithAITimed(String contractText) {
        long startTime = System.currentTimeMillis();
        String aiServiceName = this.getClass().getSimpleName().replace("AiService", "");
        
        log.info("开始调用{} AI解析合同，文本长度: {} 字符", aiServiceName, contractText.length());
        
        try {
            Map<String, Object> result = parseContractWithAI(contractText);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("{}AI解析完成，总耗时: {}ms ({}秒)", aiServiceName, duration, duration / 1000.0);
            
            // 添加耗时信息到结果中
            result.put("_aiDuration", duration);
            result.put("_aiDurationSeconds", duration / 1000.0);
            
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.error("{}AI解析失败，耗时: {}ms ({}秒)，错误: {}", 
                    aiServiceName, duration, duration / 1000.0, e.getMessage());
            throw e;
        }
    }
}
