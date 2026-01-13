package com.nexfron.identitymodulith.rbac.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RBAC 권한 평가자 (Permission Evaluator)
 *
 * Spring Security의 @PreAuthorize, @PostAuthorize 등에서 사용하여
 * 메서드 레벨의 권한 검사를 수행합니다.
 *
 * 사용 예시:
 * @PreAuthorize("@rbac.hasPermission(authentication, 'user:manage')")
 * public void manageUsers() { ... }
 *
 * 인증 정보는 Spring Security의 SecurityContext에서 자동으로 주입됩니다.
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 */
@Component("rbac")
@RequiredArgsConstructor
public class RbacPermissionEvaluator {

    private final RbacQueryService rbacQueryService;

    /**
     * 주어진 권한을 사용자가 보유하고 있는지 검증합니다.
     *
     * 동작 흐름:
     * 1. Authentication 객체의 유효성 검사
     * 2. Authentication에서 Principal(AuthPrincipal) 추출
     * 3. Principal에서 테넌트ID와 에이전트ID 획득
     * 4. RbacQueryService를 통해 해당 사용자의 모든 권한 조회
     * 5. 요청한 권한이 사용자의 권한 목록에 포함되어 있는지 확인
     *
     * @param authentication 현재 인증 정보 (Spring Security에서 제공)
     * @param permissionCode 검증할 권한 코드 (예: "user:manage", "org:view")
     * @return true: 권한 있음 / false: 권한 없음 또는 인증 정보 없음
     *
     * @apiNote
     *  - Authentication 또는 Principal이 null인 경우 false를 반환하므로,
     *    미인증 요청은 자동으로 거부됩니다.
     *  - TenantId와 AgentId는 인증 필터에서 Principal에 주입되어야 합니다.
     *    (예: JWT 토큰 파싱, Keycloak 헤더 필터 등)
     */
    public boolean hasPermission(Authentication authentication, String permissionCode) {
        // null 체크: 인증 정보가 없거나 Principal이 없으면 권한 없음
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        // Principal에서 테넌트ID와 에이전트ID 추출
        var principal = (AuthPrincipal) authentication.getPrincipal();

        // RbacQueryService를 통해 해당 사용자의 모든 권한을 조회하고,
        // 요청한 권한코드가 포함되어 있는지 확인
        return rbacQueryService.permissionsOf(principal.tenantId(), principal.agentId())
                .contains(permissionCode);
    }

    /**
     * 인증 Principal 정보
     *
     * Spring Security의 Authentication 객체에 포함되어야 할
     * RBAC 관련 정보를 담는 Record입니다.
     *
     * @param tenantId 테넌트 ID (멀티테넌시 환경에서 조직/회사를 식별)
     * @param agentId 에이전트 ID (사용자를 고유하게 식별하는 UUID)
     */
    public record AuthPrincipal(String tenantId, UUID agentId) {}
}
