package com.nexfron.identitymodulith.rbac.domain;

/**
 * RBAC 모듈 관련 상수 정의
 * <p>
 * 이 클래스는 RBAC(Role-Based Access Control) 모듈에서 사용되는 모든 상수 값을 중앙 집중식으로 관리합니다.
 * 매직 넘버(Magic Number)를 제거하고 코드의 가독성과 유지보수성을 향상시킵니다.
 * </p>
 *
 * <h3>주요 상수 그룹</h3>
 * <ul>
 *   <li>역할(Role) 관련: 이름/설명 길이</li>
 *   <li>권한(Permission) 관련: 코드/설명 길이</li>
 *   <li>권한 그룹(PermissionGroup) 관련: 이름/설명 길이</li>
 * </ul>
 *
 * @since 2026-01-19 (P2 개선)
 * @see com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity
 * @see com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity
 */
public final class RbacConstants {

    // ============================================================
    // 역할 (Role) 관련 상수
    // ============================================================

    /**
     * 역할명 최소 길이
     * <p>예: "ADMIN", "AGENT" - 최소 2자</p>
     */
    public static final int ROLE_NAME_MIN_LENGTH = 2;

    /**
     * 역할명 최대 길이
     * <p>
     * 예: "TEAM_LEADER", "SENIOR_AGENT"
     * <br>
     * DB 컬럼: VARCHAR(64)
     * </p>
     */
    public static final int ROLE_NAME_MAX_LENGTH = 64;

    /**
     * 역할 설명 최대 길이
     * <p>DB 컬럼: VARCHAR(255)</p>
     */
    public static final int ROLE_DESCRIPTION_MAX_LENGTH = 255;

    // ============================================================
    // 권한 (Permission) 관련 상수
    // ============================================================

    /**
     * 권한 코드 최대 길이
     * <p>
     * 예: "user:create", "department:read", "role:manage"
     * <br>
     * 형식: "domain:action"
     * <br>
     * DB 컬럼: VARCHAR(128)
     * </p>
     */
    public static final int PERMISSION_CODE_MAX_LENGTH = 128;

    /**
     * 권한 설명 최대 길이
     * <p>DB 컬럼: VARCHAR(500)</p>
     */
    public static final int PERMISSION_DESCRIPTION_MAX_LENGTH = 500;

    // ============================================================
    // 권한 그룹 (Permission Group) 관련 상수
    // ============================================================

    /**
     * 권한 그룹명 최소 길이
     * <p>예: "기본", "관리자" - 최소 2자</p>
     */
    public static final int PERMISSION_GROUP_NAME_MIN_LENGTH = 2;

    /**
     * 권한 그룹명 최대 길이
     * <p>
     * 예: "사용자 관리 그룹", "부서 관리 그룹"
     * <br>
     * DB 컬럼: VARCHAR(64)
     * </p>
     */
    public static final int PERMISSION_GROUP_NAME_MAX_LENGTH = 64;

    /**
     * 권한 그룹 설명 최대 길이
     * <p>DB 컬럼: VARCHAR(255)</p>
     */
    public static final int PERMISSION_GROUP_DESCRIPTION_MAX_LENGTH = 255;

    // ============================================================
    // 공통 상수
    // ============================================================

    /**
     * 테넌트 ID 최대 길이
     * <p>멀티테넌시 환경에서 사용되는 테넌트 식별자</p>
     */
    public static final int TENANT_ID_MAX_LENGTH = 50;

    /**
     * 권한 코드 구분자
     * <p>
     * 형식: "domain:action"
     * <br>
     * 예: "user:create", "department:read"
     * </p>
     */
    public static final String PERMISSION_CODE_DELIMITER = ":";

    // ============================================================
    // Private Constructor (유틸리티 클래스 패턴)
    // ============================================================

    /**
     * Private 생성자 - 인스턴스 생성 방지
     * <p>
     * 이 클래스는 상수만 제공하므로 인스턴스화할 필요가 없습니다.
     * </p>
     */
    private RbacConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다");
    }
}

