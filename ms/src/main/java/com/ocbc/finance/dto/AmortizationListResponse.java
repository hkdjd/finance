package com.ocbc.finance.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摊销明细列表响应DTO
 * 包含合同信息和摊销明细数组
 */
public class AmortizationListResponse {
    
    private ContractInfo contract;
    private List<AmortizationEntryInfo> amortization;
    
    public AmortizationListResponse() {}
    
    public AmortizationListResponse(ContractInfo contract, List<AmortizationEntryInfo> amortization) {
        this.contract = contract;
        this.amortization = amortization;
    }
    
    // Getters and Setters
    public ContractInfo getContract() {
        return contract;
    }
    
    public void setContract(ContractInfo contract) {
        this.contract = contract;
    }
    
    public List<AmortizationEntryInfo> getAmortization() {
        return amortization;
    }
    
    public void setAmortization(List<AmortizationEntryInfo> amortization) {
        this.amortization = amortization;
    }
    
    /**
     * 合同信息内嵌类
     */
    public static class ContractInfo {
        private Long id;
        private Double totalAmount;
        private String startDate;
        private String endDate;
        private String vendorName;
        private Double taxRate;
        private String attachmentName;
        private String attachmentPath;
        private String createdAt;
        private Map<String, String> customFields; // 自定义字段（Map格式）
        
        public ContractInfo() {}
        
        public ContractInfo(Contract contract) {
            this.id = contract.getId();
            this.totalAmount = contract.getTotalAmount() != null ? contract.getTotalAmount().doubleValue() : null;
            this.startDate = contract.getStartDate() != null ? contract.getStartDate().toString() : null;
            this.endDate = contract.getEndDate() != null ? contract.getEndDate().toString() : null;
            this.vendorName = contract.getVendorName();
            this.taxRate = contract.getTaxRate() != null ? contract.getTaxRate().doubleValue() : null;
            this.attachmentName = contract.getAttachmentName();
            // 构造附件下载路径
            this.attachmentPath = contract.getId() != null ? 
                "http://localhost:8081/contracts/" + contract.getId() + "/attachment?download=true" : null;
            this.createdAt = contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : null;
            
            // 解析 JSON 字符串为 Map
            this.customFields = parseCustomFields(contract.getCustomFieldsJson());
        }
        
        /**
         * 解析 JSON 字符串为 Map
         */
        private Map<String, String> parseCustomFields(String jsonString) {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return new HashMap<>();
            }
            
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                // 如果解析失败，返回空 Map
                return new HashMap<>();
            }
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Double getTotalAmount() {
            return totalAmount;
        }
        
        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }
        
        public String getStartDate() {
            return startDate;
        }
        
        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }
        
        public String getEndDate() {
            return endDate;
        }
        
        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
        
        public String getVendorName() {
            return vendorName;
        }
        
        public void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }
        
        public Double getTaxRate() {
            return taxRate;
        }
        
        public void setTaxRate(Double taxRate) {
            this.taxRate = taxRate;
        }
        
        public String getAttachmentName() {
            return attachmentName;
        }
        
        public void setAttachmentName(String attachmentName) {
            this.attachmentName = attachmentName;
        }
        
        public String getAttachmentPath() {
            return attachmentPath;
        }
        
        public void setAttachmentPath(String attachmentPath) {
            this.attachmentPath = attachmentPath;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public Map<String, String> getCustomFields() {
            return customFields;
        }
        
        public void setCustomFields(Map<String, String> customFields) {
            this.customFields = customFields;
        }
    }
    
    /**
     * 摊销明细信息内嵌类（不包含合同信息，避免重复）
     */
    public static class AmortizationEntryInfo {
        private Long id;
        private String amortizationPeriod;
        private String accountingPeriod;
        private Double amount;
        private String periodDate;
        private String paymentStatus;
        private String paymentDate;
        private String createdAt;
        private String updatedAt;
        private String createdBy;
        private String updatedBy;
        
        public AmortizationEntryInfo() {}
        
        public AmortizationEntryInfo(AmortizationEntry entry) {
            this.id = entry.getId();
            this.amortizationPeriod = entry.getAmortizationPeriod();
            this.accountingPeriod = entry.getAccountingPeriod();
            this.amount = entry.getAmount() != null ? entry.getAmount().doubleValue() : null;
            this.periodDate = entry.getPeriodDate() != null ? entry.getPeriodDate().toString() : null;
            this.paymentStatus = entry.getPaymentStatus() != null ? entry.getPaymentStatus().toString() : null;
            this.paymentDate = entry.getPaymentDate() != null ? entry.getPaymentDate().toString() : null;
            this.createdAt = entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : null;
            this.updatedAt = entry.getUpdatedAt() != null ? entry.getUpdatedAt().toString() : null;
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
        
        public String getAmortizationPeriod() {
            return amortizationPeriod;
        }
        
        public void setAmortizationPeriod(String amortizationPeriod) {
            this.amortizationPeriod = amortizationPeriod;
        }
        
        public String getAccountingPeriod() {
            return accountingPeriod;
        }
        
        public void setAccountingPeriod(String accountingPeriod) {
            this.accountingPeriod = accountingPeriod;
        }
        
        public Double getAmount() {
            return amount;
        }
        
        public void setAmount(Double amount) {
            this.amount = amount;
        }
        
        public String getPeriodDate() {
            return periodDate;
        }
        
        public void setPeriodDate(String periodDate) {
            this.periodDate = periodDate;
        }
        
        public String getPaymentStatus() {
            return paymentStatus;
        }
        
        public void setPaymentStatus(String paymentStatus) {
            this.paymentStatus = paymentStatus;
        }
        
        public String getPaymentDate() {
            return paymentDate;
        }
        
        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public String getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(String updatedAt) {
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
