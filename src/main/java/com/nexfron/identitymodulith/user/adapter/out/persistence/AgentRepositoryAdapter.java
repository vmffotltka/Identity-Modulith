package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.application.port.out.AgentRepository;
import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.AgentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AgentRepositoryAdapter implements AgentRepository {

    private final AgentJpaRepository jpaRepository;
    private final AgentMapper mapper;

    @Override
    public Agent save(Agent agent) {
        AgentJpaEntity entity = mapper.toJpaEntity(agent);
        AgentJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Agent> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Agent> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public List<Agent> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findByOrganizationIdAndStatus(UUID organizationId, AgentStatus status) {
        return jpaRepository.findByOrganizationIdAndStatus(organizationId, status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findAllByStatus(AgentStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}