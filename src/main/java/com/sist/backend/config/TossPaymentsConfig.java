package com.sist.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * TossPayments 설정 클래스 application.yml의 toss 설정을 바인딩합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "toss")
@Data
public class TossPaymentsConfig {

    private Payments payments = new Payments();

    @Data
    public static class Payments {

        private String secretKey;
        private String clientKey;
        /**
         * Toss API Base URL (optional). Default: https://api.tosspayments.com
         */
        private String apiBaseUrl;
    }
}
