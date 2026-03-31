package com.identitymodulith.user.infrastructure.persistence.repository;

import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.AgentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 상담사 도메인 조회/저장 계약. */
public interface AgentRepository {

    Agent save(Agent agent);

    Optional<Agent> findById(UUID id);

    Optional<Agent> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<Agent> findByOrganizationId(String organizationId);

    List<Agent> findByOrganizationIdAndStatus(String organizationId, AgentStatus status);

    List<Agent> findAll();

    List<Agent> findAllByStatus(AgentStatus status);

    /** 테넌트 격리 조건으로 상담사를 조회한다. */
    Optional<Agent> findByIdAndTenantId(UUID agentId, String tenantId);

    Optional<Agent> findByTenantIdAndAgentId(String tenantId, UUID agentId);

    List<Agent> findRetiredWithScheduledDelete(LocalDateTime beforeDateTime);

    List<Agent> findAgentsForScheduledDelete(AgentStatus status, LocalDateTime now);

    long countByStatus(AgentStatus status);

    List<Agent> findByTenantId(String tenantId);

    List<Agent> findByNameContaining(String nameKeyword);

    List<Agent> findByLoginIdContaining(String loginIdKeyword);

    List<Agent> findByTenantIdAndStatus(String tenantId, AgentStatus status);

    List<Agent> findByTenantIdAndOrganizationId(String tenantId, String organizationId);

    /** 여러 조직 ID를 단일 IN 조건으로 조회한다. */
    List<Agent> findByTenantIdAndOrganizationIdsAndStatus(String tenantId, List<String> organizationIds, AgentStatus status);
}
