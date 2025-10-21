package com.ocbc.finance.repository;

import com.ocbc.finance.entity.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 支付时间表数据访问层
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    /**
     * 根据合同ID查询支付时间表
     */
    @Query("SELECT p FROM PaymentSchedule p WHERE p.contractId = :contractId AND p.isDeleted = false ORDER BY p.paymentDate ASC")
    List<PaymentSchedule> findByContractIdAndNotDeleted(@Param("contractId") Long contractId);

    /**
     * 根据合同ID和状态查询
     */
    @Query("SELECT p FROM PaymentSchedule p WHERE p.contractId = :contractId AND p.status = :status AND p.isDeleted = false ORDER BY p.paymentDate ASC")
    List<PaymentSchedule> findByContractIdAndStatusAndNotDeleted(@Param("contractId") Long contractId, @Param("status") String status);

    /**
     * 检查合同是否存在支付时间表
     */
    boolean existsByContractId(Long contractId);

    /**
     * 检查合同是否存在支付时间表（未删除）
     */
    boolean existsByContractIdAndIsDeletedFalse(Long contractId);
}
