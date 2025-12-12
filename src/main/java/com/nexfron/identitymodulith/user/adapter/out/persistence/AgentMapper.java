package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.Role;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AgentMapper {

    public AgentJpaEntity toJpaEntity(Agent agent) {
        AgentJpaEntity entity = AgentJpaEntity.builder()
                .id(agent.getId())
                .username(agent.getLoginId())
                .passwordHash(agent.getPasswordHash())
                .name(agent.getName())
                .organizationId(agent.getOrganizationId())
                .status(agent.getStatus())
                .passwordMustChange(agent.isPasswordMustChange())
                .createdAt(agent.getCreatedAt())
                .retiredAt(agent.getRetiredAt())
                .build();

        // Map roles
        agent.getRoles().forEach(role -> {
            AgentRoleJpaEntity roleEntity = AgentRoleJpaEntity.builder()
                    .agent(entity)
                    .roleName(role.getName())
                    .roleType(role.getType())
                    .build();
            entity.getRoles().add(roleEntity);
        });

        return entity;
    }

    public Agent toDomain(AgentJpaEntity entity) {
        return Agent.builder()
                .id(entity.getId())
                .loginId(entity.getUsername())
                .passwordHash(entity.getPasswordHash())
                .name(entity.getName())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .passwordMustChange(entity.isPasswordMustChange())
                .createdAt(entity.getCreatedAt())
                .retiredAt(entity.getRetiredAt())
                .roles(entity.getRoles().stream()
                        .map(r -> new Role(r.getRoleName(), r.getRoleType()))
                        .collect(Collectors.toSet()))
                .build();
    }
}