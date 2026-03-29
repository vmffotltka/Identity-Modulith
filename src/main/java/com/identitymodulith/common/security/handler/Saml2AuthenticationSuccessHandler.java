package com.identitymodulith.common.security.handler;

import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.common.security.principal.SimpleAuthPrincipal;
import com.identitymodulith.rbac.RbacModuleApi;
import com.identitymodulith.user.AgentExternalInfo;
import com.identitymodulith.user.UserModuleApi;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SAML 2.0 인증 성공 핸들러
 *
 * 인증 성공 시:
 * 1. SAML Assertion에서 사용자 정보 추출
 * 2. UserModuleApi를 통해 로컬 DB의 Agent와 매핑 (loginId 기반)
 * 3. RbacModuleApi를 통해 Agent의 권한 로드 → GrantedAuthority 변환
 * 4. SimpleAuthPrincipal로 SecurityContext 교체
 * 5. JwtUserContext에 사용자 정보 설정 (ThreadLocal 동기화)
 *
 * DDD/모듈러 모놀리식 원칙 준수:
 * - AgentRepository/RoleRepository 직접 접근 대신 ModuleApi 사용
 * - 모듈 간 경계 존중
 */
@Component
@Slf4j
public class Saml2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserModuleApi userModuleApi;
    private final RbacModuleApi rbacModuleApi;

    @Value("${app.frontend.login-success-url:http://localhost:3000}")
    private String loginSuccessUrl;

    public Saml2AuthenticationSuccessHandler(UserModuleApi userModuleApi, RbacModuleApi rbacModuleApi) {
        this.userModuleApi = userModuleApi;
        this.rbacModuleApi = rbacModuleApi;
        // 기본값은 프론트엔드 URL - @Value 주입 전에는 빈 문자열이므로 onAuthenticationSuccess에서 동적 설정
        setAlwaysUseDefaultTargetUrl(true); // 항상 프론트엔드로 리디렉션
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        // @Value 주입 후 동적으로 설정
        setDefaultTargetUrl(loginSuccessUrl);

        log.info("====================================");
        log.info("✅ SAML 2.0 인증 성공!");
        log.info("====================================");

        if (authentication instanceof Saml2Authentication saml2Auth) {
            Saml2AuthenticatedPrincipal samlPrincipal = (Saml2AuthenticatedPrincipal) saml2Auth.getPrincipal();

            String username = samlPrincipal.getName();
            log.info("👤 SAML 사용자 정보:");
            log.info("  - Username: {}", username);
            log.info("  - Registration ID: {}", samlPrincipal.getRelyingPartyRegistrationId());
            log.info("  - Attributes: {}", samlPrincipal.getAttributes());

            // ─── 1. 로컬 DB Agent 매핑 ───────────────────────────────────────
            var agentOpt = userModuleApi.findAgentByLoginId(username);

            if (agentOpt.isEmpty()) {
                log.warn("====================================");
                log.warn("⚠️  Agent 매핑 실패 - 인증 거부");
                log.warn("  SAML 사용자 '{}' 가 로컬 DB에 없습니다.", username);
                log.warn("  1. DB agent 테이블에 login_id='{}' 확인", username);
                log.warn("  2. /api/agents POST 로 Agent 생성 후 재시도");
                log.warn("====================================");
                response.sendRedirect(loginSuccessUrl + "?error=not_registered");
                return;
            }

            AgentExternalInfo agent = agentOpt.get();

            // ─── 2. 비활성 Agent 차단 ────────────────────────────────────────
            if (!agent.isActive()) {
                log.warn("⚠️  비활성 Agent 로그인 시도 차단: loginId={}, agentId={}", username, agent.getId());
                response.sendRedirect(loginSuccessUrl + "?error=inactive");
                return;
            }

            log.info("✅ Agent 매핑 성공: agentId={}, name={}, tenantId={}",
                agent.getId(), agent.getName(), agent.getTenantId());

            // ─── 3. SimpleAuthPrincipal 생성 + SecurityContext 임시 설정 ─────
            //        (getEffectivePermissions 내부에서 TenantContextHolder 사용하므로 먼저 설정)
            SimpleAuthPrincipal authPrincipal = new SimpleAuthPrincipal(
                agent.getTenantId(), agent.getId()
            );
            UsernamePasswordAuthenticationToken tempAuth =
                new UsernamePasswordAuthenticationToken(authPrincipal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(tempAuth);

            // ─── 4. RBAC 권한 로드 → GrantedAuthority 변환 ───────────────────
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AGENT")); // 기본 역할

            try {
                Set<String> permissions = rbacModuleApi.getEffectivePermissions(agent.getId().toString());
                permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

                log.info("✅ RBAC 권한 로드 완료: agentId={}, 권한 수={}", agent.getId(), permissions.size());
                log.debug("  - 권한 목록: {}", permissions);
            } catch (Exception e) {
                log.warn("⚠️  RBAC 권한 로드 실패 (기본 ROLE_AGENT만 부여): agentId={}, error={}",
                    agent.getId(), e.getMessage());
            }

            // ─── 5. 최종 Authentication 교체 (권한 포함) ─────────────────────
            UsernamePasswordAuthenticationToken finalAuth =
                new UsernamePasswordAuthenticationToken(authPrincipal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(finalAuth);

            // ─── 6. JwtUserContext ThreadLocal 동기화 ─────────────────────────
            JwtUserContext.setCurrentTenantId(agent.getTenantId());
            JwtUserContext.setCurrentUserId(agent.getId().toString());
            JwtUserContext.setCurrentUsername(agent.getLoginId());

            log.info("====================================");
            log.info("✅ SAML 연동 완료!");
            log.info("  1. ✅ SAML 인증 완료");
            log.info("  2. ✅ Agent DB 매핑 완료 (loginId={})", agent.getLoginId());
            log.info("  3. ✅ RBAC 권한 로드 완료 (권한 수={})", authorities.size());
            log.info("  4. ✅ JwtUserContext 설정 완료 (tenantId={})", agent.getTenantId());
            log.info("====================================");
        }

        log.info("🔀 프론트엔드 리디렉션: targetUrl={}", loginSuccessUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

