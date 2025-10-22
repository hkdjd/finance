package com.ocbc.finance.constants;

/**
 * 用户相关常量
 */
public class UserConstants {
    
    /**
     * 默认管理员用户ID
     * 当接口未传入userId时，使用此默认值
     */
    public static final Long DEFAULT_ADMIN_USER_ID = 1L;
    
    private UserConstants() {
        // 私有构造函数，防止实例化
    }
}
