package com.identitymodulith.rbac.infrastructure.persistence.repository;

import com.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 에이전트-역할 매핑 JPA Repository
 *
 * 에이전트(Agent)와 역할(Role)의 다대다 관계를 관리합니다.
 * 사용자에게 역할을 할당하거나 제거할 때 이 Repository를 사용합니다.
 *
 * 기본 메서드:
 * - save(AgentRoleJpaEntity): 에이전트-역할 매핑 추가
 * - delete(AgentRoleJpaEntity): 에이전트-역할 매핑 삭제
 * - findAll(): 모든 에이전트-역할 매핑 조회
 *
 * 커스텀 메서드들은 특정 조건에 맞는 데이터를 빠르게 조회하거나
 * 일괄 삭제할 때 사용됩니다.
 *
 * @see AgentRoleJpaEntity
 */
public interface AgentRoleJpaRepository extends JpaRepository<AgentRoleJpaEntity, Long> {

    /**
     * 특정 에이전트가 가진 모든 역할을 조회합니다.
     *
     * 사용 시나리오:
     * - 특정 사용자가 어떤 역할을 가지고 있는지 확인
     * - 사용자의 모든 역할 기반으로 권한을 조회할 때
     *
     * @param agentId 에이전트 ID
     * @return 에이전트에게 할당된 모든 역할 매핑
     *
     * @apiNote
     *  쿼리: SELECT * FROM agent_roles WHERE agent_id = ?
     */
    List<AgentRoleJpaEntity> findByAgentId(String agentId);

    /**
     * 특정 역할이 할당된 모든 에이전트를 조회합니다.
     *
     * 사용 시나리오:
     * - 특정 역할을 가진 모든 사용자 목록 조회
     * - 역할 변경 시 영향받을 사용자들 파악
     *
     * @param roleId 역할 ID
     * @return 해당 역할이 할당된 모든 에이전트의 매핑
     *
     * @apiNote
     *  쿼리: SELECT * FROM agent_roles WHERE role_id = ?
     */
    List<AgentRoleJpaEntity> findByRoleId(String roleId);

    /**
     * 특정 에이전트의 모든 역할 ID를 조회합니다.
     *
     * 사용 시나리오:
     * - 에이전트의 역할 ID 목록만 필요할 때 (성능 최적화)
     * - RbacQueryService에서 권한 조회 시 사용
     *
     * @param agentId 에이전트 ID
     * @return 에이전트에게 할당된 역할 ID 집합
     *
     * @apiNote
     *  쿼리: SELECT role_id FROM agent_roles WHERE agent_id = ?
     */
    @Query("SELECT ar.roleId FROM AgentRoleJpaEntity ar WHERE ar.agentId = :agentId")
    Set<String> findRoleIdsByAgentId(@Param("agentId") String agentId);

    /**
     * 특정 에이전트와 역할의 할당 관계를 확인합니다.
     *
     * 사용 시나리오:
     * - 사용자가 특정 역할을 가지고 있는지 확인
     * - 중복 할당 방지 전에 사전 확인
     *
     * @param agentId 에이전트 ID
     * @param roleId 역할 ID
     * @return true: 할당됨 / false: 미할당
     *
     * @apiNote
     *  쿼리: SELECT COUNT(*) FROM agent_roles WHERE agent_id = ? AND role_id = ?
     */
    boolean existsByAgentIdAndRoleId(String agentId, String roleId);

    /**
     * 특정 역할이 할당된 에이전트 수를 조회합니다.
     *
     * <h3>사용 시나리오:</h3>
     * <ul>
     *   <li>역할 삭제 전 영향받을 사용자 수 확인</li>
     *   <li>역할 통계 정보 제공</li>
     * </ul>
     *
     * @param roleId 역할 ID
     * @return 해당 역할이 할당된 에이전트 수
     *
     * @apiNote 쿼리: SELECT COUNT(*) FROM agent_roles WHERE role_id = ?
     */
    long countByRoleId(String roleId);

    /**
     * 특정 에이전트의 모든 역할 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 사용자를 삭제할 때 관련된 모든 역할 할당 제거
     * - 사용자의 모든 권한을 즉시 회수
     * - 주의: 이 작업은 돌이킬 수 없으므로 신중하게 사용해야 합니다.
     *
     * @param agentId 에이전트 ID
     *
     * @apiNote
     *  쿼리: DELETE FROM agent_roles WHERE agent_id = ?
     */
    void deleteByAgentId(String agentId);

    /**
     * 특정 역할이 할당된 모든 에이전트 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 역할을 삭제할 때 관련된 모든 에이전트 할당 제거
     * - 역할 구조 변경 시 기존 할당 정리
     * - 주의: 이 작업은 돌이킬 수 없으므로 신중하게 사용해야 합니다.
     *
     * @param roleId 역할 ID
     *
     * @apiNote
     *  쿼리: DELETE FROM agent_roles WHERE role_id = ?
     */
    void deleteByRoleId(String roleId);

    /**
     * 특정 에이전트와 역할의 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 사용자에게서 특정 역할 제거
     * - 부서 이동 시 불필요한 역할 회수
     *
     * @param agentId 에이전트 ID
     * @param roleId 역할 ID
     *
     * @apiNote
     *  쿼리: DELETE FROM agent_roles WHERE agent_id = ? AND role_id = ?
     */
    void deleteByAgentIdAndRoleId(String agentId, String roleId);

    /**
     * 상담사의 모든 권한 코드를 단일 3-JOIN 쿼리로 조회합니다. (N+1 완전 해결)
     *
     * agent_roles → role_permissions → permissions 를 한 번에 JOIN합니다.
     *
     * @param agentId  상담사 ID
     * @param tenantId 테넌트 ID
     * @return 권한 코드 목록 (중복 제거)
     */
    @Query("""
        SELECT DISTINCT p.code
        FROM AgentRoleJpaEntity ar
        JOIN RolePermissionJpaEntity rp ON ar.roleId = rp.roleId
        JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
        WHERE ar.agentId = :agentId
          AND p.tenantId = :tenantId
    """)
    List<String> findPermissionCodesByAgentIdAndTenant(@Param("agentId") String agentId,
                                                       @Param("tenantId") String tenantId);
}
