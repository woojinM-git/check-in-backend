package com.sist.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 쉼표로 구분된 여러 origin 지원
        String[] origins = allowedOrigins.split(",");
        
        logger.info("=================================================");
        logger.info("CORS Configuration Loaded");
        logger.info("Allowed Origins: {}", allowedOrigins);
        logger.info("Origins Array: {}", String.join(", ", origins));
        logger.info("=================================================");
        
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
