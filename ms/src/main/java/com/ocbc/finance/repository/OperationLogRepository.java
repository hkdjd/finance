package com.ocbc.finance.repository;

import com.ocbc.finance.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 操作记录数据访问层
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    /**
     * 根据合同ID查询操作记录，按操作时间倒序排列
     * @param contractId 合同ID
     * @return 操作记录列表
     */
    @Query("SELECT ol FROM OperationLog ol WHERE ol.contractId = :contractId ORDER BY ol.operationTime DESC")
    List<OperationLog> findByContractIdOrderByOperationTimeDesc(@Param("contractId") Long contractId);
    
    /**
     * 根据合同ID和操作类型查询操作记录
     * @param contractId 合同ID
     * @param operationType 操作类型
     * @return 操作记录列表
     */
    List<OperationLog> findByContractIdAndOperationType(Long contractId, String operationType);
    
    /**
     * 根据操作人查询操作记录
     * @param operator 操作人
     * @return 操作记录列表
     */
    List<OperationLog> findByOperatorOrderByOperationTimeDesc(String operator);
}
