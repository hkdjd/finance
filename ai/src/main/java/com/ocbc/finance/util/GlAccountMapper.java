package com.ocbc.finance.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 会计科目代码映射工具类
 * 将科目代码转换为中文名称
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
public class GlAccountMapper {

    private static final Map<String, String> GL_ACCOUNT_MAPPING = new HashMap<>();

    static {
        GL_ACCOUNT_MAPPING.put("6001", "费用");
        GL_ACCOUNT_MAPPING.put("2202", "应付");
        GL_ACCOUNT_MAPPING.put("1002", "活期存款");
        GL_ACCOUNT_MAPPING.put("1122", "预付");
    }

    /**
     * 根据科目代码获取科目名称
     * 
     * @param glAccount 科目代码
     * @return 科目名称，如果找不到则返回原代码
     */
    public static String getGlAccountName(String glAccount) {
        return GL_ACCOUNT_MAPPING.getOrDefault(glAccount, glAccount);
    }

    /**
     * 检查科目代码是否存在
     * 
     * @param glAccount 科目代码
     * @return 是否存在映射
     */
    public static boolean isValidGlAccount(String glAccount) {
        return GL_ACCOUNT_MAPPING.containsKey(glAccount);
    }

    /**
     * 获取所有科目映射
     * 
     * @return 科目代码到名称的映射
     */
    public static Map<String, String> getAllMappings() {
        return new HashMap<>(GL_ACCOUNT_MAPPING);
    }
}
