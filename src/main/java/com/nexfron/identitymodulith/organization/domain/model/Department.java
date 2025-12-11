package com.nexfron.identitymodulith.organization.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Department
 *
 * - 조직의 한 단위(부서)를 나타내는 엔티티
 * - 트리 구조를 유지하기 위해 parent / orgPath / depth 를 함께 관리
 * - orgPath 예: "/1/5/10"
 *   - 1 : 최상위 부서
 *   - 5 : 1의 하위 부서
 *   - 10: 5의 하위 부서
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "departments")
public class Department {

    /** 부서 ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId;

    /** 멀티 테넌시 구분용 tenant 키 */
    @Column(nullable = false)
    private String tenantId;

    /**
     * 상위 부서
     * - null 이면 최상위 부서
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    /** 부서 이름 */
    @Column(nullable = false)
    private String name;

    /**
     * 부서 트리 경로
     * - 예: "/1/5/10"
     * - parent, deptId 를 기준으로 계산됨
     */
    @Column(name = "org_path", nullable = false)
    private String orgPath;

    /**
     * 트리 상의 깊이
     * - 최상위: 0
     * - 그 하위: 1, ...
     */
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer depth;

    /** 부서 타입 (팀, 본부, BU 등) – ERD 의 type 컬럼 */
    private String type;

    /** 생성 시각 */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /* ========= 정적 팩토리 ========= */

    /**
     * 부서 생성용 정적 팩토리
     *
     * @param tenantId 테넌트 키
     * @param name     부서명
     * @param type     부서 타입
     * @param parent   상위 부서 (없으면 null)
     */
    public static Department create(String tenantId, String name, String type, Department parent) {
        Department dept = new Department();
        dept.tenantId = tenantId;
        dept.name = name;
        dept.type = type;
        // parent / depth / orgPath 를 한 번에 설정
        dept.changeParent(parent);
        return dept;
    }

    /* ========= 비즈니스 로직 ========= */

    /**
     * 부모 변경 + depth / orgPath 재계산
     *
     * - 신규 생성 시(this.orgPath == null)에는 순환 검사를 스킵
     *   -> 아직 자신이 트리 어딘지 확정 안 된 상태이기 때문
     * - 이미 저장된 부서를 이동할 때는
     *   "자신의 하위 부서로는 이동할 수 없다" 규칙을 강제
     */
    public void changeParent(Department newParent) {
        // 순환 참조 방지: newParent 가 나의 하위 부서인 경우 금지
        if (this.orgPath != null  // 이미 저장된 엔티티일 때만 검사
                && newParent != null
                && newParent.getOrgPath() != null
                && newParent.getOrgPath().startsWith(this.orgPath)) {
            throw new IllegalArgumentException("자신의 하위 부서로 이동할 수 없습니다.");
        }

        this.parent = newParent;

        // depth 재계산
        if (newParent == null) {
            this.depth = 0;
        } else {
            this.depth = newParent.getDepth() + 1;
        }

        // orgPath 재계산
        updatePath();
    }

    /**
     * 현재 parent / deptId / depth 를 기준으로 orgPath 재계산
     *
     * - 아직 ID 가 없는 상태(영속 전)에는 "/temp" 형태로 임시값을 사용
     * - 실제 ID 가 할당된 후(@PostPersist) 다시 한 번 호출되어 최종 경로를 만든다
     */
    public void updatePath() {
        if (this.parent == null) {
            // 최상위 부서
            this.orgPath = this.deptId == null
                    ? "/temp"           // 아직 ID 없는 상태(영속 전)
                    : "/" + this.deptId;
        } else {
            String parentPath = parent.getOrgPath();
            if (parentPath == null) {
                parentPath = "/temp";
            }
            this.orgPath = this.deptId == null
                    ? parentPath + "/temp"
                    : parentPath + "/" + this.deptId;
        }
    }

    /* ========= JPA 콜백 ========= */

    /**
     * INSERT 직전에 호출
     * - createdAt, depth, orgPath 의 기본값 보정
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.depth == null) {
            this.depth = (parent == null) ? 0 : parent.getDepth() + 1;
        }
        if (this.orgPath == null) {
            // ID 할당 전까지는 임시 경로 사용
            this.orgPath = "/temp";
        }
    }

    /**
     * INSERT 이후, ID가 생성된 직후에 호출
     * - 이제 deptId 가 생겼으므로 실제 orgPath 를 다시 계산
     */
    @PostPersist
    public void postPersist() {
        updatePath();
    }
}
