package com.sist.backend.config.handler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sist.backend.entity.Customer;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.CustomerService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class oAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final CustomerService customerService;

    @Value("${jwt.access-token-expire-time}")
    private int accessTokenExpireTime;

    @Value("${jwt.refresh-token-expire-time}")
    private int refreshTokenExpireTime;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공 처리 시작");
        
        try {
            // OAuth2User 정보 추출
            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                Map<String, Object> attributes = oauth2User.getAttributes();
                log.info("OAuth2 사용자 속성: {}", attributes);
                
                // SUB(사용자 ID) 추출 - 네이버의 경우 sub 또는 id 필드
                String userId = null;
                Object sub = attributes.get("sub");
                if (sub != null) {
                    userId = sub.toString();
                } else {
                    Object id = attributes.get("id");
                    if (id != null) {
                        userId = id.toString();
                    }
                }
                
                log.info("추출된 사용자 ID: {}", userId);
                
                if (userId != null && !userId.isEmpty()) {
                    // DB에서 Customer 조회
                    Optional<Customer> customerOpt = customerService.findById(userId);
                    
                    if (customerOpt.isPresent()) {
                        Customer customer = customerOpt.get();
                        log.info("OAuth2 로그인 - 기존 사용자: {}", customer.getId());
                        
                        // JWT 토큰 생성
                        String tokenId = UUID.randomUUID().toString();
                        
                        // AccessToken payload 생성
                        Map<String, Object> accessPayload = new HashMap<>();
                        accessPayload.put("id", customer.getId());
                        accessPayload.put("customerIdx", customer.getCustomerIdx());
                        accessPayload.put("nickname", customer.getNickname());
                        accessPayload.put("cash", customer.getCash());
                        accessPayload.put("point", customer.getPoint());
                        accessPayload.put("role", "customer");
                        
                        String accessToken = jwtProvider.getToken(accessPayload, accessTokenExpireTime);
                        
                        // RefreshToken payload 생성
                        Map<String, Object> refreshPayload = new HashMap<>();
                        refreshPayload.put("customerIdx", customer.getCustomerIdx());
                        refreshPayload.put("tokenID", tokenId);
                        refreshPayload.put("role", "customer");
                        
                        String refreshToken = jwtProvider.getToken(refreshPayload, refreshTokenExpireTime);
                        
                        // 쿠키에 토큰 설정
                        String accessTokenCookieHeader = String.format("accessToken=%s; Path=/; HttpOnly; SameSite=Lax", accessToken);
                        String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax", refreshToken, refreshTokenExpireTime);
                        response.setHeader("Set-Cookie", accessTokenCookieHeader);
                        response.addHeader("Set-Cookie", refreshTokenCookieHeader);
                        
                        // DB에 refToken 업데이트
                        customer.setRefToken(tokenId);
                        customer.setRefTokenUpdatedAt(LocalDateTime.now());
                        customerService.save(customer);
                        
                        log.info("OAuth2 로그인 성공 - JWT 토큰 발급 완료. Customer IDX: {}", customer.getCustomerIdx());
                        
                        // 메인 페이지로 리다이렉트
                        response.sendRedirect("http://localhost:3333");
                        return;
                    } else {
                        log.error("OAuth2 로그인 실패 - Customer를 찾을 수 없음: {}", userId);
                        // Customer가 없으면 회원가입 페이지로 리다이렉트하거나 에러 처리
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Customer not found. Please sign up first.");
                        return;
                    }
                } else {
                    log.error("OAuth2 로그인 실패 - 사용자 ID를 추출할 수 없음. Attributes: {}", attributes);
                }
            } else {
                log.error("OAuth2 로그인 실패 - Principal이 OAuth2User가 아님: {}", authentication.getPrincipal().getClass());
            }
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 login processing failed");
            return;
        }
        
        // 실패 시 기본 처리
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
