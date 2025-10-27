package com.sist.backend.util;

import com.sist.backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JWT 관련 유틸리티 클래스
 * HTTP 요청에서 JWT 토큰을 추출하고 adminIdx를 반환하는 공통 기능 제공
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProvider jwtProvider;

    /**
     * HTTP 요청의 쿠키에서 JWT 토큰을 추출하고 adminIdx를 반환
     * @param request HTTP 요청
     * @return adminIdx (관리자 고유 ID), 실패 시 null
     */
    public Integer getAdminIdxFromRequest(HttpServletRequest request) {
        try {
            // 1. 쿠키에서 accessToken 가져오기
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return null;
            }

            String accessToken = null;
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }

            if (accessToken == null) {
                return null;
            }

            // 2. JWT 토큰 검증
            if (!jwtProvider.verify(accessToken)) {
                return null;
            }

            // 3. JWT에서 adminIdx 추출
            Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            Object adminIdxObj = claims.get("adminIdx");
            
            if (adminIdxObj == null) {
                return null;
            }
            
            // 4. adminIdx 반환
            if (adminIdxObj instanceof Integer) {
                return (Integer) adminIdxObj;
            } else if (adminIdxObj instanceof String) {
                try {
                    return Integer.parseInt((String) adminIdxObj);
                } catch (NumberFormatException e) {
                    System.err.println("adminIdx 파싱 오류: " + e.getMessage());
                    return null;
                }
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

