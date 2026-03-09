package com.identitymodulith.user.infrastructure.persistence.repository;

import com.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentJpaRepository extends JpaRepository<AgentJpaEntity, String> {

    Optional<AgentJpaEntity> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<AgentJpaEntity> findByDeptId(String deptId);

    List<AgentJpaEntity> findByDeptIdAndStatus(String deptId, String status);

    List<AgentJpaEntity> findByStatus(String status);

    Optional<AgentJpaEntity> findByTenantIdAndAgentId(String tenantId, String agentId);

    /**
     * 테넌트와 로그인 ID로 상담사를 조회합니다 (JWT 매핑용)
     */
    Optional<AgentJpaEntity> findByTenantIdAndLoginId(String tenantId, String loginId);

    List<AgentJpaEntity> findByTenantId(String tenantId);

    /**
     * 예약 삭제 대상 조회 (스케줄러용)
     * status='RETIRED' AND scheduledDeleteAt <= now AND scheduledDeleteAt IS NOT NULL
     */
    @Query("SELECT a FROM AgentJpaEntity a WHERE a.status = :status AND a.scheduledDeleteAt <= :now AND a.scheduledDeleteAt IS NOT NULL")
    List<AgentJpaEntity> findByStatusAndScheduledDeleteAtBefore(@Param("status") String status, @Param("now") LocalDateTime now);

    /**
     * 이름에 특정 키워드가 포함된 상담사를 검색합니다.
     */
    List<AgentJpaEntity> findByNameContaining(String name);

    /**
     * 로그인 ID에 특정 키워드가 포함된 상담사를 검색합니다.
     */
    List<AgentJpaEntity> findByLoginIdContaining(String loginId);

    /**
     * 테넌트와 상태로 상담사를 필터링합니다.
     */
    List<AgentJpaEntity> findByTenantIdAndStatus(String tenantId, String status);

    /**
     * 테넌트와 조직 ID로 상담사를 조회합니다.
     */
    List<AgentJpaEntity> findByTenantIdAndDeptId(String tenantId, String deptId);

    /**
     * 상태별 카운트 조회
     */
    long countByStatus(String status);
}
