package com.ocbc.finance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 审计日志实体类
 * 记录摊销明细的付款操作历史
 */
@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 摊销明细ID
     */
    @Column(name = "amortization_entry_id", nullable = false)
    private Long amortizationEntryId;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationType operationType;

    /**
     * 操作人ID
     */
    @Column(name = "operator_id", nullable = false, length = 100)
    private String operatorId;

    /**
     * 操作时间
     */
    @Column(name = "operation_time", nullable = false)
    private OffsetDateTime operationTime;

    /**
     * 支付金额
     */
    @Column(name = "payment_amount", precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    /**
     * 付款时间
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * 付款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus;

    /**
     * 修改前支付金额
     */
    @Column(name = "old_payment_amount", precision = 15, scale = 2)
    private BigDecimal oldPaymentAmount;

    /**
     * 修改前付款时间
     */
    @Column(name = "old_payment_date")
    private LocalDate oldPaymentDate;

    /**
     * 修改前付款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_payment_status", length = 20)
    private PaymentStatus oldPaymentStatus;

    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        PAYMENT("付款"),
        UPDATE("更新"),
        DELETE("删除");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 付款状态枚举
     */
    public enum PaymentStatus {
        PENDING("待付款"),
        PAID("已付款"),
        CANCELLED("已取消");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
