package com.nexfron.identitymodulith.user.adapter.in.web.dto;

import com.nexfron.identitymodulith.user.application.port.in.GetAgentUseCase.AgentInfo;
import com.nexfron.identitymodulith.user.domain.AgentStatus;
import com.nexfron.identitymodulith.user.domain.Role;
import com.nexfron.identitymodulith.user.domain.Skill;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class AgentResponse {

    private UUID id;
    private String username;
    private String name;
    private UUID organizationId;
    private AgentStatus status;
    private boolean passwordMustChange;
    private LocalDateTime createdAt;
    private LocalDateTime retiredAt;
    private Set<RoleDto> roles;
    private Set<String> skills;

    public static AgentResponse from(AgentInfo info) {
        return AgentResponse.builder()
                .id(info.getId())
                .username(info.getUsername())
                .name(info.getName())
                .organizationId(info.getOrganizationId())
                .status(info.getStatus())
                .passwordMustChange(info.isPasswordMustChange())
                .createdAt(info.getCreatedAt())
                .retiredAt(info.getRetiredAt())
                .roles(info.getRoles().stream()
                        .map(RoleDto::from)
                        .collect(Collectors.toSet()))
                .skills(info.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Getter
    @Builder
    public static class RoleDto {
        private String name;
        private Role.RoleType type;

        public static RoleDto from(Role role) {
            return RoleDto.builder()
                    .name(role.getName())
                    .type(role.getType())
                    .build();
        }
    }
}