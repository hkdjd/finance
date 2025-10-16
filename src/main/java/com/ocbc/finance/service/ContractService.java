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
    // TODO: 待外部解析接口就绪后重新启用
    // private final ExternalContractParseService externalContractParseService;

    public ContractService(ContractRepository contractRepository,
                           AmortizationEntryRepository amortizationEntryRepository,
                           PaymentRepository paymentRepository,
                           AmortizationCalculationService calculationService,
                           FileUploadService fileUploadService) {
                           // TODO: 待外部解析接口就绪后重新添加参数
                           // ExternalContractParseService externalContractParseService) {
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
        this.paymentRepository = paymentRepository;
        this.calculationService = calculationService;
        this.fileUploadService = fileUploadService;
        // TODO: 待外部解析接口就绪后重新启用
        // this.externalContractParseService = externalContractParseService;
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
                    String status = paidPeriods.contains(e.getAmortizationPeriod()) ? "COMPLETED" : "PENDING";
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
     */
    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file) {
        try {
            // 1.1 上传文件到指定目录
            String savedFileName = fileUploadService.uploadContractFile(file);
            
            // 1.2 使用mock数据代替外部接口解析（外部接口暂未就绪）
            // TODO: 待外部解析接口就绪后，替换为真实的解析调用
            
            // 1.3 保存合同信息到数据库（使用动态mock数据）
            Contract contract = new Contract();
            
            // 生成动态的mock数据，避免重复
            long timestamp = System.currentTimeMillis();
            Random random = new Random(timestamp);
            
            // 动态生成开始日期（2025年内的随机月份）
            int startMonth = 1 + random.nextInt(8); // 1-8月
            LocalDate startDate = LocalDate.of(2025, startMonth, 1);
            contract.setStartDate(startDate);
            
            // 结束日期为开始日期后3-12个月
            int durationMonths = 3 + random.nextInt(10);
            LocalDate endDate = startDate.plusMonths(durationMonths).withDayOfMonth(
                startDate.plusMonths(durationMonths).lengthOfMonth()
            );
            contract.setEndDate(endDate);
            
            // 根据月数计算总金额（每月1000元）
            BigDecimal totalAmount = new BigDecimal(durationMonths * 1000).setScale(2, RoundingMode.HALF_UP);
            contract.setTotalAmount(totalAmount);
            
            // 动态生成供应商名称
            String[] vendors = {
                "史密斯净水设备有限公司",
                "华为技术服务有限公司", 
                "阿里云计算有限公司",
                "腾讯云服务有限公司",
                "京东物流有限公司",
                "美团配送服务公司",
            };
            contract.setVendorName(vendors[random.nextInt(vendors.length)]);
            
            // 动态生成税率（0.03-0.13之间）
            BigDecimal taxRate = new BigDecimal("0.03").add(
                new BigDecimal(random.nextDouble() * 0.10).setScale(2, RoundingMode.HALF_UP)
            );
            contract.setTaxRate(taxRate);
            
            // 在数据库中保存原始文件名，便于显示
            contract.setAttachmentName(file.getOriginalFilename());
            
            contract = contractRepository.save(contract);
            
            // 构建响应（使用mock数据格式，但文件名使用真实的原始文件名）
            ContractUploadResponse response = new ContractUploadResponse();
            response.setContractId(contract.getId());
            response.setTotalAmount(contract.getTotalAmount());
            response.setStartDate(contract.getStartDate().toString());
            response.setEndDate(contract.getEndDate().toString());
            response.setVendorName(contract.getVendorName());
            response.setTaxRate(contract.getTaxRate());
            // 使用原始文件名而不是保存后的文件名
            response.setAttachmentName(file.getOriginalFilename());
            response.setCreatedAt(java.time.OffsetDateTime.now());
            response.setMessage("合同上传和解析成功");
            
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
        summary.setContractId(contract.getId());
        summary.setTotalAmount(contract.getTotalAmount());
        summary.setStartDate(contract.getStartDate().toString());
        summary.setEndDate(contract.getEndDate().toString());
        summary.setVendorName(contract.getVendorName());
        summary.setAttachmentName(contract.getAttachmentName());
        summary.setCreatedAt(java.time.OffsetDateTime.now()); // 简化处理，实际应该从数据库获取
        summary.setStatus("ACTIVE");
        return summary;
    }
}
