package com.ocbc.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义关键字请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomKeywordRequest {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 关键字
     */
    private String keyword;
}
