package com.identitymodulith.organization.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.identitymodulith.organization.application.exception.OrganizationException;
import com.identitymodulith.organization.application.exception.OrganizationException.OrganizationErrorCode;
import com.identitymodulith.organization.domain.OrganizationConstants;
import com.identitymodulith.organization.domain.model.DepartmentStatus;
import com.identitymodulith.organization.domain.model.DepartmentType;
import java.time.LocalDateTime;

/** 조직 계층(부모/깊이/경로) 일관성을 유지하는 부서 엔티티. */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "org_departments")
public class DepartmentEntity {

    private static final String TEMP_PATH = "/temp";

    @Id
    @Column(name = "dept_id", length = 36)
    private String deptId;

    @Column(nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DepartmentEntity parent;

    @Column(nullable = false)
    private String name;

    @Column(name = "org_path", nullable = false)
    private String orgPath;

    @Column(nullable = false)
    private Integer depth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DepartmentType type;

    @Column(length = 30, nullable = false)
    private String code;

    @Column(name = "custom_type_name", length = 50)
    private String customTypeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    public static DepartmentEntity create(String tenantId, String name, DepartmentType type,
                                          String code, String customTypeName, DepartmentEntity parent) {
        DepartmentEntity dept = new DepartmentEntity();
        dept.deptId = java.util.UUID.randomUUID().toString();
        dept.tenantId = tenantId;
        dept.name = name;
        dept.type = type;
        dept.code = code;
        dept.customTypeName = customTypeName;
        dept.changeParent(parent);
        return dept;
    }

    public void changeParent(DepartmentEntity newParent) {
        // 하위 노드를 부모로 지정하는 순환 참조를 방지한다.
        if (this.orgPath != null
                && newParent != null
                && newParent.getOrgPath() != null
                && newParent.getOrgPath().startsWith(this.orgPath)) {
            throw new OrganizationException(
                    OrganizationErrorCode.CIRCULAR_REFERENCE,
                    "자신의 하위 부서로 이동할 수 없습니다."
            );
        }

        this.parent = newParent;
        this.depth = (newParent == null) ? 0 : newParent.getDepth() + 1;

        if (this.depth > OrganizationConstants.MAX_ORGANIZATION_DEPTH) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "조직 깊이가 최대 허용치(" + OrganizationConstants.MAX_ORGANIZATION_DEPTH +
                    ")를 초과합니다. 현재 깊이: " + this.depth
            );
        }

        updatePath();
    }

    public void setOrgPath(String orgPath) {
        this.orgPath = orgPath;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public void updatePath() {
        if (this.parent == null) {
            this.orgPath = (this.deptId == null)
                    ? TEMP_PATH
                    : "/" + this.deptId;
        } else {
            String parentPath = parent.getOrgPath() != null
                    ? parent.getOrgPath()
                    : TEMP_PATH;

            this.orgPath = (this.deptId == null)
                    ? parentPath + TEMP_PATH
                    : parentPath + "/" + this.deptId;
        }

        if (this.orgPath.length() > OrganizationConstants.MAX_ORG_PATH_LENGTH) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "조직 경로 길이가 최대 허용치(" + OrganizationConstants.MAX_ORG_PATH_LENGTH +
                    "자)를 초과합니다. 현재 길이: " + this.orgPath.length()
            );
        }
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.depth == null) {
            this.depth = (parent == null) ? 0 : parent.getDepth() + 1;
        }
        if (this.orgPath == null) {
            this.orgPath = TEMP_PATH;
        }
    }

    @PostPersist
    void postPersist() {
        updatePath();
    }

    public void deactivate() {
        if (this.status == DepartmentStatus.INACTIVE) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "이미 비활성화된 부서입니다."
            );
        }
        this.status = DepartmentStatus.INACTIVE;
    }

    public void activate() {
        if (this.status == DepartmentStatus.ACTIVE) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "이미 활성화된 부서입니다."
            );
        }
        this.status = DepartmentStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == DepartmentStatus.ACTIVE;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public void updateInfo(String name, DepartmentType type) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (type != null) {
            this.type = type;
        }
    }
}
