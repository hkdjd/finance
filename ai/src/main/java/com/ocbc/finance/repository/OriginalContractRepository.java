package com.ocbc.finance.repository;

import com.ocbc.finance.entity.OriginalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 合同原件数据访问层
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Repository
public interface OriginalContractRepository extends JpaRepository<OriginalContract, Long> {

    /**
     * 根据合同ID查询原件
     */
    @Query("SELECT oc FROM OriginalContract oc JOIN oc.contract c WHERE c.id = :contractId AND oc.isDeleted = false")
    Optional<OriginalContract> findByContractIdAndNotDeleted(@Param("contractId") Long contractId);

    /**
     * 根据文件名查询
     */
    @Query("SELECT oc FROM OriginalContract oc WHERE oc.fileName = :fileName AND oc.isDeleted = false")
    Optional<OriginalContract> findByFileNameAndNotDeleted(@Param("fileName") String fileName);

    /**
     * 根据ID查询未删除的原始合同（Spring Data JPA命名规范）
     */
    Optional<OriginalContract> findByIdAndIsDeletedFalse(Long id);
}
