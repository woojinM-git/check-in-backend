package com.sist.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sist.backend.config.handler.JwtAccessDeniedHandler;
import com.sist.backend.config.handler.JwtAuthenticationEntryPoint;
import com.sist.backend.filter.JwtFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityJavaConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /* 
        // 개발 단계에서는 모든 요청을 허용 (JWT 구현 전까지)
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
         */
        /* 
        // JWT 구현 후 사용할 설정
        http.csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(
                HeadersConfigurer.FrameOptionsConfig::sameOrigin
            )).authorizeHttpRequests(
                authorize -> authorize
                    .requestMatchers("/**","/reg","/login","/regLogin","/logout")
                    .permitAll().anyRequest().authenticated()   
        );
        return http.build();
        */

        http
            // 1. CSRF, formLogin, httpBasic 비활성화 (JWT 사용으로 세션 기반 인증 비활성화)
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // 2. CORS 설정 (아래 corsConfigurationSource 빈을 사용)
            .cors(c -> c.configurationSource(corsConfigurationSource()))

            // 3. 예외 처리 설정: 인증 실패(401) 및 인가 실패(403) 핸들러 등록
            .exceptionHandling(e -> e
                .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 Unauthorized
                .accessDeniedHandler(jwtAccessDeniedHandler)          // 403 Forbidden
            )

            // 4. 세션 관리: JWT 기반 인증을 위해 세션을 사용하지 않음 (STATELESS)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 5. 요청별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // SpringDoc OpenAPI 문서 경로 (인증 없이 접근 허용)
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // 인증 없이 접근 허용 (회원가입, 로그인, 에러 페이지 등)
                .requestMatchers("/api/login/**").permitAll()
                .requestMatchers("/api/hotel/**").permitAll()
                .requestMatchers("/api/hotels/**").permitAll()
                .requestMatchers("/api/used/list").permitAll()
                .requestMatchers("/api/used/search").permitAll()
                .requestMatchers("/api/used/detail").permitAll()
                .requestMatchers("/api/dining/list").permitAll()
                .requestMatchers("/api/dining/search").permitAll()
                .requestMatchers("/api/dining/detail").permitAll()


                .requestMatchers("/api/**").permitAll()
                // 예약 락 API는 인증 필요 (ROLE_CUSTOMER)
                .requestMatchers("/api/reservations/lock").hasRole("CUSTOMER")
                .requestMatchers("/api/reservations/unlock").permitAll() // beforeunload에서 인증 없이 호출 가능

                // customer 권한만 허용 (ROLE_CUSTOMER)
                .requestMatchers("/api/mypage/**").hasRole("CUSTOMER")
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                // 그 외 모든 요청은 인증 필요 (AccessToken 필수)
                .anyRequest().authenticated()
            )

            // 6. 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 추가하여 토큰 검증
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 실제 운영 환경에서는 * 대신 정확한 도메인을 지정하는 것이 안전합니다.
        configuration.setAllowedOrigins(List.of("http://localhost:3333", "http://127.0.0.1:3333")); // 허용할 출처
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // 허용할 HTTP 메서드
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 자격 증명 (쿠키, 인증 헤더) 허용
        configuration.setMaxAge(3600L); // 캐시 유효 시간

        // 'Authorization' 및 'accessToken', 'RefreshToken' 헤더를 클라이언트가 접근할 수 있도록 노출
        configuration.setExposedHeaders(List.of("Authorization", "accessToken", "RefreshToken")); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
        return source;
    }
}
