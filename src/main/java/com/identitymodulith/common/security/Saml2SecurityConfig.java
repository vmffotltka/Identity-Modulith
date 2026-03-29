package com.identitymodulith.common.security;

import com.identitymodulith.common.security.filter.SamlSecurityContextFilter;
import com.identitymodulith.common.security.handler.Saml2AuthenticationFailureHandler;
import com.identitymodulith.common.security.handler.Saml2AuthenticationSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
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

    @Value("${keycloak.saml.idp-metadata-url}")
    private String idpMetadataUrl;

    @Value("${keycloak.saml.sp-entity-id}")
    private String spEntityId;

    @Value("${keycloak.saml.acs-url}")
    private String acsUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.frontend.logout-success-url:http://localhost:3000/login}")
    private String logoutSuccessUrl;

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
            log.info("Keycloak IdP 메타데이터 URL: {}", idpMetadataUrl);

            String sloUrl = acsUrl.replaceFirst("/login/saml2/sso/", "/logout/saml2/slo");

            RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(idpMetadataUrl)
                .registrationId("keycloak")
                .entityId(spEntityId)
                .assertionConsumerServiceLocation(acsUrl)
                .singleLogoutServiceLocation(sloUrl)
                .signingX509Credentials(c -> c.clear())
                .assertingPartyMetadata(party -> party
                    .wantAuthnRequestsSigned(false)
                )
                .build();

            log.info("✅ SAML 2.0 RelyingPartyRegistration 초기화 성공");
            log.info("- Registration ID: keycloak");
            log.info("- Entity ID: {}", spEntityId);
            log.info("- ACS URL: {}", acsUrl);
            log.info("- SLO URL: {}", sloUrl);
            log.info("- IdP Entity ID: {}", registration.getAssertingPartyMetadata().getEntityId());
            log.info("- AuthnRequest Signing: DISABLED");
            log.info("- Assertion Encryption: DISABLED");
            log.info("====================================");

            return new InMemoryRelyingPartyRegistrationRepository(registration);

        } catch (Exception e) {
            log.error("❌ SAML 2.0 RelyingPartyRegistration 초기화 실패!", e);
            log.error("Keycloak 서버 연결을 확인하세요: {}", idpMetadataUrl);
            throw new RuntimeException("SAML 2.0 설정 초기화 실패", e);
        }
    }

    /**
     * Security Filter Chain 설정
     *
     * 핸들러와 필터를 파라미터로 받아 순환 참조를 방지합니다.
     * - SamlSecurityContextFilter: 매 요청마다 JwtUserContext ThreadLocal 동기화
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Saml2AuthenticationSuccessHandler saml2AuthenticationSuccessHandler,
            Saml2AuthenticationFailureHandler saml2AuthenticationFailureHandler,
            SamlSecurityContextFilter samlSecurityContextFilter) throws Exception {

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
                .requestMatchers("/api/me/status").permitAll()   // 로그인 상태 확인은 누구나 가능
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
                .logoutSuccessUrl(logoutSuccessUrl)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // SAML 2.0 로그아웃 설정
            .saml2Logout(saml2Logout -> saml2Logout
                .logoutUrl("/saml2/logout")
            )

            // JwtUserContext ThreadLocal 동기화 필터 등록
            .addFilterAfter(samlSecurityContextFilter, SecurityContextHolderAwareRequestFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     * 프론트엔드 서버(app.frontend.url)와 localhost 개발 환경, AWS Connect를 허용합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 URL + 개발 환경 localhost 패턴 + AWS Connect 허용
        configuration.setAllowedOrigins(List.of(
            frontendUrl,               // ex) http://localhost:3000
            "http://localhost:3000",   // 명시적 개발 환경
            "http://127.0.0.1:3000",
            "https://ssotest.my.connect.aws"  // AWS Connect CCP 임베드용
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);  // 쿠키(JSESSIONID) 전송 허용
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

