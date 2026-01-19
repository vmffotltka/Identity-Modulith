package com.nexfron.identitymodulith.organization.domain;

/**
 * 조직 모듈 관련 상수 정의
 * <p>
 * 이 클래스는 조직(Organization) 모듈에서 사용되는 모든 상수 값을 중앙 집중식으로 관리합니다.
 * 매직 넘버(Magic Number)를 제거하고 코드의 가독성과 유지보수성을 향상시킵니다.
 * </p>
 *
 * <h3>주요 상수 그룹</h3>
 * <ul>
 *   <li>부서명 관련: 최소/최대 길이</li>
 *   <li>부서 타입 관련: 최대 길이</li>
 *   <li>조직 구조 관련: 최대 깊이, 경로 길이</li>
 * </ul>
 *
 * @since 2026-01-19 (P2 개선)
 * @see com.nexfron.identitymodulith.organization.domain.model.Department
 */
public final class OrganizationConstants {

    // ============================================================
    // 부서명 (Department Name) 관련 상수
    // ============================================================

    /**
     * 부서명 최소 길이
     * <p>예: "IT", "HR" - 최소 2자</p>
     */
    public static final int DEPARTMENT_NAME_MIN_LENGTH = 2;

    /**
     * 부서명 최대 길이
     * <p>예: "고객 서비스 센터 A팀" - 최대 100자</p>
     */
    public static final int DEPARTMENT_NAME_MAX_LENGTH = 100;

    // ============================================================
    // 부서 타입 (Department Type) 관련 상수
    // ============================================================

    /**
     * 부서 타입 최대 길이
     * <p>예: "HEADQUARTERS", "BRANCH", "TEAM" - 최대 50자</p>
     */
    public static final int DEPARTMENT_TYPE_MAX_LENGTH = 50;

    // ============================================================
    // 조직 구조 (Organization Structure) 관련 상수
    // ============================================================

    /**
     * 조직 트리 최대 깊이
     * <p>
     * 예: 본사(0) > 사업부(1) > 팀(2) > 파트(3) > 그룹(4) > 팀원(5)
     * <br>
     * 최대 5단계까지 허용
     * </p>
     */
    public static final int MAX_ORGANIZATION_DEPTH = 5;

    /**
     * 조직 경로(orgPath) 최대 길이
     * <p>
     * 예: "/dept-123/dept-456/dept-789/dept-012/dept-345"
     * <br>
     * UUID 기반 경로가 길어질 수 있으므로 500자로 설정
     * </p>
     */
    public static final int MAX_ORG_PATH_LENGTH = 500;

    /**
     * 부서 ID (UUID) 길이
     * <p>
     * UUID v4 형식: "550e8400-e29b-41d4-a716-446655440000" (36자)
     * </p>
     */
    public static final int DEPARTMENT_ID_LENGTH = 36;

    // ============================================================
    // Private Constructor (유틸리티 클래스 패턴)
    // ============================================================

    /**
     * Private 생성자 - 인스턴스 생성 방지
     * <p>
     * 이 클래스는 상수만 제공하므로 인스턴스화할 필요가 없습니다.
     * </p>
     */
    private OrganizationConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다");
    }
}

