package com.sist.backend.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
public class ApiMdcAspect {

    // 제외할 URI 패턴 목록
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/internal", "/swagger", "/v3/api-docs", "/health", "/actuator"
    );

    @Around("execution(* com.sist.backend.controller..*(..))")
    public Object logApiAndManageMdc(ProceedingJoinPoint joinPoint) throws Throwable {

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String uri = request.getRequestURI();

            // 제외 대상이면 그냥 진행
            if (EXCLUDED_PATHS.stream().anyMatch(uri::startsWith)) {
                return joinPoint.proceed();
            }

            // /api/로 시작하면 apiName 추출
            if (uri.startsWith("/api/")) {
                try {
                    String[] parts = uri.split("/");
                    if (parts.length >= 3) {
                        String apiName = parts[2]; // ex: /api/hotels → hotels
                        MDC.put("apiName", apiName);
                    } else {
                        MDC.put("apiName", "unknown");
                    }
                } catch (Exception e) {
                    MDC.put("apiName", "error");
                }
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 요청 종료 후 반드시 제거
            MDC.remove("apiName");
        }
    }
}
