package com.nexfron.identitymodulith.rbac.application;

import java.util.List;
import java.util.Set;

/**
 * RBAC 관리 서비스 인터페이스
 *
 * 역할(Role)과 권한(Permission)의 생성/조회/삭제 및 관계 관리를 정의합니다.
 * RBAC(Role-Based Access Control) 시스템의 마스터 데이터 관리를 담당하며,
 * 구현체는 persistence 계층의 Repository를 활용하여 데이터를 관리합니다.
 *
 * 주요 책임:
 * 1. 역할(Role) 관리
 *    - 역할 생성/조회/삭제
 *    - 역할 타입 분류 (POSITION, CHANNEL, SKILL)
 *
 * 2. 권한(Permission) 관리
 *    - 권한 정의 생성/조회/삭제
 *    - 권한 코드 표준화 (domain:action 형식)
 *
 * 3. 역할-권한 관계 관리
 *    - 역할에 권한 할당/회수
 *    - 특정 역할이 가진 권한 조회
 *
 * 4. 사용자-역할 관계 관리
 *    - 사용자에게 역할 할당/회수
 *    - 특정 사용자가 가진 역할 조회
 *
 * 멀티테넌시 지원:
 * - 모든 역할/권한은 특정 테넌트에 속함
 * - tenantId별로 역할/권한을 분리하여 관리
 * - 같은 이름의 역할이라도 테넌트별로 독립적으로 관리
 *
 * 트랜잭션 관리:
 * - 권한/역할 관계 변경은 동일 트랜잭션 내에서 일관성 있게 처리
 * - 데이터 무결성 보장 (원자성)
 *
 * 호출 흐름:
 * Controller → Service → Repository → Database
 *
 * 사용 예시:
 * 1. 역할 생성: createRole("TEAM_LEADER", "POSITION")
 * 2. 권한 생성: createPermission("team:manage", "팀 관리")
 * 3. 역할에 권한 할당: assignPermissionToRole("TEAM_LEADER", "team:manage")
 * 4. 사용자에게 역할 할당: assignRoleToAgent("user123", "TEAM_LEADER")
 *
 * @see RbacQueryService 인가 결정용 쿼리 서비스
 * @see com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity
 * @see com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity
 * @see com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity
 * @see com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity
 */
public interface RbacManagementService {

    /**
     * 특정 테넌트의 모든 역할 조회
     *
     * @return 역할 DTO 목록 (역할명, 타입 등 포함)
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/roles
     * 응답: [
     *   { roleId: "550e...", name: "ADMIN", type: "POSITION", ... },
     *   { roleId: "550e...", name: "TEAM_LEADER", type: "POSITION", ... }
     * ]
     */
    List<RoleDto> getAllRoles();

    /**
     * 역할명으로 특정 역할 조회
     *
     * @param roleName 조회할 역할명 (예: "ADMIN", "TEAM_LEADER")
     * @return 역할 정보 DTO
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 해당 역할이 존재하지 않을 때
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/roles/ADMIN
     * 응답: { roleId: "550e...", name: "ADMIN", type: "POSITION", ... }
     */
    RoleDto getRoleByName(String roleName);

    /**
     * 새로운 역할 생성
     *
     * 역할 생성 절차:
     * 1. 역할명 유일성 확인 (tenantId + name으로 중복 방지)
     * 2. 역할 ID(UUID) 자동 생성
     * 3. 역할 저장
     * 4. 생성된 역할 정보 반환
     *
     * @param request 역할 생성 요청
     *        - name: 역할명 (예: "ADMIN", "TEAM_LEADER")
     *        - type: 역할 타입 (POSITION, CHANNEL, SKILL 중 선택)
     *        - description: 역할 설명 (선택)
     * @return 생성된 역할의 DTO
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_ALREADY_EXISTS: 같은 이름의 역할이 이미 존재할 때
     *
     * @apiNote
     * 호출 예시: POST /api/rbac/roles
     * 요청: { name: "NEW_ROLE", type: "POSITION", description: "새로운 역할" }
     * 응답: { roleId: "550e...", name: "NEW_ROLE", type: "POSITION", ... }
     */
    RoleDto createRole(CreateRoleRequest request);

    /**
     * 특정 테넌트의 모든 권한 조회
     *
     * @return 권한 DTO 목록 (권한 코드, 설명 등 포함)
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/permissions
     * 응답: [
     *   { permissionId: "550e...", code: "user:create", description: "사용자 생성", ... },
     *   { permissionId: "550e...", code: "user:read", description: "사용자 조회", ... }
     * ]
     */
    List<PermissionDto> getAllPermissions();

    /**
     * 권한 코드로 특정 권한 조회
     *
     * @param code 권한 코드 (예: "user:manage", "org:view")
     * @return 권한 정보 DTO
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         PERMISSION_NOT_FOUND: 해당 권한이 존재하지 않을 때
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/permissions/user:manage
     * 응답: { permissionId: "550e...", code: "user:manage", description: "사용자 관리", ... }
     */
    PermissionDto getPermissionByCode(String code);

    /**
     * 새로운 권한 정의 생성
     *
     * 권한 생성 절차:
     * 1. 권한 코드 유일성 확인 (tenantId + code로 중복 방지)
     * 2. 권한 ID(UUID) 자동 생성
     * 3. 권한 저장
     * 4. 생성된 권한 정보 반환
     *
     * @param request 권한 생성 요청
     *        - code: 권한 코드 (예: "user:manage", "org:view")
     *        - description: 권한 설명 (예: "사용자 생성, 수정, 삭제 권한")
     * @return 생성된 권한의 DTO
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         PERMISSION_ALREADY_EXISTS: 같은 코드의 권한이 이미 존재할 때
     *
     * @apiNote
     * 호출 예시: POST /api/rbac/permissions
     * 요청: { code: "org:manage", description: "조직 관리" }
     * 응답: { permissionId: "550e...", code: "org:manage", description: "조직 관리", ... }
     */
    PermissionDto createPermission(CreatePermissionRequest request);

    /**
     * 특정 역할에 권한 할당
     *
     * 할당 절차:
     * 1. 역할 존재 확인
     * 2. 권한 존재 확인
     * 3. 중복 할당 확인 (이미 할당된 권한인지)
     * 4. RolePermission 엔티티 생성 및 저장
     * 5. assigned_at에 현재 시간 기록 (감시 추적용)
     *
     * 할당 결과:
     * - 해당 역할을 가진 모든 사용자가 자동으로 이 권한을 갖게 됨
     * - 예: TEAM_LEADER에 "team:manage" 할당
     *   → 모든 TEAM_LEADER 사용자가 "team:manage" 권한 획득
     *
     * @param roleName 대상 역할명 (예: "ADMIN")
     * @param permissionCode 할당할 권한 코드 (예: "user:manage")
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 역할이 존재하지 않을 때
     *         PERMISSION_NOT_FOUND: 권한이 존재하지 않을 때
     *         PERMISSION_ALREADY_ASSIGNED: 이미 할당된 역할-권한 조합일 때
     *
     * @apiNote
     * 호출 예시: POST /api/rbac/roles/ADMIN/permissions/user:manage
     * 결과: ADMIN 역할에 user:manage 권한이 할당됨
     */
    void assignPermissionToRole(String roleName, String permissionCode);

    /**
     * 특정 역할에서 권한 회수
     *
     * 회수 절차:
     * 1. 역할 존재 확인
     * 2. 권한 존재 확인
     * 3. RolePermission 관계 삭제
     * 4. deleted_at 또는 실제 삭제 처리
     *
     * 회수 결과:
     * - 해당 역할을 가진 모든 사용자가 이 권한을 잃게 됨
     * - 예: MEMBER에서 "org:delete" 회수
     *   → 모든 MEMBER 사용자가 "org:delete" 권한 상실
     *
     * @param roleName 대상 역할명 (예: "MEMBER")
     * @param permissionCode 회수할 권한 코드 (예: "org:delete")
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 역할이 존재하지 않을 때
     *         PERMISSION_NOT_FOUND: 권한이 존재하지 않을 때
     *         PERMISSION_ALREADY_ASSIGNED: 할당되지 않은 역할-권한 조합일 때
     *
     * @apiNote
     * 호출 예시: DELETE /api/rbac/roles/MEMBER/permissions/org:delete
     * 결과: MEMBER 역할에서 org:delete 권한이 회수됨
     */
    void revokePermissionFromRole(String roleName, String permissionCode);

    /**
     * 특정 역할이 가진 모든 권한 조회
     *
     * 조회 경로:
     * roles → (N:M) → role_permissions → (N:M) → permissions
     *
     * @param roleName 조회 대상 역할명 (예: "ADMIN")
     * @return 권한 DTO 집합 (예: {"user:manage", "org:manage", "report:export"})
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 해당 역할이 존재하지 않을 때
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/roles/ADMIN/permissions
     * 응답: [
     *   { permissionId: "550e...", code: "user:manage", ... },
     *   { permissionId: "550e...", code: "org:manage", ... }
     * ]
     */
    Set<PermissionDto> getPermissionsByRole(String roleName);

    /**
     * 특정 역할 삭제
     *
     * 삭제 절차:
     * 1. 역할 존재 확인
     * 2. 해당 역할에 할당된 권한이 있는지 확인
     * 3. 역할을 할당받은 사용자가 있는지 확인
     * 4. 카스케이드 삭제 또는 경고 메시지 반환
     * 5. 역할 및 관련 매핑 데이터 삭제
     *
     * 삭제 영향:
     * - role_permissions에서 해당 역할의 모든 권한 할당 삭제
     * - agent_roles에서 해당 역할을 할당받은 모든 사용자의 매핑 삭제
     * - 이로 인해 해당 사용자들의 권한이 변경될 수 있음
     *
     * @param roleName 삭제할 역할명 (예: "DEPRECATED_ROLE")
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 역할이 존재하지 않을 때
     *         ROLE_HAS_USERS: 사용자에게 할당된 역할이라 삭제할 수 없을 때
     *
     * @apiNote
     * 호출 예시: DELETE /api/rbac/roles/DEPRECATED_ROLE
     * 결과: 역할 및 관련 모든 매핑 데이터 삭제
     *
     * 주의: 삭제하기 전에 영향받는 사용자 수를 확인하는 것이 권장됨
     */
    void deleteRole(String roleName);

    /**
     * 특정 권한 삭제
     *
     * 삭제 절차:
     * 1. 권한 존재 확인
     * 2. 해당 권한이 할당된 역할이 있는지 확인
     * 3. role_permissions에서 모든 매핑 삭제
     * 4. 권한 자체 삭제
     *
     * 삭제 영향:
     * - role_permissions에서 해당 권한의 모든 역할 할당 삭제
     * - 이로 인해 그 역할을 가진 모든 사용자의 권한이 변경될 수 있음
     * - 예: "org:delete" 삭제 시 이 권한을 가진 모든 역할의 사용자에게서 제거
     *
     * @param code 삭제할 권한 코드 (예: "deprecated:action")
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         PERMISSION_NOT_FOUND: 권한이 존재하지 않을 때
     *
     * @apiNote
     * 호출 예시: DELETE /api/rbac/permissions/deprecated:action
     * 결과: 권한 및 관련 모든 매핑 데이터 삭제
     *
     * 주의: 삭제하기 전에 영향받는 역할/사용자 수를 확인하는 것이 권장됨
     */
    void deletePermission(String code);

    // ========== 사용자-역할 관리 메서드 ==========

    /**
     * 사용자에게 역할을 할당합니다.
     *
     * 할당 절차:
     * 1. Agent(사용자) 존재 확인
     * 2. Role(역할) 존재 확인
     * 3. 중복 할당 확인 (이미 할당된 역할이면 예외)
     * 4. Agent-Role 매핑 생성 및 저장
     * 5. 사용자의 권한이 즉시 업데이트됨 (캐시 무효화 필요)
     *
     * 할당 결과:
     * - 사용자가 해당 역할에 할당됨
     * - 그 역할이 가진 모든 권한이 사용자에게 부여됨
     * - 예: Agent "user-001"에 "TEAM_LEADER" 역할 할당
     *   → Agent는 TEAM_LEADER의 모든 권한 획득
     *
     * @param agentId 대상 사용자 ID (UUID 문자열 형식)
     * @param roleName 할당할 역할명 (예: "ADMIN", "TEAM_LEADER")
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 역할이 존재하지 않을 때
     *         ROLE_ALREADY_ASSIGNED: 사용자가 이미 해당 역할을 가지고 있을 때
     *
     * @apiNote
     * 호출 예시: POST /api/rbac/agents/550e8400-e29b-41d4-a716-446655440000/roles
     * 요청: { roleName: "TEAM_LEADER" }
     * 결과: 사용자가 TEAM_LEADER 역할 획득
     */
    void assignRoleToAgent(String agentId, String roleName);

    /**
     * 사용자에게서 역할을 회수합니다.
     *
     * 회수 절차:
     * 1. Agent(사용자) 존재 확인
     * 2. Role(역할) 존재 확인
     * 3. Agent-Role 매핑 확인
     * 4. 매핑 삭제
     * 5. 사용자의 권한이 즉시 업데이트됨
     *
     * 회수 결과:
     * - 사용자가 해당 역할을 더 이상 가지지 않음
     * - 그 역할이 가진 권한을 더 이상 보유하지 않음 (다른 역할에서 제공하지 않는 경우)
     * - 예: Agent "user-001"에서 "TEAM_LEADER" 역할 회수
     *   → Agent는 TEAM_LEADER의 권한 상실
     *
     * @param agentId 대상 사용자 ID (UUID 문자열 형식)
     * @param roleName 회수할 역할명 (예: "TEAM_LEADER")
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 역할이 존재하지 않을 때
     *
     * @apiNote
     * 호출 예시: DELETE /api/rbac/agents/550e8400-e29b-41d4-a716-446655440000/roles/TEAM_LEADER
     * 결과: 사용자가 TEAM_LEADER 역할 상실
     */
    void revokeRoleFromAgent(String agentId, String roleName);

    /**
     * 사용자가 가지고 있는 모든 역할 조회
     *
     * 조회 경로:
     * agents → (N:M) → agent_roles → (N:M) → roles
     *
     * @param agentId 조회할 사용자 ID (UUID 문자열 형식)
     * @return 사용자가 할당받은 역할 DTO 집합
     *         할당된 역할이 없으면 빈 Set 반환
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/agents/550e8400-e29b-41d4-a716-446655440000/roles
     * 응답: [
     *   { name: "TEAM_LEADER", type: "POSITION" },
     *   { name: "CHANNEL_MANAGER", type: "CHANNEL" }
     * ]
     */
    Set<RoleDto> getRolesByAgent(String agentId);

    /**
     * 특정 역할을 가진 사용자 수 조회
     *
     * 용도:
     * - 역할 삭제 전 영향받을 사용자 수 파악
     * - 역할 사용 통계 파악
     *
     * @param roleName 조회할 역할명 (예: "ADMIN")
     * @return 해당 역할을 가진 사용자 수
     *         할당된 사용자가 없으면 0 반환
     *
     * @throws com.nexfron.identitymodulith.rbac.application.exception.RbacException
     *         ROLE_NOT_FOUND: 역할이 존재하지 않을 때
     *
     * @apiNote
     * 호출 예시: GET /api/rbac/roles/ADMIN/agent-count
     * 응답: { count: 5 }
     */
    int getAgentCountByRole(String roleName);

    // ========== DTO 정의 ==========

    /**
     * 역할 정보 DTO
     * @param name 역할명 (예: "ADMIN", "TEAM_LEADER")
     * @param type 역할 타입 (POSITION, CHANNEL, SKILL)
     */
    record RoleDto(String name, String type) {}

    /**
     * 권한 정보 DTO
     * @param code 권한 코드 (예: "user:manage", "org:view")
     */
    record PermissionDto(String code) {}

    /**
     * 역할 생성 요청 DTO
     * @param name 생성할 역할명
     * @param type 생성할 역할의 타입
     */
    record CreateRoleRequest(String name, String type) {}

    /**
     * 권한 생성 요청 DTO
     * @param code 생성할 권한 코드
     */
    record CreatePermissionRequest(String code) {}
}
