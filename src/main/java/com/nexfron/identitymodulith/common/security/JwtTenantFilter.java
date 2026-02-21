package com.nexfron.identitymodulith.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT에서 Tenant ID를 추출하여 TenantContextHolder에 설정하는 필터
 */
@Component
@Slf4j
public class JwtTenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                // JWT에서 tenant_id 클레임 추출
                String tenantId = jwt.getClaim("tenant_id");

                if (tenantId == null || tenantId.isBlank()) {
                    // tenant_id가 없으면 기본값 사용
                    tenantId = "default-tenant";
                    log.debug("[JWT Filter] tenant_id 클레임 없음, 기본값 사용: {}", tenantId);
                }

                JwtTenantContext.setCurrentTenantId(tenantId);
                log.debug("[JWT Filter] Tenant 설정 완료 - tenantId: {}, username: {}",
                    tenantId, jwt.getClaim("preferred_username"));
            }

            filterChain.doFilter(request, response);

        } finally {
            // 요청 종료 시 컨텍스트 클리어
            JwtTenantContext.clear();
        }
    }
}



