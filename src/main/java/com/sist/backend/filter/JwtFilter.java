package com.sist.backend.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Admin;
import com.sist.backend.entity.Customer;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.admin.AdminService;

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
    

    
    private final JwtProvider jwtProvider;

    private final AdminService adminService;
    private final CustomerService customerService;

    @Value("${jwt.access-token-expire-time}")
    private int accessTokenExpireTime;

    @Value("${jwt.refresh-token-expire-time}")
    private int refreshTokenExpireTime;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("필터 요청: "+request.getRequestURI());
        // 1. 이미 인증된 사용자인지 확인 (SecurityContext에 Authentication 객체가 있다면 재인증 불필요)
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 헤더에서 토큰을 찾지 못했을 때 쿠키에서 토큰 확인
        String accessToken = null;
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(jakarta.servlet.http.Cookie cookie : cookies) {
                if("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        

        if(accessToken != null){

            try{
                if(jwtProvider.verify(accessToken)){
                    // accessToken 유효할 때
                    System.out.println("AccessToken 유효");
                    authenticateUser(accessToken);
                }else{
                    // accessToken 만료되었을 때
                    System.out.println("AccessToken 만료");
                    handleExpiredRefreshToken(request, response);
                }
            }catch(AuthenticationFailedException e){
                log.error("AccessToken 검증 실패", e);
                sendUnauthorizedResponse(response, "AccessToken 검증 실패");
                return;
            }
        }
        

        // 4. Access Token이 없거나, 유효성 검사를 통과했거나,
        //    Access Token이 만료되어 Refresh Token으로 재발급을 시도한 후 다음 필터로 체인 연결
        filterChain.doFilter(request, response);
    }


    public void handleExpiredRefreshToken(HttpServletRequest request, HttpServletResponse response) throws AuthenticationFailedException {
        String refreshToken = null;
        
        // 헤더에서 토큰을 찾지 못했을 때 쿠키에서 토큰 확인
        
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(jakarta.servlet.http.Cookie cookie : cookies) {
                if("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        
        if(refreshToken != null && jwtProvider.verify(refreshToken)){
            // refreshToken 유효할 때
                // accessToken 재발급
            Object role = jwtProvider.getClaims(refreshToken).get("role");
            String roleString = null;
            if(role != null){
                roleString = role.toString();
                if(roleString.equals("customer")){
                    System.out.println("customer RefreshToken 유효, 검증 후 accessToken 재발급 시작");
                Object tokenID = jwtProvider.getClaims(refreshToken).get("tokenID");
                Object customerIdx = jwtProvider.getClaims(refreshToken).get("customerIdx");
                    Optional<Customer> customer =null;
                    System.out.println("customerIdx: " + customerIdx);
                    if(customerIdx != null){
                        customer = customerService.findByCustomerIdxAndStatus(Integer.parseInt(customerIdx.toString()), 0);
                        if(customer.isPresent()){
                            // tokenID가 일치하는 Customer가 존재할 경우
                            System.out.println("Customer 조회 성공");
                            Customer customer_entity = customer.get();
                            System.out.println("customer_entity.getRefToken(): " + customer_entity.getRefToken());
                            System.out.println("tokenID: " + tokenID);
    
                            if(customer_entity.getRefToken().equals(tokenID.toString())){
                                // accessToken + refreshToken 재발급 (RTR)
                                System.out.println("TokenID 일치, access/refresh 재발급 시작");
                                Map<String, Object> accesspayload = new HashMap<>();
                                accesspayload.put("id", customer_entity.getId());
                                accesspayload.put("nickname", customer_entity.getNickname());
                                accesspayload.put("cash", customer_entity.getCash());
                                accesspayload.put("point", customer_entity.getPoint());
                                accesspayload.put("role", "customer");
                                accesspayload.put("customerIdx", customer_entity.getCustomerIdx());

                                String newAccessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime); // 1h

                                // 새 refreshToken 발급
                                String newTokenId = UUID.randomUUID().toString();
                                Map<String, Object> refreshPayload = new HashMap<>();
                                refreshPayload.put("role", "customer");
                                refreshPayload.put("customerIdx", customer_entity.getCustomerIdx());
                                refreshPayload.put("tokenID", newTokenId);
                                String newRefreshToken = jwtProvider.getToken(refreshPayload, refreshTokenExpireTime); // 7d

                                // DB refToken 갱신
                                customer_entity.setRefToken(newTokenId);
                                customerService.save(customer_entity);

                                // 쿠키로 둘 다 내려주기
                                String accessTokenCookieHeader = String.format("accessToken=%s;  Path=/; HttpOnly; SameSite=Lax", newAccessToken);
                                String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax", newRefreshToken, 604800);
                                response.setHeader("Set-Cookie", accessTokenCookieHeader);
                                response.addHeader("Set-Cookie", refreshTokenCookieHeader);

                                // SecurityContext 갱신
                                authenticateUser(newAccessToken);
                            }else{
                                System.out.println("TokenID 불일치");
                                throw new AuthenticationFailedException("TokenID 불일치");
                            }
                        }else{
                        // tokenID가 일치하는 Customer가 존재하지 않을 경우
                        System.out.println("해당 tokenID와 일치하는 Customer가 존재하지 않습니다.");
                        throw new AuthenticationFailedException("tokenID와 일치하는 customer 없음");
                        }
                    }
                    
    
                }else if(roleString.equals("admin")){
                    System.out.println("admin RefreshToken 유효, 검증 후 accessToken 재발급 시작");
    
                    Object tokenID = jwtProvider.getClaims(refreshToken).get("tokenID");
                    Object adminIdx = jwtProvider.getClaims(refreshToken).get("adminIdx");
                    Optional<Admin> admin =null;
                    if(adminIdx != null){
                        admin = adminService.findByAdminIdxAndStatus(Integer.parseInt(adminIdx.toString()), false);
                        if(admin.isPresent()){
                            // tokenID가 일치하는 Customer가 존재할 경우
                            System.out.println("admin 조회 성공");
                            Admin admin_entity = admin.get();
        
                            if(admin_entity.getRefToken().equals(tokenID.toString())){
                                // accessToken + refreshToken 재발급 (RTR)
                                System.out.println("TokenID 일치, access/refresh 재발급 시작");
                                Map<String, Object> accesspayload = new HashMap<>();
                                accesspayload.put("role", "admin");
                                accesspayload.put("adminIdx", admin_entity.getAdminIdx());
                                String newAccessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime); // 1h

                                // 새 refreshToken 발급
                                String newTokenId = UUID.randomUUID().toString();
                                Map<String, Object> refreshPayload = new HashMap<>();
                                refreshPayload.put("role", "admin");
                                refreshPayload.put("adminIdx", admin_entity.getAdminIdx());
                                refreshPayload.put("tokenID", newTokenId);
                                String newRefreshToken = jwtProvider.getToken(refreshPayload, refreshTokenExpireTime); // 7d

                                // DB refToken 갱신
                                admin_entity.setRefToken(newTokenId);
                                adminService.save(admin_entity);

                                // 쿠키로 둘 다 내려주기
                                String accessTokenCookieHeader = String.format("accessToken=%s;  Path=/; HttpOnly; SameSite=Lax", newAccessToken);
                                String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax", newRefreshToken, 604800);
                                response.setHeader("Set-Cookie", accessTokenCookieHeader);
                                response.addHeader("Set-Cookie", refreshTokenCookieHeader);
        
                                authenticateUser(newAccessToken);
                            }else{
                                System.out.println("TokenID 불일치");
                                throw new AuthenticationFailedException("TokenID 불일치");
                            }
                        }else{
                        // tokenID가 일치하는 Customer가 존재하지 않을 경우
                        System.out.println("해당 tokenID와 일치하는 admin 존재하지 않습니다.");
                        throw new AuthenticationFailedException("tokenID와 일치하는 admin 없음");
                        }
                    }
                    
                }
            }
            
                
            


        }else{
            // refreshToken 만료되었을 때
            // 로그인 페이지로 이동
            System.out.println("RefreshToken 만료");
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
    private void authenticateUser(String token) throws AuthenticationFailedException {
        CustomerAdminSignupDTO customerAdminSignupDTO = new CustomerAdminSignupDTO();
        // 1. 토큰에서 사용자 ID 추출 (Principal)
        Object role = jwtProvider.getClaims(token).get("role");
        String roleString = null;
        if(role != null){
            System.out.println("role 존재");
            roleString = role.toString();
            if(roleString.equals("customer")){
                System.out.println("customer role 존재");
                Object customerIdx = jwtProvider.getClaims(token).get("customerIdx");
                customerAdminSignupDTO.setCustomerIdx(Integer.parseInt(customerIdx.toString()));
            }else if(roleString.equals("admin")){
                System.out.println("admin role 존재");
                Object adminIdx = jwtProvider.getClaims(token).get("adminIdx");
                customerAdminSignupDTO.setAdminIdx(Integer.parseInt(adminIdx.toString()));
            }
        }else{
            System.out.println("role 없음");
            throw new AuthenticationFailedException("role 없음");
        }
        
        
        customerAdminSignupDTO.setRole(roleString);
        String roleUpperString = roleString.toUpperCase();
        // 2. 권한 목록 생성 (여기서는 간단히 ROLE_USER만 설정한다고 가정)
        // 실제 구현에서는 DB에서 사용자의 실제 권한을 조회해야 합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            customerAdminSignupDTO, // Principal: @AuthenticationPrincipal로 가져올 Long 타입 ID
            null,   // Credentials: 토큰 값은 노출하지 않기 위해 null 설정
            Collections.singleton(new SimpleGrantedAuthority("ROLE_"+roleUpperString)) // 권한 설정
        );

        // 3. SecurityContextHolder에 인증 정보 저장 (해당 요청 스레드에 격리)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        if(customerAdminSignupDTO.getCustomerIdx() != null){
            System.out.println("사용자 인증 완료 및 Security Context 설정. Costomer IDX: " + customerAdminSignupDTO.getCustomerIdx().toString());
        }else if(customerAdminSignupDTO.getAdminIdx() != null){
            System.out.println("사용자 인증 완료 및 Security Context 설정. Admin IDX: " + customerAdminSignupDTO.getAdminIdx().toString());
        }else{
            throw new AuthenticationFailedException("customerIdx 와 adminIdx 없음");
        }
    }



    private static class AuthenticationFailedException extends RuntimeException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }
}
