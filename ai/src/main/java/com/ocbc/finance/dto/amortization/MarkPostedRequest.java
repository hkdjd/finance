package com.ocbc.finance.dto.amortization;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 标记已生成分录请求DTO
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Data
@Builder
public class MarkPostedRequest {

    /**
     * 摊销时间表ID列表
     */
    @NotEmpty(message = "摊销时间表ID列表不能为空")
    private List<Long> scheduleIds;

    /**
     * 操作人
     */
    @NotBlank(message = "操作人不能为空")
    private String updatedBy;

    /**
     * 备注
     */
    private String remarks;
}
