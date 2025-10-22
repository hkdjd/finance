package com.ocbc.finance.controller;

import com.ocbc.finance.constants.UserConstants;
import com.ocbc.finance.dto.CustomKeywordRequest;
import com.ocbc.finance.dto.CustomKeywordResponse;
import com.ocbc.finance.service.CustomKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义关键字控制器
 * 提供自定义合同提取关键字的管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/custom-keywords")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomKeywordController {
    
    private final CustomKeywordService customKeywordService;
    
    /**
     * 创建自定义关键字
     *
     * @param request 请求对象
     * @return 创建的关键字
     */
    @PostMapping
    public ResponseEntity<?> createKeyword(@RequestBody CustomKeywordRequest request) {
        try {
            // 如果未传入userId，使用默认admin用户ID
            if (request.getUserId() == null) {
                request.setUserId(UserConstants.DEFAULT_ADMIN_USER_ID);
            }
            
            log.info("接收到创建关键字请求: userId={}, keyword={}", 
                    request.getUserId(), request.getKeyword());
            
            CustomKeywordResponse response = customKeywordService.createKeyword(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("创建关键字失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("创建关键字异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户的所有自定义关键字
     *
     * @param userId 用户ID（可选，默认使用admin用户ID）
     * @return 关键字列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getKeywordsByUserId(@PathVariable(required = false) Long userId) {
        try {
            // 如果未传入userId，使用默认admin用户ID
            if (userId == null) {
                userId = UserConstants.DEFAULT_ADMIN_USER_ID;
            }
            
            log.info("查询用户关键字: userId={}", userId);
            List<CustomKeywordResponse> keywords = customKeywordService.getKeywordsByUserId(userId);
            return ResponseEntity.ok(keywords);
            
        } catch (Exception e) {
            log.error("查询用户关键字异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户的所有自定义关键字（仅返回字符串列表）
     *
     * @param userId 用户ID（可选，默认使用admin用户ID）
     * @return 关键字字符串列表
     */
    @GetMapping("/user/{userId}/strings")
    public ResponseEntity<?> getKeywordStringsByUserId(@PathVariable(required = false) Long userId) {
        try {
            // 如果未传入userId，使用默认admin用户ID
            if (userId == null) {
                userId = UserConstants.DEFAULT_ADMIN_USER_ID;
            }
            
            log.info("查询用户关键字字符串列表: userId={}", userId);
            List<String> keywords = customKeywordService.getKeywordStringsByUserId(userId);
            return ResponseEntity.ok(keywords);
            
        } catch (Exception e) {
            log.error("查询用户关键字字符串列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误: " + e.getMessage()));
        }
    }
    
    /**
     * 更新自定义关键字
     *
     * @param id      关键字ID
     * @param request 请求对象
     * @return 更新后的关键字
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateKeyword(@PathVariable Long id,
                                           @RequestBody CustomKeywordRequest request) {
        try {
            // 如果未传入userId，使用默认admin用户ID
            if (request.getUserId() == null) {
                request.setUserId(UserConstants.DEFAULT_ADMIN_USER_ID);
            }
            
            log.info("接收到更新关键字请求: id={}, keyword={}", id, request.getKeyword());
            
            CustomKeywordResponse response = customKeywordService.updateKeyword(id, request);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("更新关键字失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("更新关键字异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误: " + e.getMessage()));
        }
    }
    
    /**
     * 删除自定义关键字
     *
     * @param id     关键字ID
     * @param userId 用户ID（通过请求参数传递，用于权限校验，可选）
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteKeyword(@PathVariable Long id,
                                           @RequestParam(required = false) Long userId) {
        try {
            // 如果未传入userId，使用默认admin用户ID
            if (userId == null) {
                userId = UserConstants.DEFAULT_ADMIN_USER_ID;
            }
            
            log.info("接收到删除关键字请求: id={}, userId={}", id, userId);
            
            customKeywordService.deleteKeyword(id, userId);
            return ResponseEntity.ok(createSuccessResponse("关键字删除成功"));
            
        } catch (IllegalArgumentException e) {
            log.warn("删除关键字失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("删除关键字异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误: " + e.getMessage()));
        }
    }
    
    /**
     * 批量设置自定义关键字（先删除用户所有关键字，再批量插入）
     *
     * @param userId   用户ID（可选，默认使用admin用户ID）
     * @param keywords 关键字列表
     * @return 创建的关键字列表
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchCreateKeywords(@RequestParam(required = false) Long userId,
                                                  @RequestBody List<String> keywords) {
        try {
            // 如果未传入userId，使用默认admin用户ID
            if (userId == null) {
                userId = UserConstants.DEFAULT_ADMIN_USER_ID;
            }
            
            log.info("接收到批量设置关键字请求: userId={}, count={}", userId, keywords.size());
            
            List<CustomKeywordResponse> responses = customKeywordService.batchCreateKeywords(userId, keywords);
            return ResponseEntity.status(HttpStatus.CREATED).body(responses);
            
        } catch (Exception e) {
            log.error("批量创建关键字异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("系统错误: " + e.getMessage()));
        }
    }
    
    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
    
    /**
     * 创建成功响应
     *
     * @param message 成功消息
     * @return 成功响应
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
}
