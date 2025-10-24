package com.sist.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class LoggingConfig {
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);     // 클라이언트 IP 포함
        filter.setIncludeQueryString(true);    // 쿼리 파라미터 포함
        filter.setIncludePayload(true);        // 요청 본문(body)
        filter.setMaxPayloadLength(1000);      // 로그로 찍을 body 길이 제한
        filter.setIncludeHeaders(false);       // 헤더는 보통 X
        filter.setAfterMessagePrefix("📡 [REQUEST] ");
        return filter;
    }
}
