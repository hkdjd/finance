package com.ocbc.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 应用配置类
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@Configuration
public class AppConfig {

    @Value("${app.gemini.timeout:30000}")
    private int geminiTimeout;

    /**
     * 配置RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(geminiTimeout));
        factory.setReadTimeout(Duration.ofMillis(geminiTimeout));
        
        return new RestTemplate(factory);
    }
}
