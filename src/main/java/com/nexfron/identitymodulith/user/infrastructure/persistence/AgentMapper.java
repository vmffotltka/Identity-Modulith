package com.nexfron.identitymodulith.user.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.AgentStatus;
import com.nexfron.identitymodulith.user.domain.Role;
import com.nexfron.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class AgentMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentJpaEntity toJpaEntity(Agent agent) {
        return AgentJpaEntity.builder()
                .agentId(agent.getId().toString())
                .tenantId(agent.getTenantId())
                .loginId(agent.getLoginId())
                .password(agent.getPassword())
                .name(agent.getName())
                .deptId(agent.getOrganizationId())
                .status(agent.getStatus().name())
                .createdAt(agent.getCreatedAt())
                .retiredAt(agent.getRetiredAt())
                .passwordMustChange(agent.isPasswordMustChange())
                .roleId(rolesToJson(agent.getRoles()))
                .build();
    }

    public Agent toDomain(AgentJpaEntity entity) {
        return Agent.builder()
                .id(UUID.fromString(entity.getAgentId()))
                .tenantId(entity.getTenantId())
                .loginId(entity.getLoginId())
                .password(entity.getPassword())
                .name(entity.getName())
                .organizationId(entity.getDeptId())
                .status(AgentStatus.valueOf(entity.getStatus()))
                .passwordMustChange(entity.getPasswordMustChange() != null && entity.getPasswordMustChange())
                .createdAt(entity.getCreatedAt())
                .retiredAt(entity.getRetiredAt())
                .roles(jsonToRoles(entity.getRoleId()))
                .build();
    }

    private String rolesToJson(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        try {
            List<RoleDto> roleDtos = roles.stream()
                    .map(role -> new RoleDto(role.getName(), role.getType()))
                    .toList();
            return objectMapper.writeValueAsString(roleDtos);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize roles to JSON", e);
        }
    }

    private Set<Role> jsonToRoles(String rolesJson) {
        if (rolesJson == null || rolesJson.isBlank()) {
            return new HashSet<>();
        }
        try {
            List<RoleDto> roleDtos = objectMapper.readValue(rolesJson, new TypeReference<>() {});
            Set<Role> roles = new HashSet<>();
            for (RoleDto dto : roleDtos) {
                roles.add(new Role(dto.name(), dto.type()));
            }
            return roles;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize roles from JSON", e);
        }
    }

    private record RoleDto(String name, Role.RoleType type) {}
}
