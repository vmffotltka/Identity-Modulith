package com.identitymodulith.user.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_agents")
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

    @Column(name = "employee_id", length = 30)
    private String employeeId;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "dept_id", length = 36)
    private String deptId;

    @Column(length = 20)
    private String status;

    @Column(name = "password_must_change")
    private Boolean passwordMustChange;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "scheduled_delete_at")
    private LocalDateTime scheduledDeleteAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Integer version;


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
