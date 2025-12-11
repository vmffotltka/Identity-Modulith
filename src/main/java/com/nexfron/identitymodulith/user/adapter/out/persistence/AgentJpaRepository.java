package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.domain.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentJpaRepository extends JpaRepository<AgentJpaEntity, UUID> {

    Optional<AgentJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AgentJpaEntity> findByOrganizationId(UUID organizationId);

    List<AgentJpaEntity> findByOrganizationIdAndStatus(UUID organizationId, AgentStatus status);

    List<AgentJpaEntity> findByStatus(AgentStatus status);
}