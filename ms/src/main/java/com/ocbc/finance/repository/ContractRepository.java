package com.ocbc.finance.repository;

import com.ocbc.finance.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    
    /**
     * 查询所有合同，按创建时间倒序排列
     */
    List<Contract> findAllByOrderByCreatedAtDesc();
}
