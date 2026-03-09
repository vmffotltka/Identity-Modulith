package com.identitymodulith.rbac.application.service;

import com.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;

import java.util.Set;
import java.util.UUID;

/**
 * RBAC 쿼리 서비스 인터페이스
 *
 * 역할(Role) 기반으로 권한(Permission) 코드를 빠르게 조회하는 서비스입니다.
 * 이는 주로 인가(Authorization) 체크 시에 사용됩니다.
 *
 * 데이터 표준:
 * - role_name: "ADMIN", "TEAM_LEADER", "MEMBER" 등 (대문자, 언더스코어 조합)
 * - permission_code: "user:create", "org:view", "report:export" 등 (도메인:액션)
 * - agent_id: UUID 문자열 형식 (예: "550e8400-e29b-41d4-a716-446655440000")
 * - tenant_id: "tenant-001", "acme-corp" 등
 *
 * @see RolePermissionJpaRepository
 * @see AgentRoleJpaRepository
 */
public interface RbacQueryService {

    /**
     * 주어진 역할명들에 대한 모든 권한 코드를 조회합니다.
     *
     * 사용 시나리오:
     * - 여러 역할이 할당된 사용자의 통합 권한 계산
     * - 역할 집합: {ADMIN, TEAM_LEADER} -> 권한 코드: {user:create, org:view, ...}
     *
     * @param roleNames 역할명 집합 (예: {ADMIN, MANAGER})
     * @return 권한 코드 집합 (예: {user:create, user:read, org:manage})
     *
     * @apiNote
     *  내부: SELECT DISTINCT permission_code FROM role_permissions rp
     *        JOIN roles r ON rp.role_id = r.role_id
     *        JOIN permissions p ON rp.permission_id = p.permission_id
     *        WHERE r.name IN (?)
     */
    Set<String> permissionsOfRoles(Set<String> roleNames);

    /**
     * 특정 테넌트에서 역할명 집합에 대한 권한 코드 집합을 조회합니다.
     *
     * @param tenantId  테넌트 ID
     * @param roleNames 역할명 집합
     * @return 권한 코드 집합
     */
    default Set<String> permissionsOfRoles(String tenantId, Set<String> roleNames) {
        // 기본 구현: 멀티테넌트가 아닌 환경에서는 기존 메서드로 위임
        return permissionsOfRoles(roleNames);
    }

    /**
     * 특정 테넌트의 에이전트(사용자)가 가진 모든 권한 코드를 조회합니다.
     *
     * 사용 시나리오:
     * - 사용자 로그인/인증 후 권한 계산
     * - 특정 기능/API 접근 허가 결정 전 권한 조회
     * - 예: agentId="550e8400...", tenantId="tenant-001" -> 권한 코드 반환
     *
     * 조회 경로:
     * 1. agent_roles 테이블에서 agentId의 모든 roleId 조회
     * 2. 각 roleId에 대해 role_permissions 테이블에서 permissionId 조회
     * 3. permissions 테이블에서 해당 permission의 code 조회
     *
     * @param tenantId 테넌트 ID (멀티테넌시 필터링)
     * @param agentId 에이전트 ID (UUID 문자열 또는 UUID 타입)
     * @return 권한 코드 집합 (예: {user:read, user:update, org:view})
     *         권한이 없으면 빈 Set 반환
     *
     * @apiNote
     *  내부: SELECT DISTINCT p.code
     *        FROM agent_roles ar
     *        JOIN roles r ON ar.role_id = r.role_id
     *        JOIN role_permissions rp ON r.role_id = rp.role_id
     *        JOIN permissions p ON rp.permission_id = p.permission_id
     *        WHERE ar.agent_id = ? AND r.tenant_id = ?
     */
    Set<String> permissionsOf(String tenantId, UUID agentId);
}