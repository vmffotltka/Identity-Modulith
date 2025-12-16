package com.nexfron.identitymodulith.organization.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.nexfron.identitymodulith.organization.common.exception.InvalidDepartmentMoveException;
import java.time.LocalDateTime;

/**
 * Department
 *
 * 역할:
 * - 조직 트리의 한 노드를 표현하는 도메인 엔티티
 *
 * 핵심 규칙:
 * - parent / depth / orgPath 는 항상 일관성을 유지해야 한다.
 * - orgPath 는 문자열 기반 트리 탐색을 위한 비즈니스 키이다.
 *
 * 제약:
 * - DB 스키마 변경 불가
 * - ID 생성 이후(@PostPersist)에 orgPath 를 확정한다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "departments")
public class Department {

    private static final String TEMP_PATH = "/temp";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId;

    @Column(nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    @Column(nullable = false)
    private String name;

    @Column(name = "org_path", nullable = false)
    private String orgPath;

    @Column(nullable = false)
    private Integer depth;

    private String type;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public static Department create(String tenantId, String name, String type, Department parent) {
        Department dept = new Department();
        dept.tenantId = tenantId;
        dept.name = name;
        dept.type = type;
        dept.changeParent(parent);
        return dept;
    }

    /**
     * 부모 변경 (조직 이동)
     *
     * 규칙:
     * - 자신의 하위 부서로는 이동할 수 없다.
     * - parent 변경 시 depth / orgPath 는 함께 재계산된다.
     */
    public void changeParent(Department newParent) {
        if (this.orgPath != null
                && newParent != null
                && newParent.getOrgPath() != null
                && newParent.getOrgPath().startsWith(this.orgPath)) {
            throw new InvalidDepartmentMoveException(
                    "자신의 하위 부서로 이동할 수 없습니다."
            );
        }

        this.parent = newParent;
        this.depth = (newParent == null) ? 0 : newParent.getDepth() + 1;
        updatePath();
    }

    /**
     * orgPath 재계산
     *
     * 호출 전제:
     * - parent 가 정상적으로 설정되어 있어야 한다.
     * - 신규 엔티티의 경우 deptId 는 null 일 수 있다.
     *
     * 주의:
     * - 외부에서 직접 호출하지 말 것
     */
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
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
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
}
