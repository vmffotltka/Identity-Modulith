package com.nexfron.identitymodulith.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keycloak 연동 Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF 비활성화 (JWT 사용 시)
            .csrf(csrf -> csrf.disable())

            // 세션 미사용 (Stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 인증/인가 규칙
            .authorizeHttpRequests(auth -> auth
                // Public 엔드포인트 (인증 불필요)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/api/v1/auth/**"  // 로그인/토큰 발급
                ).permitAll()

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )

            // OAuth 2.0 Resource Server 설정
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * JWT에서 권한(Role) 추출
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Keycloak의 roles를 GrantedAuthority로 변환
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

        // JWT의 'preferred_username' 또는 'sub'를 Principal name으로 사용
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",          // React 개발 서버
            "http://localhost:8080",          // 로컬 서버
            "http://1.224.162.188:8080"       // 운영 서버
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Keycloak JWT에서 역할 추출 Converter
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // 1. realm_access.roles에서 역할 추출
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<String> realmRoles = realmAccess != null
                ? (Collection<String>) realmAccess.get("roles")
                : List.of();

            // 2. resource_access.{client-id}.roles에서 역할 추출
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            Collection<String> clientRoles = List.of();
            if (resourceAccess != null) {
                Map<String, Object> client = (Map<String, Object>) resourceAccess.get("identity-modulith");
                if (client != null) {
                    clientRoles = (Collection<String>) client.get("roles");
                }
            }

            // 3. 모든 역할을 ROLE_ 접두사와 함께 GrantedAuthority로 변환
            return Stream.concat(
                    realmRoles.stream(),
                    clientRoles.stream()
                )
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        }
    }
}

