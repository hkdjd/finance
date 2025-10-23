package com.ocbc.finance.service;

import com.ocbc.finance.dto.AuditLogResponse;
import com.ocbc.finance.model.AuditLog;
import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 根据摊销明细ID查询审计日志
     *
     * @param amortizationEntryId 摊销明细ID
     * @return 审计日志响应
     */
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogsByAmortizationEntryId(Long amortizationEntryId) {
        log.info("查询摊销明细ID: {} 的审计日志", amortizationEntryId);

        List<AuditLog> auditLogs = auditLogRepository.findByAmortizationEntryIdOrderByOperationTimeDesc(amortizationEntryId);

        List<AuditLogResponse.AuditLogInfo> auditLogInfos = auditLogs.stream()
                .map(this::convertToAuditLogInfo)
                .collect(Collectors.toList());

        return AuditLogResponse.builder()
                .auditLogs(auditLogInfos)
                .totalCount(auditLogInfos.size())
                .amortizationEntryId(amortizationEntryId)
                .message("查询成功")
                .build();
    }

    /**
     * 记录付款操作审计日志
     *
     * @param amortizationEntryId 摊销明细ID
     * @param operatorId 操作人ID
     * @param paymentAmount 支付金额
     * @param paymentDate 付款时间
     * @param paymentStatus 付款状态
     * @param remark 备注
     */
    @Transactional
    public void recordPaymentAuditLog(Long amortizationEntryId, String operatorId, 
                                    BigDecimal paymentAmount, LocalDate paymentDate, 
                                    AmortizationEntry.PaymentStatus paymentStatus, String remark) {
        log.info("记录付款操作审计日志 - 摊销明细ID: {}, 操作人: {}, 支付金额: {}", 
                amortizationEntryId, operatorId, paymentAmount);

        AuditLog auditLog = AuditLog.builder()
                .amortizationEntryId(amortizationEntryId)
                .operationType(AuditLog.OperationType.PAYMENT)
                .operatorId(operatorId)
                .operationTime(OffsetDateTime.now())
                .paymentAmount(paymentAmount)
                .paymentDate(paymentDate)
                .paymentStatus(convertToAuditLogPaymentStatus(paymentStatus))
                .remark(remark)
                .createdBy(operatorId)
                .updatedBy(operatorId)
                .build();

        auditLogRepository.save(auditLog);
        log.info("付款操作审计日志记录成功，ID: {}", auditLog.getId());
    }

    /**
     * 记录更新操作审计日志
     *
     * @param amortizationEntryId 摊销明细ID
     * @param operatorId 操作人ID
     * @param oldPaymentAmount 修改前支付金额
     * @param oldPaymentDate 修改前付款时间
     * @param oldPaymentStatus 修改前付款状态
     * @param newPaymentAmount 修改后支付金额
     * @param newPaymentDate 修改后付款时间
     * @param newPaymentStatus 修改后付款状态
     * @param remark 备注
     */
    @Transactional
    public void recordUpdateAuditLog(Long amortizationEntryId, String operatorId,
                                   BigDecimal oldPaymentAmount, LocalDate oldPaymentDate, 
                                   AmortizationEntry.PaymentStatus oldPaymentStatus,
                                   BigDecimal newPaymentAmount, LocalDate newPaymentDate, 
                                   AmortizationEntry.PaymentStatus newPaymentStatus,
                                   String remark) {
        log.info("记录更新操作审计日志 - 摊销明细ID: {}, 操作人: {}", amortizationEntryId, operatorId);

        AuditLog auditLog = AuditLog.builder()
                .amortizationEntryId(amortizationEntryId)
                .operationType(AuditLog.OperationType.UPDATE)
                .operatorId(operatorId)
                .operationTime(OffsetDateTime.now())
                .paymentAmount(newPaymentAmount)
                .paymentDate(newPaymentDate)
                .paymentStatus(convertToAuditLogPaymentStatus(newPaymentStatus))
                .oldPaymentAmount(oldPaymentAmount)
                .oldPaymentDate(oldPaymentDate)
                .oldPaymentStatus(convertToAuditLogPaymentStatus(oldPaymentStatus))
                .remark(remark)
                .createdBy(operatorId)
                .updatedBy(operatorId)
                .build();

        auditLogRepository.save(auditLog);
        log.info("更新操作审计日志记录成功，ID: {}", auditLog.getId());
    }

    /**
     * 转换为审计日志信息DTO
     */
    private AuditLogResponse.AuditLogInfo convertToAuditLogInfo(AuditLog auditLog) {
        return AuditLogResponse.AuditLogInfo.builder()
                .id(auditLog.getId())
                .amortizationEntryId(auditLog.getAmortizationEntryId())
                .operationType(auditLog.getOperationType().name())
                .operationTypeDesc(auditLog.getOperationType().getDescription())
                .operatorId(auditLog.getOperatorId())
                .operationTime(auditLog.getOperationTime())
                .paymentAmount(auditLog.getPaymentAmount())
                .paymentDate(auditLog.getPaymentDate())
                .paymentStatus(auditLog.getPaymentStatus() != null ? auditLog.getPaymentStatus().name() : null)
                .paymentStatusDesc(auditLog.getPaymentStatus() != null ? auditLog.getPaymentStatus().getDescription() : null)
                .oldPaymentAmount(auditLog.getOldPaymentAmount())
                .oldPaymentDate(auditLog.getOldPaymentDate())
                .oldPaymentStatus(auditLog.getOldPaymentStatus() != null ? auditLog.getOldPaymentStatus().name() : null)
                .oldPaymentStatusDesc(auditLog.getOldPaymentStatus() != null ? auditLog.getOldPaymentStatus().getDescription() : null)
                .remark(auditLog.getRemark())
                .createdAt(auditLog.getCreatedAt())
                .createdBy(auditLog.getCreatedBy())
                .build();
    }

    /**
     * 转换摊销明细付款状态为审计日志付款状态
     */
    private AuditLog.PaymentStatus convertToAuditLogPaymentStatus(AmortizationEntry.PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            return null;
        }
        
        switch (paymentStatus) {
            case PENDING:
                return AuditLog.PaymentStatus.PENDING;
            case PAID:
                return AuditLog.PaymentStatus.PAID;
            default:
                return AuditLog.PaymentStatus.PENDING;
        }
    }
}
