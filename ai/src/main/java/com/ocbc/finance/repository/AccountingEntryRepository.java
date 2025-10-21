package com.ocbc.finance.repository;

import com.ocbc.finance.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会计分录数据访问层 - 严格按照需求文档设计
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Repository
public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {

    /**
     * 根据合同ID查询会计分录，按会计分录日期排序
     */
    List<AccountingEntry> findByContractIdOrderByAccountingDateAsc(Long contractId);

    /**
     * 根据合同ID和会计分录编号查询，按ID排序
     */
    List<AccountingEntry> findByContractIdAndAccountingNoOrderByIdAsc(Long contractId, String accountingNo);

    /**
     * 统计指定合同在指定时间范围内的会计分录数量
     */
    long countByContractIdAndAccountingDateBetween(Long contractId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 根据摊销时间表ID查询会计分录
     */
    List<AccountingEntry> findByAmortizationScheduleId(Long amortizationScheduleId);

    /**
     * 检查合同是否存在会计分录
     */
    boolean existsByContractId(Long contractId);

    /**
     * 根据合同ID和记账日期范围查询
     */
    @Query("SELECT a FROM AccountingEntry a WHERE a.contractId = :contractId AND a.bookingDate BETWEEN :startDate AND :endDate ORDER BY a.bookingDate ASC")
    List<AccountingEntry> findByContractIdAndBookingDateBetween(@Param("contractId") Long contractId, 
                                                               @Param("startDate") LocalDateTime startDate, 
                                                               @Param("endDate") LocalDateTime endDate);
}
