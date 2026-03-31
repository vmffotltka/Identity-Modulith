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

/** Keycloak SAML 2.0 기반 보안 설정. */
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

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        try {
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

            log.info("SAML RelyingPartyRegistration 초기화 완료 - entityId={}, acsUrl={}", spEntityId, acsUrl);

            return new InMemoryRelyingPartyRegistrationRepository(registration);

        } catch (Exception e) {
            log.error("SAML RelyingPartyRegistration 초기화 실패", e);
            log.error("Keycloak 서버 연결을 확인하세요: {}", idpMetadataUrl);
            throw new RuntimeException("SAML 2.0 설정 초기화 실패", e);
        }
    }

    /** Security filter chain 설정. */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Saml2AuthenticationSuccessHandler saml2AuthenticationSuccessHandler,
            Saml2AuthenticationFailureHandler saml2AuthenticationFailureHandler,
            SamlSecurityContextFilter samlSecurityContextFilter) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // SAML 엔드포인트는 브라우저 리다이렉트 플로우 특성상 CSRF 검증에서 제외한다.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/saml2/**",
                    "/login/saml2/**",
                    "/logout/saml2/**"
                )
            )

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
                .requestMatchers("/api/me/status").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )

            .saml2Login(saml2 -> saml2
                .successHandler(saml2AuthenticationSuccessHandler)
                .failureHandler(saml2AuthenticationFailureHandler)
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl(logoutSuccessUrl)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .saml2Logout(saml2Logout -> saml2Logout
                .logoutUrl("/saml2/logout")
            )

            .addFilterAfter(samlSecurityContextFilter, SecurityContextHolderAwareRequestFilter.class);

        return http.build();
    }

    /** CORS 설정. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            frontendUrl,
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "https://ssotest.my.connect.aws"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** Method security expression handler 설정. */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }
}

