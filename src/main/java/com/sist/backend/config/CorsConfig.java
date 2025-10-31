package com.sist.backend.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    /**
     * Spring Security에서 사용할 CorsConfigurationSource Bean
     * SecurityFilterChain에서 이 Bean을 사용하여 CORS를 처리합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 환경 변수에서 허용할 출처를 가져옴 (쉼표로 구분된 여러 origin 지원)
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        
        logger.info("=================================================");
        logger.info("CORS Configuration Loaded");
        logger.info("Allowed Origins: {}", allowedOrigins);
        logger.info("Origins Array: {}", String.join(", ", origins));
        logger.info("=================================================");
        
        configuration.setAllowedOrigins(Arrays.asList(origins)); // 허용할 출처
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // 허용할 HTTP 메서드
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 자격 증명 (쿠키, 인증 헤더) 허용
        configuration.setMaxAge(3600L); // 캐시 유효 시간

        // 'Authorization', 'accessToken', 'RefreshToken' 헤더를 클라이언트가 접근할 수 있도록 노출
        configuration.setExposedHeaders(List.of("Authorization", "accessToken", "RefreshToken")); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
        return source;
    }
}
