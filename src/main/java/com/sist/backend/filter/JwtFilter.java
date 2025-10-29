package com.sist.backend.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sist.backend.entity.Customer;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.CustomerService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CustomerService customerService;
    
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        // 1. 이미 인증된 사용자인지 확인 (SecurityContext에 Authentication 객체가 있다면 재인증 불필요)
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = request.getHeader("accessToken");

        if(accessToken != null){

            try{
                if(jwtProvider.verify(accessToken)){
                    // accessToken 유효할 때
                    log.info("AccessToken 유효");
                    authenticateUser(accessToken);
                }else{
                    // accessToken 만료되었을 때
                    log.info("AccessToken 만료");
                    handleExpiredRefreshToken(request, response);
                }
            }catch(AuthenticationFailedException e){
                log.error("AccessToken 검증 실패", e);
                sendUnauthorizedResponse(response, "AccessToken 검증 실패");
            }
        }
        

        // 4. Access Token이 없거나, 유효성 검사를 통과했거나,
        //    Access Token이 만료되어 Refresh Token으로 재발급을 시도한 후 다음 필터로 체인 연결
        filterChain.doFilter(request, response);
    }


    public void handleExpiredRefreshToken(HttpServletRequest request, HttpServletResponse response) throws AuthenticationFailedException {
        String refreshToken = request.getHeader("RefreshToken");
        if(jwtProvider.verify(refreshToken)){
            // refreshToken 유효할 때
            // accessToken 재발급
            log.info("RefreshToken 유효, 검증 후 accessToken 재발급 시작");
            Object tokenID = jwtProvider.getClaims(refreshToken).get("tokenID");
            Object customerIdx = jwtProvider.getClaims(refreshToken).get("customerIdx");
            Optional<Customer> customer = customerService.findByCustomerIdx(Integer.parseInt(customerIdx.toString()));
            
            if(customer.isPresent()){
                // tokenID가 일치하는 Customer가 존재할 경우
                log.info("Customer 조회 성공");
                Customer customer_entity = customer.get();

                if(customer_entity.getRefToken().equals(tokenID.toString())){
                    // accessToken 재발급
                    log.info("TokenID 일치, accessToken 재발급 시작");
                    Map<String, Object> accesspayload = new HashMap<>();
                    accesspayload.put("id", customer_entity.getId());
                    accesspayload.put("nickname", customer_entity.getNickname());
                    accesspayload.put("cash", customer_entity.getCash());
                    accesspayload.put("point", customer_entity.getPoint());
                    String accessToken = jwtProvider.getToken(accesspayload, 3600);
                    response.setHeader("accessToken", accessToken);
                }else{
                    log.info("TokenID 불일치");
                    throw new AuthenticationFailedException("TokenID 불일치");
                }


                
            }else{
                // tokenID가 일치하는 Customer가 존재하지 않을 경우
                log.info("해당 tokenID와 일치하는 Customer가 존재하지 않습니다.");
                throw new AuthenticationFailedException("tokenID와 일치하는 customer 없음");
            }


        }else{
            // refreshToken 만료되었을 때
            // 로그인 페이지로 이동
            log.info("RefreshToken 만료");
            throw new AuthenticationFailedException("RefreshToken 만료");
        }

    }

    /**
     * 인증 실패 응답(401)을 설정하고 클라이언트에게 보냅니다.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // 클라이언트에게 반환할 JSON 응답 바디
        String json = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        response.getWriter().write(json);
    }

    

    /**
     * 토큰에서 정보를 추출하여 Security Context에 인증 객체를 설정합니다.
     */
    private void authenticateUser(String token) {
        // 1. 토큰에서 사용자 ID 추출 (Principal)
        Object customerIdx = jwtProvider.getClaims(token).get("customerIdx");
        Object role = jwtProvider.getClaims(token).get("role");
        String roleString = role.toString().toUpperCase();
        // 2. 권한 목록 생성 (여기서는 간단히 ROLE_USER만 설정한다고 가정)
        // 실제 구현에서는 DB에서 사용자의 실제 권한을 조회해야 합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            customerIdx, // Principal: @AuthenticationPrincipal로 가져올 Long 타입 ID
            null,   // Credentials: 토큰 값은 노출하지 않기 위해 null 설정
            Collections.singleton(new SimpleGrantedAuthority("ROLE_"+roleString)) // 권한 설정
        );

        // 3. SecurityContextHolder에 인증 정보 저장 (해당 요청 스레드에 격리)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("사용자 인증 완료 및 Security Context 설정. User ID: {}", customerIdx);
    }



    private static class AuthenticationFailedException extends RuntimeException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }
}
