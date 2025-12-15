package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.Role;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AgentMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentJpaEntity toJpaEntity(Agent agent) {
        return AgentJpaEntity.builder()
                .id(agent.getId())
                .loginId(agent.getLoginId())
                .passwordHash(agent.getPasswordHash())
                .name(agent.getName())
                .organizationId(agent.getOrganizationId())
                .status(agent.getStatus())
                .passwordMustChange(agent.isPasswordMustChange())
                .createdAt(agent.getCreatedAt())
                .retiredAt(agent.getRetiredAt())
                .roles(rolesToJson(agent.getRoles()))
                .build();
    }

    public Agent toDomain(AgentJpaEntity entity) {
        return Agent.builder()
                .id(entity.getId())
                .loginId(entity.getLoginId())
                .passwordHash(entity.getPasswordHash())
                .name(entity.getName())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .passwordMustChange(entity.isPasswordMustChange())
                .createdAt(entity.getCreatedAt())
                .retiredAt(entity.getRetiredAt())
                .roles(jsonToRoles(entity.getRoles()))
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