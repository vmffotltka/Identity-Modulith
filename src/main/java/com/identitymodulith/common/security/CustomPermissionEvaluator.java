package com.identitymodulith.common.security;

import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.rbac.RbacModuleApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;

/**
 * 커스텀 권한 평가자 - Identity Modulith의 로컬 RBAC 사용
 *
 * Keycloak은 인증만 담당하고, 실제 권한 검증은 로컬 DB의 RBAC 시스템 사용
 *
 * 사용법:
 * {@code @PreAuthorize("hasPermission(null, 'user:create')")}
 * {@code @PreAuthorize("hasPermission(#deptId, 'department', 'org:delete')")}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final RbacModuleApi rbacModuleApi;

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission) {

        // JWT에서 매핑된 Agent ID 가져오기
        String userId = JwtUserContext.getCurrentUserId();

        if (userId == null) {
            log.warn("[Permission] User ID가 없음 - JWT 매핑 실패");
            return false;
        }

        try {
            // 로컬 RBAC에서 권한 확인
            Set<String> permissions = rbacModuleApi.getEffectivePermissions(userId);
            boolean hasPermission = permissions.contains(permission.toString());

            log.debug("[Permission] 권한 확인 - userId: {}, permission: {}, granted: {}",
                userId, permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("[Permission] 권한 확인 중 오류 - userId: {}, permission: {}, error: {}",
                userId, permission, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission) {

        // targetId와 targetType을 사용한 세밀한 권한 검증
        // 예: 특정 부서에 대한 권한 검증

        String userId = JwtUserContext.getCurrentUserId();

        if (userId == null) {
            log.warn("[Permission] User ID가 없음 - JWT 매핑 실패");
            return false;
        }

        try {
            Set<String> permissions = rbacModuleApi.getEffectivePermissions(userId);
            boolean hasPermission = permissions.contains(permission.toString());

            log.debug("[Permission] 권한 확인 (타겟 포함) - userId: {}, targetType: {}, targetId: {}, permission: {}, granted: {}",
                userId, targetType, targetId, permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("[Permission] 권한 확인 중 오류 - userId: {}, permission: {}, error: {}",
                userId, permission, e.getMessage());
            return false;
        }
    }
}




