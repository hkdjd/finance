package com.ocbc.finance.service;

import com.ocbc.finance.dto.JournalEntryListResponse;
import com.ocbc.finance.dto.OperationRequest;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.JournalEntry;
import com.ocbc.finance.repository.AmortizationEntryRepository;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会计分录服务类
 * 实现步骤3：根据摊销明细生成会计分录
 */
@Service
@Transactional(readOnly = true)
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final ContractRepository contractRepository;
    private final AmortizationEntryRepository amortizationEntryRepository;

    public JournalEntryService(JournalEntryRepository journalEntryRepository,
                               ContractRepository contractRepository,
                               AmortizationEntryRepository amortizationEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.contractRepository = contractRepository;
        this.amortizationEntryRepository = amortizationEntryRepository;
    }

    /**
     * 步骤3：根据合同ID生成会计分录
     * 前端页面通过合同id请求后端，后端根据步骤2的预提摊销表，生成相应的会计分录列表
     */
    @Transactional
    public List<JournalEntry> generateJournalEntries(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));

        // 获取摊销明细
        List<AmortizationEntry> amortizationEntries = amortizationEntryRepository.findByContractIdOrderByAmortizationPeriodAsc(contractId);
        
        if (amortizationEntries.isEmpty()) {
            throw new IllegalArgumentException("合同尚未生成摊销明细，请先完成步骤2");
        }

        // 仅清除“摊销类型”的会计分录（重新生成步骤3）
        journalEntryRepository.deleteByContractIdAndEntryType(contractId, JournalEntry.EntryType.AMORTIZATION);

        List<JournalEntry> journalEntries = new ArrayList<>();
        int orderCounter = 1; // 分录顺序计数器

        // 根据摊销明细生成会计分录
        for (AmortizationEntry entry : amortizationEntries) {
            // 计算记账日期（入账期间的最后一天）
            YearMonth accountingMonth = YearMonth.parse(entry.getAccountingPeriod());
            LocalDate bookingDate = accountingMonth.atEndOfMonth();

            // 创建借方分录（费用）
            JournalEntry debitEntry = new JournalEntry();
            debitEntry.setContract(contract);
            debitEntry.setBookingDate(bookingDate);
            debitEntry.setAccountName("费用");
            debitEntry.setDebitAmount(entry.getAmount());
            debitEntry.setCreditAmount(BigDecimal.ZERO);
            debitEntry.setMemo("摊销费用 - " + entry.getAmortizationPeriod());
            debitEntry.setDescription("合同摊销费用");
            debitEntry.setEntryOrder(orderCounter++);
            debitEntry.setEntryType(JournalEntry.EntryType.AMORTIZATION); // 设置为摊销类型

            // 创建贷方分录（应付）
            JournalEntry creditEntry = new JournalEntry();
            creditEntry.setContract(contract);
            creditEntry.setBookingDate(bookingDate);
            creditEntry.setAccountName("应付");
            creditEntry.setDebitAmount(BigDecimal.ZERO);
            creditEntry.setCreditAmount(entry.getAmount());
            creditEntry.setMemo("摊销应付 - " + entry.getAmortizationPeriod());
            creditEntry.setDescription("合同摊销应付");
            creditEntry.setEntryOrder(orderCounter++);
            creditEntry.setEntryType(JournalEntry.EntryType.AMORTIZATION); // 设置为摊销类型

            journalEntries.add(debitEntry);
            journalEntries.add(creditEntry);
        }

        // 保存到数据库
        return journalEntryRepository.saveAll(journalEntries);
    }

    /**
     * 步骤3：根据合同ID生成会计分录（返回包装格式）
     * 合同信息提取到根节点，避免在每个分录中重复
     */
    @Transactional
    public JournalEntryListResponse generateJournalEntriesWithResponse(Long contractId) {
        // 调用原有方法生成会计分录
        List<JournalEntry> journalEntries = generateJournalEntries(contractId);
        
        // 获取合同信息
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("未找到合同，ID=" + contractId));
        
        // 构造响应
        JournalEntryListResponse.ContractInfo contractInfo = new JournalEntryListResponse.ContractInfo(contract);
        List<JournalEntryListResponse.JournalEntryInfo> entryInfoList = journalEntries.stream()
                .map(JournalEntryListResponse.JournalEntryInfo::new)
                .collect(Collectors.toList());
        
        return new JournalEntryListResponse(contractInfo, entryInfoList);
    }

    /**
     * 查询合同的会计分录列表
     */
    public List<JournalEntry> getJournalEntriesByContract(Long contractId) {
        return journalEntryRepository.findByContractIdOrderByBookingDateAscIdAsc(contractId);
    }

    /**
     * 创建会计分录
     */
    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        return journalEntryRepository.save(journalEntry);
    }

    /**
     * 更新会计分录
     */
    @Transactional
    public JournalEntry updateJournalEntry(Long entryId, JournalEntry updatedEntry) {
        JournalEntry existingEntry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("未找到会计分录，ID=" + entryId));

        // 更新字段
        existingEntry.setBookingDate(updatedEntry.getBookingDate());
        existingEntry.setAccountName(updatedEntry.getAccountName());
        existingEntry.setDebitAmount(updatedEntry.getDebitAmount());
        existingEntry.setCreditAmount(updatedEntry.getCreditAmount());
        existingEntry.setDescription(updatedEntry.getDescription());

        return journalEntryRepository.save(existingEntry);
    }

    /**
     * 删除会计分录
     */
    @Transactional
    public void deleteJournalEntry(Long entryId) {
        if (!journalEntryRepository.existsById(entryId)) {
            throw new IllegalArgumentException("未找到会计分录，ID=" + entryId);
        }
        journalEntryRepository.deleteById(entryId);
    }

    /**
     * 根据ID查询会计分录
     */
    public JournalEntry getJournalEntryById(Long entryId) {
        return journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("未找到会计分录，ID=" + entryId));
    }

    /**
     * 批量操作会计分录
     * 支持批量增删改操作
     */
    @Transactional
    public List<JournalEntry> batchOperateJournalEntries(List<OperationRequest<JournalEntry>> requests) {
        List<JournalEntry> results = new ArrayList<>();
        
        for (OperationRequest<JournalEntry> request : requests) {
            switch (request.getOperate()) {
                case CREATE:
                    JournalEntry createdEntry = createJournalEntry(request.getData());
                    results.add(createdEntry);
                    break;
                    
                case UPDATE:
                    JournalEntry updatedEntry = updateJournalEntry(request.getId(), request.getData());
                    results.add(updatedEntry);
                    break;
                    
                case DELETE:
                    deleteJournalEntry(request.getId());
                    // DELETE操作不返回实体
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的操作类型: " + request.getOperate());
            }
        }
        
        return results;
    }
}
