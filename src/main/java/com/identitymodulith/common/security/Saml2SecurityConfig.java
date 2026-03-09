package com.identitymodulith.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Keycloak SAML 2.0 연동 Spring Security 설정
 *
 * AWS Connect SSO 연동을 위한 SAML 2.0 IdP 구성
 *
 * 순환 참조 방지:
 * - Saml2AuthenticationSuccessHandler, Saml2AuthenticationFailureHandler, CustomPermissionEvaluator
 *   를 필드 주입 대신 @Bean 메서드 파라미터로 받아 Spring이 알아서 주입
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class Saml2SecurityConfig {

    /**
     * RelyingPartyRegistrationRepository Bean - SAML SP 설정
     * Keycloak IdP 메타데이터에서 설정 로드
     */
    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        try {
            log.info("====================================");
            log.info("SAML 2.0 RelyingPartyRegistration 초기화 시작");
            log.info("====================================");

            String metadataUrl = "http://1.224.162.188:51446/realms/identity-system/protocol/saml/descriptor";
            log.info("Keycloak IdP 메타데이터 URL: {}", metadataUrl);

            // Keycloak IdP 메타데이터에서 설정 로드 후 서명 비활성화
            RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(metadataUrl)
                .registrationId("keycloak")
                .entityId("http://localhost:8080/saml2/service-provider-metadata/keycloak")
                .assertionConsumerServiceLocation("http://localhost:8080/login/saml2/sso/keycloak")
                .singleLogoutServiceLocation("http://localhost:8080/logout/saml2/slo")
                // AuthnRequest 서명 비활성화 (Keycloak에서 Client Signature Required: OFF 설정과 일치)
                .signingX509Credentials(credentials -> credentials.clear())
                .assertingPartyMetadata(party -> party
                    .wantAuthnRequestsSigned(false)
                )
                .build();

            log.info("✅ SAML 2.0 RelyingPartyRegistration 초기화 성공");
            log.info("- Registration ID: keycloak");
            log.info("- Entity ID: http://localhost:8080/saml2/service-provider-metadata/keycloak");
            log.info("- ACS URL: http://localhost:8080/login/saml2/sso/keycloak");
            log.info("- SLO URL: http://localhost:8080/logout/saml2/slo");
            log.info("- IdP Entity ID: {}", registration.getAssertingPartyMetadata().getEntityId());
            log.info("- AuthnRequest Signing: DISABLED (서명 안 함)");
            log.info("- Assertion Encryption: DISABLED (암호화 안 함)");
            log.info("====================================");
            log.info("📝 Keycloak 클라이언트 필수 설정:");
            log.info("   1. Client ID: http://localhost:8080/saml2/service-provider-metadata/keycloak");
            log.info("   2. Valid Redirect URIs: http://localhost:8080/login/saml2/sso/keycloak");
            log.info("   3. ⚠️  Client Signature Required: OFF  (매우 중요!)");
            log.info("   4. ⚠️  Encrypt Assertions: OFF");
            log.info("   5. ⚠️  Sign Documents: OFF");
            log.info("   6. ⚠️  Sign Assertions: ON  (IdP가 assertion만 서명)");
            log.info("   7. Force POST Binding: OFF");
            log.info("   8. Front Channel Logout: ON");
            log.info("====================================");

            return new InMemoryRelyingPartyRegistrationRepository(registration);

        } catch (Exception e) {
            log.error("❌ SAML 2.0 RelyingPartyRegistration 초기화 실패!", e);
            log.error("Keycloak 서버 연결을 확인하세요: http://1.224.162.188:51446");
            throw new RuntimeException("SAML 2.0 설정 초기화 실패", e);
        }
    }

    /**
     * Security Filter Chain 설정
     *
     * 핸들러를 파라미터로 받아 순환 참조를 방지합니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Saml2AuthenticationSuccessHandler saml2AuthenticationSuccessHandler,
            Saml2AuthenticationFailureHandler saml2AuthenticationFailureHandler) throws Exception {

        http
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF 설정: SAML 엔드포인트는 CSRF 검증 제외
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/saml2/**",
                    "/login/saml2/**",
                    "/logout/saml2/**"
                )
            )

            // 인증/인가 규칙
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**",
                    "/error",
                    "/saml2/**",
                    "/login/**",
                    "/logout/**",
                    "/favicon.ico",
                    "/.well-known/**"
                ).permitAll()
                .requestMatchers("/saml-info").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )

            // SAML 2.0 로그인 설정
            .saml2Login(saml2 -> saml2
                .successHandler(saml2AuthenticationSuccessHandler)
                .failureHandler(saml2AuthenticationFailureHandler)
            )

            // 일반 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=success")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // SAML 2.0 로그아웃 설정
            .saml2Logout(saml2Logout -> saml2Logout
                .logoutUrl("/saml2/logout")
            );

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Method Security Expression Handler 설정
     * CustomPermissionEvaluator를 파라미터로 받아 순환 참조를 방지합니다.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }
}

