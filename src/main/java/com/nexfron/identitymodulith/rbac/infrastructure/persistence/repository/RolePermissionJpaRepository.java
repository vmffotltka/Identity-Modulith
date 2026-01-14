package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 역할-권한 매핑 JPA Repository
 *
 * 역할(Role)과 권한(Permission)의 다대다 관계를 관리합니다.
 * 역할에 권한을 할당하거나 제거할 때 이 Repository를 사용합니다.
 *
 * 기본 메서드:
 * - save(RolePermissionJpaEntity): 역할-권한 매핑 추가
 * - delete(RolePermissionJpaEntity): 역할-권한 매핑 삭제
 * - findAll(): 모든 역할-권한 매핑 조회
 *
 * 커스텀 메서드들은 특정 조건에 맞는 데이터를 빠르게 조회하거나
 * 일괄 삭제할 때 사용됩니다.
 *
 * @see RolePermissionJpaEntity
 */
public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {

    /**
     * 특정 역할의 모든 권한을 조회합니다.
     *
     * 사용 시나리오:
     * - 특정 역할이 어떤 권한을 가지고 있는지 확인
     * - 역할에 포함된 모든 권한 조회
     *
     * @param roleId 역할 ID
     * @return 역할에 할당된 모든 권한 ID 집합
     *
     * @apiNote
     *  쿼리: SELECT permission_id FROM role_permissions WHERE role_id = ?
     */
    @Query("SELECT rp.permissionId FROM RolePermissionJpaEntity rp WHERE rp.roleId = :roleId")
    Set<String> findPermissionIdsByRoleId(@Param("roleId") String roleId);

    /**
     * 주어진 역할 ID들에 대한 모든 권한을 조회합니다.
     *
     * 사용 시나리오:
     * - 여러 역할을 가진 사용자의 모든 권한을 한 번에 조회할 때
     * - 역할 ID 목록: ["role1", "role2"] -> 두 역할의 모든 권한을 조회
     *
     * @param roleIds 역할 ID 컬렉션 (Collection은 List, Set 등 모든 컬렉션 타입 가능)
     * @return 매칭되는 모든 역할-권한 매핑 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM role_permissions WHERE role_id IN (?)
     */
    List<RolePermissionJpaEntity> findByRoleIdIn(Collection<String> roleIds);

    /**
     * 특정 역할에서 특정 권한의 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 역할에서 특정 권한 하나를 제거할 때
     * - 예: ADMIN 역할에서 "user:delete" 권한 제거
     *
     * @param roleId 역할 ID
     * @param permissionId 권한 ID
     *
     * @apiNote
     *  쿼리: DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?
     */
    void deleteByRoleIdAndPermissionId(String roleId, String permissionId);

    /**
     * 특정 역할과 권한이 이미 매핑되어 있는지 확인합니다.
     *
     * 사용 시나리오:
     * - 역할에 권한을 할당하기 전에 중복 할당 여부 확인
     * - 같은 권한을 두 번 할당하는 것을 방지
     *
     * @param roleId 역할 ID
     * @param permissionId 권한 ID
     * @return 매핑 존재 여부
     *
     * @apiNote
     *  쿼리: SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_id = ?
     */
    boolean existsByRoleIdAndPermissionId(String roleId, String permissionId);

    /**
     * 특정 역할의 모든 권한 엔티티를 조회합니다.
     *
     * 사용 시나리오:
     * - 역할이 가진 모든 권한 정보를 조회할 때
     * - 권한 ID뿐만 아니라 권한의 코드, 설명 등도 필요할 때
     *
     * @param roleId 역할 ID
     * @return 권한 엔티티 리스트
     */
    @Query("""
        SELECT DISTINCT p FROM PermissionJpaEntity p 
        WHERE p.permissionId IN (
            SELECT rp.permissionId FROM RolePermissionJpaEntity rp 
            WHERE rp.roleId = :roleId
        )
    """)
    List<com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity>
        findPermissionsByRoleId(@Param("roleId") String roleId);

    /**
     * 특정 역할의 모든 권한 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 역할을 삭제하기 전에 관련된 모든 권한 매핑을 삭제할 때
     * - 주의: 이 작업은 돌이킬 수 없으므로 신중하게 사용해야 합니다.
     *
     * @param roleId 역할 ID
     *
     * @apiNote
     *  쿼리: DELETE FROM role_permissions WHERE role_id = ?
     *
     * @see com.nexfron.identitymodulith.rbac.application.RbacManagementService#deleteRole(String)
     */
    void deleteByRoleId(String roleId);

    /**
     * 특정 권한이 할당된 모든 역할-권한 매핑을 제거합니다.
     *
     * 사용 시나리오:
     * - 권한을 삭제하기 전에 관련된 모든 역할 매핑을 삭제할 때
     * - 권한 업그레이드나 이름 변경 시 기존 매핑 정리
     * - 주의: 이 작업은 돌이킬 수 없으므로 신중하게 사용해야 합니다.
     *
     * @param permissionId 권한 ID
     *
     * @apiNote
     *  쿼리: DELETE FROM role_permissions WHERE permission_id = ?
     *
     * @see com.nexfron.identitymodulith.rbac.application.RbacManagementService#deletePermission(String)
     */
    void deleteByPermissionId(String permissionId);
}


