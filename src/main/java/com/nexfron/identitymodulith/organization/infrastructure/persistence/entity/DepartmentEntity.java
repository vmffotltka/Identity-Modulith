package com.nexfron.identitymodulith.organization.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.nexfron.identitymodulith.organization.application.exception.OrganizationException;
import com.nexfron.identitymodulith.organization.application.exception.OrganizationException.OrganizationErrorCode;
import com.nexfron.identitymodulith.organization.domain.OrganizationConstants;
import com.nexfron.identitymodulith.organization.domain.model.DepartmentStatus;
import com.nexfron.identitymodulith.organization.domain.model.DepartmentType;
import java.time.LocalDateTime;

/**
 * Department (부서/조직) 도메인 엔티티
 *
 * 개요:
 * 조직의 계층적 구조를 나타내는 도메인 모델입니다.
 * 각 부서는 자기 참조(self-reference)를 통해 부모 부서를 가질 수 있으며,
 * 이를 통해 조직의 트리 구조를 형성합니다.
 *
 * 조직 구조 예시:
 *
 * 총무부 (depth=0, orgPath=/총무부ID)
 * ├─ 총무팀 (depth=1, orgPath=/총무부ID/총무팀ID)
 * ├─ HR팀 (depth=1, orgPath=/총무부ID/HR팀ID)
 * │  ├─ 채용팀 (depth=2, orgPath=/총무부ID/HR팀ID/채용팀ID)
 * │  └─ 교육팀 (depth=2, orgPath=/총무부ID/HR팀ID/교육팀ID)
 * └─ 경리팀 (depth=1, orgPath=/총무부ID/경리팀ID)
 *
 * 핵심 특징:
 * - 자기참조: parent 필드로 상위 부서 지정
 * - 깊이 추적: depth 필드로 계층 레벨 관리
 * - 경로 관리: orgPath로 전체 경로를 문자열로 저장 (빠른 검색 가능)
 * - 일관성 보장: parent, depth, orgPath가 항상 동기화
 *
 * 불변 규칙:
 * - parent와 depth는 항상 일치해야 함
 * - orgPath는 계층 구조를 정확히 반영해야 함
 * - 자신의 하위 부서로는 이동할 수 없음 (순환 방지)
 *
 * 데이터 베이스 제약:
 * - DB 스키마는 마이그레이션으로 이미 확정됨
 * - 스키마 변경 불가능 (다른 모듈에 영향)
 * - 모든 ID는 UUID (VARCHAR(36))로 통일
 * - parent_id는 NULL 가능 (루트 부서)
 *
 * 생성 흐름:
 * 1. create() 메서드로 엔티티 생성
 * 2. @PrePersist에서 초기값 설정 (depth, orgPath)
 * 3. DB 저장
 * 4. @PostPersist에서 UUID 기반 정확한 orgPath 계산
 *
 * 주의사항:
 * - orgPath는 저장 후(@PostPersist)에 확정됨
 * - parent 변경 시 하위 부서들의 depth/orgPath도 업데이트 필요
 * - 조직 이동은 신중하게 처리 (권한 범위 변경 가능성)
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "org_departments")  // V1_0_20: 표준 명명 규칙 적용 (departmentEntities → org_departments)
public class DepartmentEntity {

    private static final String TEMP_PATH = "/temp";

    /**
     * 부서 ID (Primary Key)
     * - UUID 형식의 고유 식별자
     * - 데이터베이스 전체에서 유일성 보장
     * - 예: "550e8400-e29b-41d4-a716-446655440200"
     * - 멀티테넌시에서는 tenantId와 함께 사용해야 한다
     */
    @Id
    @Column(name = "dept_id", length = 36)
    private String deptId;

    /**
     * 테넌트 ID (Foreign Key)
     * - 멀티테넌시 환경에서 조직/회사를 구분
     * - 같은 부서명이라도 테넌트별로 독립적으로 관리
     * - 길이: 최대 50자
     * - 필수값: NOT NULL
     * - 예: "tenant-001", "company-xyz"
     */
    @Column(nullable = false)
    private String tenantId;

    /**
     * 부모 부서 (Foreign Key to departments table - Self Reference)
     * - 상위 부서를 참조 (자기참조)
     * - NULL인 경우 루트 부서 (최상위 조직)
     * - 조직 이동 시 이 값을 변경
     * - LAZY 로딩으로 성능 최적화
     *
     * 예시:
     * - 루트: 총무부 (parent = null, depth = 0)
     * - 자식: HR팀 (parent = 총무부, depth = 1)
     * - 손자: 채용팀 (parent = HR팀, depth = 2)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DepartmentEntity parent;

    /**
     * 부서명
     * - 사용자가 이해하기 쉬운 부서 이름
     * - 예: "총무부", "HR팀", "채용팀"
     * - 길이: 제한 없음 (VARCHAR)
     * - 필수값: NOT NULL
     * - 중복 허용 (같은 이름의 부서가 다른 상위부서에 있을 수 있음)
     */
    @Column(nullable = false)
    private String name;

    /**
     * 조직 경로 (Unique with tenantId)
     * - 루트부터 현재 부서까지의 전체 경로를 저장
     * - 형식: /부모ID/현재ID (계층 관계를 / 로 구분)
     * - 예: "/총무부ID", "/총무부ID/HR팀ID", "/총무부ID/HR팀ID/채용팀ID"
     * - 부서 생성 시 임시로 "/temp"로 설정, @PostPersist 후 UUID로 업데이트
     * - 빠른 범위 검색이 가능 (LIKE 쿼리로 하위 부서 조회)
     * - 길이: 최대 1000자 (깊이 제한)
     * - 필수값: NOT NULL
     *
     * 활용:
     * - "특정 부서의 모든 하위 부서 조회"
     *   SELECT * FROM departments WHERE org_path LIKE '/총무부ID/%'
     * - "특정 부서의 상위 경로 확인"
     *   SELECT * FROM departments WHERE '/총무부ID/HR팀ID' LIKE CONCAT(org_path, '%')
     */
    @Column(name = "org_path", nullable = false)
    private String orgPath;

    /**
     * 계층 깊이 (Depth)
     * - 루트부터 현재 부서까지의 거리
     * - depth = 0: 루트 부서 (최상위)
     * - depth = 1: 루트의 직속 자식
     * - depth = 2: 루트의 손자
     * - 예시:
     *   - 총무부: depth = 0
     *   - HR팀: depth = 1 (총무부의 자식)
     *   - 채용팀: depth = 2 (HR팀의 자식)
     * - 필수값: NOT NULL
     * - 조직 이동 시 자동으로 재계산됨
     *
     * 활용:
     * - 조직 트리 출력 시 들여쓰기 깊이 결정
     * - "특정 깊이 이상의 부서만 조회"
     * - 조직 계층 쿼리 최적화
     */
    @Column(nullable = false)
    private Integer depth;

    /**
     * 부서 타입 (Optional)
     * - EVENT_STORMING.md 명세에 따른 Enum 타입
     * - COMPANY: 회사 (루트 전용)
     * - DIVISION: 본부/사업부
     * - TEAM: 팀
     * - GROUP: 그룹/파트
     * - CUSTOM: 커스텀 (customTypeName 필수)
     *
     * JPA 매핑:
     * - EnumType.STRING: DB에 "COMPANY", "TEAM" 등 문자열 저장
     * - VARCHAR(20) 컬럼으로 저장
     * - NULL 허용
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DepartmentType type;

    /**
     * 부서 코드 (V1_0_16 추가)
     * - 사용자 친화적 식별자 (UUID 대신 사람이 읽을 수 있는 코드)
     * - 예: "CS-HQ", "DEV-BACKEND", "SALES-001"
     * - 길이: VARCHAR(30)
     * - 필수값: NOT NULL
     * - 테넌트별 고유: UNIQUE (tenant_id, code)
     */
    @Column(length = 30, nullable = false)
    private String code;

    /**
     * 커스텀 타입명 (V1_0_16 추가)
     * - type='CUSTOM'일 때 사용자 정의 타입명
     * - 예: "센터", "파트", "그룹" 등
     * - 길이: VARCHAR(50)
     * - 선택값: type='CUSTOM'일 때만 NOT NULL
     */
    @Column(name = "custom_type_name", length = 50)
    private String customTypeName;

    /**
     * 부서 상태
     * - ACTIVE: 활성 (정상 운영)
     * - INACTIVE: 비활성 (일시 중단)
     * - 기본값: ACTIVE
     * - 필수값: NOT NULL
     *
     * 비활성화 시 제약:
     * - 신규 직원 배치 불가
     * - 조직도에서 비활성으로 표시
     * - 재활성화 가능
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;

    /**
     * 비활성화 일시 (V1_0_16 추가)
     * - 부서가 비활성화된 정확한 시간
     * - 상태 변경 이력 추적용
     */
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    /**
     * 생성 일시
     * - 부서가 생성된 정확한 시간
     * - 데이터베이스에서 자동 설정 (현재 시간)
     * - 수정 불가능 (updatable = false)
     * - 감시 추적(Audit Trail)용으로 사용
     * - 필수값: NOT NULL
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 최종 수정 일시 (V1_0_16 추가)
     * - 부서 정보가 마지막으로 변경된 시간
     * - 트리거로 자동 업데이트
     * - 필수값: NOT NULL
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 생성자 ID (V1_0_16 추가)
     * - 부서를 생성한 사용자 ID
     * - 감사 추적용
     */
    @Column(name = "created_by", length = 36)
    private String createdBy;

    /**
     * 최종 수정자 ID (V1_0_16 추가)
     * - 부서를 마지막으로 수정한 사용자 ID
     * - 감사 추적용
     */
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    /**
     * 버전 (Optimistic Locking)
     * - 동시성 제어를 위한 낙관적 락
     * - 동시에 여러 트랜잭션이 같은 부서를 수정하려 할 때 충돌 방지
     * - 업데이트할 때마다 자동으로 증가
     * - 충돌 발생 시 OptimisticLockException 발생
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 정적 팩토리 메서드: 새 부서 생성
     *
     * 역할:
     * - Department 엔티티를 생성하는 유일한 방법
     * - 불변성 보장: 생성자를 private으로 막고 이 메서드만 노출
     * - 생성 시점에 parent, depth, orgPath를 자동으로 계산
     *
     * 불변 규칙:
     * - parent와 depth는 항상 일치해야 함
     * - orgPath는 계층 구조를 정확히 반영해야 함
     *
     * 생성 예시:
     * 1. 루트 부서 생성:
     *    Department 총무부 = Department.create(
     *        "tenant-001", "총무부", DepartmentType.DIVISION, null
     *    )
     *    → deptId: "550e...", depth: 0, orgPath: "/temp"
     *    → 저장 후 postPersist에서 orgPath: "/{deptId}"로 업데이트
     *
     * 2. 자식 부서 생성:
     *    Department HR팀 = Department.create(
     *        "tenant-001", "HR팀", DepartmentType.TEAM, 총무부
     *    )
     *    → deptId: "550e...", depth: 1, orgPath: "/{총무부ID}/{HR팀ID}"
     *
     * 호출 시점:
     * - 부서 생성 비즈니스 로직에서 호출
     * - 스프링 빈(new)으로 생성하지 말 것 (이 메서드 사용)
     *
     * @param tenantId 테넌트 ID
     * @param name 부서명
     * @param type 부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM)
     * @param parent 부모 부서 (NULL 가능 - 루트 부서)
     * @return 새로 생성된 Department 엔티티
     *
     * @throws InvalidDepartmentMoveException 부모로 설정할 부서가 자신의 하위 부서인 경우
     */
    public static DepartmentEntity create(String tenantId, String name, DepartmentType type, DepartmentEntity parent) {
        DepartmentEntity dept = new DepartmentEntity();
        dept.deptId = java.util.UUID.randomUUID().toString();  // UUID 생성
        dept.tenantId = tenantId;
        dept.name = name;
        dept.type = type;
        dept.changeParent(parent);
        return dept;
    }

    /**
     * 부모 부서 변경 (조직 이동)
     *
     * 역할:
     * - 부서를 다른 부서 아래로 이동
     * - parent 변경 시 depth와 orgPath 자동으로 재계산
     * - 조직 구조의 일관성을 유지
     *
     * 변경 예시:
     * 변경 전: HR팀 (부모: 총무부, depth: 1, path: /총무부ID/HR팀ID)
     * 변경 후: HR팀 (부모: 경영진팀, depth: 1, path: /경영진팀ID/HR팀ID)
     *
     * 규칙:
     * - 자신의 하위 부서로는 이동할 수 없음 (순환 참조 방지)
     * - NULL을 전달하면 루트 부서로 설정 (depth = 0)
     * - parent 변경 후 depth와 orgPath가 자동으로 재계산됨
     *
     * 제약사항:
     * - 자신의 하위 부서로 이동 시 InvalidDepartmentMoveException 발생
     * - 예: 채용팀의 부모를 채용팀의 자식으로 설정 불가
     * - 순환 참조를 방지하여 무한 루프 방지
     *
     * 부작용 (Side Effects):
     * - depth 값 변경
     * - orgPath 값 변경
     * - 하위 부서들의 depth/orgPath도 업데이트 필요 (별도 처리)
     *
     * @param newParent 새로운 부모 부서 (NULL 가능 - 루트로 설정)
     * @throws OrganizationException 자신의 하위 부서로 이동하려 할 때
     */
    public void changeParent(DepartmentEntity newParent) {
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

        // 조직 깊이 제한 검증
        if (this.depth > OrganizationConstants.MAX_ORGANIZATION_DEPTH) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "조직 깊이가 최대 허용치(" + OrganizationConstants.MAX_ORGANIZATION_DEPTH +
                    ")를 초과합니다. 현재 깊이: " + this.depth
            );
        }

        updatePath();
    }

    /**
     * 조직 경로 직접 설정 (서비스 계층에서만 사용)
     *
     * <b>주의: 이 메서드는 서비스 계층에서 하위 부서의 경로를 일괄 업데이트할 때만 사용됩니다.</b>
     *
     * @param orgPath 설정할 새로운 조직 경로
     */
    public void setOrgPath(String orgPath) {
        this.orgPath = orgPath;
    }

    /**
     * 계층 깊이 직접 설정 (서비스 계층에서만 사용)
     *
     * <b>주의: 이 메서드는 서비스 계층에서 하위 부서의 깊이를 일괄 업데이트할 때만 사용됩니다.</b>
     *
     * @param depth 설정할 새로운 깊이
     */
    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    /**
     * 조직 경로 재계산
     *
     * 역할:
     * - 현재 부서의 orgPath를 부모 부서와 자신의 ID 기반으로 계산
     * - parent, depth 변경 후 항상 호출되어야 함
     * - 조직 구조의 일관성을 유지하는 핵심 메서드
     *
     * 경로 계산 로직:
     * 1. parent가 없으면 (루트 부서):
     *    - deptId가 null이면: "/temp" (임시값, 저장 전)
     *    - deptId가 있으면: "/{deptId}" (루트 부서 확정)
     * 2. parent가 있으면:
     *    - "{parent의 orgPath}/{자신의 deptId}"
     *
     * 계산 예시:
     * - 부모: /총무부ID, 자신: HR팀ID → /총무부ID/HR팀ID
     * - 부모: /총무부ID/HR팀ID, 자신: 채용팀ID → /총무부ID/HR팀ID/채용팀ID
     *
     * 호출 시점:
     * - parent 변경 후 (changeParent에서 호출)
     * - 저장 후 (postPersist에서 호출해서 UUID 확정)
     * - 조직 이동 시
     *
     * 전제 조건:
     * - parent가 정상적으로 설정되어 있어야 함
     * - 신규 엔티티의 경우 deptId는 null일 수 있음
     *
     * 주의사항:
     * - 하위 부서들의 orgPath를 함께 업데이트하지 않음
     * - 하위 부서들의 orgPath 업데이트는 별도의 서비스 로직에서 처리
     * - 직접 호출하지 말 것 (changeParent에서 자동으로 호출됨)
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

        // 조직 경로 길이 검증
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
        /**
         * DB 저장 직전 실행되는 콜백
         *
         * 역할:
         * - 저장하기 전에 필수값 초기화
         * - createdAt 설정
         * - depth 초기값 설정
         * - orgPath 임시값 설정
         */
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
        /**
         * DB 저장 직후 실행되는 콜백
         *
         * 역할:
         * - UUID 기반 정확한 orgPath 계산
         * - prePersist에서 설정한 임시값 "/temp"를 확정된 경로로 업데이트
         * - deptId가 생성된 후 호출되므로 정확한 계산 가능
         *
         * 동작:
         * - updatePath() 호출로 orgPath를 UUID 기반으로 재계산
         * - 예: "/temp" → "/{deptId}" (루트의 경우)
         * - 예: "/parent/temp" → "/parent/{deptId}" (자식의 경우)
         */
        updatePath();
    }

    /**
     * 부서 비활성화
     *
     * 역할:
     * - 부서 상태를 INACTIVE로 변경
     * - 이미 비활성화된 경우 예외 발생
     *
     * 제약사항:
     * - 활성 하위 부서가 있으면 비활성화 불가
     * - 소속 활성 직원이 있으면 비활성화 불가 (권장)
     *
     * @throws OrganizationException 이미 비활성 상태인 경우
     */
    public void deactivate() {
        if (this.status == DepartmentStatus.INACTIVE) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "이미 비활성화된 부서입니다."
            );
        }
        this.status = DepartmentStatus.INACTIVE;
    }

    /**
     * 부서 활성화
     *
     * 역할:
     * - 부서 상태를 ACTIVE로 변경
     * - 이미 활성화된 경우 예외 발생
     *
     * 제약사항:
     * - 상위 부서가 비활성 상태면 활성화 불가
     *
     * @throws OrganizationException 이미 활성 상태인 경우
     */
    public void activate() {
        if (this.status == DepartmentStatus.ACTIVE) {
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "이미 활성화된 부서입니다."
            );
        }
        this.status = DepartmentStatus.ACTIVE;
    }

    /**
     * 활성 상태 확인
     *
     * @return ACTIVE 상태이면 true
     */
    public boolean isActive() {
        return this.status == DepartmentStatus.ACTIVE;
    }

    /**
     * 루트 부서 여부 확인
     *
     * 역할:
     * - 부모 부서가 없으면 루트 부서로 판단
     *
     * 사용처:
     * - 루트 부서 이동 방지 (moveDepartment)
     * - 루트 부서 비활성화 방지 (deactivateDepartment)
     * - 루트 부서 삭제 방지 (deleteDepartment)
     * - 루트 부서 중복 생성 방지 (createDepartment)
     *
     * @return 부모 부서가 없으면 true (루트 부서)
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * 부서 정보 업데이트
     *
     * 역할:
     * - 부서명, 타입 등 기본 정보를 변경
     * - 조직 구조(parent, depth, orgPath)는 변경하지 않음
     * - 조직 구조 변경은 changeParent() 메서드를 사용
     *
     * 타입 변경 규칙:
     * - CUSTOM 타입으로 변경 시 customTypeName 필요 (경고)
     * - CUSTOM에서 다른 타입으로 변경 가능 (customTypeName 유지)
     *
     * @param name 변경할 부서명 (null이면 변경하지 않음)
     * @param type 변경할 부서 타입 (null이면 변경하지 않음)
     */
    public void updateInfo(String name, DepartmentType type) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (type != null) {
            // CUSTOM 타입으로 변경 시 customTypeName 검증 (향후 강화 가능)
            if (type == DepartmentType.CUSTOM &&
                (this.customTypeName == null || this.customTypeName.trim().isEmpty())) {
                // 현재는 경고만 (SERVICE 레이어에서 처리)
                // throw new OrganizationException(
                //         OrganizationErrorCode.CUSTOM_TYPE_NAME_REQUIRED);
            }
            this.type = type;
        }
    }
}
