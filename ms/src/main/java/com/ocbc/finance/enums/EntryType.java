package com.ocbc.finance.enums;

/**
 * 会计分录类型枚举
 */
public enum EntryType {
    
    /**
     * 摊销分录 - 步骤3生成的摊销会计分录
     */
    AMORTIZATION("摊销分录"),
    
    /**
     * 付款分录 - 步骤4执行付款时生成的会计分录
     */
    PAYMENT("付款分录");
    
    private final String description;
    
    EntryType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return this.name() + "(" + this.description + ")";
    }
}
