package com.ocbc.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 外部接口解析合同返回的数据
 */
@Data
public class ExternalContractParseResponse {
    
    /** 合同总金额 */
    private BigDecimal totalAmount;
    
    /** 合同开始时间 */
    private String startDate;
    
    /** 合同结束时间 */
    private String endDate;
    
    /** 税率 */
    private BigDecimal taxRate;
    
    /** 乙方公司名称（供应商名称） */
    private String vendorName;
    
    /** 解析是否成功 */
    private boolean success;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 自定义字段提取结果 (key: 字段名称, value: 字段值) */
    private Map<String, String> customFields;
}
