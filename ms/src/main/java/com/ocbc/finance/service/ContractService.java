package com.ocbc.finance.service;

import com.ocbc.finance.dto.*;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.Payment;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.PaymentRepository;
import com.ocbc.finance.service.calculation.AmortizationCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;
    private final PaymentRepository paymentRepository;
    private final AmortizationCalculationService calculationService;
    private final FileUploadService fileUploadService;
    private final ExternalContractParseService externalContractParseService;
    private final CustomKeywordService customKeywordService;

    public ContractService(ContractRepository contractRepository,
                           AmortizationEntryRepository amortizationEntryRepository,
                           PaymentRepository paymentRepository,
                           AmortizationCalculationService calculationService,
                           FileUploadService fileUploadService,
                           ExternalContractParseService externalContractParseService,
                           CustomKeywordService customKeywordService) {
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
        this.paymentRepository = paymentRepository;
        this.calculationService = calculationService;
        this.fileUploadService = fileUploadService;
        this.externalContractParseService = externalContractParseService;
        this.customKeywordService = customKeywordService;
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
                .totalAmount(contract.getTotalAmount())
                .startDate(contract.getStartDate().getYear() + "-" + String.format("%02d", contract.getStartDate().getMonthValue()))
                .endDate(contract.getEndDate().getYear() + "-" + String.format("%02d", contract.getEndDate().getMonthValue()))
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
     * 上传合同文件并解析
     * @param file 上传的合同文件
     * @param userId 用户ID（用于获取自定义关键字）
     * @return 合同上传响应
     */
    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file, Long userId) {
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
                contract.setTotalAmount(parseResponse.getTotalAmount());
                contract.setStartDate(LocalDate.parse(parseResponse.getStartDate()));
                contract.setEndDate(LocalDate.parse(parseResponse.getEndDate()));
                contract.setVendorName(parseResponse.getVendorName());
                contract.setTaxRate(parseResponse.getTaxRate());
            } else {
                // 解析失败，使用默认值
                contract.setTotalAmount(BigDecimal.ZERO);
                contract.setStartDate(LocalDate.now());
                contract.setEndDate(LocalDate.now().plusMonths(1));
                contract.setVendorName("未知供应商");
                contract.setTaxRate(BigDecimal.ZERO);
            }
            
            // 在数据库中保存原始文件名，便于显示
            contract.setAttachmentName(file.getOriginalFilename());
            
            contract = contractRepository.save(contract);
            
            // 构建响应
            ContractUploadResponse response = new ContractUploadResponse();
            response.setContractId(contract.getId());
            response.setTotalAmount(contract.getTotalAmount());
            response.setStartDate(contract.getStartDate().toString());
            response.setEndDate(contract.getEndDate().toString());
            response.setVendorName(contract.getVendorName());
            response.setTaxRate(contract.getTaxRate());
            response.setAttachmentName(file.getOriginalFilename());
            
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
        
        contract = contractRepository.save(contract);
        
        // 构建响应
        ContractUploadResponse response = new ContractUploadResponse();
        response.setContractId(contract.getId());
        response.setTotalAmount(contract.getTotalAmount());
        response.setStartDate(contract.getStartDate().toString());
        response.setEndDate(contract.getEndDate().toString());
        response.setVendorName(contract.getVendorName());
        response.setTaxRate(contract.getTaxRate());
        response.setAttachmentName(contract.getAttachmentName());
        response.setAttachmentPath(contract.getFilePath());
        response.setCreatedAt(java.time.OffsetDateTime.now());
        response.setMessage("合同信息更新成功");
        
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
        response.setAttachmentName(contract.getAttachmentName());
        response.setAttachmentPath(contract.getFilePath());
        response.setCreatedAt(java.time.OffsetDateTime.now());
        response.setMessage("查询成功");
        
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
        summary.setAttachmentName(contract.getAttachmentName());
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
}
