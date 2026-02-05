package com.nexfron.identitymodulith.user.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_agents")  // V1_0_20: 표준 명명 규칙 적용 (agents → user_agents)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AgentJpaEntity {

    @Id
    @Column(name = "agent_id", length = 36)
    private String agentId;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "login_id", length = 100, unique = true, nullable = false)
    private String loginId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String password;

    // ========== V1_0_15: 추가된 연락처 정보 ==========
    @Column(name = "employee_id", length = 30)
    private String employeeId;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    // ========== 부서 정보 ==========
    @Column(name = "dept_id", length = 36)
    private String deptId;

    // ========== 상태 관리 ==========
    @Column(length = 20)
    private String status;

    @Column(name = "password_must_change")
    private Boolean passwordMustChange;

    // ========== V1_0_15: 추가된 상태 추적 ==========
    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "scheduled_delete_at")
    private LocalDateTime scheduledDeleteAt;

    // ========== 감사 추적 ==========
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== V1_0_15: 추가된 감사 컬럼 ==========
    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    // ========== V1_0_15: 낙관적 잠금 ==========
    @Version
    @Column(name = "version")
    private Integer version;

    // ========== 임시: 역할 JSON (agent_roles 테이블로 대체 예정) ==========
    // V1_0_15에서 제거 예정이지만 기존 코드 호환성을 위해 유지
    @Column(name = "role_id", length = 50)
    @Deprecated
    private String roleId;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
