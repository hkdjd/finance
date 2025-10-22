package com.ocbc.finance.dto.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 合同解析响应DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractParseResponse {

    /**
     * 合同总金额
     */
    private BigDecimal totalAmount;

    /**
     * 合同开始时间
     */
    private LocalDateTime startDate;

    /**
     * 合同结束时间
     */
    private LocalDateTime endDate;

    /**
     * 税率（百分比，如10.5表示10.5%）
     */
    private BigDecimal taxRate;

    /**
     * 乙方公司名称（甲方为OCBC）
     */
    private String vendorName;

    /**
     * 解析状态
     */
    private String parseStatus;

    /**
     * 解析消息
     */
    private String parseMessage;

    /**
     * 是否使用AI解析
     */
    private Boolean aiParsed;

    /**
     * 自定义字段提取结果
     * key: 字段名称
     * value: 字段值
     */
    private Map<String, String> customFields;
}
