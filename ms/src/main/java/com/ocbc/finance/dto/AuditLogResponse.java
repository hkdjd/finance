package com.ocbc.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 审计日志响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    /**
     * 审计日志列表
     */
    private List<AuditLogInfo> auditLogs;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 摊销明细ID
     */
    private Long amortizationEntryId;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 审计日志信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogInfo {

        /**
         * 审计日志ID
         */
        private Long id;

        /**
         * 摊销明细ID
         */
        private Long amortizationEntryId;

        /**
         * 操作类型
         */
        private String operationType;

        /**
         * 操作类型描述
         */
        private String operationTypeDesc;

        /**
         * 操作人ID
         */
        private String operatorId;

        /**
         * 操作时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private OffsetDateTime operationTime;

        /**
         * 支付金额
         */
        private BigDecimal paymentAmount;

        /**
         * 付款时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate paymentDate;

        /**
         * 付款状态
         */
        private String paymentStatus;

        /**
         * 付款状态描述
         */
        private String paymentStatusDesc;

        /**
         * 修改前支付金额
         */
        private BigDecimal oldPaymentAmount;

        /**
         * 修改前付款时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate oldPaymentDate;

        /**
         * 修改前付款状态
         */
        private String oldPaymentStatus;

        /**
         * 修改前付款状态描述
         */
        private String oldPaymentStatusDesc;

        /**
         * 备注
         */
        private String remark;

        /**
         * 创建时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private OffsetDateTime createdAt;

        /**
         * 创建人
         */
        private String createdBy;
    }
}
