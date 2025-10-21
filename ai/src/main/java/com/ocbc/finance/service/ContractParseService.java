package com.ocbc.finance.service;

import com.ocbc.finance.dto.contract.ContractParseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同解析服务
 * 负责解析合同文件并提取关键信息
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractParseService {

    private final ContractService contractService;
    
    @Autowired(required = false)
    private DeepSeekAiService deepSeekAiService;
    
    @Autowired(required = false)
    private GeminiAiService geminiAiService;
    
    @Autowired(required = false)
    private Gemma3AiService gemma3AiService;
    
    @Value("${app.ai.provider:none}")
    private String aiProvider;

    /**
     * 解析合同文件并提取关键信息
     * 
     * @param file 合同文件
     * @param customFields 自定义字段列表
     * @return 解析结果
     */
    public ContractParseResponse parseContractFile(MultipartFile file, List<String> customFields) {
        try {
            log.info("开始解析合同文件: {}, 自定义字段: {}", file.getOriginalFilename(), customFields);
            
            // 1. 提取PDF文本内容
            String textContent = extractTextFromPdfInternal(file.getBytes());
            
            // 2. 使用AI服务或关键字匹配解析
            Map<String, Object> extractedInfo = parseContractContent(textContent);
            
            // 3. 提取自定义字段
            Map<String, String> customFieldsResult = extractCustomFields(textContent, customFields);
            extractedInfo.put("_customFields", customFieldsResult);
            
            // 4. 转换为标准响应格式
            ContractParseResponse response = convertToResponse(extractedInfo);
            
            log.info("合同解析完成: {}", response.getParseMessage());
            return response;
            
        } catch (Exception e) {
            log.error("合同解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("合同解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从PDF文件提取文本内容
     */
    private String extractTextFromPdfInternal(byte[] pdfData) {
        try {
            // 复用ContractService中的PDF文本提取逻辑
            return contractService.extractTextFromPdf(pdfData);
        } catch (Exception e) {
            log.warn("PDF文本提取失败，将使用空文本进行关键字匹配: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 公共方法：从PDF文件提取文本内容（用于调试）
     */
    public String extractTextFromPdf(byte[] pdfData) {
        return extractTextFromPdfInternal(pdfData);
    }

    /**
     * 解析合同内容
     */
    private Map<String, Object> parseContractContent(String textContent) {
        log.info("AI服务提供商配置: {}", aiProvider);
        
        // 1. 尝试使用AI服务解析
        if (!"none".equalsIgnoreCase(aiProvider)) {
            Map<String, Object> aiResult = tryAiParsing(textContent);
            if (aiResult != null) {
                return aiResult;
            }
        }
        
        // 2. 降级到关键字匹配
        log.info("使用关键字匹配解析合同");
        return parseWithKeywords(textContent);
    }

    /**
     * 尝试AI解析
     */
    private Map<String, Object> tryAiParsing(String textContent) {
        try {
            // DeepSeek AI
            if ("deepseek".equalsIgnoreCase(aiProvider) && deepSeekAiService != null && deepSeekAiService.isAvailable()) {
                Map<String, Object> result = deepSeekAiService.parseContractWithAI(textContent);
                result.put("_aiParsed", true);
                result.put("_parseMessage", "使用DeepSeek AI解析成功");
                return result;
            }
            
            // Gemini AI
            if ("gemini".equalsIgnoreCase(aiProvider) && geminiAiService != null && geminiAiService.isAvailable()) {
                Map<String, Object> result = geminiAiService.parseContractWithAITimed(textContent);
                result.put("_aiParsed", true);
                result.put("_parseMessage", "使用Gemini AI解析成功");
                return result;
            }
            
            // Gemma3 AI
            if ("gemma3".equalsIgnoreCase(aiProvider) && gemma3AiService != null && gemma3AiService.isAvailable()) {
                Map<String, Object> result = gemma3AiService.parseContractWithAITimed(textContent);
                result.put("_aiParsed", true);
                result.put("_parseMessage", "使用Gemma3 AI解析成功");
                return result;
            }
            
        } catch (Exception e) {
            log.warn("AI解析失败: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * 使用关键字匹配解析
     */
    private Map<String, Object> parseWithKeywords(String textContent) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        // 调试：输出PDF文本内容前500字符
        log.info("PDF文本内容预览: {}", textContent.length() > 500 ? textContent.substring(0, 500) + "..." : textContent);
        
        // 合同总金额 - 根据实际合同内容优化
        String totalAmount = extractByPattern(textContent, 
            "租赁总金额为人民币\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*元", 1);
        if (totalAmount == null) {
            totalAmount = extractByPattern(textContent, 
                "人民币\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*元\\s*（含税）", 1);
        }
        if (totalAmount == null) {
            totalAmount = extractByPattern(textContent, 
                "(?:总金额|金额)为人民币\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*元", 1);
        }
        result.put("contractAmount", totalAmount);
        
        // 乙方公司名称 - 根据实际格式优化
        String partyB = extractByPattern(textContent, 
            "乙方（承租方）：([^\\n\\r地址]{1,50})", 1);
        if (partyB == null) {
            partyB = extractByPattern(textContent, 
                "乙方\\s*（承租方）\\s*：\\s*([^\\n\\r地址]{1,50})", 1);
        }
        if (partyB == null) {
            partyB = extractByPattern(textContent, 
                "承租方）：([^\\n\\r地址]{1,50})", 1);
        }
        result.put("partyB", partyB);
        
        // 合同开始日期 - 根据实际格式优化
        String startDate = extractByPattern(textContent, 
            "租赁期限自\\s*(\\d{4}-\\d{1,2}-\\d{1,2})\\s*起", 1);
        if (startDate == null) {
            startDate = extractByPattern(textContent, 
                "自\\s*(\\d{4}-\\d{1,2}-\\d{1,2})\\s*起", 1);
        }
        if (startDate == null) {
            startDate = extractByPattern(textContent, 
                "(\\d{4}-\\d{1,2}-\\d{1,2})\\s*起至", 1);
        }
        result.put("startDate", startDate);
        
        // 合同结束日期 - 根据实际格式优化
        String endDate = extractByPattern(textContent, 
            "至\\s*(\\d{4}-\\d{1,2}-\\d{1,2})\\s*止", 1);
        if (endDate == null) {
            endDate = extractByPattern(textContent, 
                "起至\\s*(\\d{4}-\\d{1,2}-\\d{1,2})", 1);
        }
        result.put("endDate", endDate);
        
        // 税率 - 根据实际格式优化
        String taxRate = extractByPattern(textContent, 
            "税率为\\s*([0-9]+(?:\\.[0-9]{1,2})?)%", 1);
        if (taxRate == null) {
            taxRate = extractByPattern(textContent, 
                "\\s+([0-9]+(?:\\.[0-9]{1,2})?)%", 1);
        }
        result.put("taxRate", taxRate);
        
        result.put("_aiParsed", false);
        result.put("_parseMessage", "使用关键字匹配解析完成");
        
        log.info("关键字匹配结果: {}", result);
        return result;
    }

    /**
     * 使用正则表达式提取信息
     */
    private String extractByPattern(String text, String patternStr, int group) {
        try {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(group).trim();
            }
        } catch (Exception e) {
            log.warn("正则表达式匹配失败: pattern={}, error={}", patternStr, e.getMessage());
        }
        return null;
    }

    /**
     * 转换为标准响应格式
     */
    private ContractParseResponse convertToResponse(Map<String, Object> extractedInfo) {
        ContractParseResponse.ContractParseResponseBuilder builder = ContractParseResponse.builder();
        
        // 解析状态信息
        Boolean aiParsed = (Boolean) extractedInfo.get("_aiParsed");
        String parseMessage = (String) extractedInfo.get("_parseMessage");
        
        builder.aiParsed(aiParsed != null ? aiParsed : false)
               .parseStatus("SUCCESS")
               .parseMessage(parseMessage != null ? parseMessage : "解析完成");
        
        // 合同总金额
        Object amountObj = extractedInfo.get("contractAmount");
        if (amountObj != null) {
            builder.totalAmount(parseAmount(amountObj.toString()));
        }
        
        // 乙方公司名称
        Object vendorObj = extractedInfo.get("partyB");
        if (vendorObj != null) {
            builder.vendorName(vendorObj.toString().trim());
        }
        
        // 开始日期
        Object startDateObj = extractedInfo.get("startDate");
        if (startDateObj != null) {
            builder.startDate(parseDate(startDateObj.toString()));
        }
        
        // 结束日期
        Object endDateObj = extractedInfo.get("endDate");
        if (endDateObj != null) {
            builder.endDate(parseDate(endDateObj.toString()));
        }
        
        // 税率
        Object taxRateObj = extractedInfo.get("taxRate");
        if (taxRateObj != null) {
            builder.taxRate(parseTaxRate(taxRateObj.toString()));
        }
        
        // 自定义字段
        @SuppressWarnings("unchecked")
        Map<String, String> customFields = (Map<String, String>) extractedInfo.get("_customFields");
        if (customFields != null) {
            builder.customFields(customFields);
        }
        
        return builder.build();
    }

    /**
     * 解析金额字符串
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除逗号和其他非数字字符（保留小数点）
            String cleanAmount = amountStr.replaceAll("[,，]", "")
                                         .replaceAll("[^0-9.]", "");
            return new BigDecimal(cleanAmount);
        } catch (Exception e) {
            log.warn("金额解析失败: {}", amountStr);
            return null;
        }
    }

    /**
     * 解析税率字符串，将百分比转换为小数
     */
    private BigDecimal parseTaxRate(String taxRateStr) {
        if (taxRateStr == null || taxRateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除百分号和其他非数字字符（保留小数点）
            String cleanTaxRate = taxRateStr.replaceAll("[%％]", "")
                                           .replaceAll("[^0-9.]", "");
            
            BigDecimal rate = new BigDecimal(cleanTaxRate);
            
            // 将百分比转换为小数（例如：6.0% -> 0.06）
            return rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("税率解析失败: {}", taxRateStr);
            return null;
        }
    }

    /**
     * 解析日期字符串
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 标准化日期格式
            String normalizedDate = dateStr.replaceAll("[年月]", "-")
                                          .replaceAll("[日]", "")
                                          .replaceAll("[-/]{2,}", "-");
            
            // 尝试不同的日期格式
            String[] patterns = {
                "yyyy-MM-dd",
                "yyyy-M-d",
                "yyyy/MM/dd",
                "yyyy/M/d"
            };
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDateTime.parse(normalizedDate + " 00:00:00", 
                            DateTimeFormatter.ofPattern(pattern + " HH:mm:ss"));
                } catch (DateTimeParseException ignored) {
                    // 继续尝试下一个格式
                }
            }
            
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
        }
        
        return null;
    }

    /**
     * 提取自定义字段
     * 
     * @param textContent PDF文本内容
     * @param customFields 自定义字段列表
     * @return 自定义字段提取结果
     */
    private Map<String, String> extractCustomFields(String textContent, List<String> customFields) {
        Map<String, String> result = new HashMap<>();
        
        if (customFields == null || customFields.isEmpty()) {
            log.debug("没有自定义字段需要提取");
            return result;
        }
        
        log.info("开始提取自定义字段: {}", customFields);
        
        for (String fieldName : customFields) {
            if (fieldName == null || fieldName.trim().isEmpty()) {
                continue;
            }
            
            // 使用关键字匹配提取自定义字段值
            String fieldValue = extractCustomFieldValue(textContent, fieldName.trim());
            result.put(fieldName.trim(), fieldValue != null ? fieldValue : "");
        }
        
        log.info("自定义字段提取完成: {}", result);
        return result;
    }

    /**
     * 提取单个自定义字段的值
     * 使用多种模式尝试匹配字段值
     * 
     * @param textContent PDF文本内容
     * @param fieldName 字段名称
     * @return 字段值
     */
    private String extractCustomFieldValue(String textContent, String fieldName) {
        // 尝试多种匹配模式
        String[] patterns = {
            // 模式1: 字段名：值
            fieldName + "[：:]+\\s*([^\\n\\r]{1,100})",
            // 模式2: 字段名为 值
            fieldName + "为\\s*([^\\n\\r]{1,100})",
            // 模式3: 字段名是 值
            fieldName + "是\\s*([^\\n\\r]{1,100})",
            // 模式4: 字段名 值（空格分隔）
            fieldName + "\\s+([^\\n\\r]{1,100})"
        };
        
        for (String patternStr : patterns) {
            String value = extractByPattern(textContent, patternStr, 1);
            if (value != null && !value.trim().isEmpty()) {
                log.debug("字段 '{}' 提取成功，使用模式: {}", fieldName, patternStr);
                return value.trim();
            }
        }
        
        log.debug("字段 '{}' 未找到匹配值", fieldName);
        return null;
    }
}
