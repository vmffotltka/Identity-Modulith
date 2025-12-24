package com.nexfron.identitymodulith.user.domain.repository;

import com.nexfron.identitymodulith.user.domain.model.Agent;
import com.nexfron.identitymodulith.user.domain.model.AgentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 상담사(Agent) Repository 인터페이스
 * <p>
 * DDD의 DIP(의존성 역전 원칙)에 따라 Domain Layer에 위치합니다.
 * Infrastructure Layer에서 이 인터페이스를 구현합니다.
 * </p>
 */
public interface AgentRepository {

    Agent save(Agent agent);

    Optional<Agent> findById(UUID id);

    Optional<Agent> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<Agent> findByOrganizationId(String organizationId);

    List<Agent> findByOrganizationIdAndStatus(String organizationId, AgentStatus status);

    List<Agent> findAll();

    List<Agent> findAllByStatus(AgentStatus status);

    // 추가: tenantId 와 agentId 를 동시에 where 조건으로 사용하는 조회 메서드
    Optional<Agent> findByTenantIdAndAgentId(String tenantId, UUID agentId);
}
