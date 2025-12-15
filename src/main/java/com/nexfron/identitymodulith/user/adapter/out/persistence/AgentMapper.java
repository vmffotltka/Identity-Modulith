package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.AgentStatus;
import com.nexfron.identitymodulith.user.domain.Role;
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
                .loginId(agent.getLoginId())
                .password(agent.getPassword())
                .name(agent.getName())
                .deptId(agent.getOrganizationId() != null ? agent.getOrganizationId().toString() : null)
                .status(agent.getStatus().name())
                .createdAt(agent.getCreatedAt())
                .roleId(rolesToJson(agent.getRoles()))
                .build();
    }

    public Agent toDomain(AgentJpaEntity entity) {
        return Agent.builder()
                .id(UUID.fromString(entity.getAgentId()))
                .loginId(entity.getLoginId())
                .password(entity.getPassword())
                .name(entity.getName())
                .organizationId(entity.getDeptId() != null ? UUID.fromString(entity.getDeptId()) : null)
                .status(AgentStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
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
