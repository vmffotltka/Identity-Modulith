package com.nexfron.identitymodulith.user.application.port.out;

import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.AgentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository {

    Agent save(Agent agent);

    Optional<Agent> findById(UUID id);

    Optional<Agent> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<Agent> findByOrganizationId(String organizationId);

    List<Agent> findByOrganizationIdAndStatus(String organizationId, AgentStatus status);

    List<Agent> findAll();

    List<Agent> findAllByStatus(AgentStatus status);
}