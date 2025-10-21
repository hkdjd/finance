package com.ocbc.finance.repository;

import com.ocbc.finance.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 合同数据访问层
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * 根据ID查询未删除的合同
     */
    @Query("SELECT c FROM Contract c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Contract> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 根据ID查询未删除的合同（Spring Data JPA命名规范）
     */
    Optional<Contract> findByIdAndIsDeletedFalse(Long id);

    /**
     * 查询所有未删除的合同（分页）
     */
    @Query("SELECT c FROM Contract c WHERE c.isDeleted = false ORDER BY c.createdDate DESC")
    Page<Contract> findAllNotDeleted(Pageable pageable);

    /**
     * 根据合同编号查询
     */
    @Query("SELECT c FROM Contract c WHERE c.contractNo = :contractNo AND c.isDeleted = false")
    Optional<Contract> findByContractNoAndNotDeleted(@Param("contractNo") String contractNo);

    /**
     * 根据创建时间范围查询
     */
    @Query("SELECT c FROM Contract c WHERE c.createdDate BETWEEN :startDate AND :endDate AND c.isDeleted = false ORDER BY c.createdDate DESC")
    List<Contract> findByCreatedDateBetweenAndNotDeleted(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 查询所有未删除的合同（分页，Spring Data JPA命名规范）
     */
    Page<Contract> findByIsDeletedFalse(Pageable pageable);

    /**
     * 根据合同别名查询未删除的合同
     */
    Optional<Contract> findByContractAliasAndIsDeletedFalse(String contractAlias);

    /**
     * 检查合同别名是否已存在
     */
    boolean existsByContractAliasAndIsDeletedFalse(String contractAlias);

    /**
     * 多条件组合查询合同列表（支持contract_no, contract_alias, is_finished, is_deleted）
     */
    @Query("SELECT c FROM Contract c WHERE " +
           "(:contractNo IS NULL OR c.contractNo LIKE %:contractNo%) AND " +
           "(:contractAlias IS NULL OR c.contractAlias LIKE %:contractAlias%) AND " +
           "(:isFinished IS NULL OR c.isFinished = :isFinished) AND " +
           "(:isDeleted IS NULL OR c.isDeleted = :isDeleted) " +
           "ORDER BY c.createdDate DESC")
    Page<Contract> findByMultipleConditions(
        @Param("contractNo") String contractNo,
        @Param("contractAlias") String contractAlias,
        @Param("isFinished") Boolean isFinished,
        @Param("isDeleted") Boolean isDeleted,
        Pageable pageable);

    /**
     * 根据文件ID查询未删除的合同（Spring Data JPA命名规范）
     */
    Optional<Contract> findByFileIdAndIsDeletedFalse(Long fileId);
}
