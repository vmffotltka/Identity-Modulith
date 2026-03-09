package com.identitymodulith.user.infrastructure.persistence;

import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.AgentStatus;
import com.identitymodulith.user.infrastructure.persistence.repository.AgentRepository;
import com.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
import com.identitymodulith.user.infrastructure.persistence.repository.AgentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Override
    public Optional<Agent> findByIdAndTenantId(UUID agentId, String tenantId) {
        return jpaRepository.findByTenantIdAndAgentId(tenantId, agentId.toString())
                .map(mapper::toDomain);
    }

    @Override
    public List<Agent> findByTenantId(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findRetiredWithScheduledDelete(LocalDateTime beforeDateTime) {
        // 퇴직 상태(RETIRED)이고 퇴직일시가 주어진 시간 이전인 상담사 조회
        return jpaRepository.findByStatus(AgentStatus.RETIRED.name()).stream()
                .filter(entity -> entity.getRetiredAt() != null && entity.getRetiredAt().isBefore(beforeDateTime))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findAgentsForScheduledDelete(AgentStatus status, LocalDateTime now) {
        return jpaRepository.findByStatusAndScheduledDeleteAtBefore(status.name(), now).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(AgentStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public List<Agent> findByNameContaining(String nameKeyword) {
        return jpaRepository.findByNameContaining(nameKeyword).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findByLoginIdContaining(String loginIdKeyword) {
        return jpaRepository.findByLoginIdContaining(loginIdKeyword).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findByTenantIdAndStatus(String tenantId, AgentStatus status) {
        return jpaRepository.findByTenantIdAndStatus(tenantId, status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Agent> findByTenantIdAndOrganizationId(String tenantId, String organizationId) {
        return jpaRepository.findByTenantIdAndDeptId(tenantId, organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
