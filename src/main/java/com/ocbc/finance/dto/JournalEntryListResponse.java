package com.ocbc.finance.dto;

import com.ocbc.finance.model.Contract;
import com.ocbc.finance.model.JournalEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会计分录列表响应DTO
 * 包含合同信息和会计分录列表，避免在每个分录中重复合同信息
 */
public class JournalEntryListResponse {
    
    private ContractInfo contract;
    private List<JournalEntryInfo> journalEntries;
    
    public JournalEntryListResponse() {}
    
    public JournalEntryListResponse(ContractInfo contract, List<JournalEntryInfo> journalEntries) {
        this.contract = contract;
        this.journalEntries = journalEntries;
    }
    
    // Getters and Setters
    public ContractInfo getContract() {
        return contract;
    }
    
    public void setContract(ContractInfo contract) {
        this.contract = contract;
    }
    
    public List<JournalEntryInfo> getJournalEntries() {
        return journalEntries;
    }
    
    public void setJournalEntries(List<JournalEntryInfo> journalEntries) {
        this.journalEntries = journalEntries;
    }
    
    /**
     * 合同信息内嵌类
     */
    public static class ContractInfo {
        private Long id;
        private BigDecimal totalAmount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String vendorName;
        
        public ContractInfo() {}
        
        public ContractInfo(Contract contract) {
            this.id = contract.getId();
            this.totalAmount = contract.getTotalAmount();
            this.startDate = contract.getStartDate();
            this.endDate = contract.getEndDate();
            this.vendorName = contract.getVendorName();
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
        
        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
        
        public LocalDate getStartDate() {
            return startDate;
        }
        
        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }
        
        public LocalDate getEndDate() {
            return endDate;
        }
        
        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
        
        public String getVendorName() {
            return vendorName;
        }
        
        public void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }
    }
    
    /**
     * 会计分录信息内嵌类（不包含合同信息）
     */
    public static class JournalEntryInfo {
        private Long id;
        private LocalDate bookingDate;
        private String accountName;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String description;
        private String memo;
        private Integer entryOrder;
        private String entryType;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        
        public JournalEntryInfo() {}
        
        public JournalEntryInfo(JournalEntry entry) {
            this.id = entry.getId();
            this.bookingDate = entry.getBookingDate();
            this.accountName = entry.getAccountName();
            this.debitAmount = entry.getDebitAmount();
            this.creditAmount = entry.getCreditAmount();
            this.description = entry.getDescription();
            this.memo = entry.getMemo();
            this.entryOrder = entry.getEntryOrder();
            this.entryType = entry.getEntryType() != null ? entry.getEntryType().name() : null;
            this.createdAt = entry.getCreatedAt();
            this.updatedAt = entry.getUpdatedAt();
            this.createdBy = entry.getCreatedBy();
            this.updatedBy = entry.getUpdatedBy();
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public LocalDate getBookingDate() {
            return bookingDate;
        }
        
        public void setBookingDate(LocalDate bookingDate) {
            this.bookingDate = bookingDate;
        }
        
        public String getAccountName() {
            return accountName;
        }
        
        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }
        
        public BigDecimal getDebitAmount() {
            return debitAmount;
        }
        
        public void setDebitAmount(BigDecimal debitAmount) {
            this.debitAmount = debitAmount;
        }
        
        public BigDecimal getCreditAmount() {
            return creditAmount;
        }
        
        public void setCreditAmount(BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getMemo() {
            return memo;
        }
        
        public void setMemo(String memo) {
            this.memo = memo;
        }
        
        public Integer getEntryOrder() {
            return entryOrder;
        }
        
        public void setEntryOrder(Integer entryOrder) {
            this.entryOrder = entryOrder;
        }
        
        public String getEntryType() {
            return entryType;
        }
        
        public void setEntryType(String entryType) {
            this.entryType = entryType;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
        
        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        public String getCreatedBy() {
            return createdBy;
        }
        
        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
        
        public String getUpdatedBy() {
            return updatedBy;
        }
        
        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }
    }
}
