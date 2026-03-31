package com.identitymodulith.rbac.infrastructure.persistence.repository;

import com.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** 역할-권한 매핑 조회/삭제 리포지토리. */
public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {

    @Query("SELECT rp.permissionId FROM RolePermissionJpaEntity rp WHERE rp.roleId = :roleId")
    Set<String> findPermissionIdsByRoleId(@Param("roleId") String roleId);

    List<RolePermissionJpaEntity> findByRoleId(String roleId);

    List<RolePermissionJpaEntity> findByPermissionId(String permissionId);

    List<RolePermissionJpaEntity> findByRoleIdIn(Collection<String> roleIds);

    void deleteByRoleIdAndPermissionId(String roleId, String permissionId);

    boolean existsByRoleIdAndPermissionId(String roleId, String permissionId);

    @Query("""
        SELECT DISTINCT p FROM PermissionJpaEntity p
        WHERE p.permissionId IN (
            SELECT rp.permissionId FROM RolePermissionJpaEntity rp
            WHERE rp.roleId = :roleId
          )
          AND p.tenantId = :tenantId
    """)
    List<PermissionJpaEntity>
        findPermissionsByRoleIdAndTenant(@Param("roleId") String roleId,
                                          @Param("tenantId") String tenantId);

    @Query("""
        SELECT rp.permissionId FROM RolePermissionJpaEntity rp
        WHERE rp.roleId IN :roleIds
    """)
    List<String> findPermissionIdsByRoleIds(@Param("roleIds") Collection<String> roleIds);

    /** tenantId 파라미터는 시그니처 호환을 위해 유지되며 현재 쿼리 본문에서는 사용하지 않는다. */
    @Query("""
        SELECT rp.permissionId FROM RolePermissionJpaEntity rp
        WHERE rp.roleId IN :roleIds
    """)
    List<String> findPermissionIdsByRoleIdsAndTenant(@Param("roleIds") Collection<String> roleIds,
                                                     @Param("tenantId") String tenantId);

    /** 권한 코드만 필요할 때 DTO 프로젝션으로 조회한다. */
    @Query("""
        SELECT p.code 
        FROM RolePermissionJpaEntity rp
        JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
        WHERE rp.roleId = :roleId 
          AND p.tenantId = :tenantId
    """)
    List<String> findPermissionCodesByRoleIdAndTenant(@Param("roleId") String roleId,
                                                       @Param("tenantId") String tenantId);

    /** 여러 역할의 권한 코드를 단일 JOIN 쿼리로 조회한다. */
    @Query("""
        SELECT DISTINCT p.code 
        FROM RolePermissionJpaEntity rp
        JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
        WHERE rp.roleId IN :roleIds 
          AND p.tenantId = :tenantId
    """)
    List<String> findPermissionCodesByRoleIdsAndTenant(@Param("roleIds") Collection<String> roleIds,
                                                        @Param("tenantId") String tenantId);

    /** 권한 코드에 매핑된 역할명을 JOIN 쿼리로 조회한다. */
    @Query("""
        SELECT DISTINCT r.name
        FROM RolePermissionJpaEntity rp
        JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
        JOIN RoleJpaEntity r ON rp.roleId = r.roleId
        WHERE p.code = :permissionCode
          AND p.tenantId = :tenantId
          AND r.tenantId = :tenantId
    """)
    List<String> findRoleNamesByPermissionCodeAndTenant(@Param("permissionCode") String permissionCode,
                                                        @Param("tenantId") String tenantId);

    void deleteByRoleId(String roleId);

    void deleteByPermissionId(String permissionId);
}
