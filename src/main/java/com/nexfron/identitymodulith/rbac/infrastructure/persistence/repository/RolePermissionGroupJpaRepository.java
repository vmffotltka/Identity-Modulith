package com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 역할-권한 그룹 매핑 JPA Repository
 *
 * 역할과 권한 그룹의 다대다 관계를 관리합니다.
 */
public interface RolePermissionGroupJpaRepository extends JpaRepository<RolePermissionGroupJpaEntity, Long> {

    /**
     * 특정 역할에 할당된 모든 권한 그룹 ID 조회
     *
     * @param roleId 역할 ID
     * @return 권한 그룹 ID 리스트
     */
    List<RolePermissionGroupJpaEntity> findByRoleId(String roleId);

    /**
     * 특정 권한 그룹이 할당된 모든 역할 조회
     *
     * @param permissionGroupId 권한 그룹 ID
     * @return 매핑 리스트
     */
    List<RolePermissionGroupJpaEntity> findByPermissionGroupId(String permissionGroupId);

    /**
     * 역할-그룹 할당 존재 확인
     *
     * @param roleId 역할 ID
     * @param permissionGroupId 권한 그룹 ID
     * @return 존재 여부
     */
    boolean existsByRoleIdAndPermissionGroupId(String roleId, String permissionGroupId);

    /**
     * 역할의 모든 그룹 할당 삭제 (역할 삭제 시 사용)
     *
     * @param roleId 역할 ID
     */
    void deleteByRoleId(String roleId);

    /**
     * 그룹이 할당된 모든 역할 매핑 삭제 (그룹 삭제 시 사용)
     *
     * @param permissionGroupId 권한 그룹 ID
     */
    void deleteByPermissionGroupId(String permissionGroupId);

    /**
     * 특정 역할-그룹 할당 삭제
     *
     * @param roleId 역할 ID
     * @param permissionGroupId 권한 그룹 ID
     */
    void deleteByRoleIdAndPermissionGroupId(String roleId, String permissionGroupId);

    /**
     * 특정 역할에 할당된 모든 권한 그룹 ID 조회 (쿼리 최적화)
     *
     * @param roleId 역할 ID
     * @return 권한 그룹 ID 리스트
     */
    @Query("SELECT rpg.permissionGroupId FROM RolePermissionGroupJpaEntity rpg WHERE rpg.roleId = :roleId")
    List<String> findPermissionGroupIdsByRoleId(@Param("roleId") String roleId);
}

