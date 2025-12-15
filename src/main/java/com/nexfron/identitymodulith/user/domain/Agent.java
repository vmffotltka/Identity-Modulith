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
    private String loginId;
    private String password;
    private String name;
    private String organizationId;
    private AgentStatus status;
    private boolean passwordMustChange;
    private final LocalDateTime createdAt;
    private LocalDateTime retiredAt;
    private final Set<Role> roles;

    @Builder
    public Agent(UUID id, String loginId, String password, String name,
                 String organizationId, AgentStatus status, boolean passwordMustChange,
                 LocalDateTime createdAt, LocalDateTime retiredAt,
                 Set<Role> roles) {
        this.id = id != null ? id : UUID.randomUUID();
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.organizationId = organizationId;
        this.status = status != null ? status : AgentStatus.ACTIVE;
        this.passwordMustChange = passwordMustChange;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.retiredAt = retiredAt;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }

    // 비밀번호 초기화 (관리자가 리셋)
    public void resetPassword(String newPassword) {
        this.password = newPassword;
        this.passwordMustChange = true;
    }

    // 비밀번호 변경 (본인이 변경)
    public void changePassword(String newPassword) {
        this.password = newPassword;
        this.passwordMustChange = false;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void transferOrganization(String newOrganizationId) {
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
}