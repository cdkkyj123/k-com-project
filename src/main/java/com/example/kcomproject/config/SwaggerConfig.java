package com.example.kcomproject.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI kComOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("K-Com Coffee Shop API")
                        .description("K-Com Coffee Shop 주문 및 포인트 시스템 API 문서")
                        .version("v0.0.1"));
    }
}
