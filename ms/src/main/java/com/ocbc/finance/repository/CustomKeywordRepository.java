package com.ocbc.finance.repository;

import com.ocbc.finance.model.CustomKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 自定义关键字Repository
 */
@Repository
public interface CustomKeywordRepository extends JpaRepository<CustomKeyword, Long> {
    
    /**
     * 根据用户ID查询所有关键字
     *
     * @param userId 用户ID
     * @return 关键字列表
     */
    List<CustomKeyword> findByUserId(Long userId);
    
    /**
     * 根据用户ID和关键字查询
     *
     * @param userId  用户ID
     * @param keyword 关键字
     * @return 关键字实体
     */
    CustomKeyword findByUserIdAndKeyword(Long userId, String keyword);
    
    /**
     * 根据用户ID删除所有关键字
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}
