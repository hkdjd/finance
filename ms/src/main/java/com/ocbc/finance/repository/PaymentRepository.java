package com.ocbc.finance.repository;

import com.ocbc.finance.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 根据合同ID查询付款记录
     */
    List<Payment> findByContractIdOrderByBookingDateDesc(Long contractId);

    /**
     * 根据合同ID和状态查询付款记录
     */
    List<Payment> findByContractIdAndStatusOrderByBookingDateDesc(Long contractId, Payment.PaymentStatus status);

    /**
     * 查询指定合同的所有已确认付款记录
     */
    @Query("SELECT p FROM Payment p WHERE p.contract.id = :contractId AND p.status = 'CONFIRMED' ORDER BY p.bookingDate DESC")
    List<Payment> findConfirmedPaymentsByContractId(@Param("contractId") Long contractId);
}
