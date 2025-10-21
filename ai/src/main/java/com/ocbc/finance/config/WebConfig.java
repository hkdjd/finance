package com.ocbc.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web配置类
 * 处理HTTP编码和响应问题
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置HTTP消息转换器，解决中文编码问题
     */
    @Override
    public void configureMessageConverters(@org.springframework.lang.NonNull List<HttpMessageConverter<?>> converters) {
        // 添加字符串消息转换器，使用UTF-8编码
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // 避免在响应头中添加charset参数
        converters.add(0, stringConverter);
    }
}
