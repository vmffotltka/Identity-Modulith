package com.nexfron.identitymodulith.user.domain.model;

import com.nexfron.identitymodulith.user.domain.exception.BusinessException;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 상담사(Agent) 도메인 모델
 * <p>
 * 상담사의 생명주기(Lifecycle), 상태 전이(State Transition), 역할 관리,
 * 개인정보 보호(Privacy) 등 상담사와 관련된 모든 비즈니스 규칙을 포함합니다.
 * </p>
 */
@Getter
public class Agent {

    /**
     * 역할(Role) Value Object
     * 상담사가 수행할 수 있는 역할을 나타냅니다.
     * (직급: POSITION, 채널: CHANNEL)
     */
    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class Role {

        private final String name;
        private final RoleType type;

        /**
         * 역할 타입
         * - POSITION: 직급 (사원, 주임, 과장 등)
         * - CHANNEL: 채널 (전화, 채팅, 이메일 등)
         */
        public enum RoleType {
            POSITION,  // 직급
            CHANNEL    // 채널 (전화, 채팅 등)
        }
    }

    /**
     * 퇴사 후 데이터 처리 정책
     * - IMMEDIATE: 즉시 익명화 (GDPR 대응)
     * - SCHEDULED: 일정 기간 후 자동 삭제
     * - PRESERVE: 데이터 영구 보존
     */
    public enum RetireDeletePolicy {
        IMMEDIATE,   // 즉시 익명화
        SCHEDULED,   // 스케줄 기반 삭제
        PRESERVE     // 데이터 영구 보존
    }

    // ========== Core Fields ==========
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

    // ========== Lifecycle Fields ==========
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime retiredAt;
    private LocalDateTime scheduledDeleteAt;

    // ========== Audit Fields ==========
    private String createdBy;
    private String updatedBy;
    private String suspendedBy;
    private String retiredBy;

    // ========== Optimistic Locking ==========
    private Long version;

    // ========== Relationships ==========
    private final Set<Role> roles;

    // ========== Retire Policy ==========
    private RetireDeletePolicy retireDeletePolicy;

    @Builder
    public Agent(UUID id, String tenantId, String loginId, String password, String name,
                 String organizationId, AgentStatus status, boolean passwordMustChange,
                 LocalDateTime createdAt, LocalDateTime retiredAt,
                 Set<Role> roles) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.organizationId = organizationId;
        this.status = status != null ? status : AgentStatus.ACTIVE;
        this.passwordMustChange = passwordMustChange;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.retiredAt = retiredAt;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.version = 0L;
    }

    // ========== Password Management ==========

    /**
     * 비밀번호를 초기화합니다 (관리자가 리셋).
     * 초기화 후 사용자는 다음 로그인 시 비밀번호 변경이 필요합니다.
     *
     * @param newPassword 새로운 비밀번호 (암호화된 상태로 전달되어야 함)
     */
    public void resetPassword(String newPassword) {
        validateNotRetired();
        this.password = newPassword;
        this.passwordMustChange = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 비밀번호를 변경합니다 (사용자가 직접 변경).
     * 변경 후 비밀번호 변경 필요 플래그는 해제됩니다.
     *
     * @param newPassword 새로운 비밀번호 (암호화된 상태로 전달되어야 함)
     */
    public void changePassword(String newPassword) {
        validateNotRetired();
        this.password = newPassword;
        this.passwordMustChange = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Basic Information Management ==========

    /**
     * 상담사 이름을 수정합니다.
     *
     * @param name 새로운 이름
     */
    public void updateName(String name) {
        validateNotRetired();
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상담사 이메일을 수정합니다.
     *
     * @param email 새로운 이메일
     */
    public void updateEmail(String email) {
        validateNotRetired();
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상담사 전화번호를 수정합니다.
     *
     * @param phone 새로운 전화번호
     */
    public void updatePhone(String phone) {
        validateNotRetired();
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상담사를 다른 부서로 이동시킵니다.
     *
     * @param newOrganizationId 새로 배정될 부서 ID
     */
    public void transferOrganization(String newOrganizationId) {
        validateNotRetired();
        this.organizationId = newOrganizationId;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Status Management ==========

    /**
     * 상담사를 정지(Suspend)합니다.
     * ACTIVE 상태만 정지 가능하며, 정지된 상담사는 activate()로 복귀 가능합니다.
     *
     * @param suspendedByUserId 정지를 수행한 관리자 ID
     * @throws BusinessException ACTIVE 상태가 아닌 경우
     */
    public void suspend(String suspendedByUserId) {
        if (this.status != AgentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "ACTIVE 상태의 상담사만 정지할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = AgentStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
        this.suspendedBy = suspendedByUserId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 정지된 상담사를 활성화합니다.
     * SUSPENDED 상태만 활성화 가능하며, RETIRED는 복구 불가능합니다.
     *
     * @param activatedByUserId 활성화를 수행한 관리자 ID
     * @throws BusinessException SUSPENDED 상태가 아닌 경우
     */
    public void activate() {
        if (this.status != AgentStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "SUSPENDED 상태의 상담사만 활성화할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = AgentStatus.ACTIVE;
        this.suspendedAt = null;
        this.suspendedBy = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상담사를 퇴사 처리합니다 (Soft Delete).
     * 모든 상태에서 퇴사 처리 가능하며, 퇴사 후 복구는 불가능합니다.
     * 퇴사 정책에 따라 데이터 처리 방식이 달라집니다.
     *
     * @param retiredByUserId 퇴사를 처리한 관리자 ID
     * @param deletePolicy 퇴사 후 데이터 처리 정책
     * @param retentionDays SCHEDULED 정책일 경우 보관 기간 (일 단위), nullable
     * @throws BusinessException 이미 RETIRED인 경우
     */
    public void retire(String retiredByUserId, RetireDeletePolicy deletePolicy, Integer retentionDays) {
        if (this.status == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "이미 퇴사 처리된 상담사입니다.");
        }

        this.status = AgentStatus.RETIRED;
        this.retiredAt = LocalDateTime.now();
        this.retiredBy = retiredByUserId;
        this.retireDeletePolicy = deletePolicy;
        this.updatedAt = LocalDateTime.now();

        // SCHEDULED 정책인 경우 삭제 예정일 계산
        if (deletePolicy == RetireDeletePolicy.SCHEDULED && retentionDays != null && retentionDays > 0) {
            this.scheduledDeleteAt = this.retiredAt.plusDays(retentionDays);
        }

        // IMMEDIATE 정책인 경우 즉시 익명화
        if (deletePolicy == RetireDeletePolicy.IMMEDIATE) {
            anonymizePersonalInfo();
        }
    }

    /**
     * 개인정보를 익명화합니다.
     * GDPR 등 개인정보 보호 규정 준수를 위해 사용됩니다.
     * 익명화 후 email, phone 등 개인정보는 복구 불가능합니다.
     */
    public void anonymize() {
        anonymizePersonalInfo();
    }

    /**
     * 예약 삭제 일시를 설정합니다.
     *
     * @param scheduledDeleteAt 삭제 예정일
     */
    public void setScheduledDeleteAt(LocalDateTime scheduledDeleteAt) {
        this.scheduledDeleteAt = scheduledDeleteAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 비밀번호를 업데이트합니다.
     * PC-003: 비밀번호 변경 시 passwordMustChange 플래그를 설정할 수 있습니다.
     *
     * @param encodedPassword 암호화된 비밀번호
     * @param mustChange 다음 로그인 시 비밀번호 변경 강제 여부
     */
    public void updatePassword(String encodedPassword, boolean mustChange) {
        this.password = encodedPassword;
        this.passwordMustChange = mustChange;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 개인정보를 익명화합니다.
     * GDPR 등 개인정보 보호 규정 준수를 위해 사용됩니다.
     * 익명화 후 email, phone 등 개인정보는 복구 불가능합니다.
     */
    public void anonymizePersonalInfo() {
        this.loginId = "ANONYMOUS_" + this.id.toString().substring(0, 8);
        this.name = "Anonymous";
        this.email = null;
        this.phone = null;
        this.employeeId = null;
        this.password = null;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Role Management ==========

    /**
     * 최소 1개의 역할을 유지해야 합니다.
     *
     * @return true if agent has at least one role
     */
    public boolean hasMinimumRoles() {
        return !this.roles.isEmpty();
    }

    /**
     * 상담사에게 역할을 추가합니다.
     *
     * @param role 추가할 역할
     */
    public void addRole(Role role) {
        validateNotRetired();
        this.roles.add(role);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상담사로부터 역할을 제거합니다.
     * 최소 1개 역할은 유지되어야 합니다.
     *
     * @param role 제거할 역할
     * @throws BusinessException 마지막 역할을 제거하려는 경우
     */
    public void removeRole(Role role) {
        validateNotRetired();
        if (this.roles.size() <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "상담사는 최소 1개 이상의 역할을 유지해야 합니다.");
        }
        this.roles.remove(role);
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Status Checks ==========

    /**
     * 상담사가 활성 상태인지 확인합니다.
     *
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return this.status == AgentStatus.ACTIVE;
    }

    /**
     * 상담사가 정지 상태인지 확인합니다.
     *
     * @return true if status is SUSPENDED
     */
    public boolean isSuspended() {
        return this.status == AgentStatus.SUSPENDED;
    }

    /**
     * 상담사가 퇴사 처리되었는지 확인합니다.
     *
     * @return true if status is RETIRED
     */
    public boolean isRetired() {
        return this.status == AgentStatus.RETIRED;
    }

    /**
     * 상담사가 로그인 가능한 상태인지 확인합니다.
     * ACTIVE 상태만 로그인 가능합니다.
     *
     * @return true if agent can login
     */
    /**
     * 상담사가 로그인 가능한 상태인지 확인합니다.
     * ACTIVE 상태에서만 로그인이 가능합니다.
     * 현재는 사용되지 않으나 향후 인증 서비스에 통합될 예정입니다.
     *
     * @return 로그인 가능하면 true
     */
    public boolean canLogin() {
        return this.status == AgentStatus.ACTIVE;
    }

    /**
     * 상담사가 상담을 배정받을 수 있는 상태인지 확인합니다.
     * ACTIVE 상태만 상담 배정 가능합니다.
     *
     * @return true if agent can receive assignment
     */
    /**
     * 상담사가 상담 배정을 받을 수 있는 상태인지 확인합니다.
     * ACTIVE 상태에서만 배정을 받을 수 있습니다.
     * 현재는 사용되지 않으나 향후 업무 배정 서비스에 통합될 예정입니다.
     *
     * @return 배정 가능하면 true
     */
    public boolean canReceiveAssignment() {
        return this.status == AgentStatus.ACTIVE;
    }

    // ========== Validation Helpers ==========

    /**
     * 퇴사 처리된 상담사는 대부분의 변경이 불가능합니다.
     *
     * @throws BusinessException 상담사가 RETIRED 상태인 경우
     */
    private void validateNotRetired() {
        if (this.status == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "퇴사 처리된 상담사는 변경할 수 없습니다.");
        }
    }
}