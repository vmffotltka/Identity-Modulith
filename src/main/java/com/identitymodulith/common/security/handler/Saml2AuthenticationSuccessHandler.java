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

/** SAML 인증 성공 후 로컬 Agent/RBAC 정보를 SecurityContext에 반영한다. */
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
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        setDefaultTargetUrl(loginSuccessUrl);

        log.info("SAML 인증 성공");

        if (authentication instanceof Saml2Authentication saml2Auth) {
            Saml2AuthenticatedPrincipal samlPrincipal = (Saml2AuthenticatedPrincipal) saml2Auth.getPrincipal();

            String username = samlPrincipal.getName();
            log.info("👤 SAML 사용자 정보:");
            log.info("  - Username: {}", username);
            log.info("  - Registration ID: {}", samlPrincipal.getRelyingPartyRegistrationId());
            log.info("  - Attributes: {}", samlPrincipal.getAttributes());

            var agentOpt = userModuleApi.findAgentByLoginId(username);

            if (agentOpt.isEmpty()) {
                log.warn("Agent 매핑 실패 - 인증 거부");
                log.warn("  SAML 사용자 '{}' 가 로컬 DB에 없습니다.", username);
                log.warn("  1. DB agent 테이블에 login_id='{}' 확인", username);
                log.warn("  2. /api/agents POST 로 Agent 생성 후 재시도");
                response.sendRedirect(loginSuccessUrl + "?error=not_registered");
                return;
            }

            AgentExternalInfo agent = agentOpt.get();

            if (!agent.isActive()) {
                log.warn("⚠️  비활성 Agent 로그인 시도 차단: loginId={}, agentId={}", username, agent.getId());
                response.sendRedirect(loginSuccessUrl + "?error=inactive");
                return;
            }

            log.info("✅ Agent 매핑 성공: agentId={}, name={}, tenantId={}",
                agent.getId(), agent.getName(), agent.getTenantId());

            // 권한 조회에서 tenant 컨텍스트가 필요하므로 principal을 먼저 세팅한다.
            SimpleAuthPrincipal authPrincipal = new SimpleAuthPrincipal(
                agent.getTenantId(), agent.getId()
            );
            UsernamePasswordAuthenticationToken tempAuth =
                new UsernamePasswordAuthenticationToken(authPrincipal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(tempAuth);

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AGENT"));

            try {
                Set<String> permissions = rbacModuleApi.getEffectivePermissions(agent.getId().toString());
                permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

                log.info("✅ RBAC 권한 로드 완료: agentId={}, 권한 수={}", agent.getId(), permissions.size());
                log.debug("  - 권한 목록: {}", permissions);
            } catch (Exception e) {
                log.warn("⚠️  RBAC 권한 로드 실패 (기본 ROLE_AGENT만 부여): agentId={}, error={}",
                    agent.getId(), e.getMessage());
            }

            UsernamePasswordAuthenticationToken finalAuth =
                new UsernamePasswordAuthenticationToken(authPrincipal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(finalAuth);

            JwtUserContext.setCurrentTenantId(agent.getTenantId());
            JwtUserContext.setCurrentUserId(agent.getId().toString());
            JwtUserContext.setCurrentUsername(agent.getLoginId());

            log.info("SAML 연동 완료 - loginId={}, tenantId={}, authorityCount={}",
                    agent.getLoginId(), agent.getTenantId(), authorities.size());
        }

        log.info("🔀 프론트엔드 리디렉션: targetUrl={}", loginSuccessUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

