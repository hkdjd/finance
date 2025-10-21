package com.ocbc.finance.service;

import com.ocbc.finance.dto.contract.ContractUploadResponse;
import com.ocbc.finance.dto.contract.ContractWithStatusResponse;
import com.ocbc.finance.dto.contract.OriginalContractPreviewResponse;
import com.ocbc.finance.entity.Contract;
import com.ocbc.finance.entity.OriginalContract;
import com.ocbc.finance.exception.BusinessException;
import com.ocbc.finance.repository.AccountingEntryRepository;
import com.ocbc.finance.repository.AmortizationScheduleRepository;
import com.ocbc.finance.repository.ContractDetailsRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.OriginalContractRepository;
import com.ocbc.finance.repository.PaymentScheduleRepository;
import com.ocbc.finance.util.PDFBoxConfigManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executor;

/**
 * 合同服务类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    private final OriginalContractRepository originalContractRepository;
    private final ContractRepository contractRepository;
    private final ContractDetailsRepository contractDetailsRepository;
    private final AmortizationScheduleRepository amortizationScheduleRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final AccountingEntryRepository accountingEntryRepository;
    
    @Autowired(required = false)
    private DeepSeekAiService deepSeekAiService;
    
    @Autowired(required = false)
    private GeminiAiService geminiAiService;
    
    @Autowired(required = false)
    private Gemma3AiService gemma3AiService;
    
    @Autowired
    @Qualifier("pdfProcessingExecutor")
    private Executor pdfProcessingExecutor;
    
    @Value("${app.ai.provider:none}")
    private String aiProvider;

    /**
     * 上传合同文件并解析关键信息
     * 
     * @param file 上传的文件
     * @param createdBy 创建人
     * @return 上传响应
     */
    public ContractUploadResponse uploadAndParseContract(MultipartFile file, String createdBy) {
        try {
            // 1. 验证文件格式
            validateFileFormat(file);
            
            // 2. 保存原始文件
            OriginalContract originalContract = saveOriginalContract(file, createdBy);
            
            // 3. 提取PDF文本内容
            String textContent = extractTextFromPdf(file.getBytes());
            
            // 4. 解析财务信息（按配置优先级：deepseek -> gemini -> gemma3 -> 正则表达式）
            Map<String, Object> extractedInfo;
            boolean aiParsed = false;
            String parseMessage;
            
            extractedInfo = parseContractWithAIServices(textContent);
            if (extractedInfo.containsKey("_aiParsed")) {
                aiParsed = (Boolean) extractedInfo.remove("_aiParsed");
                parseMessage = (String) extractedInfo.remove("_parseMessage");
            } else {
                aiParsed = false;
                parseMessage = "使用关键字匹配解析成功";
            }
            
            // 5. 构建响应对象
            return ContractUploadResponse.builder()
                    .fileId(originalContract.getId())
                    .fileName(file.getOriginalFilename())
                    .extractedInfo(extractedInfo)
                    .aiParsed(aiParsed)
                    .parseMessage(parseMessage)
                    .status("SUCCESS")
                    .build();
                    
        } catch (Exception e) {
            log.error("合同文件上传解析失败: {}", e.getMessage(), e);
            throw new BusinessException("合同文件上传解析失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询合同
     * 
     * @param contractId 合同ID
     * @return 合同信息
     */
    @Transactional(readOnly = true)
    public Optional<Contract> findById(Long contractId) {
        return contractRepository.findByIdAndNotDeleted(contractId);
    }

    /**
     * 根据合同别名查询合同
     * 
     * @param contractAlias 合同别名
     * @return 合同信息
     */
    @Transactional(readOnly = true)
    public Optional<Contract> findByContractAlias(String contractAlias) {
        return contractRepository.findByContractAliasAndIsDeletedFalse(contractAlias);
    }

    /**
     * 保存合同信息
     * 
     * @param contract 合同信息
     * @return 保存后的合同
     */
    public Contract saveContract(Contract contract) {
        // 检查合同别名是否已存在
        if (contract.getId() == null && contractRepository.existsByContractAliasAndIsDeletedFalse(contract.getContractAlias())) {
            throw new BusinessException("合同别名已存在: " + contract.getContractAlias());
        }
        
        return contractRepository.save(contract);
    }

    /**
     * 删除合同（软删除）- 严格按照需求文档第143行实现
     * 需要同时删除tbl_original_contract和tbl_contract表的记录
     * 
     * @param contractId 合同ID
     * @param updatedBy 更新人
     */
    public void deleteContract(Long contractId, String updatedBy) {
        Contract contract = contractRepository.findByIdAndNotDeleted(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已删除"));
        
        // 1. 删除合同表记录
        contract.setIsDeleted(true);
        contract.setUpdatedBy(updatedBy);
        contract.setUpdatedDate(LocalDateTime.now());
        contractRepository.save(contract);
        
        // 2. 删除原件表记录（根据需求文档第143行）
        Long fileId = contract.getFileId();
        if (fileId != null) {
            originalContractRepository.findById(fileId).ifPresent(originalContract -> {
                originalContract.setIsDeleted(true);
                originalContract.setUpdatedBy(updatedBy);
                originalContract.setUpdatedDate(LocalDateTime.now());
                originalContractRepository.save(originalContract);
            });
        }
        
        log.info("合同及原件已删除: contractId={}, fileId={}, updatedBy={}", contractId, fileId, updatedBy);
    }

    /**
     * 验证文件格式
     */
    private void validateFileFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("只支持PDF格式文件");
        }
        
        // 检查文件大小（50MB限制）
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过50MB");
        }
    }

    /**
     * 保存原始合同文件
     */
    private OriginalContract saveOriginalContract(MultipartFile file, String createdBy) {
        try {
            OriginalContract originalContract = new OriginalContract();
            originalContract.setFileName(file.getOriginalFilename());
            originalContract.setFileType(file.getContentType());
            originalContract.setFileData(file.getBytes());
            originalContract.setCreatedBy(createdBy);
            originalContract.setCreatedDate(LocalDateTime.now());
            
            return originalContractRepository.save(originalContract);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败", e);
        }
    }

    /**
     * 从PDF文件提取文本内容（带超时控制）
     * 使用公用线程池，避免频繁创建和销毁线程
     */
    public String extractTextFromPdf(byte[] pdfData) {
        long startTime = System.currentTimeMillis();
        
        // 使用公用PDF处理线程池
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return extractTextFromPdfInternal(pdfData);
        }, pdfProcessingExecutor);
        
        try {
            log.info("开始提取PDF文本内容，文件大小: {} bytes", pdfData.length);
            
            // 设置10秒超时（快速方案）
            String result = future.get(10, TimeUnit.SECONDS);
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("PDF文本提取完成，总耗时: {}ms", totalTime);
            return result;
            
        } catch (TimeoutException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("PDF文本提取超时，耗时: {}ms，降级到关键字匹配", totalTime);
            future.cancel(true);
            return "";
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("PDF文本提取失败，总耗时: {}ms, 错误: {}", totalTime, e.getMessage(), e);
            return "";
        }
        // 注意：不再需要finally块来关闭线程池，因为使用的是公用线程池
    }
    
    /**
     * 内部PDF文本提取实现 - 快速版本
     */
    private String extractTextFromPdfInternal(byte[] pdfData) {
        String result = null;
        
        try {
            log.info("使用快速PDF文本提取方案，文件大小: {} bytes", pdfData.length);
            
            // 方案1: 尝试使用优化的PDFBox配置
            result = extractWithOptimizedPDFBox(pdfData);
            if (isValidExtractedText(result)) {
                log.info("优化PDFBox提取成功，文本长度: {}", result.length());
                return result;
            }
            
            log.warn("优化PDFBox提取失败，尝试简化方案");
            
            // 方案2: 使用简化的PDFBox配置
            result = extractWithSimplePDFBox(pdfData);
            if (isValidExtractedText(result)) {
                log.info("简化PDFBox提取成功，文本长度: {}", result.length());
                return result;
            }
            
            log.warn("简化PDFBox提取失败，尝试备用方案");
            
            // 方案3: 使用基础PDF解析（适合简单PDF）
            result = extractWithBasicPdfParser(pdfData);
            if (isValidExtractedText(result)) {
                log.info("基础PDF解析成功，文本长度: {}", result.length());
                return result;
            }
            
            log.warn("所有PDF提取方案失败，返回空字符串");
            return "";
            
        } catch (Exception e) {
            log.error("PDF文本提取内部错误: {}", e.getMessage());
            // 不抛出异常，返回空字符串以便继续处理
            return "";
        }
    }
    
    /**
     * 检查提取的文本是否有效
     */
    private boolean isValidExtractedText(String text) {
        return text != null && !text.trim().isEmpty() && text.length() > 10;
    }
    
    /**
     * 使用优化配置的PDFBox提取（最优方案）
     */
    private String extractWithOptimizedPDFBox(byte[] pdfData) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 应用最优化的配置
            configureOptimizedPDFBox();
            
            // 临时重定向System.err以抑制PDFBox错误输出
            java.io.PrintStream originalErr = System.err;
            try (java.io.ByteArrayOutputStream suppressedErr = new java.io.ByteArrayOutputStream();
                 java.io.PrintStream nullErr = new java.io.PrintStream(suppressedErr)) {
                
                System.setErr(nullErr);
                
                try (org.apache.pdfbox.pdmodel.PDDocument document = 
                     org.apache.pdfbox.Loader.loadPDF(pdfData)) {
                    
                    // 快速检查文档
                    if (document.isEncrypted()) {
                        log.warn("PDF已加密，跳过解密尝试");
                        return null;
                    }
                    
                    int totalPages = document.getNumberOfPages();
                    int maxPages = Math.min(totalPages, 5); // 限制5页以内以提高速度
                    
                    log.debug("PDF文档页数: {}，处理页数: {}", totalPages, maxPages);
                    
                    // 使用最简化的文本提取器
                    org.apache.pdfbox.text.PDFTextStripper stripper = 
                        new org.apache.pdfbox.text.PDFTextStripper();
                    
                    // 关闭所有可能导致问题的功能
                    stripper.setSortByPosition(false);
                    stripper.setAddMoreFormatting(false);
                    stripper.setStartPage(1);
                    stripper.setEndPage(maxPages);
                    
                    String text = stripper.getText(document);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    log.debug("优化PDFBox提取完成，耗时: {}ms，文本长度: {}", duration, text != null ? text.length() : 0);
                    return text;
                    
                } finally {
                    // 恢复原始的System.err
                    System.setErr(originalErr);
                }
            }
        } catch (Exception e) {
            log.debug("优化PDFBox提取失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 配置最优化的PDFBox设置
     */
    private void configureOptimizedPDFBox() {
        PDFBoxConfigManager.configureOptimizedPDFBox();
    }
    
    
    /**
     * 使用简化配置的PDFBox提取（快速版）
     */
    private String extractWithSimplePDFBox(byte[] pdfData) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 配置PDFBox以避免字体相关错误
            configurePDFBoxForSafeExtraction();
            
            try (org.apache.pdfbox.pdmodel.PDDocument document = 
                 org.apache.pdfbox.Loader.loadPDF(pdfData)) {
                
                // 快速检查文档
                if (document.isEncrypted()) {
                    log.warn("PDF已加密，跳过解密尝试");
                    return null;
                }
                
                int totalPages = document.getNumberOfPages();
                int maxPages = Math.min(totalPages, 10); // 限制10页以内
                
                log.info("PDF文档页数: {}，处理页数: {}", totalPages, maxPages);
                
                // 使用简化的文本提取器
                org.apache.pdfbox.text.PDFTextStripper stripper = 
                    new org.apache.pdfbox.text.PDFTextStripper();
                
                // 关闭位置排序以提高速度
                stripper.setSortByPosition(false);
                stripper.setStartPage(1);
                stripper.setEndPage(maxPages);
                
                String text = stripper.getText(document);
                long duration = System.currentTimeMillis() - startTime;
                
                log.info("简化PDFBox提取成功，耗时: {}ms，文本长度: {}", duration, text.length());
                return text;
                
            }
        } catch (Exception e) {
            log.warn("简化PDFBox提取失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 配置PDFBox以避免字体相关错误（简化版）
     */
    private void configurePDFBoxForSafeExtraction() {
        PDFBoxConfigManager.configureBasicPDFBox();
    }
    
    
    /**
     * 基础PDF解析器（适合简单PDF文档）
     */
    private String extractWithBasicPdfParser(byte[] pdfData) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 将PDF转换为字符串进行简单解析
            String pdfString = new String(pdfData, "ISO-8859-1");
            StringBuilder extractedText = new StringBuilder();
            
            // 查找PDF中的文本对象
            Pattern streamPattern = Pattern.compile("stream\\s*\\n(.*?)\\nendstream", 
                Pattern.DOTALL | Pattern.MULTILINE);
            Matcher streamMatcher = streamPattern.matcher(pdfString);
            
            while (streamMatcher.find()) {
                String streamContent = streamMatcher.group(1);
                
                // 提取文本内容（简单方法）
                String text = extractTextFromStream(streamContent);
                if (text != null && !text.trim().isEmpty()) {
                    extractedText.append(text).append(" ");
                }
            }
            
            // 直接搜索可见文本（备用方法）
            if (extractedText.length() == 0) {
                extractedText.append(extractVisibleText(pdfString));
            }
            
            String result = extractedText.toString().trim();
            long duration = System.currentTimeMillis() - startTime;
            
            if (!result.isEmpty()) {
                log.info("基础PDF解析成功，耗时: {}ms，文本长度: {}", duration, result.length());
                return result;
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("基础PDF解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从PDF流中提取文本
     */
    private String extractTextFromStream(String streamContent) {
        try {
            StringBuilder text = new StringBuilder();
            
            // 查找文本显示命令 (Tj, TJ)
            Pattern textPattern = Pattern.compile("\\((.*?)\\)\\s*Tj|\\[(.*?)\\]\\s*TJ");
            Matcher textMatcher = textPattern.matcher(streamContent);
            
            while (textMatcher.find()) {
                String textContent = textMatcher.group(1);
                if (textContent == null) {
                    textContent = textMatcher.group(2);
                }
                
                if (textContent != null) {
                    // 简单的文本清理
                    textContent = textContent.replaceAll("\\\\[0-9]{3}", " ")
                                           .replaceAll("\\\\.", "")
                                           .trim();
                    if (!textContent.isEmpty()) {
                        text.append(textContent).append(" ");
                    }
                }
            }
            
            return text.toString().trim();
            
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 提取PDF中的可见文本（最简单方法）
     */
    private String extractVisibleText(String pdfContent) {
        try {
            StringBuilder visibleText = new StringBuilder();
            
            // 查找常见的中文和英文字符
            Pattern visiblePattern = Pattern.compile("[\\u4e00-\\u9fff\\w\\s.,;:!?()\\-]+");
            Matcher visibleMatcher = visiblePattern.matcher(pdfContent);
            
            while (visibleMatcher.find()) {
                String match = visibleMatcher.group().trim();
                if (match.length() > 2) { // 过滤太短的匹配
                    visibleText.append(match).append(" ");
                }
            }
            
            return visibleText.toString().trim();
            
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 使用AI服务解析合同（按优先级降级）
     * 
     * @param textContent 合同文本内容
     * @return 解析结果（包含_aiParsed和_parseMessage标记）
     */
    private Map<String, Object> parseContractWithAIServices(String textContent) {
        // 根据配置选择AI服务
        log.info("AI服务提供商配置: {}", aiProvider);
        
        long totalStartTime = System.currentTimeMillis();
        
        // 1. 尝试使用配置的主要AI服务
        if ("deepseek".equalsIgnoreCase(aiProvider) && deepSeekAiService != null && deepSeekAiService.isAvailable()) {
            try {
                Map<String, Object> extractedInfo = deepSeekAiService.parseContractWithAI(textContent);
                extractedInfo.put("_aiParsed", true);
                extractedInfo.put("_parseMessage", "使用DeepSeek AI解析成功");
                log.info("使用DeepSeek AI成功解析合同");
                return extractedInfo;
            } catch (Exception e) {
                log.warn("DeepSeek AI解析失败，尝试降级: {}", e.getMessage());
            }
        }
        
        if ("gemini".equalsIgnoreCase(aiProvider) && geminiAiService != null && geminiAiService.isAvailable()) {
            try {
                Map<String, Object> extractedInfo = geminiAiService.parseContractWithAITimed(textContent);
                extractedInfo.put("_aiParsed", true);
                extractedInfo.put("_parseMessage", "使用Gemini AI解析成功");
                log.info("使用Gemini AI成功解析合同");
                return extractedInfo;
            } catch (Exception e) {
                log.warn("Gemini AI解析失败，尝试降级: {}", e.getMessage());
            }
        }
        
        if ("gemma3".equalsIgnoreCase(aiProvider) && gemma3AiService != null && gemma3AiService.isAvailable()) {
            try {
                Map<String, Object> extractedInfo = gemma3AiService.parseContractWithAITimed(textContent);
                extractedInfo.put("_aiParsed", true);
                extractedInfo.put("_parseMessage", "使用Gemma3 AI解析成功");
                log.info("使用Gemma3 AI成功解析合同");
                return extractedInfo;
            } catch (Exception e) {
                log.warn("Gemma3 AI解析失败，尝试降级: {}", e.getMessage());
            }
        }
        
        // 2. 如果主要服务失败，尝试备用AI服务（DeepSeek优先）
        if (!"deepseek".equalsIgnoreCase(aiProvider) && deepSeekAiService != null && deepSeekAiService.isAvailable()) {
            try {
                Map<String, Object> extractedInfo = deepSeekAiService.parseContractWithAI(textContent);
                extractedInfo.put("_aiParsed", true);
                extractedInfo.put("_parseMessage", "主要AI服务失败，使用DeepSeek AI解析成功");
                log.info("降级使用DeepSeek AI成功解析合同");
                return extractedInfo;
            } catch (Exception e) {
                log.warn("备用DeepSeek AI解析失败: {}", e.getMessage());
            }
        }
        
        if (!"gemini".equalsIgnoreCase(aiProvider) && geminiAiService != null && geminiAiService.isAvailable()) {
            try {
                Map<String, Object> extractedInfo = geminiAiService.parseContractWithAITimed(textContent);
                extractedInfo.put("_aiParsed", true);
                extractedInfo.put("_parseMessage", "主要AI服务失败，使用Gemini AI解析成功");
                log.info("降级使用Gemini AI成功解析合同");
                return extractedInfo;
            } catch (Exception e) {
                log.warn("备用Gemini AI解析失败: {}", e.getMessage());
            }
        }
        
        if (!"gemma3".equalsIgnoreCase(aiProvider) && gemma3AiService != null && gemma3AiService.isAvailable()) {
            try {
                Map<String, Object> extractedInfo = gemma3AiService.parseContractWithAITimed(textContent);
                extractedInfo.put("_aiParsed", true);
                extractedInfo.put("_parseMessage", "主要AI服务失败，使用Gemma3 AI解析成功");
                log.info("降级使用Gemma3 AI成功解析合同");
                return extractedInfo;
            } catch (Exception e) {
                log.warn("备用Gemma3 AI解析失败: {}", e.getMessage());
            }
        }
        
        // 3. 最终降级到关键字匹配
        long totalEndTime = System.currentTimeMillis();
        long totalDuration = totalEndTime - totalStartTime;
        log.info("所有AI服务不可用或解析失败，总尝试耗时: {}ms ({}秒)，使用关键字匹配解析", 
                totalDuration, totalDuration / 1000.0);
        return extractFinanceInfoByKeywords(textContent);
    }

    /**
     * 使用关键字匹配提取财务信息（最终降级方案）
     */
    private Map<String, Object> extractFinanceInfoByKeywords(String textContent) {
        Map<String, Object> extractedInfo = new HashMap<>();
        
        // 合同编号
        extractedInfo.put("contractNo", extractByPattern(textContent, 
            "(?:合同编号|合同号|编号)[:：]?\\s*([A-Za-z0-9\\-_]+)", 1));
        
        // 合同名称
        extractedInfo.put("contractName", extractByPattern(textContent, 
            "(?:合同名称|项目名称|标题)[:：]?\\s*([^\\n\\r]{1,100})", 1));
        
        // 甲方
        extractedInfo.put("partyA", extractByPattern(textContent, 
            "甲方[:：]?\\s*([^\\n\\r]{1,100})", 1));
        
        // 乙方
        extractedInfo.put("partyB", extractByPattern(textContent, 
            "乙方[:：]?\\s*([^\\n\\r]{1,100})", 1));
        
        // 合同金额
        extractedInfo.put("contractAmount", extractByPattern(textContent, 
            "(?:合同金额|总金额|金额)[:：]?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", 1));
        
        // 币种
        extractedInfo.put("currency", extractByPattern(textContent, 
            "(?:币种|货币)[:：]?\\s*(SGD|USD|CNY|人民币|新币|美元)", 1));
        
        // 合同开始日期
        extractedInfo.put("startDate", extractByPattern(textContent, 
            "(?:开始日期|生效日期|起始日期)[:：]?\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)", 1));
        
        // 合同结束日期
        extractedInfo.put("endDate", extractByPattern(textContent, 
            "(?:结束日期|到期日期|终止日期)[:：]?\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)", 1));
        
        log.info("关键字匹配提取的信息: {}", extractedInfo);
        return extractedInfo;
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
     * 上传合同文件（Controller适配方法）
     */
    public ContractUploadResponse uploadContract(MultipartFile file, String createdBy) {
        return uploadAndParseContract(file, createdBy);
    }

    /**
     * 查询合同列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<Contract> getContracts(Pageable pageable) {
        return contractRepository.findByIsDeletedFalse(pageable);
    }

    /**
     * 多条件查询合同列表（分页）
     * 
     * @param contractNo 合同编号（支持模糊查询）
     * @param contractAlias 合同别名（支持模糊查询）
     * @param isFinished 是否完成
     * @param isDeleted 是否删除
     * @param pageable 分页参数
     * @return 合同列表
     */
    @Transactional(readOnly = true)
    public Page<Contract> getContractsByConditions(String contractNo, String contractAlias, 
                                                  Boolean isFinished, Boolean isDeleted, 
                                                  Pageable pageable) {
        log.info("多条件查询合同: contractNo={}, contractAlias={}, isFinished={}, isDeleted={}", 
                contractNo, contractAlias, isFinished, isDeleted);
        
        return contractRepository.findByMultipleConditions(
            contractNo, contractAlias, isFinished, isDeleted, pageable);
    }

    /**
     * 下载合同原件
     */
    @Transactional(readOnly = true)
    public String downloadOriginal(Long contractId) {
        contractRepository.findByIdAndNotDeleted(contractId)
                .orElseThrow(() -> new BusinessException("合同不存在或已删除"));
        
        // 这里应该返回文件下载逻辑，暂时返回提示信息
        return "合同原件下载功能，合同ID: " + contractId;
    }

    /**
     * 预览原始合同接口
     * 根据file_id查询原始合同文件数据
     * 
     * @param fileId 文件ID
     * @return 原始合同预览响应
     */
    @Transactional(readOnly = true)
    public OriginalContractPreviewResponse previewOriginalContract(Long fileId) {
        log.info("预览原始合同: fileId={}", fileId);
        
        // 查询原始合同文件
        OriginalContract originalContract = originalContractRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在，fileId: " + fileId));
        
        // 检查文件是否已删除
        if (originalContract.getIsDeleted()) {
            throw new RuntimeException("文件已删除，无法预览");
        }
        
        // 构建响应对象
        return OriginalContractPreviewResponse.builder()
                .fileName(originalContract.getFileName())
                .fileType(originalContract.getFileType())
                .fileData(originalContract.getFileData())
                .fileSize((long) originalContract.getFileData().length)
                .createdDate(originalContract.getCreatedDate().toString())
                .createdBy(originalContract.getCreatedBy())
                .build();
    }

    /**
     * 删除合同（Controller适配方法）
     */
    public void deleteContract(Long contractId) {
        deleteContract(contractId, "system");
    }

    /**
     * 查询合同列表（带关联状态信息）
     */
    @Transactional(readOnly = true)
    public Page<ContractWithStatusResponse> getContractsWithStatus(Pageable pageable) {
        Page<Contract> contracts = contractRepository.findByIsDeletedFalse(pageable);
        List<ContractWithStatusResponse> contractsWithStatus = new ArrayList<>();
        
        for (Contract contract : contracts.getContent()) {
            ContractWithStatusResponse contractWithStatus = new ContractWithStatusResponse();
            BeanUtils.copyProperties(contract, contractWithStatus);
            
            // 设置关联状态标志位
            setRelatedStatus(contractWithStatus, contract.getId());
            
            contractsWithStatus.add(contractWithStatus);
        }
        
        return new PageImpl<>(contractsWithStatus, pageable, contracts.getTotalElements());
    }

    /**
     * 多条件查询合同列表（带关联状态信息）
     */
    @Transactional(readOnly = true)
    public Page<ContractWithStatusResponse> getContractsByConditionsWithStatus(String contractNo, String contractAlias, 
                                                                              Boolean isFinished, Boolean isDeleted, 
                                                                              Pageable pageable) {
        Page<Contract> contracts = contractRepository.findByMultipleConditions(
            contractNo, contractAlias, isFinished, isDeleted, pageable);
        List<ContractWithStatusResponse> contractsWithStatus = new ArrayList<>();
        
        for (Contract contract : contracts.getContent()) {
            ContractWithStatusResponse contractWithStatus = new ContractWithStatusResponse();
            BeanUtils.copyProperties(contract, contractWithStatus);
            
            // 设置关联状态标志位
            setRelatedStatus(contractWithStatus, contract.getId());
            
            contractsWithStatus.add(contractWithStatus);
        }
        
        return new PageImpl<>(contractsWithStatus, pageable, contracts.getTotalElements());
    }

    /**
     * 设置合同的关联状态标志位
     */
    private void setRelatedStatus(ContractWithStatusResponse contractWithStatus, Long contractId) {
        // 查询合同详细信息状态
        contractDetailsRepository.findByContractId(contractId).ifPresent(details -> {
            contractWithStatus.setHasBaseInfo(isJsonFieldNotEmpty(details.getBaseInfo()));
            contractWithStatus.setHasFinanceInfo(isJsonFieldNotEmpty(details.getFinanceInfo()));
            contractWithStatus.setHasTimeInfo(isJsonFieldNotEmpty(details.getTimeInfo()));
            contractWithStatus.setHasSettlementInfo(isJsonFieldNotEmpty(details.getSettlementInfo()));
            contractWithStatus.setHasFeeInfo(isJsonFieldNotEmpty(details.getFeeInfo()));
            contractWithStatus.setHasTaxInfo(isJsonFieldNotEmpty(details.getTaxInfo()));
            contractWithStatus.setHasRiskInfo(isJsonFieldNotEmpty(details.getRiskInfo()));
        });
        
        // 查询预提/待摊时间表状态
        boolean hasAmortizationSchedule = amortizationScheduleRepository
            .existsByContractIdAndIsDeletedFalse(contractId);
        contractWithStatus.setHasAmortizationSchedule(hasAmortizationSchedule);
        
        // 查询支付时间表状态
        boolean hasPaymentSchedule = paymentScheduleRepository
            .existsByContractIdAndIsDeletedFalse(contractId);
        contractWithStatus.setHasPaymentSchedule(hasPaymentSchedule);
        
        // 查询会计分录状态
        boolean hasAccountingEntry = accountingEntryRepository
            .existsByContractId(contractId);
        contractWithStatus.setHasAccountingEntry(hasAccountingEntry);
    }

    /**
     * 检查JSONB字段是否不为空
     */
    private Boolean isJsonFieldNotEmpty(Object jsonField) {
        if (jsonField == null) {
            return false;
        }
        
        // 如果是字符串类型的JSON
        if (jsonField instanceof String) {
            String jsonStr = (String) jsonField;
            return !jsonStr.trim().isEmpty() && 
                   !jsonStr.trim().equals("null") && 
                   !jsonStr.trim().equals("{}") &&
                   !jsonStr.trim().equals("[]");
        }
        
        // 如果是Map类型
        if (jsonField instanceof Map) {
            Map<?, ?> jsonMap = (Map<?, ?>) jsonField;
            return !jsonMap.isEmpty();
        }
        
        // 其他类型，只要不为null就认为有值
        return true;
    }
}
