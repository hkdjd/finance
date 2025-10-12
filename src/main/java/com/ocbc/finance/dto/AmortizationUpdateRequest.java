package com.ocbc.finance.dto;

import java.util.List;

/**
 * 摊销明细更新请求DTO
 * 请求格式与列表接口响应格式保持一致
 */
public class AmortizationUpdateRequest {
    
    private Long contractId;
    private List<AmortizationEntryData> amortization;
    
    public AmortizationUpdateRequest() {}
    
    public AmortizationUpdateRequest(Long contractId, List<AmortizationEntryData> amortization) {
        this.contractId = contractId;
        this.amortization = amortization;
    }
    
    // Getters and Setters
    public Long getContractId() {
        return contractId;
    }
    
    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }
    
    public List<AmortizationEntryData> getAmortization() {
        return amortization;
    }
    
    public void setAmortization(List<AmortizationEntryData> amortization) {
        this.amortization = amortization;
    }
    
    /**
     * 摊销明细数据内嵌类
     */
    public static class AmortizationEntryData {
        private Long id;
        private String amortizationPeriod;
        private String accountingPeriod;
        private Double amount;
        private String periodDate;
        private String paymentStatus;
        private String createdAt;
        private String updatedAt;
        private String createdBy;
        private String updatedBy;
        
        public AmortizationEntryData() {}
        
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
