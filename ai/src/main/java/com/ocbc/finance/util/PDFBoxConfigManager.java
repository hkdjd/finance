package com.ocbc.finance.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PDFBox配置管理器
 * 统一管理PDF解析相关的系统属性和日志配置
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class PDFBoxConfigManager {

    private static final AtomicBoolean CONFIGURED = new AtomicBoolean(false);

    /**
     * 应用启动时自动配置PDFBox
     */
    @PostConstruct
    public void initializePDFBoxConfig() {
        if (CONFIGURED.compareAndSet(false, true)) {
            configureOptimizedPDFBox();
            log.info("PDFBox配置管理器已初始化");
        }
    }

    /**
     * 配置最优化的PDFBox设置以避免字体相关错误
     */
    public static void configureOptimizedPDFBox() {
        try {
            log.debug("开始配置PDFBox优化设置");
            
            // 1. 禁用字体缓存相关功能
            System.setProperty("org.apache.pdfbox.fontcache", "false");
            System.setProperty("org.apache.pdfbox.fontcache.ttf.lazy", "true");
            System.setProperty("org.apache.pdfbox.pdmodel.font.cache", "false");
            
            // 2. 禁用字体替换和系统字体扫描
            System.setProperty("org.apache.pdfbox.pdmodel.font.substitution", "false");
            System.setProperty("org.apache.pdfbox.pdmodel.font.provider.ttf", "false");
            System.setProperty("org.apache.pdfbox.pdmodel.font.provider.system", "false");
            
            // 3. 优化渲染设置
            System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
            System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
            
            // 4. 禁用复杂的字体处理功能
            System.setProperty("org.apache.pdfbox.pdmodel.font.fallback", "false");
            System.setProperty("org.apache.pdfbox.pdmodel.font.embedding", "false");
            
            // 5. 完全禁用字体相关日志
            suppressAllFontLogs();
            
            log.debug("PDFBox优化配置已应用");
            
        } catch (Exception e) {
            log.warn("配置PDFBox时出现异常，继续使用默认配置: {}", e.getMessage());
        }
    }

    /**
     * 配置基础的PDFBox设置（简化版）
     */
    public static void configureBasicPDFBox() {
        try {
            // 基础配置
            System.setProperty("org.apache.pdfbox.fontcache", "false");
            System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
            System.setProperty("org.apache.pdfbox.fontcache.ttf.lazy", "true");
            
            // 抑制主要的字体警告
            suppressBasicFontWarnings();
            
        } catch (Exception e) {
            // 忽略配置错误
        }
    }

    /**
     * 完全禁用字体相关日志
     */
    private static void suppressAllFontLogs() {
        try {
            // 设置所有相关的Logger为OFF
            String[] loggerNames = {
                "org.apache.pdfbox",
                "org.apache.fontbox",
                "org.apache.pdfbox.pdmodel.font",
                "org.apache.pdfbox.pdmodel.font.FileSystemFontProvider",
                "org.apache.pdfbox.pdmodel.font.FontMapperImpl",
                "org.apache.pdfbox.pdmodel.font.PDFontFactory",
                "org.apache.fontbox.ttf.CmapSubtable",
                "org.apache.fontbox.ttf.GlyphSubstitutionTable",
                "org.apache.fontbox.ttf.TTFParser",
                "org.apache.fontbox.ttf.TrueTypeFont",
                "org.apache.fontbox.ttf.TrueTypeCollection",
                "org.apache.fontbox.ttf.TTFDataStream",
                "org.apache.fontbox.ttf.GlyphTable"
            };
            
            // 1. 设置Java Logging API级别
            for (String loggerName : loggerNames) {
                java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);
                logger.setLevel(java.util.logging.Level.OFF);
                // 移除所有处理器以确保完全静默
                logger.setUseParentHandlers(false);
            }
            
            // 2. 尝试设置SLF4J/Logback级别（如果可用）
            suppressSLF4JLogs(loggerNames);
            
            log.debug("已禁用所有字体相关日志");
            
        } catch (Exception e) {
            // 忽略日志配置错误
        }
    }

    /**
     * 尝试通过SLF4J设置日志级别（适用于Spring Boot环境）
     */
    private static void suppressSLF4JLogs(String[] loggerNames) {
        try {
            // 检查是否有Logback可用
            Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            Object offLevel = levelClass.getField("OFF").get(null);
            
            for (String loggerName : loggerNames) {
                org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(loggerName);
                if (loggerClass.isInstance(slf4jLogger)) {
                    // 设置Logback日志级别为OFF
                    loggerClass.getMethod("setLevel", levelClass).invoke(slf4jLogger, offLevel);
                }
            }
            
            log.debug("已通过SLF4J/Logback禁用字体相关日志");
            
        } catch (Exception e) {
            // Logback不可用或设置失败，忽略
            log.debug("SLF4J/Logback日志级别设置失败，将依赖配置文件设置: {}", e.getMessage());
        }
    }

    /**
     * 抑制基础的字体警告日志
     */
    private static void suppressBasicFontWarnings() {
        try {
            // 设置主要的Logger级别
            java.util.logging.Logger pdfboxLogger = java.util.logging.Logger.getLogger("org.apache.pdfbox");
            java.util.logging.Logger fontboxLogger = java.util.logging.Logger.getLogger("org.apache.fontbox");
            
            pdfboxLogger.setLevel(java.util.logging.Level.SEVERE);
            fontboxLogger.setLevel(java.util.logging.Level.SEVERE);
            
            // 特别抑制字体提供商的警告
            java.util.logging.Logger fontProviderLogger = java.util.logging.Logger.getLogger("org.apache.pdfbox.pdmodel.font.FileSystemFontProvider");
            fontProviderLogger.setLevel(java.util.logging.Level.OFF);
            
            // 抑制字体表相关警告
            java.util.logging.Logger cmapLogger = java.util.logging.Logger.getLogger("org.apache.fontbox.ttf.CmapSubtable");
            java.util.logging.Logger glyphLogger = java.util.logging.Logger.getLogger("org.apache.fontbox.ttf.GlyphSubstitutionTable");
            
            cmapLogger.setLevel(java.util.logging.Level.OFF);
            glyphLogger.setLevel(java.util.logging.Level.OFF);
            
        } catch (Exception e) {
            // 忽略日志配置错误
        }
    }

    /**
     * 重置PDFBox配置为默认值（用于测试）
     */
    public static void resetPDFBoxConfig() {
        try {
            // 清除自定义属性
            System.clearProperty("org.apache.pdfbox.fontcache");
            System.clearProperty("org.apache.pdfbox.fontcache.ttf.lazy");
            System.clearProperty("org.apache.pdfbox.pdmodel.font.cache");
            System.clearProperty("org.apache.pdfbox.pdmodel.font.substitution");
            System.clearProperty("org.apache.pdfbox.pdmodel.font.provider.ttf");
            System.clearProperty("org.apache.pdfbox.pdmodel.font.provider.system");
            System.clearProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion");
            System.clearProperty("org.apache.pdfbox.pdmodel.font.fallback");
            System.clearProperty("org.apache.pdfbox.pdmodel.font.embedding");
            
            CONFIGURED.set(false);
            log.debug("PDFBox配置已重置");
            
        } catch (Exception e) {
            log.warn("重置PDFBox配置时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 检查PDFBox是否已配置
     */
    public static boolean isConfigured() {
        return CONFIGURED.get();
    }

    /**
     * 获取当前PDFBox配置信息
     */
    public static String getConfigInfo() {
        StringBuilder info = new StringBuilder();
        info.append("PDFBox配置信息:\n");
        info.append("- 字体缓存: ").append(System.getProperty("org.apache.pdfbox.fontcache", "默认")).append("\n");
        info.append("- 字体替换: ").append(System.getProperty("org.apache.pdfbox.pdmodel.font.substitution", "默认")).append("\n");
        info.append("- 系统字体提供商: ").append(System.getProperty("org.apache.pdfbox.pdmodel.font.provider.system", "默认")).append("\n");
        info.append("- 已配置: ").append(CONFIGURED.get());
        return info.toString();
    }
}
