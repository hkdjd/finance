package com.ocbc.finance.repository;

import com.ocbc.finance.entity.AmortizationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 预提/待摊时间表数据访问层
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Repository
public interface AmortizationScheduleRepository extends JpaRepository<AmortizationSchedule, Long> {

    /**
     * 根据合同ID查询摊销时间表（未删除）
     */
    List<AmortizationSchedule> findByContractIdAndIsDeletedFalse(Long contractId);

    /**
     * 根据合同ID查询摊销时间表（未删除，按摊销日期排序）
     */
    List<AmortizationSchedule> findByContractIdAndIsDeletedFalseOrderByScheduleDate(Long contractId);

    /**
     * 根据ID列表和合同ID查询摊销时间表（未删除）
     */
    List<AmortizationSchedule> findByIdInAndContractIdAndIsDeletedFalse(List<Long> ids, Long contractId);

    /**
     * 根据合同ID和状态查询
     */
    @Query("SELECT a FROM AmortizationSchedule a WHERE a.contractId = :contractId AND a.isPosted = :isPosted AND a.isDeleted = false ORDER BY a.scheduleDate ASC")
    List<AmortizationSchedule> findByContractIdAndIsPostedAndIsDeletedFalse(@Param("contractId") Long contractId, @Param("isPosted") Boolean isPosted);

    /**
     * 检查合同是否存在摊销时间表
     */
    boolean existsByContractId(Long contractId);

    /**
     * 检查合同是否存在摊销时间表（未删除）
     */
    boolean existsByContractIdAndIsDeletedFalse(Long contractId);

    /**
     * 根据合同ID和摊销日期范围查询
     */
    List<AmortizationSchedule> findByContractIdAndScheduleDateBetween(Long contractId, 
                                                                     java.time.LocalDateTime startDate, 
                                                                     java.time.LocalDateTime endDate);
}
