package com.sist.backend.config.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint{
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // 401 Unauthorized 에러 발생 시 처리
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드 설정

        // 클라이언트에게 반환할 JSON 본문 구성
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "인증 정보가 유효하지 않거나 토큰이 만료되었습니다. 다시 로그인하십시오.");
        errorDetails.put("path", request.getRequestURI());

        // JSON 응답 작성
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}
