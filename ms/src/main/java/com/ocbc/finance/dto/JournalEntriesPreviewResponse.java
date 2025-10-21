package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预提会计分录预览响应DTO
 * 符合文档规范的响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntriesPreviewResponse {
    /** 合同信息 */
    private ContractInfoDto contract;
    
    /** 预提分录列表 */
    private List<PreviewEntryDto> previewEntries;
}
