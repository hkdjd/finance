package com.ocbc.finance.repository;

import com.ocbc.finance.entity.ContractDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 合同详细信息数据访问层
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Repository
public interface ContractDetailsRepository extends JpaRepository<ContractDetails, Long> {

    /**
     * 根据合同ID查询详细信息
     */
    @Query("SELECT cd FROM ContractDetails cd WHERE cd.contractId = :contractId AND cd.isDeleted = false")
    Optional<ContractDetails> findByContractIdAndNotDeleted(@Param("contractId") Long contractId);

    /**
     * 根据合同ID查询详细信息（Spring Data JPA命名规范）
     */
    Optional<ContractDetails> findByContractId(Long contractId);
}
