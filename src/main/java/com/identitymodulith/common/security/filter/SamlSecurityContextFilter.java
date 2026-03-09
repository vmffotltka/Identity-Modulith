package com.identitymodulith.common.security.filter;

import com.identitymodulith.common.security.CustomPermissionEvaluator;
import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.common.security.principal.SimpleAuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SAML 세션 기반 JwtUserContext 동기화 필터
 *
 * <h2>목적:</h2>
 * SAML 인증 성공 시 SecurityContext에 저장된 {@link SimpleAuthPrincipal}로부터
 * 매 요청마다 {@link JwtUserContext} ThreadLocal을 동기화합니다.
 *
 * <h2>동작 흐름:</h2>
 * <ol>
 *   <li>Spring Security가 세션에서 SecurityContext를 복원 (자동)</li>
 *   <li>이 필터가 SecurityContext의 Principal을 확인</li>
 *   <li>Principal이 {@link SimpleAuthPrincipal}이면 JwtUserContext에 정보 복사</li>
 *   <li>요청 처리 완료 후 {@code finally}에서 JwtUserContext.clear() 호출</li>
 * </ol>
 *
 * <h2>왜 필요한가:</h2>
 * {@link JwtUserContext}는 ThreadLocal 기반입니다. SAML 로그인 시 최초 1회만 설정되면
 * 다음 요청부터는 새로운 스레드가 할당되어 ThreadLocal이 비어 있습니다.
 * {@link CustomPermissionEvaluator}가 JwtUserContext를 사용하므로
 * 매 요청마다 반드시 동기화가 필요합니다.
 *
 * @see JwtUserContext
 * @see SimpleAuthPrincipal
 * @see CustomPermissionEvaluator
 */
@Component
@Slf4j
public class SamlSecurityContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            syncJwtUserContextFromSecurityContext();
            filterChain.doFilter(request, response);
        } finally {
            // 메모리 누수 방지: 반드시 요청 종료 시 ThreadLocal 초기화
            JwtUserContext.clear();
            log.trace("[SamlFilter] JwtUserContext cleared for uri={}", request.getRequestURI());
        }
    }

    /**
     * SecurityContext의 Principal이 SimpleAuthPrincipal이면 JwtUserContext에 동기화
     */
    private void syncJwtUserContextFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof SimpleAuthPrincipal authPrincipal) {
            String tenantId = authPrincipal.getTenantId();
            String userId   = authPrincipal.getUserId();

            JwtUserContext.setCurrentTenantId(tenantId);
            JwtUserContext.setCurrentUserId(userId);
            // username은 userId(agentId) 사용 (loginId가 없을 경우 대체)
            JwtUserContext.setCurrentUsername(userId);

            log.debug("[SamlFilter] JwtUserContext 동기화: tenantId={}, userId={}", tenantId, userId);
        }
    }
}



