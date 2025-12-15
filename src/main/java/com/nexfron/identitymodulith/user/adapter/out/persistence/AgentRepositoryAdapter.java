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
        return jpaRepository.findById(id.toString())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Agent> findByLoginId(String loginId) {
        return jpaRepository.findByLoginId(loginId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return jpaRepository.existsByLoginId(loginId);
    }

    @Override
    public List<Agent> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByDeptId(organizationId.toString()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findByOrganizationIdAndStatus(UUID organizationId, AgentStatus status) {
        return jpaRepository.findByDeptIdAndStatus(organizationId.toString(), status.name()).stream()
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
        return jpaRepository.findByStatus(status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
