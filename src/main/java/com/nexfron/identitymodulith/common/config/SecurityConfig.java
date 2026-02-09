package com.nexfron.identitymodulith.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Spring Security 설정
 *
 * <h2>현재 설정 (개발/테스트 환경):</h2>
 * <ul>
 *   <li>모든 API 엔드포인트 허용</li>
 *   <li>Mock 인증 정보 자동 주입 (tenantId:userId 형식)</li>
 *   <li>CSRF 비활성화</li>
 *   <li>Swagger UI 접근 허용</li>
 * </ul>
 *
 * <h2>TODO (프로덕션):</h2>
 * <ul>
 *   <li>JWT 인증 추가</li>
 *   <li>역할 기반 접근 제어 (RBAC)</li>
 *   <li>CSRF 활성화</li>
 *   <li>MockAuthenticationFilter 제거</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security Filter Chain 설정
     *
     * 개발/테스트 환경에서는 모든 요청을 허용하고,
     * Mock 인증 정보를 자동으로 주입합니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API에서는 일반적으로 비활성화)
            .csrf(AbstractHttpConfigurer::disable)

            // Mock 인증 필터 추가 (개발/테스트 환경)
            .addFilterBefore(new MockAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

            // 모든 요청 허용 (개발/테스트 환경)
            .authorizeHttpRequests(auth -> auth
                // Swagger UI 및 API Docs 허용
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // 모든 API 엔드포인트 허용 (개발/테스트)
                .requestMatchers(
                    "/api/**"
                ).permitAll()

                // 나머지 모든 요청도 허용
                .anyRequest().permitAll()
            )

            // HTTP Basic 인증 비활성화 (테스트 환경)
            .httpBasic(AbstractHttpConfigurer::disable)

            // Form 로그인 비활성화
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder Bean 등록
     *
     * 비밀번호 암호화에 사용되는 BCrypt 알고리즘 구현체를 제공합니다.
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Mock 인증 필터 (개발/테스트 환경 전용)
     *
     * <h2>동작:</h2>
     * <ul>
     *   <li>X-Tenant-Id 헤더가 있으면 해당 값 사용</li>
     *   <li>없으면 기본값 "default-tenant" 사용</li>
     *   <li>X-User-Id 헤더가 있으면 해당 값 사용</li>
     *   <li>없으면 기본값 "default-user" 사용</li>
     *   <li>"tenantId:userId" 형식으로 Principal 생성</li>
     * </ul>
     *
     * <h2>⚠️ 주의:</h2>
     * 프로덕션 환경에서는 반드시 제거하고 실제 JWT 인증으로 대체해야 합니다.
     */
    private static class MockAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain
        ) throws ServletException, IOException {

            // API 요청에만 Mock 인증 적용
            if (request.getRequestURI().startsWith("/api/")) {
                // 헤더에서 Tenant ID와 User ID 추출 (없으면 기본값 사용)
                String tenantId = request.getHeader("X-Tenant-Id");
                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = "default-tenant";
                }

                String userId = request.getHeader("X-User-Id");
                if (userId == null || userId.isBlank()) {
                    userId = "default-user";
                }

                // "tenantId:userId" 형식으로 Principal 생성 (TenantContextHolder가 파싱 가능)
                String principal = tenantId + ":" + userId;

                // Mock Authentication 생성
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

                // SecurityContext에 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        }
    }
}

