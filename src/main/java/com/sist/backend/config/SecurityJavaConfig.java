package com.sist.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

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
    private final CorsConfigurationSource corsConfigurationSource;
    
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                // 정적 리소스 (css, js, images 등)
                "/css/**", "/js/**", "/favicon.ico", "/images/**",
                
                // 로그인, 회원가입, 공개 API 등
                "/auth/**", 
                "/api/public/**",
                "/error",
                // 소셜 로그인 관련 경로 (콜백 등)
                "/oauth2/**", 
                "/login/oauth2/code/**"
        );
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
            
            // 2. CORS 설정 (CorsConfig에서 생성한 CorsConfigurationSource Bean 사용)
            .cors(c -> c.configurationSource(corsConfigurationSource))

            

            // 3. 예외 처리 설정: 인증 실패(401) 및 인가 실패(403) 핸들러 등록
            .exceptionHandling(e -> e
                .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 Unauthorized
                .accessDeniedHandler(jwtAccessDeniedHandler)          // 403 Forbidden
            )

            // 4. 세션 관리: JWT 기반 인증을 위해 세션을 사용하지 않음 (STATELESS)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 5. 요청별 접근 권한 설정
            // 주의: 더 구체적인 경로를 먼저 배치해야 함 (위에서 아래로 순차적으로 매칭)
            .authorizeHttpRequests(auth -> auth
                // SpringDoc OpenAPI 문서 경로 (인증 없이 접근 허용)
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // 인증이 필요한 경로를 먼저 명시 (더 구체적인 경로 우선)
                // customer 권한만 허용 (ROLE_CUSTOMER)
                .requestMatchers("/api/mypage/**").hasRole("CUSTOMER")
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                
                // 예약 락 API는 인증 필요 (ROLE_CUSTOMER)
                .requestMatchers("/api/reservations/lock").hasRole("CUSTOMER")
                
                // 인증 없이 접근 허용 (회원가입, 로그인, 에러 페이지 등)
                .requestMatchers("/api/login/**").permitAll()
                .requestMatchers("/api/hotel/**").permitAll()
                /* .requestMatchers("/api/hotels/**").permitAll()
                .requestMatchers("/api/used/list").permitAll()
                .requestMatchers("/api/used/search").permitAll()
                .requestMatchers("/api/used/detail").permitAll()
                .requestMatchers("/api/dining/list").permitAll()
                .requestMatchers("/api/dining/search").permitAll()
                .requestMatchers("/api/dining/detail").permitAll()
                .requestMatchers("/api/reservations/unlock").permitAll() // beforeunload에서 인증 없이 호출 가능
                 */
                // 그 외 모든 /api 경로는 인증 없이 접근 허용
                .requestMatchers("/api/**").permitAll()
                
                // 그 외 모든 요청은 인증 필요 (AccessToken 필수)
                .anyRequest().authenticated()
            )

            // 6. 커스텀 JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 추가하여 토큰 검증
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
