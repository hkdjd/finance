package com.ocbc.finance.service;

import com.ocbc.finance.dto.*;
import com.ocbc.finance.dto.CreateOperationLogRequest;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.Payment;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.PaymentRepository;
import com.ocbc.finance.service.calculation.AmortizationCalculationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;
    private final PaymentRepository paymentRepository;
    private final AmortizationCalculationService calculationService;
    private final FileUploadService fileUploadService;
    private final ExternalContractParseService externalContractParseService;
    private final CustomKeywordService customKeywordService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContractService(ContractRepository contractRepository,
                           AmortizationEntryRepository amortizationEntryRepository,
                           PaymentRepository paymentRepository,
                           AmortizationCalculationService calculationService,
                           FileUploadService fileUploadService,
                           ExternalContractParseService externalContractParseService,
                           CustomKeywordService customKeywordService,
                           OperationLogService operationLogService) {
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
        this.paymentRepository = paymentRepository;
        this.calculationService = calculationService;
        this.fileUploadService = fileUploadService;
        this.externalContractParseService = externalContractParseService;
        this.customKeywordService = customKeywordService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public AmortizationResponse updateAmortizationEntry(Long entryId, BigDecimal amount) {
        AmortizationEntry entry = amortizationEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("未找到摊销明细，ID=" + entryId));
        entry.setAmount(amount);
        amortizationEntryRepository.save(entry);
        return buildResponseForContract(entry.getContract().getId());
    }


    @Transactional
    public AmortizationResponse createContractAndInitialize(CalculateAmortizationRequest req) {
        // 保存合同行
        YearMonth startYm = parseYm(req.getStartDate());
        YearMonth endYm = parseYm(req.getEndDate());
        Contract contract = new Contract();
        contract.setTotalAmount(req.getTotalAmount());
        contract.setVendorName(req.getVendorName());
        contract.setTaxRate(req.getTaxRate());
        contract.setStartDate(startYm.atDay(1));
        contract.setEndDate(endYm.atEndOfMonth());
        contract.setOriginalFileName(req.getAttachmentName());
        
        //保存自定义字段
        Object customFieldsObj = req.getCustomFields();
        String customFieldsJson = null;
        if (customFieldsObj != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                customFieldsJson = objectMapper.writeValueAsString(customFieldsObj);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize customFields to JSON", e);
                customFieldsJson = "{}";
            }
        }
        contract.setCustomFieldsJson(customFieldsJson);
        
        contract = contractRepository.save(contract);

        // 计算摊销表并落库
        AmortizationResponse calc = calculationService.calculate(req);
        for (AmortizationEntryDto e : calc.getEntries()) {
            AmortizationEntry entry = new AmortizationEntry();
            entry.setContract(contract);
            entry.setAmortizationPeriod(e.getAmortizationPeriod());
            entry.setAccountingPeriod(e.getAccountingPeriod());
            entry.setAmount(e.getAmount());
            entry.setPeriodDate(LocalDate.parse(e.getAmortizationPeriod() + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            amortizationEntryRepository.save(entry);
        }

        // 创建操作日志：合同生成
        try {
            String desc = String.format("合同生成：供应商=%s，总金额=¥%s，期间=%s 至 %s",
                    contract.getVendorName(),
                    contract.getTotalAmount() != null ? contract.getTotalAmount().toPlainString() : "0",
                    req.getStartDate(),
                    req.getEndDate());
            CreateOperationLogRequest logReq = new CreateOperationLogRequest(
                    contract.getId(),
                    "生成",
                    desc,
                    req.getOperatorId() != null ? req.getOperatorId() : "system",
                    LocalDateTime.now()
            );
            operationLogService.createOperationLog(logReq);
        } catch (Exception ex) {
            // 保底不中断主流程
            System.out.println("记录合同生成操作日志失败: " + ex.getMessage());
        }
        return buildResponseForContract(contract.getId());
    }

    public AmortizationResponse getContractAmortization(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        return buildResponseForContract(contract.getId());
    }

    private AmortizationResponse buildResponseForContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        List<AmortizationEntry> entries = amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(contractId);
        
        // 获取该合同的所有已确认付款记录
        List<Payment> payments = paymentRepository.findByContractIdAndStatusOrderByBookingDateDesc(contractId, Payment.PaymentStatus.CONFIRMED);
        
        // 构建已付款期间的集合
        Set<String> paidPeriods = new HashSet<>();
        for (Payment payment : payments) {
            if (payment.getSelectedPeriods() != null && !payment.getSelectedPeriods().isEmpty()) {
                String[] periods = payment.getSelectedPeriods().split(",");
                for (String period : periods) {
                    paidPeriods.add(period.trim());
                }
            }
        }
        
        List<AmortizationEntryDto> dtoList = entries.stream()
                .sorted(Comparator.comparing(AmortizationEntry::getPeriodDate))
                .map(e -> {
                    String status = e.getPaymentStatus() != null ? e.getPaymentStatus().name() : "PENDING";
                    return new AmortizationEntryDto(
                            e.getId(),
                            e.getAmortizationPeriod(),
                            e.getAccountingPeriod(),
                            e.getAmount(),
                            status
                    );
                })
                .collect(Collectors.toList());
        return AmortizationResponse.builder()
                .contractId(contract.getId())
                .totalAmount(contract.getTotalAmount())
                .startDate(contract.getStartDate().getYear() + "-" + String.format("%02d", contract.getStartDate().getMonthValue()))
                .endDate(contract.getEndDate().getYear() + "-" + String.format("%02d", contract.getEndDate().getMonthValue()))
                .vendorName(contract.getVendorName())
                .taxRate(contract.getTaxRate())
                .scenario(null)
                .generatedAt(java.time.OffsetDateTime.now())
                .entries(dtoList)
                .build();
    }

    private YearMonth parseYm(String s) {
        String v = s.trim();
        if (v.matches("\\d{4}-\\d{2}")) {
            return YearMonth.parse(v);
        } else if (v.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate ld = LocalDate.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return YearMonth.of(ld.getYear(), ld.getMonth());
        }
        throw new IllegalArgumentException("日期格式应为 yyyy-MM 或 yyyy-MM-dd: " + s);
    }

    /**
     * 仅解析合同文件，不保存到数据库
     * @param file 上传的合同文件
     * @param userId 用户ID（用于获取自定义关键字）
     * @return 合同解析响应（不包含contractId）
     */
    public ContractUploadResponse parseContractOnly(MultipartFile file, Long userId) {
        try {
            // 1. 上传文件到指定目录
            String savedFileName = fileUploadService.uploadContractFile(file);
            
            // 2. 获取用户的自定义关键字
            List<String> customKeywords = null;
            if (userId != null) {
                customKeywords = customKeywordService.getKeywordStringsByUserId(userId);
            }
            
            // 3. 调用AI模块解析合同（传递自定义关键字）
            File contractFile = fileUploadService.getContractFile(savedFileName);
            ExternalContractParseResponse parseResponse = externalContractParseService.parseContract(contractFile, customKeywords);
            
            // 4. 构建响应（不保存到数据库）
            ContractUploadResponse response = new ContractUploadResponse();
            response.setContractId(null); // 没有contractId，因为还未保存
            response.setTotalAmount(parseResponse.getTotalAmount() != null ? parseResponse.getTotalAmount() : BigDecimal.ZERO);
            response.setStartDate(parseResponse.getStartDate() != null && !parseResponse.getStartDate().trim().isEmpty() 
                    ? parseResponse.getStartDate() : LocalDate.now().toString());
            response.setEndDate(parseResponse.getEndDate() != null && !parseResponse.getEndDate().trim().isEmpty() 
                    ? parseResponse.getEndDate() : LocalDate.now().plusMonths(12).toString());
            response.setVendorName(parseResponse.getVendorName() != null ? parseResponse.getVendorName() : "未知供应商");
            response.setTaxRate(parseResponse.getTaxRate() != null ? parseResponse.getTaxRate() : BigDecimal.ZERO);
            response.setAttachmentName(file.getOriginalFilename());
            
            // 设置attachmentPath为可访问的URL（用于PDF预览）
            String tempFileUrl = "http://localhost:8081/contracts/temp/" + savedFileName;
            response.setAttachmentPath(tempFileUrl);
            
            // 如果有自定义字段结果，也返回给前端
            if (parseResponse.getCustomFields() != null && !parseResponse.getCustomFields().isEmpty()) {
                response.setCustomFields(parseResponse.getCustomFields());
            }
            
            response.setCreatedAt(java.time.OffsetDateTime.now());
            response.setMessage(parseResponse.isSuccess() ? "合同解析成功" : "合同解析失败：" + parseResponse.getErrorMessage());
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("合同解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传合同文件并解析
     * @param file 上传的合同文件
     * @param userId 用户ID（用于获取自定义关键字）
     * @param operatorId 操作人ID，用于操作日志记录
     * @return 合同上传响应
     */
    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file, Long userId, String operatorId) {
        try {
            // 1.1 上传文件到指定目录
            String savedFileName = fileUploadService.uploadContractFile(file);
            
            // 1.2 获取用户的自定义关键字
            List<String> customKeywords = null;
            if (userId != null) {
                customKeywords = customKeywordService.getKeywordStringsByUserId(userId);
            }
            
            // 1.3 调用AI模块解析合同（传递自定义关键字）
            File contractFile = fileUploadService.getContractFile(savedFileName);
            ExternalContractParseResponse parseResponse = externalContractParseService.parseContract(contractFile, customKeywords);
            
            // 1.4 保存合同信息到数据库（使用AI解析结果）
            Contract contract = new Contract();
            
            // 使用AI解析结果
            if (parseResponse.isSuccess()) {
                contract.setTotalAmount(parseResponse.getTotalAmount() != null ? parseResponse.getTotalAmount() : BigDecimal.ZERO);
                
                // 安全解析日期，如果为null则使用默认值
                if (parseResponse.getStartDate() != null && !parseResponse.getStartDate().trim().isEmpty()) {
                    try {
                        contract.setStartDate(LocalDate.parse(parseResponse.getStartDate()));
                    } catch (Exception e) {
                        contract.setStartDate(LocalDate.now());
                    }
                } else {
                    contract.setStartDate(LocalDate.now());
                }
                
                if (parseResponse.getEndDate() != null && !parseResponse.getEndDate().trim().isEmpty()) {
                    try {
                        contract.setEndDate(LocalDate.parse(parseResponse.getEndDate()));
                    } catch (Exception e) {
                        contract.setEndDate(LocalDate.now().plusMonths(12));
                    }
                } else {
                    contract.setEndDate(LocalDate.now().plusMonths(12));
                }
                
                contract.setVendorName(parseResponse.getVendorName() != null ? parseResponse.getVendorName() : "未知供应商");
                contract.setTaxRate(parseResponse.getTaxRate() != null ? parseResponse.getTaxRate() : BigDecimal.ZERO);
            } else {
                // 解析失败，使用默认值
                contract.setTotalAmount(BigDecimal.ZERO);
                contract.setStartDate(LocalDate.now());
                contract.setEndDate(LocalDate.now().plusMonths(12));
                contract.setVendorName("未知供应商");
                contract.setTaxRate(BigDecimal.ZERO);
            }
            
            // 在数据库中保存原始文件名，便于显示
            contract.setOriginalFileName(file.getOriginalFilename());
            
            // 设置文件路径（完整路径）
            String fullFilePath = fileUploadService.getContractFile(savedFileName).getAbsolutePath();
            contract.setFilePath(fullFilePath);
            
            // 保存自定义字段到数据库
            if (parseResponse.getCustomFields() != null && !parseResponse.getCustomFields().isEmpty()) {
                contract.setCustomFieldsJson(mapToJson(parseResponse.getCustomFields()));
            }
            
            contract = contractRepository.save(contract);
            
            // 创建操作日志：合同上传
            try {
                String desc = String.format("合同上传：供应商=%s，总金额=¥%s，期间=%s 至 %s，附件=%s",
                        contract.getVendorName(),
                        contract.getTotalAmount() != null ? contract.getTotalAmount().toPlainString() : "0",
                        contract.getStartDate(),
                        contract.getEndDate(),
                        file.getOriginalFilename());
                CreateOperationLogRequest logReq = new CreateOperationLogRequest(
                        contract.getId(),
                        "上传",
                        desc,
                        operatorId != null ? operatorId : "system",
                        LocalDateTime.now()
                );
                operationLogService.createOperationLog(logReq);
            } catch (Exception ex) {
                // 保底不中断主流程
                System.out.println("记录合同上传操作日志失败: " + ex.getMessage());
            }
            
            // 构建响应
            ContractUploadResponse response = new ContractUploadResponse();
            response.setContractId(contract.getId());
            response.setTotalAmount(contract.getTotalAmount());
            response.setStartDate(contract.getStartDate().toString());
            response.setEndDate(contract.getEndDate().toString());
            response.setVendorName(contract.getVendorName());
            response.setTaxRate(contract.getTaxRate());
            response.setAttachmentName(file.getOriginalFilename());
            // 设置attachmentPath为完整的下载URL
            String downloadUrl = "http://localhost:8081/contracts/" + contract.getId() + "/attachment?download=true";
            response.setAttachmentPath(downloadUrl);
            
            // 如果有自定义字段结果，也返回给前端
            if (parseResponse.getCustomFields() != null && !parseResponse.getCustomFields().isEmpty()) {
                response.setCustomFields(parseResponse.getCustomFields());
            }
            
            response.setCreatedAt(java.time.OffsetDateTime.now());
            response.setMessage(parseResponse.isSuccess() ? "合同上传和解析成功" : "合同上传成功，但解析失败：" + parseResponse.getErrorMessage());
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("合同上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 编辑合同信息
     */
    @Transactional
    public ContractUploadResponse editContract(Long contractId, ContractEditRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        
        // 更新合同信息
        contract.setTotalAmount(request.getTotalAmount());
        contract.setStartDate(LocalDate.parse(request.getStartDate()));
        contract.setEndDate(LocalDate.parse(request.getEndDate()));
        contract.setVendorName(request.getVendorName());
        contract.setTaxRate(request.getTaxRate());
        
        // 更新自定义字段
        if (request.getCustomFields() != null) {
            contract.setCustomFieldsJson(mapToJson(request.getCustomFields()));
        }
        
        contract = contractRepository.save(contract);
        
        // 创建操作日志：合同编辑
        try {
            String desc = String.format("合同编辑：供应商=%s，总金额=¥%s，期间=%s 至 %s",
                    contract.getVendorName(),
                    contract.getTotalAmount() != null ? contract.getTotalAmount().toPlainString() : "0",
                    contract.getStartDate(),
                    contract.getEndDate());
            CreateOperationLogRequest logReq = new CreateOperationLogRequest(
                    contract.getId(),
                    "编辑",
                    desc,
                    "system",
                    LocalDateTime.now()
            );
            operationLogService.createOperationLog(logReq);
        } catch (Exception ex) {
            // 保底不中断主流程
            System.out.println("记录合同编辑操作日志失败: " + ex.getMessage());
        }
        
        // 构建响应
        ContractUploadResponse response = new ContractUploadResponse();
        response.setContractId(contract.getId());
        response.setTotalAmount(contract.getTotalAmount());
        response.setStartDate(contract.getStartDate().toString());
        response.setEndDate(contract.getEndDate().toString());
        response.setVendorName(contract.getVendorName());
        response.setTaxRate(contract.getTaxRate());
        response.setAttachmentName(contract.getOriginalFileName());
        response.setAttachmentPath(contract.getFilePath());
        response.setCreatedAt(java.time.OffsetDateTime.now());
        response.setMessage("合同信息更新成功");
        
        // 返回自定义字段
        if (contract.getCustomFieldsJson() != null) {
            response.setCustomFields(jsonToMap(contract.getCustomFieldsJson()));
        }
        
        return response;
    }

    /**
     * 查询合同信息
     */
    public ContractUploadResponse getContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        
        ContractUploadResponse response = new ContractUploadResponse();
        response.setContractId(contract.getId());
        response.setTotalAmount(contract.getTotalAmount());
        response.setStartDate(contract.getStartDate().toString());
        response.setEndDate(contract.getEndDate().toString());
        response.setVendorName(contract.getVendorName());
        response.setTaxRate(contract.getTaxRate());
        response.setAttachmentName(contract.getOriginalFileName());
        response.setAttachmentPath(contract.getFilePath());
        response.setCreatedAt(java.time.OffsetDateTime.now());
        response.setMessage("查询成功");
        
        // 读取并返回自定义字段
        if (contract.getCustomFieldsJson() != null) {
            response.setCustomFields(jsonToMap(contract.getCustomFieldsJson()));
        }
        
        return response;
    }

    /**
     * 查询所有合同列表
     */
    public ContractListResponse getAllContracts() {
        List<Contract> contracts = contractRepository.findAllByOrderByCreatedAtDesc();
        
        List<ContractListResponse.ContractSummary> contractSummaries = contracts.stream()
                .map(this::convertToContractSummary)
                .collect(Collectors.toList());
        
        ContractListResponse response = new ContractListResponse();
        response.setContracts(contractSummaries);
        response.setTotalCount(contractSummaries.size());
        response.setMessage("查询成功");
        
        return response;
    }

    /**
     * 分页查询合同列表
     */
    public ContractListResponse getContractsPaged(int page, int size) {
        // 这里可以使用Spring Data的分页功能
        // 为简化实现，暂时返回所有数据
        return getAllContracts();
    }

    /**
     * 转换合同实体为摘要信息
     */
    private ContractListResponse.ContractSummary convertToContractSummary(Contract contract) {
        ContractListResponse.ContractSummary summary = new ContractListResponse.ContractSummary();
        // 确保合同ID正确设置
        summary.setContractId(contract.getId());
        summary.setTotalAmount(contract.getTotalAmount());
        summary.setStartDate(contract.getStartDate().toString());
        summary.setEndDate(contract.getEndDate().toString());
        summary.setVendorName(contract.getVendorName());
        summary.setAttachmentName(contract.getOriginalFileName()); // 添加附件名称
        // 使用实际的创建时间，转换为OffsetDateTime
        if (contract.getCreatedAt() != null) {
            summary.setCreatedAt(contract.getCreatedAt().atOffset(java.time.ZoneOffset.of("+08:00")));
        } else {
            summary.setCreatedAt(java.time.OffsetDateTime.now());
        }
        summary.setStatus("ACTIVE");
        
        // 调试日志
        System.out.println("转换合同摘要 - ID: " + contract.getId() + ", 供应商: " + contract.getVendorName());
        
        return summary;
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("转换自定义字段为JSON失败", e);
        }
    }
    
    /**
     * 将JSON字符串转换为Map
     */
    private Map<String, String> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析自定义字段JSON失败", e);
        }
    }

    /**
     * 获取临时上传文件的Resource
     * @param fileName 文件名
     * @return Resource
     */
    public org.springframework.core.io.Resource getTempFileResource(String fileName) {
        try {
            File file = fileUploadService.getContractFile(fileName);
            return new org.springframework.core.io.FileSystemResource(file);
        } catch (Exception e) {
            throw new RuntimeException("获取临时文件失败: " + e.getMessage(), e);
        }
    }
}
