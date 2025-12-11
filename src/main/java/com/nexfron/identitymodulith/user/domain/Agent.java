package com.nexfron.identitymodulith.user.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class Agent {

    private final UUID id;
    private String username;
    private String passwordHash;
    private String name;
    private UUID organizationId;
    private AgentStatus status;
    private boolean passwordMustChange;
    private final LocalDateTime createdAt;
    private LocalDateTime retiredAt;
    private final Set<Role> roles;
    private final Set<Skill> skills;

    @Builder
    public Agent(UUID id, String username, String passwordHash, String name,
                 UUID organizationId, AgentStatus status, boolean passwordMustChange,
                 LocalDateTime createdAt, LocalDateTime retiredAt,
                 Set<Role> roles, Set<Skill> skills) {
        this.id = id != null ? id : UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
        this.organizationId = organizationId;
        this.status = status != null ? status : AgentStatus.ACTIVE;
        this.passwordMustChange = passwordMustChange;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.retiredAt = retiredAt;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.skills = skills != null ? new HashSet<>(skills) : new HashSet<>();
    }

    // 비밀번호 초기화 (관리자가 리셋)
    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordMustChange = true;
    }

    // 비밀번호 변경 (본인이 변경)
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordMustChange = false;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void transferOrganization(UUID newOrganizationId) {
        this.organizationId = newOrganizationId;
    }

    public void retire() {
        this.status = AgentStatus.RETIRED;
        this.retiredAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == AgentStatus.ACTIVE;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public void addSkill(Skill skill) {
        this.skills.add(skill);
    }

    public void removeSkill(Skill skill) {
        this.skills.remove(skill);
    }
}