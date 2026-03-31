package com.identitymodulith.user.domain.model;

import com.identitymodulith.user.domain.exception.BusinessException;
import com.identitymodulith.user.domain.exception.ErrorCode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 상담사 도메인 모델. */
@Getter
public class Agent {

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class Role {

        private final String name;
        private final RoleType type;

        public enum RoleType {
            POSITION,
            CHANNEL
        }
    }

    /** 퇴사 후 개인정보 처리 정책. */
    public enum RetireDeletePolicy {
        IMMEDIATE,
        SCHEDULED,
        PRESERVE
    }

    private final UUID id;
    private String tenantId;
    private String loginId;
    private String password;
    private String name;
    private String employeeId;
    private String email;
    private String phone;
    private String organizationId;
    private AgentStatus status;
    private boolean passwordMustChange;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime retiredAt;
    private LocalDateTime scheduledDeleteAt;

    private String createdBy;
    private String updatedBy;

    private Long version;

    private final Set<Role> roles;

    private RetireDeletePolicy retireDeletePolicy;

    @Builder
    public Agent(UUID id, String tenantId, String loginId, String password, String name,
                 String employeeId, String email, String phone,
                 String organizationId, AgentStatus status, boolean passwordMustChange,
                 LocalDateTime createdAt, LocalDateTime updatedAt,
                 LocalDateTime suspendedAt, LocalDateTime retiredAt, LocalDateTime scheduledDeleteAt,
                 String createdBy, String updatedBy,
                 Long version, Set<Role> roles, RetireDeletePolicy retireDeletePolicy) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.employeeId = employeeId;
        this.email = email;
        this.phone = phone;
        this.organizationId = organizationId;
        this.status = status != null ? status : AgentStatus.ACTIVE;
        this.passwordMustChange = passwordMustChange;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.suspendedAt = suspendedAt;
        this.retiredAt = retiredAt;
        this.scheduledDeleteAt = scheduledDeleteAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version != null ? version : 0L;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.retireDeletePolicy = retireDeletePolicy;
    }

    public void resetPassword(String newPassword) {
        validateNotRetired();
        this.password = newPassword;
        this.passwordMustChange = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String newPassword) {
        validateNotRetired();
        this.password = newPassword;
        this.passwordMustChange = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateName(String name) {
        validateNotRetired();
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateEmail(String email) {
        validateNotRetired();
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePhone(String phone) {
        validateNotRetired();
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    public void transferOrganization(String newOrganizationId) {
        validateNotRetired();
        this.organizationId = newOrganizationId;
        this.updatedAt = LocalDateTime.now();
    }

    /** ACTIVE 상태에서만 SUSPENDED로 전이한다. */
    public void suspend(String suspendedByUserId) {
        if (this.status != AgentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "ACTIVE 상태의 상담사만 정지할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = AgentStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
        this.updatedBy = suspendedByUserId;
        this.updatedAt = LocalDateTime.now();
    }

    /** SUSPENDED 상태에서만 ACTIVE로 복귀한다. */
    public void activate() {
        if (this.status != AgentStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "SUSPENDED 상태의 상담사만 활성화할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = AgentStatus.ACTIVE;
        this.suspendedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** 퇴사 처리 후 정책에 따라 즉시 익명화 또는 예약 삭제를 설정한다. */
    public void retire(String retiredByUserId, RetireDeletePolicy deletePolicy, Integer retentionDays) {
        if (this.status == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "이미 퇴사 처리된 상담사입니다.");
        }

        this.status = AgentStatus.RETIRED;
        this.retiredAt = LocalDateTime.now();
        this.updatedBy = retiredByUserId;
        this.retireDeletePolicy = deletePolicy;
        this.updatedAt = LocalDateTime.now();

        if (deletePolicy == RetireDeletePolicy.SCHEDULED && retentionDays != null && retentionDays > 0) {
            this.scheduledDeleteAt = this.retiredAt.plusDays(retentionDays);
        }

        if (deletePolicy == RetireDeletePolicy.IMMEDIATE) {
            anonymizePersonalInfo();
        }
    }

    public void anonymize() {
        anonymizePersonalInfo();
    }

    public void setScheduledDeleteAt(LocalDateTime scheduledDeleteAt) {
        this.scheduledDeleteAt = scheduledDeleteAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(String encodedPassword, boolean mustChange) {
        this.password = encodedPassword;
        this.passwordMustChange = mustChange;
        this.updatedAt = LocalDateTime.now();
    }

    /** 개인정보 비식별화 처리. */
    public void anonymizePersonalInfo() {
        this.loginId = "ANONYMOUS_" + this.id.toString().substring(0, 8);
        this.name = "Anonymous";
        this.email = null;
        this.phone = null;
        this.employeeId = null;
        this.password = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasMinimumRoles() {
        return !this.roles.isEmpty();
    }

    public void addRole(Role role) {
        validateNotRetired();
        this.roles.add(role);
        this.updatedAt = LocalDateTime.now();
    }

    /** 최소 1개 역할 유지 규칙을 적용한다. */
    public void removeRole(Role role) {
        validateNotRetired();
        if (this.roles.size() <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "상담사는 최소 1개 이상의 역할을 유지해야 합니다.");
        }
        this.roles.remove(role);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == AgentStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return this.status == AgentStatus.SUSPENDED;
    }

    public boolean isRetired() {
        return this.status == AgentStatus.RETIRED;
    }

    public boolean canLogin() {
        return this.status == AgentStatus.ACTIVE;
    }

    public boolean canReceiveAssignment() {
        return this.status == AgentStatus.ACTIVE;
    }

    private void validateNotRetired() {
        if (this.status == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "퇴사 처리된 상담사는 변경할 수 없습니다.");
        }
    }
}