package com.ocbc.finance.dto;

import com.ocbc.finance.enums.EntryType;

/**
 * 会计分录生成请求DTO
 */
public class JournalEntryGenerateRequest {
    
    private EntryType entryType;
    private String description;
    
    public JournalEntryGenerateRequest() {}
    
    public JournalEntryGenerateRequest(EntryType entryType, String description) {
        this.entryType = entryType;
        this.description = description;
    }
    
    // Getters and Setters
    public EntryType getEntryType() {
        return entryType;
    }
    
    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "JournalEntryGenerateRequest{" +
                "entryType=" + entryType +
                ", description='" + description + '\'' +
                '}';
    }
}
