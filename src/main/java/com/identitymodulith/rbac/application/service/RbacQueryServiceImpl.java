package com.identitymodulith.rbac.application.service;

import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC 쿼리 서비스 구현체
 * 역할: 사용자(Agent)의 권한 조회, 역할별 권한 조회
 * 설계: agent_roles + role_permissions 조인하여 권한 조회, 성능 최적화를 위해 캐시 적용
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>permissionsOf() - 특정 사용자의 모든 권한 조회 (권한 검증에 사용)</li>
 *   <li>permissionsOfRoles() - 여러 역할의 통합 권한 조회</li>
 * </ul>
 *
 * @see RbacQueryService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacQueryServiceImpl implements RbacQueryService {

    private final AgentRoleJpaRepository agentRoleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final RoleJpaRepository roleRepository;

    /**
     * 주어진 역할명들에 대한 모든 권한 코드 조회
     *
     * <h3>동작 흐름:</h3>
     * <ol>
     *   <li>역할명 집합 → 역할 ID 집합으로 변환</li>
     *   <li>각 역할 ID에 해당하는 권한 ID 조회</li>
     *   <li>권한 ID 집합 → 권한 코드 집합으로 변환</li>
     * </ol>
     *
     * <h3>사용 시나리오:</h3>
     * 여러 역할이 할당된 사용자의 통합 권한 계산
     * - 역할 입력: {ADMIN, TEAM_LEADER}
     * - 권한 산출: {user:create, user:read, org:view, team:manage, ...}
     *
     * <h3>데이터 구조:</h3>
     * role_permissions 테이블에서:
     * - role_id = 조회할 여러 role_id들
     * - 결과: 모든 permission_id 합집합
     * - 최종: 합집합의 permission 코드
     *
     * @param roleNames 역할명 집합 (예: {ADMIN, TEAM_LEADER})
     * @return 권한 코드 집합 (예: {user:manage, org:view, team:manage})
     *         여러 역할의 권한을 모두 포함하는 합집합
     *
     * @see RbacQueryService
     * @see RolePermissionJpaRepository
     */
    @Override
    public Set<String> permissionsOfRoles(Set<String> roleNames) {
        // 테넌트 정보가 없는 구 버전 메서드: 현재는 사용처가 없으므로 안전하게 빈 Set 반환
        if (roleNames == null || roleNames.isEmpty()) {
            log.trace("역할 목록이 비어있습니다 (tenantId 미지정)");
            return Set.of();
        }
        // 멀티테넌시 환경에서는 반드시 tenantId 포함 오버로드를 사용할 것을 가이드한다.
        log.warn("permissionsOfRoles(Set<String>)는 멀티테넌시 환경에서 권장되지 않습니다. tenantId가 포함된 오버로드를 사용하세요. roleNames={}", roleNames);
        return Set.of();
    }

    /**
     * 테넌트 + 역할명 집합에 대한 권한 코드 집합 조회 구현 (성능 최적화됨)
     *
     * 개선 사항:
     * - 기존: 3개 쿼리 (roles 조회 + role_permissions 조회 + permissions 조회)
     * - 개선: 2개 쿼리 (roles 조회 + JOIN으로 권한 코드 한 번에 조회)
     */
    @Override
    public Set<String> permissionsOfRoles(String tenantId, Set<String> roleNames) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId는 필수입니다.");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            log.trace("[RBAC] permissionsOfRoles - 역할 목록이 비어 있습니다: tenantId={}", tenantId);
            return Set.of();
        }

        long startTime = System.currentTimeMillis();
        log.debug("[RBAC] 테넌트 내 역할 기반 권한 조회 시작: tenantId={}, roleNames={}", tenantId, roleNames);

        // 1) 테넌트 내에서 역할명 집합에 해당하는 역할 엔티티 조회
        List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(tenantId, roleNames);
        if (roles.isEmpty()) {
            log.debug("[RBAC] 해당 테넌트에서 일치하는 역할이 없습니다: tenantId={}, roleNames={}", tenantId, roleNames);
            return Set.of();
        }

        Set<String> roleIds = roles.stream()
                .map(RoleJpaEntity::getRoleId)
                .collect(Collectors.toSet());

        // 2) DTO 프로젝션으로 권한 코드 한 번에 조회 (성능 최적화)
        List<String> permissionCodes = rolePermissionRepository
                .findPermissionCodesByRoleIdsAndTenant(roleIds, tenantId);

        if (permissionCodes.isEmpty()) {
            log.debug("[RBAC] 역할은 있으나 권한 매핑이 없습니다: tenantId={}, roleNames={}", tenantId, roleNames);
            return Set.of();
        }

        Set<String> codes = new HashSet<>(permissionCodes);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC] permissionsOfRoles 완료 (최적화): tenantId={}, roles={}, roleCount={}, permissionCount={}, 소요시간={}ms",
                tenantId, roleNames, roleIds.size(), codes.size(), duration);

        return codes;
    }

    /**
     * 특정 에이전트(사용자)가 보유한 모든 권한 코드 조회
     *
     * <h3>동작 흐름 (3-JOIN 단일 조회):</h3>
     * agent_roles → role_permissions → permissions 를 단일 JOIN 쿼리로 조회합니다.
     *
     * <h3>권한 조회 SQL (conceptual):</h3>
     * <pre>
     * SELECT DISTINCT p.code
     * FROM agent_roles ar
     *   JOIN roles r ON ar.role_id = r.role_id
     *   JOIN role_permissions rp ON r.role_id = rp.role_id
     *   JOIN permissions p ON rp.permission_id = p.permission_id
     * WHERE ar.agent_id = ? AND r.tenant_id = ?
     * </pre>
     *
     * <h3>사용 예시:</h3>
     * <pre>
     * UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
     * Set<String> permissions = rbacQueryService.permissionsOf("tenant-001", userId);
     * // 결과: {"user:create", "user:read", "org:view", "team:manage"}
     * </pre>
     *
     * @param tenantId  테넌트 ID (멀티테넌시 격리, 다른 테넌트의 권한 혼입 방지)
     * @param agentId   에이전트(사용자) ID (UUID 형식)
     *
     * @return 권한 코드 집합 (예: {"user:create", "user:read", "org:view"})
     *         - 사용자가 직접 또는 역할을 통해 보유한 모든 권한 포함
     *         - 사용자가 할당된 역할이 없으면 빈 Set 반환
     *         - 역할은 있지만 권한이 없으면 빈 Set 반환
     *
     * @see RbacQueryService
     * @see AgentRoleJpaRepository
     * @see RolePermissionJpaRepository
     */
    @Override
    public Set<String> permissionsOf(String tenantId, UUID agentId) {
        if (tenantId == null || tenantId.isBlank() || agentId == null) {
            log.warn("[RBAC] 권한 조회 입력이 올바르지 않습니다: tenantId={}, agentId={} (빈 Set 반환)", tenantId, agentId);
            return Set.of();
        }
        long startTime = System.currentTimeMillis();

        // 단일 3-JOIN 쿼리로 권한 코드만 조회 (N+1 및 불필요한 엔티티 로딩 제거)
        List<String> permissionCodes = agentRoleRepository
                .findPermissionCodesByAgentIdAndTenant(agentId.toString(), tenantId);

        if (permissionCodes.isEmpty()) {
            log.debug("[RBAC 권한 조회] 사용자에게 매핑된 권한이 없음: agentId={}, tenantId={}", agentId, tenantId);
            return Set.of();
        }

        Set<String> codes = new HashSet<>(permissionCodes);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 권한 조회 완료] agentId={}, tenantId={}, 권한 수={}, 소요시간={}ms",
                agentId, tenantId, codes.size(), duration);

        return codes;
    }
}
