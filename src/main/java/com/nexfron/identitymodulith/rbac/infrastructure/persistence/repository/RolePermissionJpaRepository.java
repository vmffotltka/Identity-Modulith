package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

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
     * 주어진 역할명들에 대한 모든 역할-권한 매핑을 조회합니다.
     *
     * 사용 시나리오:
     * - 여러 역할을 가진 사용자의 모든 권한을 한 번에 조회할 때
     * - 역할명 목록: ["ADMIN", "TEAM_LEADER"] -> 두 역할의 모든 권한을 조회
     *
     * @param roleNames 역할명 컬렉션 (Collection은 List, Set 등 모든 컬렉션 타입 가능)
     * @return 매칭되는 모든 역할-권한 매핑 리스트
     *
     * @apiNote
     *  쿼리: SELECT * FROM role_permissions WHERE role_name IN (?)
     */
    List<RolePermissionJpaEntity> findByRoleNameIn(Collection<String> roleNames);

    /**
     * 특정 역할에서 특정 권한의 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 역할에서 특정 권한 하나를 제거할 때
     * - 예: ADMIN 역할에서 "user:delete" 권한 제거
     *
     * @param roleName 역할명
     * @param permissionCode 권한 코드
     *
     * @apiNote
     *  쿼리: DELETE FROM role_permissions WHERE role_name = ? AND permission_code = ?
     */
    void deleteByRoleNameAndPermissionCode(String roleName, String permissionCode);

    /**
     * 특정 역할의 모든 권한 할당을 제거합니다.
     *
     * 사용 시나리오:
     * - 역할을 삭제하기 전에 관련된 모든 권한 매핑을 삭제할 때
     * - 주의: 이 작업은 돌이킬 수 없으므로 신중하게 사용해야 합니다.
     *
     * @param roleName 역할명
     *
     * @apiNote
     *  쿼리: DELETE FROM role_permissions WHERE role_name = ?
     *
     * @see com.nexfron.identitymodulith.rbac.application.RbacManagementService#deleteRole(String)
     */
    void deleteByRoleName(String roleName);

    /**
     * 특정 권한이 할당된 모든 역할-권한 매핑을 제거합니다.
     *
     * 사용 시나리오:
     * - 권한을 삭제하기 전에 관련된 모든 역할 매핑을 삭제할 때
     * - 권한 업그레이드나 이름 변경 시 기존 매핑 정리
     * - 주의: 이 작업은 돌이킬 수 없으므로 신중하게 사용해야 합니다.
     *
     * @param permissionCode 권한 코드
     *
     * @apiNote
     *  쿼리: DELETE FROM role_permissions WHERE permission_code = ?
     *
     * @see com.nexfron.identitymodulith.rbac.application.RbacManagementService#deletePermission(String)
     */
    void deleteByPermissionCode(String permissionCode);
}


