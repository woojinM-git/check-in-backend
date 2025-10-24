package com.sist.backend.filter;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sist.backend.entity.Customer;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.CustomerService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtFilter extends OncePerRequestFilter {
    

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CustomerService customerService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        String accessToken = request.getHeader("accessToken");
        String refreshToken = request.getHeader("RefreshToken");
        
        if(!jwtProvider.verify(accessToken)){
            // accessToken 만료되었을 때

            if(!jwtProvider.verify(refreshToken)){
                // 두 토큰 다 만료되었을 때
                // 로그인 페이지로 이동
                
            }else{
                // accessToken 만료되었을 때
                // accessToken 재발급
                Object tokenID = jwtProvider.getClaims(refreshToken).get("tokenID");
                Object customerIdx = jwtProvider.getClaims(refreshToken).get("customerIdx");
                String tokenIDString = null;
                String customerIdxString = null;
                if(tokenID != null){
                    tokenIDString = tokenID.toString();
                    if(customerIdx != null){
                        customerIdxString = customerIdx.toString();
                        Optional<Customer> customer = customerService.findByCustomerIdx(Integer.parseInt(customerIdxString));
                    }
                }
            }
            
        }
    }
}
