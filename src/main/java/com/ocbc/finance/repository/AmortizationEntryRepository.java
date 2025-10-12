package com.ocbc.finance.repository;

import com.ocbc.finance.model.AmortizationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmortizationEntryRepository extends JpaRepository<AmortizationEntry, Long> {
    
    /**
     * 根据合同ID查询摊销明细，按摊销期间排序
     */
    List<AmortizationEntry> findByContractIdOrderByAmortizationPeriodAsc(Long contractId);
}
