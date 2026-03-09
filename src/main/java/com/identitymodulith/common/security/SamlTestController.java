package com.identitymodulith.common.security;

import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.common.security.principal.SimpleAuthPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * SAML 2.0 테스트 컨트롤러 (개발 환경 전용)
 *
 * ⚠️  @Profile("dev") — 운영 환경에서는 자동으로 비활성화됩니다.
 *     운영 배포 시 spring.profiles.active=prod 설정으로 이 컨트롤러가 로드되지 않습니다.
 */
@Profile("dev")
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
     *
     * SimpleAuthPrincipal 기반으로 연동 상태를 확인합니다.
     * - SAML 원본 정보 (Saml2AuthenticatedPrincipal)
     * - 로컬 매핑 정보 (SimpleAuthPrincipal)
     * - JwtUserContext (ThreadLocal)
     * - 부여된 RBAC 권한 목록
     */
    @GetMapping("/saml-info")
    @ResponseBody
    public Map<String, Object> samlInfo(Authentication authentication) {
        log.info("====================================");
        log.info("📋 SAML 사용자 정보 요청");
        log.info("====================================");

        Map<String, Object> info = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            info.put("error", "인증 정보가 없습니다");
            return info;
        }

        Object principal = authentication.getPrincipal();

        // ─── 로컬 매핑 정보 (SimpleAuthPrincipal) ───────────────────────────
        if (principal instanceof SimpleAuthPrincipal sp) {
            Map<String, Object> localInfo = new HashMap<>();
            localInfo.put("tenantId", sp.getTenantId());
            localInfo.put("agentId", sp.getUserId());
            info.put("localMapping", localInfo);

            log.info("✅ SimpleAuthPrincipal 매핑 정보: tenantId={}, agentId={}", sp.getTenantId(), sp.getUserId());
        }

        // ─── JwtUserContext (ThreadLocal) ────────────────────────────────────
        Map<String, Object> contextInfo = new HashMap<>();
        contextInfo.put("tenantId",  JwtUserContext.getCurrentTenantId());
        contextInfo.put("userId",    JwtUserContext.getCurrentUserId());
        contextInfo.put("username",  JwtUserContext.getCurrentUsername());
        info.put("jwtUserContext", contextInfo);

        // ─── RBAC 권한 목록 ───────────────────────────────────────────────────
        var authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        info.put("authorities", authorities);

        log.info("  - Authorities: {}", authorities);
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

