package com.ocbc.finance.repository;

import com.ocbc.finance.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /**
     * 根据付款ID查询会计分录
     */
    List<JournalEntry> findByPaymentIdOrderByEntryOrderAsc(Long paymentId);

    /**
     * 根据记账日期范围查询会计分录
     */
    List<JournalEntry> findByBookingDateBetweenOrderByBookingDateAscEntryOrderAsc(LocalDate startDate, LocalDate endDate);

    /**
     * 根据合同ID查询所有相关的会计分录
     */
    @Query("SELECT je FROM JournalEntry je WHERE je.payment.contract.id = :contractId ORDER BY je.bookingDate ASC, je.entryOrder ASC")
    List<JournalEntry> findByContractIdOrderByBookingDateAsc(@Param("contractId") Long contractId);

    /**
     * 根据会计科目查询分录
     */
    List<JournalEntry> findByAccountNameOrderByBookingDateDesc(String accountName);
    
    /**
     * 根据合同ID直接查询会计分录（用于步骤3）
     */
    List<JournalEntry> findByContractIdOrderByBookingDateAscIdAsc(Long contractId);

    /**
     * 按合同与类型查询分录
     */
    List<JournalEntry> findByContractIdAndEntryTypeOrderByBookingDateAscIdAsc(Long contractId, com.ocbc.finance.model.JournalEntry.EntryType entryType);

    /**
     * 按合同与类型删除分录
     */
    void deleteByContractIdAndEntryType(Long contractId, com.ocbc.finance.model.JournalEntry.EntryType entryType);
    
    /**
     * 检查是否存在指定条件的付款会计分录
     */
    boolean existsByContractIdAndAccountNameAndEntryTypeAndMemo(Long contractId, String accountName, 
            com.ocbc.finance.model.JournalEntry.EntryType entryType, String memo);
}

