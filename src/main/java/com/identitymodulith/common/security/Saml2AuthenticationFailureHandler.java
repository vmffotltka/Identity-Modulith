package com.identitymodulith.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SAML 2.0 인증 실패 핸들러
 *
 * 인증 실패 시 상세 로깅 및 에러 처리
 */
@Component
@Slf4j
public class Saml2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public Saml2AuthenticationFailureHandler() {
        setDefaultFailureUrl("/?error=saml_failed");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        log.error("====================================");
        log.error("❌ SAML 2.0 인증 실패!");
        log.error("====================================");
        log.error("🔴 에러 정보:");
        log.error("  - Exception Type: {}", exception.getClass().getName());
        log.error("  - Message: {}", exception.getMessage());
        log.error("  - Request URI: {}", request.getRequestURI());
        log.error("  - Query String: {}", request.getQueryString());
        log.error("  - Remote Addr: {}", request.getRemoteAddr());
        log.error("====================================");
        log.error("Stack Trace:", exception);

        super.onAuthenticationFailure(request, response, exception);
    }
}

