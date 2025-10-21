package com.ocbc.finance.dto.accounting;

import lombok.Data;

import java.util.List;

/**
 * 会计分录分组响应DTO
 * 按accounting_no分组的会计分录响应对象
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
public class AccountingEntryGroupResponse {

    /**
     * 会计分录编号
     */
    private String accountingNo;

    /**
     * 该分录编号下的所有会计分录明细
     */
    private List<AccountingEntryResponse> entries;

    /**
     * 构造函数
     * 
     * @param accountingNo 会计分录编号
     * @param entries 会计分录明细列表
     */
    public AccountingEntryGroupResponse(String accountingNo, List<AccountingEntryResponse> entries) {
        this.accountingNo = accountingNo;
        this.entries = entries;
    }

    /**
     * 默认构造函数
     */
    public AccountingEntryGroupResponse() {
    }
}
