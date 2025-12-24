package com.nexfron.identitymodulith.rbac.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("rbac")
@RequiredArgsConstructor
public class RbacPermissionEvaluator {

    private final RbacQueryService rbacQueryService;

    public boolean hasPermission(Authentication authentication, String permissionCode) {
        // tenantId/agentId는 너가 인증 주입 방식 정하면 거기서 꺼내면 됨.
        // Keycloak 전이라면 dev 헤더 필터에서 principal에 넣는 식으로 구현.
        if (authentication == null || authentication.getPrincipal() == null) return false;

        var principal = (AuthPrincipal) authentication.getPrincipal();
        return rbacQueryService.permissionsOf(principal.tenantId(), principal.agentId())
                .contains(permissionCode);
    }

    public record AuthPrincipal(String tenantId, UUID agentId) {}
}
