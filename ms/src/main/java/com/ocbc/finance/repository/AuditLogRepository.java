package com.ocbc.finance.repository;

import com.ocbc.finance.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据摊销明细ID查询审计日志，按操作时间倒序排列
     *
     * @param amortizationEntryId 摊销明细ID
     * @return 审计日志列表
     */
    @Query("SELECT a FROM AuditLog a WHERE a.amortizationEntryId = :amortizationEntryId ORDER BY a.operationTime DESC")
    List<AuditLog> findByAmortizationEntryIdOrderByOperationTimeDesc(@Param("amortizationEntryId") Long amortizationEntryId);

    /**
     * 根据操作人ID查询审计日志
     *
     * @param operatorId 操作人ID
     * @return 审计日志列表
     */
    List<AuditLog> findByOperatorIdOrderByOperationTimeDesc(String operatorId);

    /**
     * 根据摊销明细ID和操作类型查询审计日志
     *
     * @param amortizationEntryId 摊销明细ID
     * @param operationType 操作类型
     * @return 审计日志列表
     */
    List<AuditLog> findByAmortizationEntryIdAndOperationTypeOrderByOperationTimeDesc(
            Long amortizationEntryId, AuditLog.OperationType operationType);
}
