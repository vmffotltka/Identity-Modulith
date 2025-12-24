package com.nexfron.identitymodulith.user.infrastructure.persistence;

import com.nexfron.identitymodulith.user.domain.model.Agent;
import com.nexfron.identitymodulith.user.domain.model.AgentStatus;
import com.nexfron.identitymodulith.user.domain.repository.AgentRepository;
import com.nexfron.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
import com.nexfron.identitymodulith.user.infrastructure.persistence.repository.AgentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AgentRepository 인터페이스의 JPA 구현체
 *
 * Domain Layer의 AgentRepository 인터페이스를 구현하여
 * DIP(의존성 역전 원칙)를 적용합니다.
 */
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {

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
    public List<Agent> findByOrganizationId(String organizationId) {
        return jpaRepository.findByDeptId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findByOrganizationIdAndStatus(String organizationId, AgentStatus status) {
        return jpaRepository.findByDeptIdAndStatus(organizationId, status.name()).stream()
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

    @Override
    public Optional<Agent> findByTenantIdAndAgentId(String tenantId, UUID agentId) {
        return jpaRepository.findByTenantIdAndAgentId(tenantId, agentId.toString())
                .map(mapper::toDomain);
    }
}
