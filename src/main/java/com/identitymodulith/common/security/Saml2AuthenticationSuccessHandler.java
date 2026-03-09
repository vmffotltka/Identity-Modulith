package com.identitymodulith.common.security;

import com.identitymodulith.user.UserModuleApi;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SAML 2.0 인증 성공 핸들러
 *
 * 인증 성공 시:
 * 1. SAML Assertion에서 사용자 정보 추출
 * 2. UserModuleApi를 통해 로컬 DB의 Agent와 매핑 (loginId 기반)
 * 3. Agent의 RBAC 권한 로드 및 Spring Security에 설정
 * 4. JwtUserContext에 사용자 정보 설정 (향후 JWT 발급용)
 *
 * DDD/모듈러 모놀리식 원칙 준수:
 * - AgentRepository 직접 접근 대신 UserModuleApi 사용
 * - 모듈 간 경계 존중
 */
@Component
@Slf4j
public class Saml2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserModuleApi userModuleApi;

    public Saml2AuthenticationSuccessHandler(UserModuleApi userModuleApi) {
        this.userModuleApi = userModuleApi;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false); // SavedRequest를 우선 사용
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        log.info("====================================");
        log.info("✅ SAML 2.0 인증 성공!");
        log.info("====================================");

        if (authentication instanceof Saml2Authentication saml2Auth) {
            Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) saml2Auth.getPrincipal();

            String username = principal.getName();
            log.info("👤 SAML 사용자 정보:");
            log.info("  - Username: {}", username);
            log.info("  - Registration ID: {}", principal.getRelyingPartyRegistrationId());
            log.info("  - Session Indexes: {}", principal.getSessionIndexes());
            log.info("  - Attributes: {}", principal.getAttributes());

            // UserModuleApi를 통해 로컬 DB의 Agent와 매핑
            userModuleApi.findAgentByLoginId(username)
                .ifPresentOrElse(
                    agent -> {
                        log.info("====================================");
                        log.info("✅ Agent 매핑 성공!");
                        log.info("====================================");
                        log.info("👤 Agent 정보:");
                        log.info("  - Agent ID: {}", agent.getId());
                        log.info("  - Login ID: {}", agent.getLoginId());
                        log.info("  - Name: {}", agent.getName());
                        log.info("  - Email: {}", agent.getEmail());
                        log.info("  - Employee ID: {}", agent.getEmployeeId());
                        log.info("  - Active: {}", agent.isActive());
                        log.info("  - Organization ID: {}", agent.getOrganizationId());
                        log.info("  - Tenant ID: {}", agent.getTenantId());
                        log.info("  - Roles: {}", agent.getRoles());

                        // 🔥 TODO: RBAC 권한 로드 (다음 단계에서 구현)
                        // - RbacModuleApi를 통해 agent의 역할 조회
                        // - 각 역할의 권한(Permission) 조회
                        // - Spring Security의 GrantedAuthority로 변환
                        // - Authentication 객체 업데이트

                        log.info("====================================");
                        log.info("📋 다음 단계:");
                        log.info("  1. ✅ SAML 인증 완료");
                        log.info("  2. ✅ Agent DB 매핑 완료 (UserModuleApi 사용)");
                        log.info("  3. ⏳ RBAC 권한 로드 (다음 단계)");
                        log.info("  4. ⏳ JwtUserContext 설정 (다음 단계)");
                        log.info("====================================");
                    },
                    () -> {
                        log.warn("====================================");
                        log.warn("⚠️  Agent 매핑 실패!");
                        log.warn("====================================");
                        log.warn("SAML 사용자 '{}' 는 로컬 DB에 등록되지 않았습니다.", username);
                        log.warn("다음을 확인하세요:");
                        log.warn("  1. DB의 agent 테이블에 login_id='{}' 데이터가 있는지 확인", username);
                        log.warn("  2. Keycloak 사용자명과 DB의 login_id가 일치하는지 확인");
                        log.warn("  3. 필요 시 /api/agents POST로 Agent 생성");
                        log.warn("====================================");
                    }
                );
        }

        log.info("🔀 리디렉션:");
        log.info("  - Request URI: {}", request.getRequestURI());
        log.info("  - Target URL: {}", getDefaultTargetUrl());
        log.info("====================================");

        super.onAuthenticationSuccess(request, response, authentication);
    }
}

