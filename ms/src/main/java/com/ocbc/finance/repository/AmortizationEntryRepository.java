package com.ocbc.finance.repository;

import com.ocbc.finance.model.AmortizationEntry;
import com.ocbc.finance.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmortizationEntryRepository extends JpaRepository<AmortizationEntry, Long> {
    
    /**
     * 根据合同ID查询摊销明细，按摊销期间排序
     */
    List<AmortizationEntry> findByContractIdOrderByAmortizationPeriodAsc(Long contractId);
    
    /**
     * 根据合同对象查询摊销明细，按摊销期间排序
     */
    List<AmortizationEntry> findByContractOrderByAmortizationPeriodAsc(Contract contract);
    
    /**
     * 根据摊销期间查询摊销明细
     */
    List<AmortizationEntry> findByAmortizationPeriod(String amortizationPeriod);
    
    /**
     * 根据付款状态查询摊销明细
     */
    List<AmortizationEntry> findByPaymentStatus(AmortizationEntry.PaymentStatus paymentStatus);
}
