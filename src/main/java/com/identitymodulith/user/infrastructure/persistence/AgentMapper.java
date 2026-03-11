package com.identitymodulith.user.infrastructure.persistence;

import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.AgentStatus;
import com.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AgentMapper {

    public AgentJpaEntity toJpaEntity(Agent agent) {
        return AgentJpaEntity.builder()
                .agentId(agent.getId().toString())
                .tenantId(agent.getTenantId())
                .loginId(agent.getLoginId())
                .password(agent.getPassword())
                .name(agent.getName())
                .employeeId(agent.getEmployeeId())
                .email(agent.getEmail())
                .phone(agent.getPhone())
                .deptId(agent.getOrganizationId())
                .status(agent.getStatus().name())
                .passwordMustChange(agent.isPasswordMustChange())
                .suspendedAt(agent.getSuspendedAt())
                .retiredAt(agent.getRetiredAt())
                .scheduledDeleteAt(agent.getScheduledDeleteAt())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .createdBy(agent.getCreatedBy())
                .updatedBy(agent.getUpdatedBy())
                .version(agent.getVersion() != null ? agent.getVersion().intValue() : 0)
                .build();
    }

    public Agent toDomain(AgentJpaEntity entity) {
        return Agent.builder()
                .id(UUID.fromString(entity.getAgentId()))
                .tenantId(entity.getTenantId())
                .loginId(entity.getLoginId())
                .password(entity.getPassword())
                .name(entity.getName())
                .employeeId(entity.getEmployeeId())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .organizationId(entity.getDeptId())
                .status(AgentStatus.valueOf(entity.getStatus()))
                .passwordMustChange(entity.getPasswordMustChange() != null && entity.getPasswordMustChange())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .suspendedAt(entity.getSuspendedAt())
                .retiredAt(entity.getRetiredAt())
                .scheduledDeleteAt(entity.getScheduledDeleteAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion() != null ? entity.getVersion().longValue() : 0L)
                .build();
    }
}