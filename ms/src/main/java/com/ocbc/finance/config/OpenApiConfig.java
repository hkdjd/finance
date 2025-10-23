package com.ocbc.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置类
 * 用于生成API文档和提供Swagger UI界面
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance财务应用系统 API")
                        .version("v1.7")
                        .description("Finance财务应用系统后端API文档，包含合同管理、摊销计算、审计日志等功能")
                        .contact(new Contact()
                                .name("OCBC Finance Team")
                                .email("finance-dev@ocbc.com")
                                .url("https://github.com/ocbc/finance"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("开发环境"),
                        new Server()
                                .url("https://finance-api.ocbc.com")
                                .description("生产环境")
                ));
    }
}
