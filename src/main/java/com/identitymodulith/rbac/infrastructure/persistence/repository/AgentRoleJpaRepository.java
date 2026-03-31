package com.identitymodulith.rbac.infrastructure.persistence.repository;

import com.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/** 에이전트-역할 매핑 조회/삭제 리포지토리. */
public interface AgentRoleJpaRepository extends JpaRepository<AgentRoleJpaEntity, Long> {

    List<AgentRoleJpaEntity> findByAgentId(String agentId);

    List<AgentRoleJpaEntity> findByRoleId(String roleId);

    @Query("SELECT ar.roleId FROM AgentRoleJpaEntity ar WHERE ar.agentId = :agentId")
    Set<String> findRoleIdsByAgentId(@Param("agentId") String agentId);

    boolean existsByAgentIdAndRoleId(String agentId, String roleId);

    long countByRoleId(String roleId);

    void deleteByAgentId(String agentId);

    void deleteByRoleId(String roleId);

    void deleteByAgentIdAndRoleId(String agentId, String roleId);

    /**
     * N+1을 피하기 위해 상담사의 권한 코드를 3개 테이블 JOIN으로 한 번에 조회한다.
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
