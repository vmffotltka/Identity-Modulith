package com.identitymodulith.user.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.Agent.Role;
import com.identitymodulith.user.domain.model.AgentStatus;
import com.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
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
