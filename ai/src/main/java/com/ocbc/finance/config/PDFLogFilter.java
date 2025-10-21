package com.ocbc.finance.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * PDFBox日志过滤器
 * 用于过滤掉PDFBox和FontBox产生的字体相关错误和警告日志
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
public class PDFLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String loggerName = event.getLoggerName();
        String message = event.getFormattedMessage();
        
        // 过滤所有PDFBox和FontBox相关的日志
        if (loggerName != null && (
            loggerName.startsWith("org.apache.pdfbox") ||
            loggerName.startsWith("org.apache.fontbox")
        )) {
            return FilterReply.DENY;
        }
        
        // 过滤特定的错误消息
        if (message != null) {
            // 过滤字体替换表相关错误
            if (message.contains("The expected SubstFormat for ExtensionSubstFormat1 subtable")) {
                return FilterReply.DENY;
            }
            
            // 过滤字符映射表相关警告
            if (message.contains("Format 14 cmap table is not supported and will be ignored")) {
                return FilterReply.DENY;
            }
            
            // 过滤其他常见的PDFBox字体警告
            if (message.contains("Could not find font") ||
                message.contains("Using fallback font") ||
                message.contains("Glyph") ||
                message.contains("CMap") ||
                message.contains("Font substitution")) {
                return FilterReply.DENY;
            }
        }
        
        return FilterReply.NEUTRAL;
    }
}
