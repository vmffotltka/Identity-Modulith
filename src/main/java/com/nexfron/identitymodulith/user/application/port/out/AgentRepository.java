package com.nexfron.identitymodulith.user.application.port.out;

import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.AgentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository {

    Agent save(Agent agent);

    Optional<Agent> findById(UUID id);

    Optional<Agent> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Agent> findByOrganizationId(UUID organizationId);

    List<Agent> findByOrganizationIdAndStatus(UUID organizationId, AgentStatus status);

    List<Agent> findAll();

    List<Agent> findAllByStatus(AgentStatus status);
}