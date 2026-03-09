package com.identitymodulith.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * SAML 2.0 테스트 컨트롤러
 *
 * 브라우저에서 SAML SSO 테스트용
 */
@Controller
@Slf4j
public class SamlTestController {

    /**
     * 홈페이지 - 인증 선택사항
     */
    @GetMapping("/")
    @ResponseBody
    public String home(Authentication authentication) {
        log.info("====================================");
        log.info("🏠 홈 페이지 접근");
        log.info("====================================");

        if (authentication != null) {
            log.info("📊 인증 정보:");
            log.info("  - Authenticated: {}", authentication.isAuthenticated());
            log.info("  - Principal Type: {}", authentication.getPrincipal().getClass().getName());
            log.info("  - Name: {}", authentication.getName());
            log.info("  - Authorities: {}", authentication.getAuthorities());
        } else {
            log.info("❌ 인증 정보 없음 (미인증 사용자)");
        }
        log.info("====================================");

        if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getName())) {
            String username = authentication.getName();
            log.info("✅ 인증된 사용자로 홈 페이지 렌더링 - username: {}", username);

            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Identity Modulith - SAML 2.0 Test</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 50px; }
                        h1 { color: #4CAF50; }
                        .info { background: #f0f0f0; padding: 20px; border-radius: 5px; margin: 20px 0; }
                        .success { color: #4CAF50; }
                        a { color: #2196F3; text-decoration: none; }
                        a:hover { text-decoration: underline; }
                    </style>
                </head>
                <body>
                    <h1>✅ SAML 2.0 인증 성공!</h1>
                    <div class="info">
                        <p class="success"><strong>로그인 사용자:</strong> %s</p>
                        <p><strong>인증 방식:</strong> SAML 2.0 (Keycloak IdP)</p>
                    </div>
                    <h2>🔗 테스트 링크</h2>
                    <ul>
                        <li><a href="/saml-info">SAML 사용자 정보 확인</a></li>
                        <li><a href="/swagger-ui/index.html">Swagger UI</a></li>
                        <li><a href="/logout">로그아웃</a></li>
                    </ul>
                    <h2>📋 다음 단계</h2>
                    <ol>
                        <li>SAML Assertion에서 Agent 자동 매핑 구현</li>
                        <li>JwtUserContext에 사용자 정보 설정</li>
                        <li>로컬 RBAC 권한 검증 연동</li>
                    </ol>
                </body>
                </html>
                """.formatted(username);
        }

        log.info("🔓 미인증 사용자로 홈 페이지 렌더링");
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Identity Modulith - SAML 2.0</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 50px; }
                    h1 { color: #2196F3; }
                    .info { background: #f0f0f0; padding: 20px; border-radius: 5px; margin: 20px 0; }
                    a { 
                        display: inline-block;
                        background: #4CAF50;
                        color: white;
                        padding: 10px 20px;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 10px 0;
                    }
                    a:hover { background: #45a049; }
                    .error { color: #f44336; }
                    .success { color: #4CAF50; }
                </style>
            </head>
            <body>
                <h1>🔐 Identity Modulith - SAML 2.0 SSO</h1>
                <div class="info">
                    <p>SAML 2.0 기반 Single Sign-On (SSO) 시스템입니다.</p>
                    <p>Keycloak을 Identity Provider로 사용합니다.</p>
                </div>
                
                <h2>시작하기</h2>
                <a href="/saml2/authenticate/keycloak">🔑 SAML SSO 로그인</a>
                
                <h2>API 문서</h2>
                <ul>
                    <li><a href="/swagger-ui/index.html" style="background: #2196F3;">📚 Swagger UI</a></li>
                    <li><a href="/v3/api-docs" style="background: #2196F3;">📄 OpenAPI JSON</a></li>
                </ul>
                
                <h2>테스트 사용자</h2>
                <div class="info">
                    <p><strong>Username:</strong> test.admin</p>
                    <p><strong>Password:</strong> password123</p>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * SAML 사용자 정보 확인
     */
    @GetMapping("/saml-info")
    @ResponseBody
    public Map<String, Object> samlInfo(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal) {
        log.info("====================================");
        log.info("📋 SAML 사용자 정보 요청");
        log.info("====================================");

        Map<String, Object> info = new HashMap<>();

        if (principal != null) {
            info.put("username", principal.getName());
            info.put("attributes", principal.getAttributes());
            info.put("sessionIndexes", principal.getSessionIndexes());
            info.put("registrationId", principal.getRelyingPartyRegistrationId());

            log.info("✅ SAML Principal 정보:");
            log.info("  - Username: {}", principal.getName());
            log.info("  - Registration ID: {}", principal.getRelyingPartyRegistrationId());
            log.info("  - Attributes: {}", principal.getAttributes());
            log.info("  - Session Indexes: {}", principal.getSessionIndexes());
        } else {
            info.put("error", "SAML Principal이 없습니다");
            log.error("❌ SAML Principal이 없습니다!");
        }

        log.info("====================================");
        return info;
    }

    /**
     * 로그인 페이지 (SAML SSO 시작)
     * Spring Security가 자동으로 SAML SSO로 리디렉션하므로 별도 페이지 불필요
     */
    @GetMapping("/login")
    @ResponseBody
    public String login() {
        log.info("====================================");
        log.info("🔑 /login 엔드포인트 접근 - Spring Security가 SAML SSO로 자동 리디렉션");
        log.info("====================================");
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>로그인 중...</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 50px; text-align: center; }
                    h1 { color: #2196F3; }
                </style>
            </head>
            <body>
                <h1>🔑 SAML SSO 로그인</h1>
                <p>Keycloak으로 리디렉션 중입니다...</p>
                <p>자동으로 리디렉션되지 않으면 <a href="/saml2/authenticate/keycloak">여기</a>를 클릭하세요.</p>
            </body>
            </html>
            """;
    }
}

