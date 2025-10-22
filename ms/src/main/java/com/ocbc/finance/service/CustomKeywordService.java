package com.ocbc.finance.service;

import com.ocbc.finance.dto.CustomKeywordRequest;
import com.ocbc.finance.dto.CustomKeywordResponse;
import com.ocbc.finance.model.CustomKeyword;
import com.ocbc.finance.repository.CustomKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义关键字服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomKeywordService {
    
    private final CustomKeywordRepository customKeywordRepository;
    
    /**
     * 创建自定义关键字
     *
     * @param request 请求对象
     * @return 响应对象
     */
    @Transactional
    public CustomKeywordResponse createKeyword(CustomKeywordRequest request) {
        log.info("创建自定义关键字: userId={}, keyword={}", request.getUserId(), request.getKeyword());
        
        // 检查是否已存在
        CustomKeyword existing = customKeywordRepository.findByUserIdAndKeyword(
                request.getUserId(), request.getKeyword());
        if (existing != null) {
            throw new IllegalArgumentException("关键字已存在");
        }
        
        // 创建新关键字
        CustomKeyword customKeyword = new CustomKeyword();
        customKeyword.setUserId(request.getUserId());
        customKeyword.setKeyword(request.getKeyword());
        
        CustomKeyword saved = customKeywordRepository.save(customKeyword);
        log.info("自定义关键字创建成功: id={}", saved.getId());
        
        return convertToResponse(saved);
    }
    
    /**
     * 根据用户ID获取所有关键字
     *
     * @param userId 用户ID
     * @return 关键字列表
     */
    public List<CustomKeywordResponse> getKeywordsByUserId(Long userId) {
        log.info("查询用户的自定义关键字: userId={}", userId);
        List<CustomKeyword> keywords = customKeywordRepository.findByUserId(userId);
        return keywords.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据用户ID获取所有关键字（仅返回关键字字符串列表）
     *
     * @param userId 用户ID
     * @return 关键字字符串列表
     */
    public List<String> getKeywordStringsByUserId(Long userId) {
        log.info("查询用户的自定义关键字字符串列表: userId={}", userId);
        List<CustomKeyword> keywords = customKeywordRepository.findByUserId(userId);
        return keywords.stream()
                .map(CustomKeyword::getKeyword)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新自定义关键字
     *
     * @param id      关键字ID
     * @param request 请求对象
     * @return 响应对象
     */
    @Transactional
    public CustomKeywordResponse updateKeyword(Long id, CustomKeywordRequest request) {
        log.info("更新自定义关键字: id={}, keyword={}", id, request.getKeyword());
        
        CustomKeyword customKeyword = customKeywordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关键字不存在"));
        
        // 检查是否有权限
        if (!customKeyword.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("无权限修改此关键字");
        }
        
        // 检查新关键字是否已存在
        if (!customKeyword.getKeyword().equals(request.getKeyword())) {
            CustomKeyword existing = customKeywordRepository.findByUserIdAndKeyword(
                    request.getUserId(), request.getKeyword());
            if (existing != null) {
                throw new IllegalArgumentException("关键字已存在");
            }
        }
        
        customKeyword.setKeyword(request.getKeyword());
        CustomKeyword updated = customKeywordRepository.save(customKeyword);
        
        log.info("自定义关键字更新成功: id={}", id);
        return convertToResponse(updated);
    }
    
    /**
     * 删除自定义关键字
     *
     * @param id     关键字ID
     * @param userId 用户ID（用于权限校验）
     */
    @Transactional
    public void deleteKeyword(Long id, Long userId) {
        log.info("删除自定义关键字: id={}, userId={}", id, userId);
        
        CustomKeyword customKeyword = customKeywordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关键字不存在"));
        
        // 检查是否有权限
        if (!customKeyword.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除此关键字");
        }
        
        customKeywordRepository.deleteById(id);
        log.info("自定义关键字删除成功: id={}", id);
    }
    
    /**
     * 批量设置自定义关键字（先删除用户所有关键字，再批量插入）
     *
     * @param userId   用户ID
     * @param keywords 关键字列表
     * @return 创建成功的关键字列表
     */
    @Transactional
    public List<CustomKeywordResponse> batchCreateKeywords(Long userId, List<String> keywords) {
        log.info("批量设置自定义关键字: userId={}, count={}", userId, keywords.size());
        
        // 1. 先删除该用户的所有关键字
        customKeywordRepository.deleteByUserId(userId);
        log.info("已删除用户{}的所有关键字", userId);
        
        // 2. 批量插入新的关键字
        List<CustomKeyword> customKeywords = keywords.stream()
                .map(keyword -> {
                    CustomKeyword customKeyword = new CustomKeyword();
                    customKeyword.setUserId(userId);
                    customKeyword.setKeyword(keyword);
                    return customKeyword;
                })
                .collect(Collectors.toList());
        
        List<CustomKeyword> saved = customKeywordRepository.saveAll(customKeywords);
        log.info("批量设置完成: 成功创建{}个关键字", saved.size());
        
        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为响应DTO
     *
     * @param customKeyword 实体对象
     * @return 响应对象
     */
    private CustomKeywordResponse convertToResponse(CustomKeyword customKeyword) {
        return CustomKeywordResponse.builder()
                .id(customKeyword.getId())
                .userId(customKeyword.getUserId())
                .keyword(customKeyword.getKeyword())
                .createTime(customKeyword.getCreatedAt())
                .updateTime(customKeyword.getUpdatedAt())
                .build();
    }
}
